/**
 * test_state_machine.c - Unit tests for Keyboard Lock State Machine
 */

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include "../src/state_machine.h"

static int tests_run = 0;
static int tests_passed = 0;

#define TEST(name) \
    printf("Running %s... ", #name); \
    tests_run++; \
    test_##name(); \
    tests_passed++; \
    printf("PASSED\n");

void test_initial_state() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    assert(strcmp(lock_state_to_string(STATE_UNLOCKED), "UNLOCKED") == 0);
    assert(strcmp(lock_state_to_string(STATE_LOCKED), "LOCKED") == 0);
}

void test_normal_pass_when_unlocked() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Press 'A' */
    assert(state_machine_process_key(&sm, KEY_A, 1) == ACTION_PASS);
    /* Repeat 'A' */
    assert(state_machine_process_key(&sm, KEY_A, 2) == ACTION_PASS);
    /* Release 'A' */
    assert(state_machine_process_key(&sm, KEY_A, 0) == ACTION_PASS);

    /* Enter, Space, Esc */
    assert(state_machine_process_key(&sm, KEY_ENTER, 1) == ACTION_PASS);
    assert(state_machine_process_key(&sm, KEY_SPACE, 1) == ACTION_PASS);
    assert(state_machine_process_key(&sm, KEY_ESC, 1) == ACTION_PASS);

    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
}

void test_toggle_lock_and_unlock() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Press LeftCtrl */
    assert(state_machine_process_key(&sm, KEY_LEFTCTRL, 1) == ACTION_PASS);
    /* Press LeftAlt */
    assert(state_machine_process_key(&sm, KEY_LEFTALT, 1) == ACTION_PASS);
    /* Press K -> Must toggle to LOCKED */
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_LOCKED);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);

    /* Release K while locked -> Discard */
    assert(state_machine_process_key(&sm, KEY_K, 0) == ACTION_DISCARD);
    /* Release Alt, Ctrl while locked -> Discard */
    assert(state_machine_process_key(&sm, KEY_LEFTALT, 0) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_LEFTCTRL, 0) == ACTION_DISCARD);

    /* Verify locked behavior: typing 'A', 'B', 'Enter' are discarded */
    assert(state_machine_process_key(&sm, KEY_A, 1) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_B, 1) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_ENTER, 1) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_BACKSPACE, 1) == ACTION_DISCARD);

    /* Now Unlock: Press RightCtrl, RightAlt, K */
    assert(state_machine_process_key(&sm, KEY_RIGHTCTRL, 1) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_RIGHTALT, 1) == ACTION_DISCARD);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_UNLOCKED);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);

    /* Release K -> Pass */
    assert(state_machine_process_key(&sm, KEY_K, 0) == ACTION_PASS);
    /* Release RightAlt, RightCtrl -> Pass */
    assert(state_machine_process_key(&sm, KEY_RIGHTALT, 0) == ACTION_PASS);
    assert(state_machine_process_key(&sm, KEY_RIGHTCTRL, 0) == ACTION_PASS);

    /* Normal typing works again */
    assert(state_machine_process_key(&sm, KEY_A, 1) == ACTION_PASS);
}

void test_auto_repeat_debounce() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Press LeftCtrl + LeftAlt + K */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_LOCKED);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);

    /* Simulate auto-repeat holding K down */
    for (int i = 0; i < 50; i++) {
        assert(state_machine_process_key(&sm, KEY_K, 2) == ACTION_DISCARD);
        assert(state_machine_get_state(&sm) == STATE_LOCKED);
    }

    /* Release K */
    assert(state_machine_process_key(&sm, KEY_K, 0) == ACTION_DISCARD);

    /* Press K again while still holding Ctrl+Alt -> Must toggle to UNLOCKED */
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_UNLOCKED);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
}

void test_modifier_isolation() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Test 1: Ctrl + Alt + Shift + K -> Should NOT lock */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    state_machine_process_key(&sm, KEY_LEFTSHIFT, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_PASS);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    state_machine_process_key(&sm, KEY_K, 0);
    state_machine_process_key(&sm, KEY_LEFTSHIFT, 0);
    state_machine_process_key(&sm, KEY_LEFTALT, 0);
    state_machine_process_key(&sm, KEY_LEFTCTRL, 0);

    /* Test 2: Ctrl + Alt + Super + K -> Should NOT lock */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    state_machine_process_key(&sm, KEY_LEFTMETA, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_PASS);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    state_machine_process_key(&sm, KEY_K, 0);
    state_machine_process_key(&sm, KEY_LEFTMETA, 0);
    state_machine_process_key(&sm, KEY_LEFTALT, 0);
    state_machine_process_key(&sm, KEY_LEFTCTRL, 0);

    /* Test 3: Only Ctrl + K -> Should NOT lock */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_PASS);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    state_machine_process_key(&sm, KEY_K, 0);
    state_machine_process_key(&sm, KEY_LEFTCTRL, 0);

    /* Test 4: Only Alt + K -> Should NOT lock */
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_PASS);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    state_machine_process_key(&sm, KEY_K, 0);
    state_machine_process_key(&sm, KEY_LEFTALT, 0);

    /* Test 5: Ctrl + Alt + J (wrong key) -> Should NOT lock */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    assert(state_machine_process_key(&sm, KEY_J, 1) == ACTION_PASS);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
    state_machine_process_key(&sm, KEY_J, 0);
    state_machine_process_key(&sm, KEY_LEFTALT, 0);
    state_machine_process_key(&sm, KEY_LEFTCTRL, 0);
}

void test_out_of_order_release() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    /* Sequence: Ctrl down -> Alt down -> K down (LOCKED) -> Alt up -> Ctrl up -> K up */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_LOCKED);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);

    state_machine_process_key(&sm, KEY_LEFTALT, 0);
    state_machine_process_key(&sm, KEY_LEFTCTRL, 0);
    state_machine_process_key(&sm, KEY_K, 0);

    /* Now press K alone while locked -> Discard */
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_DISCARD);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);
    state_machine_process_key(&sm, KEY_K, 0);

    /* Unlock with clean combination */
    state_machine_process_key(&sm, KEY_LEFTCTRL, 1);
    state_machine_process_key(&sm, KEY_LEFTALT, 1);
    assert(state_machine_process_key(&sm, KEY_K, 1) == ACTION_UNLOCKED);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);
}

void test_explicit_state_setting() {
    state_machine_t sm;
    state_machine_init(&sm, KEY_K);

    assert(state_machine_set_state(&sm, STATE_LOCKED) == ACTION_LOCKED);
    assert(state_machine_get_state(&sm) == STATE_LOCKED);

    /* Setting locked again returns ACTION_DISCARD (no change) */
    assert(state_machine_set_state(&sm, STATE_LOCKED) == ACTION_DISCARD);

    assert(state_machine_set_state(&sm, STATE_UNLOCKED) == ACTION_UNLOCKED);
    assert(state_machine_get_state(&sm) == STATE_UNLOCKED);

    /* Setting unlocked again returns ACTION_PASS (no change) */
    assert(state_machine_set_state(&sm, STATE_UNLOCKED) == ACTION_PASS);
}

int main() {
    printf("=== Starting State Machine Unit Tests ===\n");
    TEST(initial_state);
    TEST(normal_pass_when_unlocked);
    TEST(toggle_lock_and_unlock);
    TEST(auto_repeat_debounce);
    TEST(modifier_isolation);
    TEST(out_of_order_release);
    TEST(explicit_state_setting);
    printf("=== All %d tests passed successfully! ===\n", tests_passed);
    return 0;
}
