/**
 * daemon.c - Main Daemon Event Loop & Signal Multiplexer Implementation
 */

#include "daemon.h"
#include "backend_evdev.h"
#include "backend_x11.h"
#include "ipc.h"
#include "logger.h"
#include "hotkey.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <sys/epoll.h>
#include <sys/signalfd.h>
#include <time.h>

#define MAX_EPOLL_EVENTS 32

/* Global backend pointer for emergency crash handler */
static backend_t *g_emergency_backend = NULL;

static void emergency_crash_handler(int sig) {
    if (g_emergency_backend) {
        /* Release all grabs immediately before aborting */
        g_emergency_backend->unlock(g_emergency_backend);
    }
    const char *sig_name = (sig == SIGSEGV) ? "SIGSEGV" : "SIGABRT";
    const char msg[] = "\n[EMERGENCY] Fatal signal caught. All keyboard grabs forcibly released.\n";
    ssize_t w = write(STDERR_FILENO, msg, sizeof(msg) - 1);
    (void)w;
    (void)sig_name;
    _exit(128 + sig);
}

static void install_crash_handlers(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = emergency_crash_handler;
    sa.sa_flags = SA_RESETHAND;
    sigaction(SIGSEGV, &sa, NULL);
    sigaction(SIGABRT, &sa, NULL);
}

static backend_t* select_backend(const char *requested_type) {
    if (!requested_type || strcmp(requested_type, "auto") == 0) {
        /* Default to evdev: authoritative across Wayland and X11 */
        return backend_evdev_create();
    }

    if (strcmp(requested_type, "x11") == 0) {
        if (!backend_x11_is_available()) {
            log_error("X11 backend requested but X11 display/library is not available.");
            return NULL;
        }
        return backend_x11_create();
    }

    if (strcmp(requested_type, "evdev") == 0) {
        return backend_evdev_create();
    }

    log_error("Unknown backend type '%s'", requested_type);
    return NULL;
}

int daemon_run(const daemon_config_t *config) {
    if (!config) return -1;

    /* Initialize logger */
    log_level_t min_lvl = config->verbose ? LOG_LEVEL_DEBUG :
                          config->quiet ? LOG_LEVEL_WARN : LOG_LEVEL_INFO;
    logger_init(min_lvl, config->use_syslog, config->quiet);

    log_info("keyboard-lock starting (PID %d)...", (int)getpid());

    /* Create epoll instance */
    int epoll_fd = epoll_create1(EPOLL_CLOEXEC);
    if (epoll_fd < 0) {
        log_error("Failed to create epoll instance: %s", strerror(errno));
        return -1;
    }

    /* Initialize single-instance IPC server */
    ipc_server_t ipc;
    if (ipc_server_init(&ipc, epoll_fd) < 0) {
        log_error("Cannot start daemon: Another instance is already running or socket error.");
        close(epoll_fd);
        return -1;
    }

    /* Block signals and set up signalfd for synchronous event handling */
    sigset_t mask;
    sigemptyset(&mask);
    sigaddset(&mask, SIGINT);
    sigaddset(&mask, SIGTERM);
    sigaddset(&mask, SIGHUP);
    sigaddset(&mask, SIGQUIT);

    if (sigprocmask(SIG_BLOCK, &mask, NULL) < 0) {
        log_warn("sigprocmask failed: %s", strerror(errno));
    }

    int sig_fd = signalfd(-1, &mask, SFD_NONBLOCK | SFD_CLOEXEC);
    if (sig_fd >= 0) {
        struct epoll_event sig_ev;
        memset(&sig_ev, 0, sizeof(sig_ev));
        sig_ev.events = EPOLLIN;
        sig_ev.data.fd = sig_fd;
        epoll_ctl(epoll_fd, EPOLL_CTL_ADD, sig_fd, &sig_ev);
    } else {
        log_warn("signalfd failed: %s", strerror(errno));
    }

    /* Select and instantiate backend */
    backend_t *backend = select_backend(config->backend_type);
    if (!backend) {
        log_error("Failed to initialize input backend.");
        ipc_server_cleanup(&ipc);
        if (sig_fd >= 0) close(sig_fd);
        close(epoll_fd);
        return -1;
    }

    g_emergency_backend = backend;
    install_crash_handlers();

    int target_kc = config->target_keycode > 0 ? config->target_keycode : KEY_K;
    if (backend->init(backend, target_kc, epoll_fd) < 0) {
        log_error("Backend initialization failed.");
        backend->cleanup(backend);
        ipc_server_cleanup(&ipc);
        if (sig_fd >= 0) close(sig_fd);
        close(epoll_fd);
        return -1;
    }

    /* Daemonize if requested */
    if (config->daemonize) {
        log_info("Detaching into background...");
        if (daemon(0, 0) < 0) {
            log_error("daemon() failed: %s", strerror(errno));
        }
    }

    log_info("keyboard-lock is ready. Hotkey: [Ctrl + Alt + %s]",
             hotkey_keycode_to_name(target_kc));

    time_t start_time = time(NULL);
    bool running = true;
    struct epoll_event events[MAX_EPOLL_EVENTS];

    while (running) {
        int nfds = epoll_wait(epoll_fd, events, MAX_EPOLL_EVENTS, -1);
        if (nfds < 0) {
            if (errno == EINTR) continue;
            log_error("epoll_wait error: %s", strerror(errno));
            break;
        }

        for (int i = 0; i < nfds; i++) {
            int fd = events[i].data.fd;

            if (fd == sig_fd) {
                struct signalfd_siginfo fdsi;
                ssize_t s = read(sig_fd, &fdsi, sizeof(fdsi));
                if (s == sizeof(fdsi)) {
                    log_info("Received termination signal %u. Initiating clean shutdown...",
                             fdsi.ssi_signo);
                    running = false;
                    break;
                }
            } else if (fd == ipc.server_fd) {
                bool should_stop = false;
                bool state_changed = false;
                int dev_count = backend->get_device_count(backend);
                ipc_server_handle_event(&ipc, backend->sm, dev_count,
                                        backend->name, start_time,
                                        &should_stop, &state_changed);

                if (state_changed && backend->sm) {
                    lock_state_t cur = state_machine_get_state(backend->sm);
                    if (cur == STATE_LOCKED) {
                        backend->lock(backend);
                    } else {
                        backend->unlock(backend);
                    }
                }

                if (should_stop) {
                    log_info("Stop command received via IPC. Shutting down.");
                    running = false;
                    break;
                }
            } else {
                /* Backend input / inotify fd event */
                backend->handle_fd_event(backend, fd, events[i].events, &running);
            }
        }
    }

    log_info("Cleaning up resources and releasing all keyboard grabs...");

    /* Always release all grabs on exit */
    backend->unlock(backend);
    backend->cleanup(backend);
    g_emergency_backend = NULL;

    ipc_server_cleanup(&ipc);

    if (sig_fd >= 0) {
        epoll_ctl(epoll_fd, EPOLL_CTL_DEL, sig_fd, NULL);
        close(sig_fd);
    }
    close(epoll_fd);

    logger_cleanup();
    log_info("keyboard-lock terminated cleanly.");
    return 0;
}
