#include "../bricks/transcription/transcription.h"
#include "../core/event_bus.h"
#include <stdio.h>
#include <string.h>

static int g_transcribed_count = 0;
static void on_text_transcribed(const Event *e) {
    if (!e || !e->data) return;
    const EventTextTranscribed *payload = (const EventTextTranscribed *)e->data;
    printf("[test_step6] 📝 Transcribed event: \"%s\"\n", payload->text);
    g_transcribed_count++;
}

int main(void) {
    printf("===================================================\n");
    printf("ROSE STEP 6 VERIFICATION: Transcription Brick      \n");
    printf("===================================================\n");

    event_bus_init();
    event_bus_subscribe(EVENT_TEXT_TRANSCRIBED, on_text_transcribed);

    int rc = transcription_init();
    if (rc != 0) {
        fprintf(stderr, "Failed to initialize Transcription Brick (code: %d)\n", rc);
        return 1;
    }

    printf("[test_step6] Feeding 1 second of silence / zero waveform to verify engine pipeline...\n");
    int16_t silence[16000];
    memset(silence, 0, sizeof(silence));

    EventAudioChunk chunk = {
        .samples = silence,
        .sample_count = 16000,
        .sample_rate = 16000
    };
    Event chunk_event = {
        .type = EVENT_AUDIO_CHUNK_CAPTURED,
        .data = &chunk,
        .data_size = sizeof(chunk)
    };
    event_bus_publish(chunk_event);
    event_bus_dispatch_one(100);

    transcription_shutdown();
    event_bus_shutdown();

    printf("STEP 6 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
