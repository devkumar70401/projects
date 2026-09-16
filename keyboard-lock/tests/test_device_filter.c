/**
 * test_device_filter.c - Unit tests for device classification and mouse isolation
 */

#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <stdbool.h>
#include <linux/input.h>

#define BITS_PER_LONG (sizeof(unsigned long) * 8)
#define NBITS(x) ((((x)-1)/BITS_PER_LONG)+1)
#define OFF(x) ((x)%BITS_PER_LONG)
#define BIT(x) (1UL<<OFF(x))
#define LONG(x) ((x)/BITS_PER_LONG)
#define set_bit(bit, array) (array[LONG(bit)] |= BIT(bit))
#define test_bit(bit, array) ((array[LONG(bit)] >> OFF(bit)) & 1UL)

bool classify_device_capabilities(const unsigned long *ev_bits, const unsigned long *key_bits) {
    /* Must support EV_KEY */
    if (!test_bit(EV_KEY, ev_bits)) {
        return false;
    }

    /* Check mouse/pointer buttons */
    bool has_mouse_btn = test_bit(BTN_LEFT, key_bits) ||
                         test_bit(BTN_RIGHT, key_bits) ||
                         test_bit(BTN_MIDDLE, key_bits) ||
                         test_bit(BTN_TOUCH, key_bits) ||
                         test_bit(BTN_TOOL_FINGER, key_bits);

    /* Check essential alphanumeric / control keys */
    bool has_alpha = test_bit(KEY_A, key_bits) &&
                     test_bit(KEY_Z, key_bits) &&
                     test_bit(KEY_1, key_bits) &&
                     test_bit(KEY_SPACE, key_bits) &&
                     test_bit(KEY_ENTER, key_bits) &&
                     test_bit(KEY_ESC, key_bits);

    bool has_keypad = test_bit(KEY_KP0, key_bits) && test_bit(KEY_KPENTER, key_bits);

    /* STRICT ISOLATION: Reject mice/touchpads */
    if (has_mouse_btn && !has_alpha) {
        return false;
    }

    if (has_alpha || has_keypad) {
        return true;
    }

    return false;
}

int main() {
    printf("=== Starting Device Classification & Mouse Isolation Tests ===\n");

    unsigned long ev_bits[NBITS(EV_MAX)];
    unsigned long key_bits[NBITS(KEY_MAX)];

    /* Test Case 1: Standard PC Keyboard */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_KEY, ev_bits);
    set_bit(KEY_A, key_bits);
    set_bit(KEY_Z, key_bits);
    set_bit(KEY_1, key_bits);
    set_bit(KEY_SPACE, key_bits);
    set_bit(KEY_ENTER, key_bits);
    set_bit(KEY_ESC, key_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == true);
    printf("Test 1 (Standard Keyboard): PASSED (Identified as Keyboard)\n");

    /* Test Case 2: Standard 3-Button Optical Mouse */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_KEY, ev_bits);
    set_bit(BTN_LEFT, key_bits);
    set_bit(BTN_RIGHT, key_bits);
    set_bit(BTN_MIDDLE, key_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == false);
    printf("Test 2 (Standard Mouse): PASSED (Rejected - Mouse Isolated)\n");

    /* Test Case 3: Laptop Multi-Touch Touchpad */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_KEY, ev_bits);
    set_bit(BTN_LEFT, key_bits);
    set_bit(BTN_TOUCH, key_bits);
    set_bit(BTN_TOOL_FINGER, key_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == false);
    printf("Test 3 (Touchpad): PASSED (Rejected - Touchpad Isolated)\n");

    /* Test Case 4: Power Button */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_KEY, ev_bits);
    set_bit(KEY_POWER, key_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == false);
    printf("Test 4 (Power Button): PASSED (Rejected)\n");

    /* Test Case 5: Dedicated Numeric Keypad */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_KEY, ev_bits);
    set_bit(KEY_KP0, key_bits);
    set_bit(KEY_KPENTER, key_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == true);
    printf("Test 5 (Numeric Keypad): PASSED (Identified as Keypad/Keyboard)\n");

    /* Test Case 6: Pure Relational Device (e.g. Trackball without EV_KEY) */
    memset(ev_bits, 0, sizeof(ev_bits));
    memset(key_bits, 0, sizeof(key_bits));
    set_bit(EV_REL, ev_bits);
    assert(classify_device_capabilities(ev_bits, key_bits) == false);
    printf("Test 6 (Pure EV_REL device): PASSED (Rejected)\n");

    printf("=== All Device Classification Tests passed successfully! ===\n");
    return 0;
}
