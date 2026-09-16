/**
 * ipc.h - Single-Instance Control & CLI IPC Protocol (UNIX Domain Sockets)
 */

#ifndef KEYBOARD_LOCK_IPC_H
#define KEYBOARD_LOCK_IPC_H

#include <stdbool.h>
#include <stddef.h>
#include <sys/types.h>
#include <sys/un.h>
#include "state_machine.h"

#ifdef __cplusplus
extern "C" {
#endif

#define IPC_PATH_MAX (sizeof(((struct sockaddr_un *)0)->sun_path))

typedef struct {
    int server_fd;
    char socket_path[IPC_PATH_MAX];
    int epoll_fd;
} ipc_server_t;

/**
 * Get standard runtime socket path for current user ($XDG_RUNTIME_DIR or /tmp).
 */
void ipc_get_socket_path(char *buf, size_t buf_size);

/**
 * Initialize single-instance IPC server.
 * Returns server_fd >= 0 on success, or -1 if another instance is already running or on error.
 */
int ipc_server_init(ipc_server_t *ipc, int epoll_fd);

/**
 * Handle incoming IPC client connections and requests.
 */
int ipc_server_handle_event(ipc_server_t *ipc, state_machine_t *sm,
                            int active_devices_count, const char *backend_name,
                            time_t start_time, bool *should_stop, bool *state_changed);

/**
 * Clean up and unlink socket.
 */
void ipc_server_cleanup(ipc_server_t *ipc);

/**
 * Send an IPC command from CLI client to running daemon.
 */
int ipc_client_send_command(const char *command, char *response_buf, size_t response_buf_size);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_IPC_H */
