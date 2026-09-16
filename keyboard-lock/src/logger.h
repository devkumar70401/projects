/**
 * logger.h - Privacy-First System Logger for Keyboard Lock
 *
 * CRITICAL SECURITY & PRIVACY GUARANTEE:
 * This logger NEVER records, stores, prints, or transmits individual keystrokes,
 * characters, or typed text. It only logs high-level system state transitions,
 * device attachment events, errors, and daemon lifecycle events.
 */

#ifndef KEYBOARD_LOCK_LOGGER_H
#define KEYBOARD_LOCK_LOGGER_H

#include <stdarg.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    LOG_LEVEL_DEBUG = 0,
    LOG_LEVEL_INFO  = 1,
    LOG_LEVEL_WARN  = 2,
    LOG_LEVEL_ERROR = 3,
    LOG_LEVEL_NONE  = 4
} log_level_t;

/**
 * Initialize logger with minimum log level and destination.
 */
void logger_init(log_level_t min_level, bool use_syslog, bool quiet);

/**
 * Clean up logger resources.
 */
void logger_cleanup(void);

/**
 * Set minimum logging level at runtime.
 */
void logger_set_level(log_level_t level);

/**
 * Core logging functions.
 */
void log_msg(log_level_t level, const char *fmt, ...);

#define log_debug(...) log_msg(LOG_LEVEL_DEBUG, __VA_ARGS__)
#define log_info(...)  log_msg(LOG_LEVEL_INFO,  __VA_ARGS__)
#define log_warn(...)  log_msg(LOG_LEVEL_WARN,  __VA_ARGS__)
#define log_error(...) log_msg(LOG_LEVEL_ERROR, __VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_LOGGER_H */
