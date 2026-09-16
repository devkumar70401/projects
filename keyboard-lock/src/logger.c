/**
 * logger.c - Privacy-First System Logger Implementation
 */

#include "logger.h"
#include <stdio.h>
#include <time.h>
#include <syslog.h>
#include <unistd.h>
#include <pthread.h>

static log_level_t g_min_level = LOG_LEVEL_INFO;
static bool g_use_syslog = false;
static bool g_quiet = false;
static pthread_mutex_t g_log_mutex = PTHREAD_MUTEX_INITIALIZER;

static const char* level_to_str(log_level_t level) {
    switch (level) {
        case LOG_LEVEL_DEBUG: return "DEBUG";
        case LOG_LEVEL_INFO:  return "INFO ";
        case LOG_LEVEL_WARN:  return "WARN ";
        case LOG_LEVEL_ERROR: return "ERROR";
        default:              return "LOG  ";
    }
}

static int level_to_syslog_priority(log_level_t level) {
    switch (level) {
        case LOG_LEVEL_DEBUG: return LOG_DEBUG;
        case LOG_LEVEL_INFO:  return LOG_INFO;
        case LOG_LEVEL_WARN:  return LOG_WARNING;
        case LOG_LEVEL_ERROR: return LOG_ERR;
        default:              return LOG_INFO;
    }
}

void logger_init(log_level_t min_level, bool use_syslog, bool quiet) {
    g_min_level = min_level;
    g_use_syslog = use_syslog;
    g_quiet = quiet;

    if (g_use_syslog) {
        openlog("keyboard-lock", LOG_PID | LOG_NDELAY, LOG_USER);
    }
}

void logger_cleanup(void) {
    if (g_use_syslog) {
        closelog();
    }
}

void logger_set_level(log_level_t level) {
    g_min_level = level;
}

void log_msg(log_level_t level, const char *fmt, ...) {
    if (level < g_min_level || g_quiet || level >= LOG_LEVEL_NONE) {
        return;
    }

    pthread_mutex_lock(&g_log_mutex);

    /* Get current local timestamp */
    time_t now = time(NULL);
    struct tm tm_buf;
    localtime_r(&now, &tm_buf);
    char time_str[32];
    strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", &tm_buf);

    char msg_buf[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(msg_buf, sizeof(msg_buf), fmt, args);
    va_end(args);

    if (g_use_syslog) {
        syslog(level_to_syslog_priority(level), "%s", msg_buf);
    }

    /* Output to stderr */
    fprintf(stderr, "[%s] [%s] %s\n", time_str, level_to_str(level), msg_buf);
    fflush(stderr);

    pthread_mutex_unlock(&g_log_mutex);
}
