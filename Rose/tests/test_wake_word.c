#include "../bricks/wake_word/wake_word.h"
#include "../core/event_bus.h"
#include <stdio.h>
#include <string.h>

static int g_wake_detected = 0;
static char g_last_remaining[512] = "";

static void on_wake_detected(const Event *e) {
    if (!e || !e->data) return;
    const EventWakeWordDetected *payload = (const EventWakeWordDetected *)e->data;
    g_wake_detected++;
    strncpy(g_last_remaining, payload->remaining_text, sizeof(g_last_remaining) - 1);
}

int main(void) {
    printf("===================================================\n");
    printf("ROSE STEP 7 VERIFICATION: Wake-Word Brick          \n");
    printf("===================================================\n");

    event_bus_init();
    wake_word_init();
    event_bus_subscribe(EVENT_WAKE_WORD_DETECTED, on_wake_detected);

    /* Test 1: Negative match (no "rose") */
    wake_word_process_text("hello there world");
    event_bus_dispatch_one(50);
    if (g_wake_detected != 0) {
        fprintf(stderr, "[FAIL] False positive on 'hello there world'\n");
        return 1;
    }
    printf("  [PASS] Negative match ignored correctly.\n");

    /* Test 2: Positive match with command */
    wake_word_process_text("Rose start timer of 10 minutes");
    event_bus_dispatch_one(50);
    if (g_wake_detected != 1 || strcmp(g_last_remaining, "start timer of 10 minutes") != 0) {
        fprintf(stderr, "[FAIL] Failed to detect or extract remaining text (got '%s')\n", g_last_remaining);
        return 1;
    }
    printf("  [PASS] Positive match caught. Remaining text: \"%s\"\n", g_last_remaining);

    /* Test 3: Mixed case with punctuation */
    wake_word_process_text("Hey, ROSE, start timer of 5 seconds");
    event_bus_dispatch_one(50);
    if (g_wake_detected != 2 || strcmp(g_last_remaining, "start timer of 5 seconds") != 0) {
        fprintf(stderr, "[FAIL] Failed mixed case match (got '%s')\n", g_last_remaining);
        return 1;
    }
    printf("  [PASS] Mixed case match caught. Remaining text: \"%s\"\n", g_last_remaining);

    wake_word_shutdown();
    event_bus_shutdown();

    printf("STEP 7 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
