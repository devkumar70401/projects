#ifndef ROSE_EVENTS_H
#define ROSE_EVENTS_H

#include <stddef.h>
#include <stdint.h>

typedef enum {
    EVENT_AUDIO_CHUNK_CAPTURED,
    EVENT_TEXT_TRANSCRIBED,
    EVENT_WAKE_WORD_DETECTED,
    EVENT_TIMER_START_REQUESTED,
    EVENT_TIMER_TICK,
    EVENT_TIMER_COMPLETED,
    EVENT_PLAY_TONE_REQUESTED,
    EVENT_COUNT
} EventType;

/*
 * EventAudioChunk Ownership Contract:
 * When published via event_bus_publish(), the Event Bus performs a complete deep copy
 * of both this struct and the dynamically allocated samples array (sample_count * sizeof(int16_t)).
 * The Event Bus owns the lifecycle of the cloned sample buffer and frees it upon event dispatch.
 * Subscribers receive an immutable, isolated sample buffer guaranteed to be valid during callback.
 */
typedef struct {
    int16_t *samples;
    size_t   sample_count;
    int      sample_rate;
} EventAudioChunk;

typedef struct {
    char   text[512];
    double confidence;
} EventTextTranscribed;

typedef struct {
    char remaining_text[512];
} EventWakeWordDetected;

typedef struct {
    int duration_seconds;
} EventTimerStart;

typedef struct {
    int seconds_remaining;
} EventTimerTick;

typedef struct {
    EventType type;
    void      *data;      /* points to payload */
    size_t     data_size;
} Event;

#endif /* ROSE_EVENTS_H */
