#ifndef ROSE_AUDIO_CAPTURE_H
#define ROSE_AUDIO_CAPTURE_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  audio_capture_init(void);
void audio_capture_shutdown(void);
int  audio_capture_is_running(void);

extern const Brick g_brick_audio_capture;

#endif /* ROSE_AUDIO_CAPTURE_H */
