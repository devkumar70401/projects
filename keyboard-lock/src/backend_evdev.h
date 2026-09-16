/**
 * backend_evdev.h - Kernel evdev Input Interception Backend
 */

#ifndef KEYBOARD_LOCK_BACKEND_EVDEV_H
#define KEYBOARD_LOCK_BACKEND_EVDEV_H

#include "backend.h"
#include "device_manager.h"
#include "state_machine.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    device_manager_t dm;
    state_machine_t sm;
    int epoll_fd;
    int target_keycode;
} backend_evdev_priv_t;

/**
 * Instantiate and configure the evdev backend.
 */
backend_t* backend_evdev_create(void);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_BACKEND_EVDEV_H */
