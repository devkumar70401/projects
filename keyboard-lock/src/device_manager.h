/**
 * device_manager.h - Low-Level Linux Input Device Enumeration, Filtering & Grabbing
 *
 * Scans /dev/input/event* nodes, accurately distinguishes physical/virtual keyboards
 * from mice, touchpads, and other peripherals, and manages exclusive kernel grabs (EVIOCGRAB).
 */

#ifndef KEYBOARD_LOCK_DEVICE_MANAGER_H
#define KEYBOARD_LOCK_DEVICE_MANAGER_H

#include <stdbool.h>
#include <limits.h>
#include <linux/input.h>
#include "state_machine.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_INPUT_DEVICES 64

typedef struct {
    int fd;
    char path[PATH_MAX];
    char name[256];
    bool grabbed;
    bool is_keyboard;
} input_device_t;

typedef struct {
    input_device_t devices[MAX_INPUT_DEVICES];
    int count;
    int inotify_fd;
    int inotify_wd;
    int epoll_fd;
} device_manager_t;

/**
 * Initialize device manager and inotify monitor for /dev/input.
 */
int device_manager_init(device_manager_t *dm, int epoll_fd);

/**
 * Scan /dev/input and open all detected keyboard devices.
 * Returns number of keyboard devices found and opened.
 */
int device_manager_scan(device_manager_t *dm);

/**
 * Check if a given open file descriptor corresponds to a keyboard.
 * Fills name_buf if non-NULL.
 */
bool device_manager_is_keyboard_fd(int fd, char *name_buf, size_t name_buf_size);

/**
 * Add a new device by path (e.g. from inotify).
 * If current_state is STATE_LOCKED, automatically grabs the newly attached keyboard.
 */
int device_manager_add_device(device_manager_t *dm, const char *path, lock_state_t current_state);

/**
 * Remove a device by file descriptor.
 */
int device_manager_remove_device_by_fd(device_manager_t *dm, int fd);

/**
 * Remove a device by path.
 */
int device_manager_remove_device_by_path(device_manager_t *dm, const char *path);

/**
 * Apply exclusive kernel grab (EVIOCGRAB 1) to all active keyboard devices.
 * Prevents any keyboard events from reaching compositor or other applications.
 */
int device_manager_grab_all(device_manager_t *dm);

/**
 * Release exclusive kernel grab (EVIOCGRAB 0) on all active keyboard devices.
 * Restores normal keyboard event flow to compositor.
 */
int device_manager_ungrab_all(device_manager_t *dm);

/**
 * Handle inotify events on /dev/input (hotplug and unplug).
 */
int device_manager_handle_inotify(device_manager_t *dm, lock_state_t current_state);

/**
 * Clean up all open devices, release all grabs, close inotify.
 */
void device_manager_cleanup(device_manager_t *dm);

/**
 * Get count of active keyboard devices.
 */
int device_manager_get_count(const device_manager_t *dm);

#ifdef __cplusplus
}
#endif

#endif /* KEYBOARD_LOCK_DEVICE_MANAGER_H */
