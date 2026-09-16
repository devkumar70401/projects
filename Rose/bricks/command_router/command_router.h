#ifndef ROSE_COMMAND_ROUTER_H
#define ROSE_COMMAND_ROUTER_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  command_router_init(void);
int  command_router_route_text(const char *post_wake_text);
void command_router_shutdown(void);

extern const Brick g_brick_command_router;

#endif /* ROSE_COMMAND_ROUTER_H */
