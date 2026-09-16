#include "command_router.h"
#include "../../core/event_bus.h"
#include "../../subsystems/timer/duration_parser.h"
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

int command_router_route_text(const char *post_wake_text) {
    if (!post_wake_text || strlen(post_wake_text) == 0) {
        printf("[command_router] NO MATCH (empty text)\n");
        return -1;
    }

    char lower[512];
    str_tolower(lower, post_wake_text, sizeof(lower));

    /* Check pattern: "start timer" or "set timer" or "timer of" */
    if (strstr(lower, "start timer") || strstr(lower, "set timer") || strstr(lower, "timer")) {
        int duration = parse_duration_to_seconds(lower);
        if (duration > 0) {
            printf("[command_router] 🎯 MATCHED: Timer command detected for %d seconds\n", duration);
            EventTimerStart payload = {.duration_seconds = duration};
            Event start_event = {
                .type = EVENT_TIMER_START_REQUESTED,
                .data = &payload,
                .data_size = sizeof(payload)
            };
            event_bus_publish(start_event);
            return duration;
        }
    }

    printf("[command_router] NO MATCH: \"%s\"\n", post_wake_text);
    return -1;
}

static void on_wake_word_detected(const Event *e) {
    if (!e || !e->data) return;
    const EventWakeWordDetected *payload = (const EventWakeWordDetected *)e->data;
    command_router_route_text(payload->remaining_text);
}

int command_router_init(void) {
    event_bus_subscribe(EVENT_WAKE_WORD_DETECTED, on_wake_word_detected);
    return 0;
}

void command_router_shutdown(void) {
    /* No resources to free */
}

const Brick g_brick_command_router = {
    .brick_id = "command_router",
    .version = 1,
    .init = command_router_init,
    .process = NULL,
    .shutdown = command_router_shutdown
};
