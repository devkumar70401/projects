/**
 * hotkey.h - Hotkey string parsing and Linux keycode mapping
 */

#ifndef KEYBOARD_LOCK_HOTKEY_H
#define KEYBOARD_LOCK_HOTKEY_H

#include <stdbool.h>
#include <linux/input-event-codes.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    char name[32];
    int keycode;
} key_mapping_t;

/**
 * Find Linux keycode for a given key name (case-insensitive).
 * Example: "k" -> KEY_K (37), "space" -> KEY_SPACE (57), "f12" -> KEY_F12 (88).
 * Returns -1 if key name is unknown.
 */
int hotkey_name_to_keycode(const char *name);

/**
 * Get human-readable name for a Linux keycode.
 */
const char* hotkey_keycode_to_name(int keycode);

/**
 * Parse a full hotkey specification string (e.g. "ctrl+alt+k" or "Ctrl+Alt+K").
 * Returns the target keycode (e.g. KEY_K) on success, or -1 on syntax error.
 */
int hotkey_parse_string(const char *str);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_HOTKEY_H */
