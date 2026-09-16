/**
 * state_machine.h - Keyboard Lock State Machine & Hotkey Interception
 *
 * Implements a robust state machine for toggling between UNLOCKED and LOCKED states.
 * Tracks modifier keys (Ctrl, Alt, Shift, Super/Meta) and guarantees:
 *  - Debounced single-activation hotkey triggers (no auto-repeat loops)
 *  - Strict modifier isolation (Ctrl+Alt+Shift+K will NOT toggle)
 *  - Clean modifier state transitions on any release order
 *  - Complete event suppression when locked
 */

#ifndef KEYBOARD_LOCK_STATE_MACHINE_H
#define KEYBOARD_LOCK_STATE_MACHINE_H

#include <stdbool.h>
#include <stdint.h>
#include <linux/input-event-codes.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    ACTION_NONE        = 0, /* No action / uninitialized */
    ACTION_PASS        = 1, /* Forward event to application (when unlocked) */
    ACTION_DISCARD     = 2, /* Consume & drop event (when locked) */
    ACTION_LOCKED      = 3, /* Hotkey triggered: state changed UNLOCKED -> LOCKED */
    ACTION_UNLOCKED    = 4  /* Hotkey triggered: state changed LOCKED -> UNLOCKED */
} state_action_t;

typedef enum {
    STATE_UNLOCKED = 0,
    STATE_LOCKED   = 1
} lock_state_t;

typedef struct {
    lock_state_t state;

    /* Modifier key states */
    bool ctrl_left;
    bool ctrl_right;
    bool alt_left;
    bool alt_right;
    bool shift_left;
    bool shift_right;
    bool meta_left;
    bool meta_right;

    /* Target key state */
    bool target_key_pressed;
    int target_keycode; /* Default: KEY_K (37) */

    /* Debounce flag: true once hotkey has triggered, reset when target key is released */
    bool hotkey_consumed;
} state_machine_t;

/**
 * Initialize state machine with target keycode (e.g. KEY_K).
 */
void state_machine_init(state_machine_t *sm, int target_keycode);

/**
 * Reset all modifier states to unpressed (e.g. on grab/ungrab transition).
 */
void state_machine_reset_modifiers(state_machine_t *sm);

/**
 * Process a single raw keyboard key event.
 *
 * @param sm Pointer to state machine
 * @param keycode Linux input event keycode (e.g. KEY_K, KEY_LEFTCTRL)
 * @param value 0 = key release, 1 = key press, 2 = auto-repeat
 * @return state_action_t Resulting action for the input pipeline
 */
state_action_t state_machine_process_key(state_machine_t *sm, int keycode, int value);

/**
 * Explicitly set lock state (e.g. from CLI command / IPC).
 */
state_action_t state_machine_set_state(state_machine_t *sm, lock_state_t new_state);

/**
 * Get current lock state.
 */
lock_state_t state_machine_get_state(const state_machine_t *sm);

/**
 * Check if exact hotkey modifiers (Ctrl + Alt, without Shift or Super) are currently held.
 */
bool state_machine_is_hotkey_modifiers_held(const state_machine_t *sm);

/**
 * Helper to convert lock state enum to string.
 */
const char* lock_state_to_string(lock_state_t state);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_STATE_MACHINE_H */
