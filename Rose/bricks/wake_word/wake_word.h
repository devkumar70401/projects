#ifndef ROSE_WAKE_WORD_H
#define ROSE_WAKE_WORD_H

#include "../../core/brick.h"
#include "../../core/events.h"

int  wake_word_init(void);
void wake_word_process_text(const char *text);
void wake_word_shutdown(void);

extern const Brick g_brick_wake_word;

#endif /* ROSE_WAKE_WORD_H */
