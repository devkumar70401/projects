/**
 * test_ipc.c - Unit tests for IPC communication
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/epoll.h>
#include "../src/ipc.h"
#include "../src/state_machine.h"
#include "../src/logger.h"

static int g_epoll_fd = -1;
static ipc_server_t g_ipc;
static state_machine_t g_sm;
static bool g_server_running = true;

void* server_thread_func(void *arg) {
    (void)arg;
    time_t start_time = time(NULL);

    while (g_server_running) {
        struct epoll_event events[4];
        int nfds = epoll_wait(g_epoll_fd, events, 4, 100);
        for (int i = 0; i < nfds; i++) {
            if (events[i].data.fd == g_ipc.server_fd) {
                bool should_stop = false;
                bool state_changed = false;
                ipc_server_handle_event(&g_ipc, &g_sm, 2, "mock_backend", start_time, &should_stop, &state_changed);
                if (should_stop) {
                    g_server_running = false;
                }
            }
        }
    }
    return NULL;
}

int main() {
    printf("=== Starting IPC Unit Tests ===\n");
    logger_init(LOG_LEVEL_ERROR, false, true);

    state_machine_init(&g_sm, KEY_K);

    g_epoll_fd = epoll_create1(EPOLL_CLOEXEC);
    assert(g_epoll_fd >= 0);

    int sfd = ipc_server_init(&g_ipc, g_epoll_fd);
    assert(sfd >= 0);

    /* Start background server thread */
    pthread_t th;
    pthread_create(&th, NULL, server_thread_func, NULL);

    usleep(50000); /* 50ms */

    char resp[512];

    /* Test 1: STATUS */
    assert(ipc_client_send_command("STATUS", resp, sizeof(resp)) == 0);
    printf("Status response: %s\n", resp);
    assert(strstr(resp, "state=UNLOCKED") != NULL);
    assert(strstr(resp, "devices=2") != NULL);

    /* Test 2: LOCK */
    assert(ipc_client_send_command("LOCK", resp, sizeof(resp)) == 0);
    printf("Lock response: %s\n", resp);
    assert(strcmp(resp, "OK LOCKED") == 0);
    assert(state_machine_get_state(&g_sm) == STATE_LOCKED);

    /* Test 3: STATUS while locked */
    assert(ipc_client_send_command("STATUS", resp, sizeof(resp)) == 0);
    assert(strstr(resp, "state=LOCKED") != NULL);

    /* Test 4: TOGGLE */
    assert(ipc_client_send_command("TOGGLE", resp, sizeof(resp)) == 0);
    assert(strcmp(resp, "OK UNLOCKED") == 0);
    assert(state_machine_get_state(&g_sm) == STATE_UNLOCKED);

    /* Test 5: VERSION */
    assert(ipc_client_send_command("VERSION", resp, sizeof(resp)) == 0);
    printf("Version response: %s\n", resp);
    assert(strstr(resp, "keyboard-lock") != NULL);

    /* Test 6: STOP */
    assert(ipc_client_send_command("STOP", resp, sizeof(resp)) == 0);
    assert(strcmp(resp, "OK STOPPING") == 0);

    pthread_join(th, NULL);

    ipc_server_cleanup(&g_ipc);
    close(g_epoll_fd);

    printf("=== All IPC tests passed successfully! ===\n");
    return 0;
}
