#define _POSIX_C_SOURCE 200809L
#include "tone_player.h"
#include "../../core/event_bus.h"
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include <time.h>
#include <fcntl.h>

#define SAMPLE_RATE 44100
#define PI 3.14159265358979323846

static void on_tone_requested(const Event *e) {
    (void)e;
    printf("\n[tone_player] >> 🔔 PLAYING NOTIFICATION RINGTONE! 🔔 <<\n");

    /* Repeating multi-pulse alarm cadence (~4.5s total duration) */
    struct timespec pulse_gap = {0, 120000000L};  /* 120ms between pulses */
    struct timespec burst_gap = {0, 650000000L};  /* 650ms between bursts */

    for (int burst = 0; burst < 2; burst++) {
        for (int pulse = 0; pulse < 4; pulse++) {
            /* Alternate between 880Hz (A5) and 1046Hz (C6) for high acoustic visibility */
            int freq = (pulse % 2 == 0) ? 880 : 1046;
            tone_player_play_tone(freq, 220);
            nanosleep(&pulse_gap, NULL);
        }
        if (burst < 1) {
            nanosleep(&burst_gap, NULL);
        }
    }
}

int tone_player_play_tone(int freq_hz, int duration_ms) {
    if (freq_hz <= 0) freq_hz = 880;
    if (duration_ms <= 0) duration_ms = 400;

    int total_samples = (int)((SAMPLE_RATE * duration_ms) / 1000);
    short *buffer = (short *)malloc(sizeof(short) * total_samples);
    if (!buffer) {
        printf("\a\a");
        fflush(stdout);
        return 0;
    }

    double duration_sec = (double)duration_ms / 1000.0;
    for (int i = 0; i < total_samples; i++) {
        double t = (double)i / SAMPLE_RATE;
        double envelope = exp(-4.0 * t / duration_sec);
        double sample = sin(2.0 * PI * freq_hz * t) * envelope;
        buffer[i] = (short)(sample * 28000);
    }

    /* BUG #4(b) FIX: Redirect stderr to /tmp/rose_audio_errors.log instead of /dev/null */
    FILE *audio_pipe = popen("aplay -f S16_LE -r 44100 -c 1 -q 2>>/tmp/rose_audio_errors.log || pw-cat -p --format=s16 --rate=44100 --channels=1 2>>/tmp/rose_audio_errors.log", "w");
    if (audio_pipe) {
        fwrite(buffer, sizeof(short), total_samples, audio_pipe);
        pclose(audio_pipe);
    } else {
        printf("\a\a");
        fflush(stdout);
    }

    free(buffer);
    return 0;
}

int tone_player_init(void) {
    event_bus_subscribe(EVENT_PLAY_TONE_REQUESTED, on_tone_requested);
    event_bus_subscribe(EVENT_TIMER_COMPLETED, on_tone_requested);
    return 0;
}

void tone_player_shutdown(void) {
    /* No persistent state to free */
}

const Brick g_brick_tone_player = {
    .brick_id = "tone_player",
    .version = 1,
    .init = tone_player_init,
    .process = NULL,
    .shutdown = tone_player_shutdown
};
