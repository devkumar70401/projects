#ifndef ROSE_TRANSCRIPTION_H
#define ROSE_TRANSCRIPTION_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  transcription_init(void);
int  transcription_init_with_model(const char *model_path);
void transcription_process_chunk(const int16_t *samples, size_t count);
void transcription_shutdown(void);

extern const Brick g_brick_transcription;

#endif /* ROSE_TRANSCRIPTION_H */
