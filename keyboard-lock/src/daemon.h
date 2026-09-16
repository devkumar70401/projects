/**
 * daemon.h - Main Daemon Event Loop & Signal Multiplexer
 */

#ifndef KEYBOARD_LOCK_DAEMON_H
#define KEYBOARD_LOCK_DAEMON_H

#include <stdbool.h>
#include "backend.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    int target_keycode;
    const char *backend_type; /* "auto", "evdev", or "x11" */
    bool daemonize;
    bool verbose;
    bool quiet;
    bool use_syslog;
} daemon_config_t;

/**
 * Run the keyboard-lock daemon process.
 * Blocks until termination signal or STOP IPC command is received.
 */
int daemon_run(const daemon_config_t *config);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_DAEMON_H */
