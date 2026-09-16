#ifndef ROSE_COUNTDOWN_ENGINE_H
#define ROSE_COUNTDOWN_ENGINE_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  countdown_engine_init(void);
int  countdown_engine_start_timer(int duration_seconds);
void countdown_engine_shutdown(void);

extern const Brick g_brick_countdown_engine;

#endif /* ROSE_COUNTDOWN_ENGINE_H */
