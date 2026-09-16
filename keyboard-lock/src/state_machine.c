/**
 * state_machine.c - Keyboard Lock State Machine Implementation
 */

#include "state_machine.h"
#include <string.h>

void state_machine_init(state_machine_t *sm, int target_keycode) {
    if (!sm) return;
    memset(sm, 0, sizeof(*sm));
    sm->state = STATE_UNLOCKED;
    sm->target_keycode = (target_keycode > 0) ? target_keycode : KEY_K;
}

void state_machine_reset_modifiers(state_machine_t *sm) {
    if (!sm) return;
    sm->ctrl_left = false;
    sm->ctrl_right = false;
    sm->alt_left = false;
    sm->alt_right = false;
    sm->shift_left = false;
    sm->shift_right = false;
    sm->meta_left = false;
    sm->meta_right = false;
    sm->target_key_pressed = false;
    sm->hotkey_consumed = false;
}

bool state_machine_is_hotkey_modifiers_held(const state_machine_t *sm) {
    if (!sm) return false;
    bool is_ctrl = sm->ctrl_left || sm->ctrl_right;
    bool is_alt = sm->alt_left || sm->alt_right;
    bool is_shift = sm->shift_left || sm->shift_right;
    bool is_meta = sm->meta_left || sm->meta_right;

    return (is_ctrl && is_alt && !is_shift && !is_meta);
}

state_action_t state_machine_process_key(state_machine_t *sm, int keycode, int value) {
    if (!sm) return ACTION_DISCARD;

    /* value: 0 = release, 1 = press, 2 = repeat */
    bool is_press = (value == 1);
    bool is_repeat = (value == 2);
    bool is_release = (value == 0);

    /* Update modifier states */
    switch (keycode) {
        case KEY_LEFTCTRL:
            sm->ctrl_left = !is_release;
            break;
        case KEY_RIGHTCTRL:
            sm->ctrl_right = !is_release;
            break;
        case KEY_LEFTALT:
            sm->alt_left = !is_release;
            break;
        case KEY_RIGHTALT:
            sm->alt_right = !is_release;
            break;
        case KEY_LEFTSHIFT:
            sm->shift_left = !is_release;
            break;
        case KEY_RIGHTSHIFT:
            sm->shift_right = !is_release;
            break;
        case KEY_LEFTMETA:
            sm->meta_left = !is_release;
            break;
        case KEY_RIGHTMETA:
            sm->meta_right = !is_release;
            break;
        default:
            break;
    }

    /* Check target hotkey */
    if (keycode == sm->target_keycode) {
        if (is_press) {
            sm->target_key_pressed = true;
            if (state_machine_is_hotkey_modifiers_held(sm) && !sm->hotkey_consumed) {
                sm->hotkey_consumed = true;
                if (sm->state == STATE_UNLOCKED) {
                    sm->state = STATE_LOCKED;
                    return ACTION_LOCKED;
                } else {
                    sm->state = STATE_UNLOCKED;
                    return ACTION_UNLOCKED;
                }
            }
        } else if (is_release) {
            sm->target_key_pressed = false;
            sm->hotkey_consumed = false;
        } else if (is_repeat) {
            /* Auto-repeat ignored for toggle */
        }
    }

    /* If locked, consume and discard all keyboard events */
    if (sm->state == STATE_LOCKED) {
        return ACTION_DISCARD;
    }

    /* If unlocked, pass through */
    return ACTION_PASS;
}

state_action_t state_machine_set_state(state_machine_t *sm, lock_state_t new_state) {
    if (!sm) return ACTION_NONE;
    if (sm->state == new_state) {
        return (new_state == STATE_LOCKED) ? ACTION_DISCARD : ACTION_PASS;
    }
    sm->state = new_state;
    state_machine_reset_modifiers(sm);
    return (new_state == STATE_LOCKED) ? ACTION_LOCKED : ACTION_UNLOCKED;
}

lock_state_t state_machine_get_state(const state_machine_t *sm) {
    return sm ? sm->state : STATE_UNLOCKED;
}

const char* lock_state_to_string(lock_state_t state) {
    switch (state) {
        case STATE_LOCKED:
            return "LOCKED";
        case STATE_UNLOCKED:
        default:
            return "UNLOCKED";
    }
}
