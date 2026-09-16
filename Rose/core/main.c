#define _POSIX_C_SOURCE 200809L
#include "event_bus.h"
#include "brick.h"
#include "../bricks/tone_player/tone_player.h"
#include "../subsystems/timer/timer_subsystem.h"
#include "../bricks/wake_word/wake_word.h"
#include "../bricks/command_router/command_router.h"
#include "../bricks/transcription/transcription.h"
#include "../bricks/audio_capture/audio_capture.h"

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>

static volatile int g_keep_running = 1;

static void handle_sigint(int sig) {
    (void)sig;
    printf("\n[ROSE CORE] Shutdown signal received. Gracefully stopping bricks...\n");
    g_keep_running = 0;
    event_bus_stop();
}

int main(int argc, char *argv[]) {
    (void)argc;
    (void)argv;

    signal(SIGINT, handle_sigint);
    signal(SIGTERM, handle_sigint);
    /* BUG #4(a) FIX: Ignore SIGPIPE so broken audio pipes do not terminate the process */
    signal(SIGPIPE, SIG_IGN);

    printf("====================================================================\n");
    printf("🌹 ROSE — MODULAR VOICE-ACTIVATED SYSTEM (v0.1 Timer Milestone) 🌹\n");
    printf("====================================================================\n");
    printf("Architecture: Pure C Lego-Brick Event-Driven Kernel\n");
    printf("Wake word   : \"Rose\"\n");
    printf("Command     : \"start timer of <N> seconds/minutes/hours\"\n");
    printf("Status      : Booting bricks...\n\n");

    /* 1. Core Event Bus */
    if (event_bus_init() != 0) {
        fprintf(stderr, "[ROSE CORE] FATAL: Failed to initialize Event Bus.\n");
        return 1;
    }
    printf("  [✓] Event Bus initialized\n");

    /* 2. Tone Player Brick (Non-critical) */
    if (tone_player_init() != 0) {
        fprintf(stderr, "[ROSE CORE] Warning: Tone Player failed to initialize.\n");
    } else {
        printf("  [✓] Tone Player Brick registered\n");
    }

    /* 3. Timer Subsystem (Non-critical) */
    if (timer_subsystem_init() != 0) {
        fprintf(stderr, "[ROSE CORE] Warning: Timer Subsystem failed to initialize.\n");
    } else {
        printf("  [✓] Timer Subsystem registered\n");
    }

    /* 4. Command Router Brick (Non-critical) */
    if (command_router_init() != 0) {
        fprintf(stderr, "[ROSE CORE] Warning: Command Router failed to initialize.\n");
    } else {
        printf("  [✓] Command Router Brick registered\n");
    }

    /* 5. Wake-Word Brick (Non-critical) */
    if (wake_word_init() != 0) {
        fprintf(stderr, "[ROSE CORE] Warning: Wake-Word Brick failed to initialize.\n");
    } else {
        printf("  [✓] Wake-Word Brick registered\n");
    }

    /* 6. Transcription Brick (CRITICAL) */
    if (transcription_init() != 0) {
        fprintf(stderr, "\n[ROSE CORE] FATAL: Transcription Brick failed to initialize — Rose cannot understand speech. Exiting.\n");
        timer_subsystem_shutdown();
        tone_player_shutdown();
        event_bus_shutdown();
        return 1;
    }
    printf("  [✓] Transcription Brick (Vosk C API) registered\n");

    /* 7. Audio Capture Brick (CRITICAL) */
    if (audio_capture_init() != 0) {
        fprintf(stderr, "\n[ROSE CORE] FATAL: Audio Capture Brick failed to initialize — Rose cannot hear speech. Exiting.\n");
        transcription_shutdown();
        timer_subsystem_shutdown();
        tone_player_shutdown();
        event_bus_shutdown();
        return 1;
    }
    printf("  [✓] Audio Capture Brick registered\n");

    printf("\n🌹 ROSE IS NOW LISTENING FOREVER. Press Ctrl+C to stop.\n");
    printf("--------------------------------------------------------------------\n");

    /* Main Kernel Loop: Event Bus Dispatcher */
    while (g_keep_running) {
        event_bus_dispatch_one(100);
    }

    /* Graceful Shutdown in reverse order */
    printf("[ROSE CORE] Terminating bricks...\n");
    audio_capture_shutdown();
    transcription_shutdown();
    wake_word_shutdown();
    command_router_shutdown();
    timer_subsystem_shutdown();
    tone_player_shutdown();
    event_bus_shutdown();

    printf("[ROSE CORE] All systems halted cleanly. Goodbye.\n");
    return 0;
}
