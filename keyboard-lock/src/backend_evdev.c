/**
 * backend_evdev.c - Kernel evdev Input Interception Backend Implementation
 */

#include "backend_evdev.h"
#include "logger.h"
#include "hotkey.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/epoll.h>

static int evdev_init(backend_t *b, int target_keycode, int epoll_fd) {
    if (!b) return -1;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;
    if (!priv) return -1;

    priv->epoll_fd = epoll_fd;
    priv->target_keycode = target_keycode;
    state_machine_init(&priv->sm, target_keycode);
    b->sm = &priv->sm;

    device_manager_init(&priv->dm, epoll_fd);
    int scanned = device_manager_scan(&priv->dm);

    log_info("evdev backend initialized with hotkey [Ctrl+Alt+%s], %d keyboard(s) active",
             hotkey_keycode_to_name(target_keycode), scanned);
    return 0;
}

static int evdev_lock(backend_t *b) {
    if (!b) return -1;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;

    state_machine_set_state(&priv->sm, STATE_LOCKED);
    int grabbed = device_manager_grab_all(&priv->dm);
    return (grabbed >= 0) ? 0 : -1;
}

static int evdev_unlock(backend_t *b) {
    if (!b) return -1;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;

    state_machine_set_state(&priv->sm, STATE_UNLOCKED);
    int ungrabbed = device_manager_ungrab_all(&priv->dm);
    return (ungrabbed >= 0) ? 0 : -1;
}

static int evdev_get_device_count(const backend_t *b) {
    if (!b) return 0;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;
    return device_manager_get_count(&priv->dm);
}

static int evdev_handle_fd_event(backend_t *b, int fd, uint32_t events, bool *should_stop) {
    (void)events;
    (void)should_stop;
    if (!b) return -1;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;

    /* Check if inotify event */
    if (fd == priv->dm.inotify_fd) {
        device_manager_handle_inotify(&priv->dm, priv->sm.state);
        return 0;
    }

    /* Read raw input events from keyboard device */
    struct input_event ev_batch[32];
    ssize_t bytes_read = read(fd, ev_batch, sizeof(ev_batch));

    if (bytes_read < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return 0;
        }
        if (errno == ENODEV) {
            /* Device disconnected */
            device_manager_remove_device_by_fd(&priv->dm, fd);
            return 0;
        }
        log_warn("Error reading input event from fd %d: %s", fd, strerror(errno));
        return -1;
    }

    if (bytes_read == 0) {
        /* EOF / Device disconnected */
        device_manager_remove_device_by_fd(&priv->dm, fd);
        return 0;
    }

    size_t count = (size_t)bytes_read / sizeof(struct input_event);
    for (size_t i = 0; i < count; i++) {
        if (ev_batch[i].type == EV_KEY) {
            state_action_t action = state_machine_process_key(&priv->sm,
                                                             ev_batch[i].code,
                                                             ev_batch[i].value);

            if (action == ACTION_LOCKED) {
                log_info("HOTKEY DETECTED: Engaging keyboard lock.");
                device_manager_grab_all(&priv->dm);
            } else if (action == ACTION_UNLOCKED) {
                log_info("HOTKEY DETECTED: Releasing keyboard lock.");
                device_manager_ungrab_all(&priv->dm);
            }
        }
    }

    return 0;
}

static void evdev_cleanup(backend_t *b) {
    if (!b) return;
    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)b->priv;
    if (priv) {
        device_manager_cleanup(&priv->dm);
        free(priv);
        b->priv = NULL;
    }
    free(b);
}

backend_t* backend_evdev_create(void) {
    backend_t *b = (backend_t*)calloc(1, sizeof(backend_t));
    if (!b) return NULL;

    backend_evdev_priv_t *priv = (backend_evdev_priv_t*)calloc(1, sizeof(backend_evdev_priv_t));
    if (!priv) {
        free(b);
        return NULL;
    }

    b->name = "evdev";
    b->description = "Linux Kernel evdev EVIOCGRAB (Wayland & X11 compatible)";
    b->init = evdev_init;
    b->handle_fd_event = evdev_handle_fd_event;
    b->lock = evdev_lock;
    b->unlock = evdev_unlock;
    b->get_device_count = evdev_get_device_count;
    b->cleanup = evdev_cleanup;
    b->priv = priv;

    return b;
}
