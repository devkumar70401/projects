/**
 * test_mock_evdev.c - End-to-End Simulation of Linux input event processing
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <assert.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <linux/input.h>
#include "../src/state_machine.h"

static void send_event(int fd, uint16_t type, uint16_t code, int32_t value) {
    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = type;
    ev.code = code;
    ev.value = value;
    ssize_t w = write(fd, &ev, sizeof(ev));
    assert(w == sizeof(ev));
}

int main() {
    printf("=== Starting Mock Evdev End-to-End Simulation Tests ===\n");

    int sv[2];
    assert(socketpair(AF_UNIX, SOCK_STREAM, 0, sv) == 0);
    int write_fd = sv[0];
    int read_fd = sv[1];

    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Phase 1: Unlocked typing */
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    send_event(write_fd, EV_KEY, KEY_H, 1);
    send_event(write_fd, EV_KEY, KEY_H, 0);
    send_event(write_fd, EV_KEY, KEY_E, 1);
    send_event(write_fd, EV_KEY, KEY_E, 0);

    struct input_event read_buf[8];
    ssize_t n = read(read_fd, read_buf, sizeof(read_buf));
    assert(n == (ssize_t)(4 * sizeof(struct input_event)));

    for (size_t i = 0; i < 4; i++) {
        state_action_t act = state_machine_process_key(&sm, read_buf[i].code, read_buf[i].value);
        assert(act == ACTION_PASS);
    }
    printf("Phase 1: Normal typing pass-through verified.\n");

    /* Phase 2: Trigger Lock with Ctrl + Alt + K */
    send_event(write_fd, EV_KEY, KEY_LEFTCTRL, 1);
    send_event(write_fd, EV_KEY, KEY_LEFTALT, 1);
    send_event(write_fd, EV_KEY, KEY_K, 1);

    n = read(read_fd, read_buf, sizeof(read_buf));
    assert(n == (ssize_t)(3 * sizeof(struct input_event)));

    assert(state_machine_process_key(&sm, read_buf[0].code, read_buf[0].value) == ACTION_PASS);
    assert(state_machine_process_key(&sm, read_buf[1].code, read_buf[1].value) == ACTION_PASS);
    assert(state_machine_process_key(&sm, read_buf[2].code, read_buf[2].value) == ACTION_LOCKED);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);
    printf("Phase 2: Transition to LOCKED via Ctrl+Alt+K verified.\n");

    /* Release keys while locked */
    send_event(write_fd, EV_KEY, KEY_K, 0);
    send_event(write_fd, EV_KEY, KEY_LEFTALT, 0);
    send_event(write_fd, EV_KEY, KEY_LEFTCTRL, 0);
    n = read(read_fd, read_buf, sizeof(read_buf));
    for (size_t i = 0; i < 3; i++) {
        assert(state_machine_process_key(&sm, read_buf[i].code, read_buf[i].value) == ACTION_DISCARD);
    }

    /* Phase 3: Verify all keyboard keys are discarded while locked */
    uint16_t test_keys[] = {
        KEY_A, KEY_B, KEY_C, KEY_1, KEY_2, KEY_ENTER, KEY_SPACE,
        KEY_BACKSPACE, KEY_ESC, KEY_TAB, KEY_UP, KEY_DOWN, KEY_F1, KEY_F12,
        KEY_LEFTMETA, KEY_RIGHTCTRL, KEY_RIGHTALT, KEY_LEFTSHIFT
    };
    size_t num_test_keys = sizeof(test_keys) / sizeof(test_keys[0]);

    for (size_t i = 0; i < num_test_keys; i++) {
        send_event(write_fd, EV_KEY, test_keys[i], 1); /* press */
        send_event(write_fd, EV_KEY, test_keys[i], 2); /* repeat */
        send_event(write_fd, EV_KEY, test_keys[i], 0); /* release */

        n = read(read_fd, read_buf, sizeof(read_buf));
        assert(n == (ssize_t)(3 * sizeof(struct input_event)));

        for (size_t j = 0; j < 3; j++) {
            state_action_t act = state_machine_process_key(&sm, read_buf[j].code, read_buf[j].value);
            assert(act == ACTION_DISCARD);
        }
        assert(state_machine_get_state(&sm) == STATE_LOCKED);
    }
    printf("Phase 3: 100%% Keyboard suppression verified across %zu keys.\n", num_test_keys);

    /* Phase 4: Unlock with Ctrl + Alt + K */
    send_event(write_fd, EV_KEY, KEY_LEFTCTRL, 1);
    send_event(write_fd, EV_KEY, KEY_LEFTALT, 1);
    send_event(write_fd, EV_KEY, KEY_K, 1);

    n = read(read_fd, read_buf, sizeof(read_buf));
    assert(n == (ssize_t)(3 * sizeof(struct input_event)));

    assert(state_machine_process_key(&sm, read_buf[0].code, read_buf[0].value) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, read_buf[1].code, read_buf[1].value) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, read_buf[2].code, read_buf[2].value) == ACTION_UNLOCKED);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    printf("Phase 4: Transition to UNLOCKED via Ctrl+Alt+K verified.\n");

    /* Release keys while unlocked */
    send_event(write_fd, EV_KEY, KEY_K, 0);
    send_event(write_fd, EV_KEY, KEY_LEFTALT, 0);
    send_event(write_fd, EV_KEY, KEY_LEFTCTRL, 0);
    n = read(read_fd, read_buf, sizeof(read_buf));
    for (size_t i = 0; i < 3; i++) {
        assert(state_machine_process_key(&sm, read_buf[i].code, read_buf[i].value) == ACTION_PASS);
    }

    /* Phase 5: Normal typing resumed */
    send_event(write_fd, EV_KEY, KEY_A, 1);
    send_event(write_fd, EV_KEY, KEY_A, 0);
    n = read(read_fd, read_buf, sizeof(read_buf));
    assert(n == (ssize_t)(2 * sizeof(struct input_event)));
    assert(state_machine_process_key(&sm, read_buf[0].code, read_buf[0].value) == ACTION_PASS);
    assert(state_machine_process_key(&sm, read_buf[1].code, read_buf[1].value) == ACTION_PASS);
    printf("Phase 5: Resumed normal pass-through verified.\n");

    close(write_fd);
    close(read_fd);

    printf("=== All Mock Evdev End-to-End Simulation Tests passed successfully! ===\n");
    return 0;
}
