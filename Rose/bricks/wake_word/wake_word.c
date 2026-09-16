#include "wake_word.h"
#include "../../core/event_bus.h"
#include <stdio.h>
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

void wake_word_process_text(const char *text) {
    if (!text || strlen(text) == 0) return;

    char lower[512];
    str_tolower(lower, text, sizeof(lower));

    const char *pos = strstr(lower, "rose");
    if (pos != NULL) {
        printf("[wake_word] 🌹 WAKE WORD DETECTED in: \"%s\"\n", text);

        /* Skip past "rose" and any trailing punctuation/spaces */
        const char *after = pos + 4;
        while (*after && (*after == ' ' || *after == ',' || *after == ':' || *after == '-')) {
            after++;
        }

        EventWakeWordDetected payload;
        strncpy(payload.remaining_text, after, sizeof(payload.remaining_text) - 1);
        payload.remaining_text[sizeof(payload.remaining_text) - 1] = '\0';

        Event wake_event = {
            .type = EVENT_WAKE_WORD_DETECTED,
            .data = &payload,
            .data_size = sizeof(payload)
        };
        event_bus_publish(wake_event);
    }
}

static void on_text_transcribed(const Event *e) {
    if (!e || !e->data) return;
    const EventTextTranscribed *payload = (const EventTextTranscribed *)e->data;
    wake_word_process_text(payload->text);
}

int wake_word_init(void) {
    event_bus_subscribe(EVENT_TEXT_TRANSCRIBED, on_text_transcribed);
    return 0;
}

void wake_word_shutdown(void) {
    /* No persistent resources to free */
}

const Brick g_brick_wake_word = {
    .brick_id = "wake_word",
    .version = 1,
    .init = wake_word_init,
    .process = NULL,
    .shutdown = wake_word_shutdown
};
