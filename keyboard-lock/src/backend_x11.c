/**
 * backend_x11.c - X11 Server Keyboard Grab Backend Implementation
 *
 * Uses dynamic dlopen("libX11.so.6") so that keyboard-lock compiles without
 * static dependencies on X11 development packages while fully supporting
 * pure X11 display servers.
 */

#include "backend_x11.h"
#include "logger.h"
#include "hotkey.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/epoll.h>

#define True 1
#define False 0

/* Opaque X11 types and constants */
typedef void Display;
typedef unsigned long Window;
typedef unsigned long Time;
typedef unsigned long KeySym;
typedef unsigned char KeyCode;

#define CurrentTime 0L
#define GrabSuccess 0
#define GrabModeAsync 1
#define KeyPress 2
#define KeyRelease 3
#define KeyPressMask (1L<<0)
#define KeyReleaseMask (1L<<1)
#define AnyModifier (1<<15)
#define ControlMask (1<<2)
#define Mod1Mask (1<<3) /* Alt */

typedef struct {
    int type;
    unsigned long serial;
    int send_event;
    Display *display;
    Window window;
    Window root;
    Window subwindow;
    Time time;
    int x, y;
    int x_root, y_root;
    unsigned int state;
    unsigned int keycode;
    int same_screen;
} XKeyEvent_t;

typedef union {
    int type;
    XKeyEvent_t xkey;
    long pad[24];
} XEvent_t;

/* Dynamic function pointers for Xlib */
static void *g_x11_handle = NULL;
static Display* (*fn_XOpenDisplay)(const char*) = NULL;
static int (*fn_XCloseDisplay)(Display*) = NULL;
static Window (*fn_XDefaultRootWindow)(Display*) = NULL;
static int (*fn_XConnectionNumber)(Display*) = NULL;
static int (*fn_XGrabKeyboard)(Display*, Window, int, int, int, Time) = NULL;
static int (*fn_XUngrabKeyboard)(Display*, Time) = NULL;
static int (*fn_XGrabKey)(Display*, int, unsigned int, Window, int, int, int) = NULL;
static int (*fn_XUngrabKey)(Display*, int, unsigned int, Window) = NULL;
static KeyCode (*fn_XKeysymToKeycode)(Display*, KeySym) = NULL;
static KeySym (*fn_XStringToKeysym)(const char*) = NULL;
static int (*fn_XNextEvent)(Display*, XEvent_t*) = NULL;
static int (*fn_XPending)(Display*) = NULL;
static int (*fn_XFlush)(Display*) = NULL;

static bool load_x11_symbols(void) {
    if (g_x11_handle) return true;

    g_x11_handle = dlopen("libX11.so.6", RTLD_LAZY);
    if (!g_x11_handle) {
        g_x11_handle = dlopen("libX11.so", RTLD_LAZY);
    }
    if (!g_x11_handle) {
        log_debug("libX11.so could not be loaded: %s", dlerror());
        return false;
    }

    fn_XOpenDisplay = dlsym(g_x11_handle, "XOpenDisplay");
    fn_XCloseDisplay = dlsym(g_x11_handle, "XCloseDisplay");
    fn_XDefaultRootWindow = dlsym(g_x11_handle, "XDefaultRootWindow");
    fn_XConnectionNumber = dlsym(g_x11_handle, "XConnectionNumber");
    fn_XGrabKeyboard = dlsym(g_x11_handle, "XGrabKeyboard");
    fn_XUngrabKeyboard = dlsym(g_x11_handle, "XUngrabKeyboard");
    fn_XGrabKey = dlsym(g_x11_handle, "XGrabKey");
    fn_XUngrabKey = dlsym(g_x11_handle, "XUngrabKey");
    fn_XKeysymToKeycode = dlsym(g_x11_handle, "XKeysymToKeycode");
    fn_XStringToKeysym = dlsym(g_x11_handle, "XStringToKeysym");
    fn_XNextEvent = dlsym(g_x11_handle, "XNextEvent");
    fn_XPending = dlsym(g_x11_handle, "XPending");
    fn_XFlush = dlsym(g_x11_handle, "XFlush");

    if (!fn_XOpenDisplay || !fn_XGrabKeyboard || !fn_XUngrabKeyboard) {
        log_warn("Failed to load required X11 symbols from libX11.so");
        dlclose(g_x11_handle);
        g_x11_handle = NULL;
        return false;
    }

    return true;
}

bool backend_x11_is_available(void) {
    const char *disp = getenv("DISPLAY");
    if (!disp || !*disp) return false;
    return load_x11_symbols();
}

typedef struct {
    Display *display;
    Window root_window;
    int x11_fd;
    int epoll_fd;
    state_machine_t sm;
    int target_keycode;
    KeyCode target_x11_keycode;
    bool is_locked;
} backend_x11_priv_t;

static int x11_init(backend_t *b, int target_keycode, int epoll_fd) {
    if (!b) return -1;
    backend_x11_priv_t *priv = (backend_x11_priv_t*)b->priv;
    if (!priv) return -1;

    if (!load_x11_symbols()) {
        log_error("Cannot initialize X11 backend: libX11.so not available");
        return -1;
    }

    priv->display = fn_XOpenDisplay(NULL);
    if (!priv->display) {
        log_error("Failed to connect to X11 display '%s'", getenv("DISPLAY") ? getenv("DISPLAY") : ":0");
        return -1;
    }

    priv->root_window = fn_XDefaultRootWindow(priv->display);
    priv->x11_fd = fn_XConnectionNumber(priv->display);
    priv->epoll_fd = epoll_fd;
    priv->target_keycode = target_keycode;
    priv->is_locked = false;

    state_machine_init(&priv->sm, target_keycode);
    b->sm = &priv->sm;

    /* Map target key name to X11 keycode */
    const char *key_name = hotkey_keycode_to_name(target_keycode);
    KeySym sym = fn_XStringToKeysym(key_name);
    if (!sym) {
        char upper[32];
        strncpy(upper, key_name, sizeof(upper) - 1);
        if (upper[0] >= 'a' && upper[0] <= 'z') upper[0] -= 32;
        sym = fn_XStringToKeysym(upper);
    }
    priv->target_x11_keycode = sym ? fn_XKeysymToKeycode(priv->display, sym) : 0;

    /* Register global hotkey when unlocked: Ctrl + Alt + Key */
    if (priv->target_x11_keycode) {
        fn_XGrabKey(priv->display, priv->target_x11_keycode,
                    ControlMask | Mod1Mask, priv->root_window,
                    True, GrabModeAsync, GrabModeAsync);
        fn_XFlush(priv->display);
    }

    /* Add X11 connection fd to epoll */
    if (epoll_fd >= 0) {
        struct epoll_event ev;
        memset(&ev, 0, sizeof(ev));
        ev.events = EPOLLIN;
        ev.data.fd = priv->x11_fd;
        epoll_ctl(epoll_fd, EPOLL_CTL_ADD, priv->x11_fd, &ev);
    }

    log_info("X11 backend initialized on display '%s' (X11 fd %d)",
             getenv("DISPLAY"), priv->x11_fd);
    return 0;
}

static int x11_lock(backend_t *b) {
    if (!b) return -1;
    backend_x11_priv_t *priv = (backend_x11_priv_t*)b->priv;
    if (!priv || !priv->display) return -1;

    /* Grab entire keyboard across the X11 session.
     * Note: Mouse pointer is NOT grabbed, allowing complete mouse freedom! */
    int res = fn_XGrabKeyboard(priv->display, priv->root_window,
                               True, GrabModeAsync, GrabModeAsync, CurrentTime);
    if (res != GrabSuccess) {
        log_error("XGrabKeyboard failed with code %d", res);
        return -1;
    }

    priv->is_locked = true;
    state_machine_set_state(&priv->sm, STATE_LOCKED);
    fn_XFlush(priv->display);

    log_info("X11 keyboard lock ENGAGED (session keyboard grabbed, mouse unaffected)");
    return 0;
}

static int x11_unlock(backend_t *b) {
    if (!b) return -1;
    backend_x11_priv_t *priv = (backend_x11_priv_t*)b->priv;
    if (!priv || !priv->display) return -1;

    fn_XUngrabKeyboard(priv->display, CurrentTime);
    priv->is_locked = false;
    state_machine_set_state(&priv->sm, STATE_UNLOCKED);
    fn_XFlush(priv->display);

    log_info("X11 keyboard lock RELEASED (session keyboard restored)");
    return 0;
}

static int x11_get_device_count(const backend_t *b) {
    (void)b;
    return 1; /* Session-level X11 keyboard */
}

static int x11_handle_fd_event(backend_t *b, int fd, uint32_t events, bool *should_stop) {
    (void)events;
    (void)should_stop;
    if (!b) return -1;
    backend_x11_priv_t *priv = (backend_x11_priv_t*)b->priv;
    if (!priv || !priv->display || fd != priv->x11_fd) return 0;

    while (fn_XPending(priv->display) > 0) {
        XEvent_t event;
        fn_XNextEvent(priv->display, &event);

        if (event.type == KeyPress || event.type == KeyRelease) {
            bool is_press = (event.type == KeyPress);
            unsigned int state = event.xkey.state;
            bool has_ctrl = (state & ControlMask) != 0;
            bool has_alt = (state & Mod1Mask) != 0;

            if (event.xkey.keycode == priv->target_x11_keycode && is_press && has_ctrl && has_alt) {
                if (priv->is_locked) {
                    log_info("X11: Hotkey detected while locked. Unlocking.");
                    x11_unlock(b);
                } else {
                    log_info("X11: Hotkey detected while unlocked. Locking.");
                    x11_lock(b);
                }
            } else if (priv->is_locked) {
                /* Consume and discard every keyboard event */
                log_debug("X11: Intercepted & discarded key event while locked");
            }
        }
    }

    return 0;
}

static void x11_cleanup(backend_t *b) {
    if (!b) return;
    backend_x11_priv_t *priv = (backend_x11_priv_t*)b->priv;
    if (priv) {
        if (priv->display) {
            if (priv->is_locked) {
                fn_XUngrabKeyboard(priv->display, CurrentTime);
            }
            if (priv->target_x11_keycode) {
                fn_XUngrabKey(priv->display, priv->target_x11_keycode,
                              ControlMask | Mod1Mask, priv->root_window);
            }
            fn_XFlush(priv->display);
            if (priv->epoll_fd >= 0) {
                epoll_ctl(priv->epoll_fd, EPOLL_CTL_DEL, priv->x11_fd, NULL);
            }
            fn_XCloseDisplay(priv->display);
            priv->display = NULL;
        }
        free(priv);
        b->priv = NULL;
    }
    free(b);
}

backend_t* backend_x11_create(void) {
    backend_t *b = (backend_t*)calloc(1, sizeof(backend_t));
    if (!b) return NULL;

    backend_x11_priv_t *priv = (backend_x11_priv_t*)calloc(1, sizeof(backend_x11_priv_t));
    if (!priv) {
        free(b);
        return NULL;
    }

    b->name = "x11";
    b->description = "X11 Server XGrabKeyboard (Pure X11 sessions)";
    b->init = x11_init;
    b->handle_fd_event = x11_handle_fd_event;
    b->lock = x11_lock;
    b->unlock = x11_unlock;
    b->get_device_count = x11_get_device_count;
    b->cleanup = x11_cleanup;
    b->priv = priv;

    return b;
}
