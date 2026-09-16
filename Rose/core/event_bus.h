#ifndef ROSE_EVENT_BUS_H
#define ROSE_EVENT_BUS_H

#include "events.h"

typedef void (*EventCallback)(const Event *e);

int  event_bus_init(void);
int  event_bus_subscribe(EventType type, EventCallback callback);
int  event_bus_publish(Event e);
int  event_bus_dispatch_one(int timeout_ms);
void event_bus_run(void);
void event_bus_stop(void);
void event_bus_shutdown(void);

#endif /* ROSE_EVENT_BUS_H */
