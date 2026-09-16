/**
 * backend.h - Abstract Input Backend Interface
 */

#ifndef KEYBOARD_LOCK_BACKEND_H
#define KEYBOARD_LOCK_BACKEND_H

#include <stdbool.h>
#include "state_machine.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct backend backend_t;

struct backend {
    const char *name;
    const char *description;

    /**
     * Initialize backend with target hotkey code and epoll instance.
     */
    int (*init)(backend_t *b, int target_keycode, int epoll_fd);

    /**
     * Run a single step/event loop tick or handle epoll fd event.
     */
    int (*handle_fd_event)(backend_t *b, int fd, uint32_t events, bool *should_stop);

    /**
     * Lock the keyboard (apply kernel/X11 grabs).
     */
    int (*lock)(backend_t *b);

    /**
     * Unlock the keyboard (release kernel/X11 grabs).
     */
    int (*unlock)(backend_t *b);

    /**
     * Get count of tracked/grabbed keyboard devices.
     */
    int (*get_device_count)(const backend_t *b);

    /**
     * Clean up and release all resources.
     */
    void (*cleanup)(backend_t *b);

    /* Pointer to state machine owned by backend */
    state_machine_t *sm;

    /* Backend-specific private data */
    void *priv;
};

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_BACKEND_H */
