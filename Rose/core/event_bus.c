#define _POSIX_C_SOURCE 200809L
#include "event_bus.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <time.h>

#define MAX_SUBSCRIBERS_PER_EVENT 16
#define MAX_QUEUE_SIZE 256

typedef struct {
    EventCallback callbacks[MAX_SUBSCRIBERS_PER_EVENT];
    int count;
} SubscriptionList;

typedef struct EventNode {
    Event event;
    struct EventNode *next;
} EventNode;

static SubscriptionList g_subscriptions[EVENT_COUNT];
static pthread_mutex_t g_sub_mutex = PTHREAD_MUTEX_INITIALIZER;

static EventNode *g_queue_head = NULL;
static EventNode *g_queue_tail = NULL;
static int g_queue_count = 0;
static pthread_mutex_t g_queue_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  g_queue_cond  = PTHREAD_COND_INITIALIZER;

static volatile int g_running = 0;

static void free_event_payload(Event *e) {
    if (!e || !e->data) return;
    if (e->type == EVENT_AUDIO_CHUNK_CAPTURED) {
        EventAudioChunk *chunk = (EventAudioChunk *)e->data;
        if (chunk->samples) {
            free(chunk->samples);
            chunk->samples = NULL;
        }
    }
    free(e->data);
    e->data = NULL;
}

int event_bus_init(void) {
    pthread_mutex_lock(&g_sub_mutex);
    for (int i = 0; i < EVENT_COUNT; i++) {
        g_subscriptions[i].count = 0;
    }
    pthread_mutex_unlock(&g_sub_mutex);

    pthread_mutex_lock(&g_queue_mutex);
    g_queue_head = NULL;
    g_queue_tail = NULL;
    g_queue_count = 0;
    g_running = 1;
    pthread_mutex_unlock(&g_queue_mutex);

    return 0;
}

int event_bus_subscribe(EventType type, EventCallback callback) {
    if (type < 0 || type >= EVENT_COUNT || callback == NULL) {
        return -1;
    }

    pthread_mutex_lock(&g_sub_mutex);
    SubscriptionList *list = &g_subscriptions[type];
    if (list->count >= MAX_SUBSCRIBERS_PER_EVENT) {
        pthread_mutex_unlock(&g_sub_mutex);
        return -2; /* List full */
    }

    list->callbacks[list->count++] = callback;
    pthread_mutex_unlock(&g_sub_mutex);
    return 0;
}

int event_bus_publish(Event e) {
    if (e.type < 0 || e.type >= EVENT_COUNT) {
        return -1;
    }

    EventNode *node = (EventNode *)malloc(sizeof(EventNode));
    if (!node) {
        return -2;
    }

    node->event.type = e.type;
    node->event.data_size = e.data_size;
    node->next = NULL;

    if (e.data != NULL && e.data_size > 0) {
        node->event.data = malloc(e.data_size);
        if (!node->event.data) {
            free(node);
            return -2;
        }
        memcpy(node->event.data, e.data, e.data_size);

        /* BUG #3 FIX: Deep copy underlying audio sample array */
        if (e.type == EVENT_AUDIO_CHUNK_CAPTURED) {
            const EventAudioChunk *src_chunk = (const EventAudioChunk *)e.data;
            EventAudioChunk *dst_chunk = (EventAudioChunk *)node->event.data;
            if (src_chunk->samples != NULL && src_chunk->sample_count > 0) {
                size_t sample_bytes = src_chunk->sample_count * sizeof(int16_t);
                dst_chunk->samples = (int16_t *)malloc(sample_bytes);
                if (dst_chunk->samples) {
                    memcpy(dst_chunk->samples, src_chunk->samples, sample_bytes);
                } else {
                    dst_chunk->sample_count = 0;
                }
            } else {
                dst_chunk->samples = NULL;
                dst_chunk->sample_count = 0;
            }
        }
    } else {
        node->event.data = NULL;
    }

    pthread_mutex_lock(&g_queue_mutex);
    if (g_queue_count >= MAX_QUEUE_SIZE) {
        pthread_mutex_unlock(&g_queue_mutex);
        free_event_payload(&node->event);
        free(node);
        return -3;
    }

    if (g_queue_tail) {
        g_queue_tail->next = node;
        g_queue_tail = node;
    } else {
        g_queue_head = node;
        g_queue_tail = node;
    }
    g_queue_count++;

    pthread_cond_signal(&g_queue_cond);
    pthread_mutex_unlock(&g_queue_mutex);
    return 0;
}

int event_bus_dispatch_one(int timeout_ms) {
    pthread_mutex_lock(&g_queue_mutex);

    while (g_queue_head == NULL && g_running) {
        if (timeout_ms < 0) {
            pthread_cond_wait(&g_queue_cond, &g_queue_mutex);
        } else if (timeout_ms == 0) {
            pthread_mutex_unlock(&g_queue_mutex);
            return 0;
        } else {
            struct timespec ts;
            clock_gettime(CLOCK_REALTIME, &ts);
            ts.tv_sec += timeout_ms / 1000;
            ts.tv_nsec += (timeout_ms % 1000) * 1000000L;
            if (ts.tv_nsec >= 1000000000L) {
                ts.tv_sec += 1;
                ts.tv_nsec -= 1000000000L;
            }
            int rc = pthread_cond_timedwait(&g_queue_cond, &g_queue_mutex, &ts);
            if (rc != 0 && g_queue_head == NULL) {
                pthread_mutex_unlock(&g_queue_mutex);
                return 0;
            }
        }
    }

    if (!g_running && g_queue_head == NULL) {
        pthread_mutex_unlock(&g_queue_mutex);
        return -1;
    }

    EventNode *node = g_queue_head;
    if (node) {
        g_queue_head = node->next;
        if (g_queue_head == NULL) {
            g_queue_tail = NULL;
        }
        g_queue_count--;
    }
    pthread_mutex_unlock(&g_queue_mutex);

    if (!node) {
        return 0;
    }

    /* Dispatch to subscribers */
    EventCallback cbs[MAX_SUBSCRIBERS_PER_EVENT];
    int count = 0;

    pthread_mutex_lock(&g_sub_mutex);
    if (node->event.type >= 0 && node->event.type < EVENT_COUNT) {
        count = g_subscriptions[node->event.type].count;
        for (int i = 0; i < count; i++) {
            cbs[i] = g_subscriptions[node->event.type].callbacks[i];
        }
    }
    pthread_mutex_unlock(&g_sub_mutex);

    for (int i = 0; i < count; i++) {
        if (cbs[i]) {
            cbs[i](&node->event);
        }
    }

    free_event_payload(&node->event);
    free(node);

    return 1;
}

void event_bus_run(void) {
    while (g_running) {
        int res = event_bus_dispatch_one(100);
        if (res < 0) {
            break;
        }
    }
}

void event_bus_stop(void) {
    pthread_mutex_lock(&g_queue_mutex);
    g_running = 0;
    pthread_cond_broadcast(&g_queue_cond);
    pthread_mutex_unlock(&g_queue_mutex);
}

void event_bus_shutdown(void) {
    event_bus_stop();

    pthread_mutex_lock(&g_queue_mutex);
    EventNode *cur = g_queue_head;
    while (cur) {
        EventNode *next = cur->next;
        free_event_payload(&cur->event);
        free(cur);
        cur = next;
    }
    g_queue_head = NULL;
    g_queue_tail = NULL;
    g_queue_count = 0;
    pthread_mutex_unlock(&g_queue_mutex);
}
