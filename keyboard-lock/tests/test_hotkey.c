/**
 * test_hotkey.c - Unit tests for hotkey parsing and lookup
 */

#include <stdio.h>
#include <assert.h>
#include <string.h>
#include "../src/hotkey.h"

int main() {
    printf("=== Starting Hotkey Lookup & Parser Tests ===\n");

    /* Lookup tests */
    assert(hotkey_name_to_keycode("k") == KEY_K);
    assert(hotkey_name_to_keycode("K") == KEY_K);
    assert(hotkey_name_to_keycode("space") == KEY_SPACE);
    assert(hotkey_name_to_keycode("enter") == KEY_ENTER);
    assert(hotkey_name_to_keycode("f12") == KEY_F12);
    assert(hotkey_name_to_keycode("nonexistent") == -1);

    assert(strcmp(hotkey_keycode_to_name(KEY_K), "k") == 0);
    assert(strcmp(hotkey_keycode_to_name(KEY_SPACE), "space") == 0);

    /* String parsing tests */
    assert(hotkey_parse_string("ctrl+alt+k") == KEY_K);
    assert(hotkey_parse_string("Ctrl + Alt + K") == KEY_K);
    assert(hotkey_parse_string("CTRL+ALT+L") == KEY_L);
    assert(hotkey_parse_string("ctrl+alt+space") == KEY_SPACE);
    assert(hotkey_parse_string("ctrl+alt+f1") == KEY_F1);

    /* Invalid combos */
    assert(hotkey_parse_string("ctrl+k") == -1);      /* missing alt */
    assert(hotkey_parse_string("alt+k") == -1);       /* missing ctrl */
    assert(hotkey_parse_string("ctrl+alt") == -1);     /* missing key */
    assert(hotkey_parse_string("ctrl+alt+invalid") == -1);
    assert(hotkey_parse_string("") == -1);

    printf("=== All Hotkey tests passed successfully! ===\n");
    return 0;
}
