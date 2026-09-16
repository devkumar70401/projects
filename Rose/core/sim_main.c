#define _POSIX_C_SOURCE 200809L
#include "event_bus.h"
#include "brick.h"
#include "../bricks/tone_player/tone_player.h"
#include "../subsystems/timer/timer_subsystem.h"
#include "../subsystems/timer/duration_parser.h"
#include "../bricks/wake_word/wake_word.h"
#include "../bricks/command_router/command_router.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <time.h>
#include <pthread.h>

static volatile int g_sim_running = 1;

static void handle_sigint(int sig) {
    (void)sig;
    printf("\n[ROSE SIM] Stopping simulator...\n");
    g_sim_running = 0;
    event_bus_stop();
}

static void *event_dispatcher_thread(void *arg) {
    (void)arg;
    while (g_sim_running) {
        event_bus_dispatch_one(100);
    }
    return NULL;
}

static void simulate_spoken_text(const char *phrase) {
    EventTextTranscribed text_payload;
    strncpy(text_payload.text, phrase, sizeof(text_payload.text) - 1);
    text_payload.text[sizeof(text_payload.text) - 1] = '\0';
    text_payload.confidence = 1.0;

    Event text_event = {
        .type = EVENT_TEXT_TRANSCRIBED,
        .data = &text_payload,
        .data_size = sizeof(text_payload)
    };
    event_bus_publish(text_event);
}

int main(int argc, char *argv[]) {
    signal(SIGINT, handle_sigint);
    signal(SIGPIPE, SIG_IGN);

    printf("====================================================================\n");
    printf("🌹 ROSE — TIMER SUB-SYSTEM INTERACTIVE SIMULATOR (No Mic Needed) 🌹\n");
    printf("====================================================================\n");
    printf("Type voice commands directly into the prompt (or 'exit' to quit).\n");
    printf("Example commands:\n");
    printf("  - Rose, start timer of 5 seconds\n");
    printf("  - start timer of 10 seconds\n");
    printf("  - Rose set timer for 1 minute\n");
    printf("====================================================================\n\n");

    /* 1. Initialize Event Bus and Subsystems */
    event_bus_init();
    tone_player_init();
    timer_subsystem_init();
    command_router_init();
    wake_word_init();

    /* 2. Start background event dispatch thread */
    pthread_t dispatch_tid;
    if (pthread_create(&dispatch_tid, NULL, event_dispatcher_thread, NULL) != 0) {
        fprintf(stderr, "Failed to start event dispatcher thread.\n");
        return 1;
    }

    /* If command passed via CLI argument, execute directly */
    if (argc > 1) {
        char full_arg[512] = "";
        for (int i = 1; i < argc; i++) {
            strncat(full_arg, argv[i], sizeof(full_arg) - strlen(full_arg) - 1);
            if (i < argc - 1) strncat(full_arg, " ", sizeof(full_arg) - strlen(full_arg) - 1);
        }
        printf("rose-sim> Simulating voice input: \"%s\"\n", full_arg);
        
        int parsed_sec = parse_duration_to_seconds(full_arg);
        if (parsed_sec <= 0) parsed_sec = 5;

        simulate_spoken_text(full_arg);

        /* Wait for countdown and tone to finish */
        struct timespec ts = {0, 100000000L}; /* 100ms */
        int total_ticks = (parsed_sec + 2) * 10;
        for (int i = 0; i < total_ticks && g_sim_running; i++) {
            nanosleep(&ts, NULL);
        }
        g_sim_running = 0;
    } else {
        /* Interactive REPL loop */
        char input[512];
        while (g_sim_running) {
            printf("\nrose-sim> ");
            fflush(stdout);

            if (!fgets(input, sizeof(input), stdin)) {
                break;
            }

            size_t len = strlen(input);
            if (len > 0 && input[len - 1] == '\n') {
                input[len - 1] = '\0';
            }

            if (strcmp(input, "exit") == 0 || strcmp(input, "quit") == 0) {
                break;
            }

            if (strlen(input) == 0) continue;

            simulate_spoken_text(input);
        }
    }

    g_sim_running = 0;
    event_bus_stop();
    pthread_join(dispatch_tid, NULL);

    timer_subsystem_shutdown();
    tone_player_shutdown();
    wake_word_shutdown();
    command_router_shutdown();
    event_bus_shutdown();

    printf("\n[ROSE SIM] Simulator exited cleanly.\n");
    return 0;
}
