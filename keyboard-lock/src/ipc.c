/**
 * ipc.c - UNIX Domain Socket Server & Client Implementation
 */

#include "ipc.h"
#include "logger.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/epoll.h>
#include <sys/stat.h>
#include <time.h>

#define VERSION_STRING "keyboard-lock v1.0.0 (Ubuntu Linux)"

void ipc_get_socket_path(char *buf, size_t buf_size) {
    if (!buf || buf_size == 0) return;

    if (getuid() == 0) {
        snprintf(buf, buf_size, "/run/kblock.sock");
        return;
    }

    /* For non-root client, check if system daemon socket exists and is connectable */
    struct stat st;
    if (stat("/run/kblock.sock", &st) == 0) {
        snprintf(buf, buf_size, "/run/kblock.sock");
        return;
    }

    const char *runtime_dir = getenv("XDG_RUNTIME_DIR");
    if (runtime_dir && *runtime_dir) {
        snprintf(buf, buf_size, "%s/kblock.sock", runtime_dir);
    } else {
        snprintf(buf, buf_size, "/tmp/kblock-%d.sock", (int)getuid());
    }
}

int ipc_server_init(ipc_server_t *ipc, int epoll_fd) {
    if (!ipc) return -1;
    memset(ipc, 0, sizeof(*ipc));
    ipc->server_fd = -1;
    ipc->epoll_fd = epoll_fd;

    if (getuid() == 0) {
        snprintf(ipc->socket_path, sizeof(ipc->socket_path), "/run/kblock.sock");
    } else {
        const char *runtime_dir = getenv("XDG_RUNTIME_DIR");
        if (runtime_dir && *runtime_dir) {
            snprintf(ipc->socket_path, sizeof(ipc->socket_path), "%s/kblock.sock", runtime_dir);
        } else {
            snprintf(ipc->socket_path, sizeof(ipc->socket_path), "/tmp/kblock-%d.sock", (int)getuid());
        }
    }

    /* Check if socket exists and if an active daemon is responding */
    int test_fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (test_fd >= 0) {
        struct sockaddr_un test_addr;
        memset(&test_addr, 0, sizeof(test_addr));
        test_addr.sun_family = AF_UNIX;
        snprintf(test_addr.sun_path, sizeof(test_addr.sun_path), "%s", ipc->socket_path);

        if (connect(test_fd, (struct sockaddr *)&test_addr, sizeof(test_addr)) == 0) {
            /* Another daemon is alive and responding! */
            close(test_fd);
            log_error("Another instance of keyboard-lock is already running on %s", ipc->socket_path);
            return -1;
        }
        close(test_fd);
    }

    /* Remove stale socket file if any */
    unlink(ipc->socket_path);

    int sfd = socket(AF_UNIX, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (sfd < 0) {
        log_error("Failed to create IPC socket: %s", strerror(errno));
        return -1;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    snprintf(addr.sun_path, sizeof(addr.sun_path), "%s", ipc->socket_path);

    if (bind(sfd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        log_error("Failed to bind IPC socket '%s': %s", ipc->socket_path, strerror(errno));
        close(sfd);
        return -1;
    }

    /* If running as root, allow all desktop users to access the socket (0666) */
    chmod(ipc->socket_path, (getuid() == 0) ? 0666 : 0600);

    if (listen(sfd, 5) < 0) {
        log_error("Failed to listen on IPC socket: %s", strerror(errno));
        close(sfd);
        unlink(ipc->socket_path);
        return -1;
    }

    ipc->server_fd = sfd;

    /* Add to epoll */
    if (epoll_fd >= 0) {
        struct epoll_event ev;
        memset(&ev, 0, sizeof(ev));
        ev.events = EPOLLIN;
        ev.data.fd = sfd;
        if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, sfd, &ev) < 0) {
            log_error("Failed to add IPC socket to epoll: %s", strerror(errno));
            close(sfd);
            unlink(ipc->socket_path);
            return -1;
        }
    }

    log_debug("IPC server listening on %s (fd %d)", ipc->socket_path, sfd);
    return sfd;
}

int ipc_server_handle_event(ipc_server_t *ipc, state_machine_t *sm,
                            int active_devices_count, const char *backend_name,
                            time_t start_time, bool *should_stop, bool *state_changed) {
    if (!ipc || ipc->server_fd < 0) return -1;

    int client_fd = accept4(ipc->server_fd, NULL, NULL, SOCK_CLOEXEC);
    if (client_fd < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) return 0;
        log_warn("IPC accept failed: %s", strerror(errno));
        return -1;
    }

    /* Read command from client */
    char req[256];
    memset(req, 0, sizeof(req));
    ssize_t n = read(client_fd, req, sizeof(req) - 1);
    if (n <= 0) {
        close(client_fd);
        return 0;
    }

    /* Trim trailing newline / whitespace */
    while (n > 0 && (req[n - 1] == '\n' || req[n - 1] == '\r' || req[n - 1] == ' ')) {
        req[--n] = '\0';
    }

    char resp[512];
    memset(resp, 0, sizeof(resp));

    if (strcasecmp(req, "LOCK") == 0) {
        if (sm) {
            lock_state_t prev = state_machine_get_state(sm);
            state_machine_set_state(sm, STATE_LOCKED);
            if (state_changed && prev != STATE_LOCKED) *state_changed = true;
        }
        snprintf(resp, sizeof(resp), "OK LOCKED\n");
    } else if (strcasecmp(req, "UNLOCK") == 0) {
        if (sm) {
            lock_state_t prev = state_machine_get_state(sm);
            state_machine_set_state(sm, STATE_UNLOCKED);
            if (state_changed && prev != STATE_UNLOCKED) *state_changed = true;
        }
        snprintf(resp, sizeof(resp), "OK UNLOCKED\n");
    } else if (strcasecmp(req, "TOGGLE") == 0) {
        if (sm) {
            lock_state_t cur = state_machine_get_state(sm);
            lock_state_t next = (cur == STATE_LOCKED) ? STATE_UNLOCKED : STATE_LOCKED;
            state_machine_set_state(sm, next);
            if (state_changed) *state_changed = true;
            snprintf(resp, sizeof(resp), "OK %s\n", lock_state_to_string(next));
        } else {
            snprintf(resp, sizeof(resp), "ERROR NO_STATE_MACHINE\n");
        }
    } else if (strcasecmp(req, "STATUS") == 0) {
        time_t now = time(NULL);
        long uptime_sec = (long)(now - start_time);
        lock_state_t cur = sm ? state_machine_get_state(sm) : STATE_UNLOCKED;
        snprintf(resp, sizeof(resp),
                 "STATUS state=%s devices=%d backend=%s pid=%d uptime_sec=%ld\n",
                 lock_state_to_string(cur),
                 active_devices_count,
                 backend_name ? backend_name : "evdev",
                 (int)getpid(),
                 uptime_sec);
    } else if (strcasecmp(req, "STOP") == 0 || strcasecmp(req, "QUIT") == 0) {
        if (should_stop) *should_stop = true;
        snprintf(resp, sizeof(resp), "OK STOPPING\n");
    } else if (strcasecmp(req, "VERSION") == 0) {
        snprintf(resp, sizeof(resp), "%s\n", VERSION_STRING);
    } else {
        snprintf(resp, sizeof(resp), "ERROR UNKNOWN_COMMAND '%s'\n", req);
    }

    /* Write response back to client */
    ssize_t written = write(client_fd, resp, strlen(resp));
    (void)written;
    close(client_fd);

    return 0;
}

void ipc_server_cleanup(ipc_server_t *ipc) {
    if (!ipc) return;
    if (ipc->server_fd >= 0) {
        if (ipc->epoll_fd >= 0) {
            epoll_ctl(ipc->epoll_fd, EPOLL_CTL_DEL, ipc->server_fd, NULL);
        }
        close(ipc->server_fd);
        ipc->server_fd = -1;
    }
    if (ipc->socket_path[0]) {
        unlink(ipc->socket_path);
        ipc->socket_path[0] = '\0';
    }
}

int ipc_client_send_command(const char *command, char *response_buf, size_t response_buf_size) {
    if (!command) return -1;

    char socket_path[IPC_PATH_MAX];
    ipc_get_socket_path(socket_path, sizeof(socket_path));

    int sfd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sfd < 0) {
        return -1;
    }

    /* Set 1-second timeout */
    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    setsockopt(sfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(sfd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    snprintf(addr.sun_path, sizeof(addr.sun_path), "%s", socket_path);

    if (connect(sfd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        close(sfd);
        return -1;
    }

    char req[256];
    snprintf(req, sizeof(req), "%s\n", command);
    if (write(sfd, req, strlen(req)) <= 0) {
        close(sfd);
        return -1;
    }

    if (response_buf && response_buf_size > 0) {
        memset(response_buf, 0, response_buf_size);
        ssize_t n = read(sfd, response_buf, response_buf_size - 1);
        if (n > 0) {
            response_buf[n] = '\0';
            /* Strip trailing newline */
            while (n > 0 && (response_buf[n - 1] == '\n' || response_buf[n - 1] == '\r')) {
                response_buf[--n] = '\0';
            }
        }
    }

    close(sfd);
    return 0;
}
