#include "duration_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

static void str_tolower(char *dst, const char *src, size_t max_len) {
    size_t i = 0;
    while (src[i] && i + 1 < max_len) {
        dst[i] = (char)tolower((unsigned char)src[i]);
        i++;
    }
    dst[i] = '\0';
}

int parse_duration_to_seconds(const char *text) {
    if (!text) return -1;

    char lower[512];
    str_tolower(lower, text, sizeof(lower));

    /* Look for a number inside the text */
    const char *p = lower;
    long val = -1;
    const char *num_start = NULL;

    while (*p) {
        if (isdigit((unsigned char)*p)) {
            num_start = p;
            char *endptr = NULL;
            val = strtol(num_start, &endptr, 10);
            p = endptr;
            break;
        }
        p++;
    }

    if (val <= 0 || !num_start) {
        /*
         * Word numbers matching the Vosk grammar in bricks/transcription/transcription.c.
         * Keep these lists strictly synchronized.
         */
        if (strstr(lower, "one")) val = 1;
        else if (strstr(lower, "two")) val = 2;
        else if (strstr(lower, "three")) val = 3;
        else if (strstr(lower, "four")) val = 4;
        else if (strstr(lower, "five")) val = 5;
        else if (strstr(lower, "six")) val = 6;
        else if (strstr(lower, "seven")) val = 7;
        else if (strstr(lower, "eight")) val = 8;
        else if (strstr(lower, "nine")) val = 9;
        else if (strstr(lower, "ten")) val = 10;
        else if (strstr(lower, "eleven")) val = 11;
        else if (strstr(lower, "twelve")) val = 12;
        else if (strstr(lower, "fifteen")) val = 15;
        else if (strstr(lower, "twenty")) val = 20;
        else if (strstr(lower, "thirty")) val = 30;
        else if (strstr(lower, "forty-five") || strstr(lower, "forty five")) val = 45;
        else if (strstr(lower, "forty")) val = 40;
        else if (strstr(lower, "sixty")) val = 60;
        else return -1;
    }

    /* Identify unit */
    if (strstr(lower, "hour") || strstr(lower, "hr") || strstr(lower, "hours") || strstr(lower, "hrs")) {
        return (int)(val * 3600);
    } else if (strstr(lower, "minute") || strstr(lower, "min") || strstr(lower, "minutes") || strstr(lower, "mins")) {
        return (int)(val * 60);
    } else if (strstr(lower, "second") || strstr(lower, "sec") || strstr(lower, "seconds") || strstr(lower, "secs")) {
        return (int)val;
    }

    /* Default assumption if unit is omitted is minutes, but per contract require valid match */
    return -1;
}
