#define _POSIX_C_SOURCE 200809L
#include "../core/event_bus.h"
#include "../bricks/tone_player/tone_player.h"
#include "../subsystems/timer/timer_subsystem.h"
#include "../subsystems/timer/duration_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static int g_dummy_event_received = 0;
static void on_dummy_event(const Event *e) {
    (void)e;
    g_dummy_event_received = 1;
    printf("[test_step4] Dummy event received on Event Bus.\n");
}

int main(int argc, char *argv[]) {
    printf("=========================================================\n");
    printf("ROSE STEP 4 VERIFICATION: Event Bus + Timer Subsystem    \n");
    printf("=========================================================\n");

    /* 1. Initialize Event Bus */
    event_bus_init();

    /* 2. Test Event Bus with dummy event */
    event_bus_subscribe(EVENT_PLAY_TONE_REQUESTED, on_dummy_event);
    Event dummy_event = {.type = EVENT_PLAY_TONE_REQUESTED, .data = NULL, .data_size = 0};
    event_bus_publish(dummy_event);
    event_bus_dispatch_one(50);
    if (!g_dummy_event_received) {
        fprintf(stderr, "[test_step4] Failed: Event Bus did not deliver dummy event.\n");
        return 1;
    }
    printf("[test_step4] Event Bus basic Pub/Sub verified.\n");

    /* 3. Initialize Bricks */
    tone_player_init();
    timer_subsystem_init();

    /* 4. Trigger via duration parser -> Event Bus */
    const char *test_command = (argc > 1) ? argv[1] : "3 seconds";
    printf("[test_step4] Simulating manual input: \"%s\"\n", test_command);

    int duration = parse_duration_to_seconds(test_command);
    if (duration <= 0) {
        fprintf(stderr, "[test_step4] Failed: could not parse duration from \"%s\"\n", test_command);
        return 1;
    }
    printf("[test_step4] Duration parsed: %d seconds. Publishing EVENT_TIMER_START_REQUESTED...\n", duration);

    EventTimerStart start_payload = {.duration_seconds = duration};
    Event start_event = {
        .type = EVENT_TIMER_START_REQUESTED,
        .data = &start_payload,
        .data_size = sizeof(start_payload)
    };
    event_bus_publish(start_event);

    /* 5. Dispatch events until countdown completes */
    printf("[test_step4] Running Event Bus dispatch loop for %d seconds...\n", duration + 2);
    for (int i = 0; i < (duration + 2) * 10; i++) {
        event_bus_dispatch_one(100);
    }

    /* 6. Cleanup */
    timer_subsystem_shutdown();
    tone_player_shutdown();
    event_bus_shutdown();

    printf("STEP 4 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
