#include "../bricks/command_router/command_router.h"
#include "../subsystems/timer/duration_parser.h"
#include "../core/event_bus.h"
#include <stdio.h>
#include <assert.h>

static int g_timer_requested_seconds = 0;

static void on_timer_start_requested(const Event *e) {
    if (!e || !e->data) return;
    const EventTimerStart *payload = (const EventTimerStart *)e->data;
    g_timer_requested_seconds = payload->duration_seconds;
}

int main(void) {
    printf("===================================================\n");
    printf("ROSE STEP 8 VERIFICATION: Command Router Brick     \n");
    printf("===================================================\n");

    event_bus_init();
    command_router_init();
    event_bus_subscribe(EVENT_TIMER_START_REQUESTED, on_timer_start_requested);

    /* Test 1: Supported pattern "start timer of 10 minutes" */
    g_timer_requested_seconds = 0;
    int res = command_router_route_text("start timer of 10 minutes");
    event_bus_dispatch_one(50);
    if (res != 600 || g_timer_requested_seconds != 600) {
        fprintf(stderr, "[FAIL] Failed pattern 1: got res=%d, bus=%d\n", res, g_timer_requested_seconds);
        return 1;
    }
    printf("  [PASS] 'start timer of 10 minutes' matched 600s\n");

    /* Test 2: Supported pattern "start timer of 45 seconds" */
    g_timer_requested_seconds = 0;
    res = command_router_route_text("start timer of 45 seconds");
    event_bus_dispatch_one(50);
    if (res != 45 || g_timer_requested_seconds != 45) {
        fprintf(stderr, "[FAIL] Failed pattern 2: got res=%d, bus=%d\n", res, g_timer_requested_seconds);
        return 1;
    }
    printf("  [PASS] 'start timer of 45 seconds' matched 45s\n");

    /* Test 3: Unsupported pattern "what is the weather" */
    g_timer_requested_seconds = 0;
    res = command_router_route_text("what is the weather today");
    event_bus_dispatch_one(50);
    if (res != -1 || g_timer_requested_seconds != 0) {
        fprintf(stderr, "[FAIL] Failed negative pattern\n");
        return 1;
    }
    printf("  [PASS] Non-timer command rejected cleanly with NO MATCH.\n");

    command_router_shutdown();
    event_bus_shutdown();

    printf("STEP 8 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
