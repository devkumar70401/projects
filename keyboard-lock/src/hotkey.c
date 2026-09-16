/**
 * hotkey.c - Hotkey parsing and Linux input keycode lookup table
 */

#include "hotkey.h"
#include <string.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

static const key_mapping_t KEY_MAP[] = {
    {"a", KEY_A}, {"b", KEY_B}, {"c", KEY_C}, {"d", KEY_D},
    {"e", KEY_E}, {"f", KEY_F}, {"g", KEY_G}, {"h", KEY_H},
    {"i", KEY_I}, {"j", KEY_J}, {"k", KEY_K}, {"l", KEY_L},
    {"m", KEY_M}, {"n", KEY_N}, {"o", KEY_O}, {"p", KEY_P},
    {"q", KEY_Q}, {"r", KEY_R}, {"s", KEY_S}, {"t", KEY_T},
    {"u", KEY_U}, {"v", KEY_V}, {"w", KEY_W}, {"x", KEY_X},
    {"y", KEY_Y}, {"z", KEY_Z},

    {"0", KEY_0}, {"1", KEY_1}, {"2", KEY_2}, {"3", KEY_3},
    {"4", KEY_4}, {"5", KEY_5}, {"6", KEY_6}, {"7", KEY_7},
    {"8", KEY_8}, {"9", KEY_9},

    {"f1", KEY_F1}, {"f2", KEY_F2}, {"f3", KEY_F3}, {"f4", KEY_F4},
    {"f5", KEY_F5}, {"f6", KEY_F6}, {"f7", KEY_F7}, {"f8", KEY_F8},
    {"f9", KEY_F9}, {"f10", KEY_F10}, {"f11", KEY_F11}, {"f12", KEY_F12},

    {"space", KEY_SPACE}, {"enter", KEY_ENTER}, {"return", KEY_ENTER},
    {"esc", KEY_ESC}, {"escape", KEY_ESC}, {"tab", KEY_TAB},
    {"backspace", KEY_BACKSPACE}, {"delete", KEY_DELETE}, {"insert", KEY_INSERT},
    {"home", KEY_HOME}, {"end", KEY_END}, {"pageup", KEY_PAGEUP}, {"pagedown", KEY_PAGEDOWN},
    {"pause", KEY_PAUSE}, {"scrolllock", KEY_SCROLLLOCK}
};

static const size_t KEY_MAP_LEN = sizeof(KEY_MAP) / sizeof(KEY_MAP[0]);

static void str_to_lower(char *dst, const char *src, size_t max_len) {
    size_t i = 0;
    while (src[i] && i + 1 < max_len) {
        dst[i] = (char)tolower((unsigned char)src[i]);
        i++;
    }
    dst[i] = '\0';
}

int hotkey_name_to_keycode(const char *name) {
    if (!name || !*name) return -1;

    char lower[64];
    str_to_lower(lower, name, sizeof(lower));

    for (size_t i = 0; i < KEY_MAP_LEN; i++) {
        if (strcmp(lower, KEY_MAP[i].name) == 0) {
            return KEY_MAP[i].keycode;
        }
    }
    return -1;
}

const char* hotkey_keycode_to_name(int keycode) {
    for (size_t i = 0; i < KEY_MAP_LEN; i++) {
        if (KEY_MAP[i].keycode == keycode) {
            return KEY_MAP[i].name;
        }
    }
    return "unknown";
}

int hotkey_parse_string(const char *str) {
    if (!str || !*str) return -1;

    char buf[128];
    str_to_lower(buf, str, sizeof(buf));

    /* Parse tokens separated by '+' */
    char *saveptr = NULL;
    char *token = strtok_r(buf, "+", &saveptr);
    bool has_ctrl = false;
    bool has_alt = false;
    int target_key = -1;

    while (token) {
        /* Trim whitespace */
        while (*token == ' ' || *token == '\t') token++;
        size_t len = strlen(token);
        while (len > 0 && (token[len - 1] == ' ' || token[len - 1] == '\t')) {
            token[len - 1] = '\0';
            len--;
        }

        if (strcmp(token, "ctrl") == 0 || strcmp(token, "control") == 0) {
            has_ctrl = true;
        } else if (strcmp(token, "alt") == 0) {
            has_alt = true;
        } else {
            int kc = hotkey_name_to_keycode(token);
            if (kc > 0) {
                target_key = kc;
            } else {
                return -1; /* Unknown token */
            }
        }
        token = strtok_r(NULL, "+", &saveptr);
    }

    if (has_ctrl && has_alt && target_key > 0) {
        return target_key;
    }

    return -1;
}
