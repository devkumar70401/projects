#ifndef ROSE_TONE_PLAYER_H
#define ROSE_TONE_PLAYER_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  tone_player_init(void);
int  tone_player_play_tone(int freq_hz, int duration_ms);
void tone_player_shutdown(void);

extern const Brick g_brick_tone_player;

#endif /* ROSE_TONE_PLAYER_H */
