#include "timer_subsystem.h"
#include <stdio.h>

int timer_subsystem_init(void) {
    int rc = countdown_engine_init();
    if (rc != 0) {
        fprintf(stderr, "[timer_subsystem] Failed to init countdown engine: %d\n", rc);
        return rc;
    }
    return 0;
}

void timer_subsystem_shutdown(void) {
    countdown_engine_shutdown();
}

const Brick g_brick_timer_subsystem = {
    .brick_id = "timer_subsystem",
    .version = 1,
    .init = timer_subsystem_init,
    .process = NULL,
    .shutdown = timer_subsystem_shutdown
};
