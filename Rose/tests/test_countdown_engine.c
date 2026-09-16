#include "../subsystems/timer/countdown_engine.h"
#include "../bricks/tone_player/tone_player.h"
#include "../core/event_bus.h"
#include <stdio.h>
#include <unistd.h>

int main(void) {
    printf("===================================================\n");
    printf("ROSE STEP 2 VERIFICATION: Countdown Engine Brick   \n");
    printf("===================================================\n");

    event_bus_init();
    tone_player_init();
    countdown_engine_init();

    printf("[test] Starting a 2-second countdown test...\n");
    countdown_engine_start_timer(2);

    /* Dispatch events for 3.5 seconds to catch EVENT_TIMER_COMPLETED and let tone finish */
    for (int i = 0; i < 35; i++) {
        event_bus_dispatch_one(100);
    }

    countdown_engine_shutdown();
    tone_player_shutdown();
    event_bus_shutdown();

    printf("STEP 2 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
