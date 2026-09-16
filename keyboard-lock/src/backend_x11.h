/**
 * backend_x11.h - X11 Server Keyboard Grab Backend
 */

#ifndef KEYBOARD_LOCK_BACKEND_X11_H
#define KEYBOARD_LOCK_BACKEND_X11_H

#include "backend.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Check if X11 display is available and libX11.so.6 can be loaded.
 */
bool backend_x11_is_available(void);

/**
 * Instantiate and configure the X11 backend.
 */
backend_t* backend_x11_create(void);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_BACKEND_X11_H */
