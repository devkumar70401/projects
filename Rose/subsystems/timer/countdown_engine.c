#define _POSIX_C_SOURCE 200809L
#include "countdown_engine.h"
#include "../../core/event_bus.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
#include <pthread.h>

typedef struct {
    int duration_seconds;
    int is_active;
    pthread_t thread;
} TimerState;

static TimerState g_timer = {0, 0, 0};
static pthread_mutex_t g_timer_mutex = PTHREAD_MUTEX_INITIALIZER;
static volatile int g_engine_running = 0;

static void *timer_worker_thread(void *arg) {
    int duration = (int)(intptr_t)arg;
    struct timespec start, now;
    clock_gettime(CLOCK_MONOTONIC, &start);

    printf("[countdown_engine] ⏳ Timer started for %d seconds...\n", duration);

    while (g_engine_running) {
        clock_gettime(CLOCK_MONOTONIC, &now);
        double elapsed = (now.tv_sec - start.tv_sec) + (now.tv_nsec - start.tv_nsec) / 1e9;
        int remaining = duration - (int)elapsed;

        if (remaining <= 0) {
            printf("[countdown_engine] ⏰ Timer finished! Publishing EVENT_TIMER_COMPLETED.\n");
            Event comp_event = {
                .type = EVENT_TIMER_COMPLETED,
                .data = NULL,
                .data_size = 0
            };
            event_bus_publish(comp_event);
            break;
        }

        /* Tick interval: sleep for 100ms */
        struct timespec sleep_time = {0, 100000000L};
        nanosleep(&sleep_time, NULL);
    }

    pthread_mutex_lock(&g_timer_mutex);
    g_timer.is_active = 0;
    pthread_mutex_unlock(&g_timer_mutex);

    return NULL;
}

int countdown_engine_start_timer(int duration_seconds) {
    if (duration_seconds <= 0) {
        fprintf(stderr, "[countdown_engine] Error: Invalid duration %d\n", duration_seconds);
        return -1;
    }

    pthread_mutex_lock(&g_timer_mutex);
    if (g_timer.is_active) {
        printf("[countdown_engine] Warning: Overwriting active timer.\n");
    }
    g_timer.duration_seconds = duration_seconds;
    g_timer.is_active = 1;

    if (pthread_create(&g_timer.thread, NULL, timer_worker_thread, (void *)(intptr_t)duration_seconds) != 0) {
        g_timer.is_active = 0;
        pthread_mutex_unlock(&g_timer_mutex);
        return -2;
    }
    pthread_detach(g_timer.thread);
    pthread_mutex_unlock(&g_timer_mutex);

    return 0;
}

static void on_timer_start_requested(const Event *e) {
    if (!e || !e->data) return;
    const EventTimerStart *payload = (const EventTimerStart *)e->data;
    printf("[countdown_engine] Received EVENT_TIMER_START_REQUESTED: %d seconds\n", payload->duration_seconds);
    countdown_engine_start_timer(payload->duration_seconds);
}

int countdown_engine_init(void) {
    g_engine_running = 1;
    event_bus_subscribe(EVENT_TIMER_START_REQUESTED, on_timer_start_requested);
    return 0;
}

void countdown_engine_shutdown(void) {
    g_engine_running = 0;
}

const Brick g_brick_countdown_engine = {
    .brick_id = "countdown_engine",
    .version = 1,
    .init = countdown_engine_init,
    .process = NULL,
    .shutdown = countdown_engine_shutdown
};
