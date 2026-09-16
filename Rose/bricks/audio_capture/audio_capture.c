#define _POSIX_C_SOURCE 200809L
#include "audio_capture.h"
#include "../../core/event_bus.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <unistd.h>
#include <time.h>
#include <pthread.h>

#define SAMPLE_RATE 16000
#define CHUNK_DURATION_MS 100
#define SAMPLES_PER_CHUNK ((SAMPLE_RATE * CHUNK_DURATION_MS) / 1000) /* 1600 samples */

static pthread_t g_capture_thread;
static volatile int g_capture_running = 0;
static FILE *g_audio_pipe = NULL;

static void *audio_capture_worker(void *arg) {
    (void)arg;
    int16_t buffer[SAMPLES_PER_CHUNK];

    /* BUG #4(b) FIX: Redirect stderr to /tmp/rose_audio_errors.log instead of /dev/null */
    g_audio_pipe = popen("arecord -t raw -f S16_LE -r 16000 -c 1 -q 2>>/tmp/rose_audio_errors.log || pw-record --format=s16 --rate=16000 --channels=1 - 2>>/tmp/rose_audio_errors.log", "r");
    if (!g_audio_pipe) {
        fprintf(stderr, "[audio_capture] Error: Failed to spawn audio recording stream.\n");
        g_capture_running = 0;
        return NULL;
    }

    printf("[audio_capture] 🎙️ Audio capture streaming started (16kHz 16-bit Mono)...\n");

    while (g_capture_running) {
        size_t samples_read = fread(buffer, sizeof(int16_t), SAMPLES_PER_CHUNK, g_audio_pipe);
        if (samples_read == 0) {
            if (!g_capture_running) break;
            struct timespec ts = {0, 10000000L};
            nanosleep(&ts, NULL);
            continue;
        }

        EventAudioChunk chunk_payload = {
            .samples = buffer,
            .sample_count = samples_read,
            .sample_rate = SAMPLE_RATE
        };

        Event chunk_event = {
            .type = EVENT_AUDIO_CHUNK_CAPTURED,
            .data = &chunk_payload,
            .data_size = sizeof(chunk_payload)
        };
        event_bus_publish(chunk_event);
    }

    if (g_audio_pipe) {
        pclose(g_audio_pipe);
        g_audio_pipe = NULL;
    }

    printf("[audio_capture] Audio capture worker terminated.\n");
    return NULL;
}

int audio_capture_init(void) {
    if (g_capture_running) return 0;
    g_capture_running = 1;

    if (pthread_create(&g_capture_thread, NULL, audio_capture_worker, NULL) != 0) {
        g_capture_running = 0;
        return -1;
    }
    return 0;
}

void audio_capture_shutdown(void) {
    if (!g_capture_running) return;
    g_capture_running = 0;
    
    if (system("pkill -f 'arecord -t raw -f S16_LE' 2>/dev/null || true") != 0) {
        /* Ignore exit code */
    }
    pthread_join(g_capture_thread, NULL);
}

int audio_capture_is_running(void) {
    return g_capture_running;
}

const Brick g_brick_audio_capture = {
    .brick_id = "audio_capture",
    .version = 1,
    .init = audio_capture_init,
    .process = NULL,
    .shutdown = audio_capture_shutdown
};
