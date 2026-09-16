#ifndef ROSE_TIMER_SUBSYSTEM_H
#define ROSE_TIMER_SUBSYSTEM_H

#include "../../core/brick.h"
#include "countdown_engine.h"
#include "duration_parser.h"

int  timer_subsystem_init(void);
void timer_subsystem_shutdown(void);

extern const Brick g_brick_timer_subsystem;

#endif /* ROSE_TIMER_SUBSYSTEM_H */
