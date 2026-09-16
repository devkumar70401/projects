#define _POSIX_C_SOURCE 200809L
#include "transcription.h"
#include "../../core/event_bus.h"
#include "../../third_party/vosk/vosk_api.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <libgen.h>

static VoskModel *g_model = NULL;
static VoskRecognizer *g_recognizer = NULL;

/* Model path is resolved dynamically relative to executable's directory via /proc/self/exe */
static void resolve_default_model_path(char *out_path, size_t max_len) {
    char exe_path[512] = {0};
    ssize_t len = readlink("/proc/self/exe", exe_path, sizeof(exe_path) - 1);
    if (len > 0) {
        exe_path[len] = '\0';
        char *dir = dirname(exe_path);
        if (dir) {
            snprintf(out_path, max_len, "%s/../models/vosk-model-small-en-us", dir);
            /* Check if directory exists */
            if (access(out_path, F_OK) == 0) {
                return;
            }
        }
    }
    /* Fallback: relative to current working directory */
    snprintf(out_path, max_len, "./models/vosk-model-small-en-us");
}

static void extract_json_text(const char *json_str, char *out_text, size_t max_len) {
    out_text[0] = '\0';
    if (!json_str) return;

    const char *text_key = strstr(json_str, "\"text\" :");
    if (!text_key) text_key = strstr(json_str, "\"text\":");
    if (!text_key) text_key = strstr(json_str, "\"partial\" :");
    if (!text_key) text_key = strstr(json_str, "\"partial\":");

    if (text_key) {
        const char *first_quote = strchr(text_key + 6, '\"');
        if (first_quote) {
            const char *second_quote = strchr(first_quote + 1, '\"');
            if (second_quote) {
                size_t len = second_quote - (first_quote + 1);
                if (len >= max_len) len = max_len - 1;
                strncpy(out_text, first_quote + 1, len);
                out_text[len] = '\0';
            }
        }
    }
}

void transcription_process_chunk(const int16_t *samples, size_t count) {
    if (!g_recognizer || !samples || count == 0) return;

    int is_final = vosk_recognizer_accept_waveform_s(g_recognizer, samples, (int)count);
    const char *result_json = NULL;

    if (is_final) {
        result_json = vosk_recognizer_result(g_recognizer);
    } else {
        result_json = vosk_recognizer_partial_result(g_recognizer);
    }

    if (result_json) {
        char recognized_text[512];
        extract_json_text(result_json, recognized_text, sizeof(recognized_text));

        if (strlen(recognized_text) > 0) {
            EventTextTranscribed text_payload;
            strncpy(text_payload.text, recognized_text, sizeof(text_payload.text) - 1);
            text_payload.text[sizeof(text_payload.text) - 1] = '\0';
            text_payload.confidence = 1.0;

            Event text_event = {
                .type = EVENT_TEXT_TRANSCRIBED,
                .data = &text_payload,
                .data_size = sizeof(text_payload)
            };
            event_bus_publish(text_event);
        }
    }
}

static void on_audio_chunk(const Event *e) {
    if (!e || !e->data) return;
    const EventAudioChunk *chunk = (const EventAudioChunk *)e->data;
    transcription_process_chunk(chunk->samples, chunk->sample_count);
}

int transcription_init_with_model(const char *model_path) {
    vosk_set_log_level(-1); /* Suppress verbose engine logs */

    char resolved_path[512];
    if (!model_path) {
        resolve_default_model_path(resolved_path, sizeof(resolved_path));
        model_path = resolved_path;
    }

    g_model = vosk_model_new(model_path);
    if (!g_model) {
        fprintf(stderr, "[transcription] Error: Failed to load Vosk model at %s\n", model_path);
        return -1;
    }

    /*
     * Grammar-constrained recognizer (Option A):
     * Restricts the decoding lattice to only valid individual dictionary words.
     * MUST stay in strict synchronization with subsystems/timer/duration_parser.c.
     */
    const char *grammar =
        "[\"rose\", \"start\", \"set\", \"timer\", \"of\", \"for\", "
        "\"one\", \"two\", \"three\", \"four\", \"five\", \"six\", "
        "\"seven\", \"eight\", \"nine\", \"ten\", \"eleven\", \"twelve\", "
        "\"fifteen\", \"twenty\", \"thirty\", \"forty\", \"fifty\", "
        "\"sixty\", \"second\", \"seconds\", \"minute\", \"minutes\", "
        "\"hour\", \"hours\", \"[unk]\"]";

    g_recognizer = vosk_recognizer_new_grm(g_model, 16000.0f, grammar);
    if (!g_recognizer) {
        fprintf(stderr, "[transcription] Error: Failed to create grammar-constrained Vosk recognizer.\n");
        vosk_model_free(g_model);
        g_model = NULL;
        return -2;
    }

    event_bus_subscribe(EVENT_AUDIO_CHUNK_CAPTURED, on_audio_chunk);
    printf("[transcription] 🧠 Vosk STT engine initialized (Grammar-Constrained) with model: %s\n", model_path);
    return 0;
}

int transcription_init(void) {
    return transcription_init_with_model(NULL);
}

void transcription_shutdown(void) {
    if (g_recognizer) {
        vosk_recognizer_free(g_recognizer);
        g_recognizer = NULL;
    }
    if (g_model) {
        vosk_model_free(g_model);
        g_model = NULL;
    }
}

const Brick g_brick_transcription = {
    .brick_id = "transcription",
    .version = 1,
    .init = transcription_init,
    .process = NULL,
    .shutdown = transcription_shutdown
};
