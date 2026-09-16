/**
 * device_manager.c - Linux Input Device Management & Filtering Implementation
 */

#include "device_manager.h"
#include "logger.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/inotify.h>
#include <sys/epoll.h>

#define BITS_PER_LONG (sizeof(unsigned long) * 8)
#define NBITS(x) ((((x)-1)/BITS_PER_LONG)+1)
#define OFF(x) ((x)%BITS_PER_LONG)
#define BIT(x) (1UL<<OFF(x))
#define LONG(x) ((x)/BITS_PER_LONG)
#define test_bit(bit, array) ((array[LONG(bit)] >> OFF(bit)) & 1UL)

static int add_fd_to_epoll(int epoll_fd, int fd) {
    if (epoll_fd < 0 || fd < 0) return 0;
    struct epoll_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.events = EPOLLIN;
    ev.data.fd = fd;
    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, fd, &ev) < 0) {
        if (errno != EEXIST) {
            log_warn("Failed to add fd %d to epoll: %s", fd, strerror(errno));
            return -1;
        }
    }
    return 0;
}

static void remove_fd_from_epoll(int epoll_fd, int fd) {
    if (epoll_fd < 0 || fd < 0) return;
    epoll_ctl(epoll_fd, EPOLL_CTL_DEL, fd, NULL);
}

int device_manager_init(device_manager_t *dm, int epoll_fd) {
    if (!dm) return -1;
    memset(dm, 0, sizeof(*dm));
    dm->epoll_fd = epoll_fd;

    dm->inotify_fd = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
    if (dm->inotify_fd >= 0) {
        dm->inotify_wd = inotify_add_watch(dm->inotify_fd, "/dev/input", IN_CREATE | IN_DELETE);
        if (dm->inotify_wd < 0) {
            log_warn("inotify_add_watch(/dev/input) failed: %s (hotplug disabled)", strerror(errno));
        } else {
            add_fd_to_epoll(dm->epoll_fd, dm->inotify_fd);
            log_debug("inotify monitor initialized for /dev/input");
        }
    } else {
        log_warn("inotify_init1 failed: %s (hotplug disabled)", strerror(errno));
    }

    return 0;
}

bool device_manager_is_keyboard_fd(int fd, char *name_buf, size_t name_buf_size) {
    if (fd < 0) return false;

    char name[256] = "Unknown Device";
    if (ioctl(fd, EVIOCGNAME(sizeof(name)), name) < 0) {
        snprintf(name, sizeof(name), "Unknown Device");
    }
    if (name_buf && name_buf_size > 0) {
        snprintf(name_buf, name_buf_size, "%s", name);
    }

    unsigned long ev_bits[NBITS(EV_MAX)];
    memset(ev_bits, 0, sizeof(ev_bits));
    if (ioctl(fd, EVIOCGBIT(0, sizeof(ev_bits)), ev_bits) < 0) {
        return false;
    }

    /* Device MUST support EV_KEY */
    if (!test_bit(EV_KEY, ev_bits)) {
        return false;
    }

    unsigned long key_bits[NBITS(KEY_MAX)];
    memset(key_bits, 0, sizeof(key_bits));
    if (ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(key_bits)), key_bits) < 0) {
        return false;
    }

    /* Check mouse/pointer buttons */
    bool has_mouse_btn = test_bit(BTN_LEFT, key_bits) ||
                         test_bit(BTN_RIGHT, key_bits) ||
                         test_bit(BTN_MIDDLE, key_bits) ||
                         test_bit(BTN_TOUCH, key_bits) ||
                         test_bit(BTN_TOOL_FINGER, key_bits);

    /* Check essential alphanumeric / control keys that indicate a true keyboard */
    bool has_alpha = test_bit(KEY_A, key_bits) &&
                     test_bit(KEY_Z, key_bits) &&
                     test_bit(KEY_1, key_bits) &&
                     test_bit(KEY_SPACE, key_bits) &&
                     test_bit(KEY_ENTER, key_bits) &&
                     test_bit(KEY_ESC, key_bits);

    /* Check if it is a keyboard extension (e.g. keypad, media keys) */
    bool has_keypad = test_bit(KEY_KP0, key_bits) && test_bit(KEY_KPENTER, key_bits);

    /* STRICT ISOLATION RULE:
     * If the device is a mouse, touchpad, or touchscreen without alpha keys, REJECT IT.
     */
    if (has_mouse_btn && !has_alpha) {
        log_debug("Device '%s' (fd %d) is a pointer/mouse/touchpad. Skipping.", name, fd);
        return false;
    }

    if (has_alpha || has_keypad) {
        log_debug("Device '%s' (fd %d) verified as keyboard.", name, fd);
        return true;
    }

    return false;
}

int device_manager_add_device(device_manager_t *dm, const char *path, lock_state_t current_state) {
    if (!dm || !path || dm->count >= MAX_INPUT_DEVICES) return -1;

    /* Check if already tracked */
    for (int i = 0; i < dm->count; i++) {
        if (strcmp(dm->devices[i].path, path) == 0) {
            return dm->devices[i].fd;
        }
    }

    int fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) {
        log_debug("Cannot open '%s': %s", path, strerror(errno));
        return -1;
    }

    char name[256];
    if (!device_manager_is_keyboard_fd(fd, name, sizeof(name))) {
        close(fd);
        return -1;
    }

    int idx = dm->count++;
    dm->devices[idx].fd = fd;
    snprintf(dm->devices[idx].path, sizeof(dm->devices[idx].path), "%s", path);
    snprintf(dm->devices[idx].name, sizeof(dm->devices[idx].name), "%s", name);
    dm->devices[idx].grabbed = false;
    dm->devices[idx].is_keyboard = true;

    add_fd_to_epoll(dm->epoll_fd, fd);

    log_info("Keyboard registered: [%s] at '%s' (fd %d)", name, path, fd);

    /* If currently in LOCKED state, immediately grab newly connected keyboard */
    if (current_state == STATE_LOCKED) {
        if (ioctl(fd, EVIOCGRAB, 1) == 0) {
            dm->devices[idx].grabbed = true;
            log_info("Exclusive kernel grab applied to new keyboard '%s'", name);
        } else {
            log_error("Failed to apply kernel grab on '%s': %s", name, strerror(errno));
        }
    }

    return fd;
}

int device_manager_remove_device_by_fd(device_manager_t *dm, int fd) {
    if (!dm || fd < 0) return -1;

    for (int i = 0; i < dm->count; i++) {
        if (dm->devices[i].fd == fd) {
            log_info("Keyboard disconnected: [%s] at '%s' (fd %d)",
                     dm->devices[i].name, dm->devices[i].path, fd);

            remove_fd_from_epoll(dm->epoll_fd, fd);
            if (dm->devices[i].grabbed) {
                ioctl(fd, EVIOCGRAB, 0);
            }
            close(fd);

            /* Shift remaining elements */
            for (int j = i; j < dm->count - 1; j++) {
                dm->devices[j] = dm->devices[j + 1];
            }
            dm->count--;
            return 0;
        }
    }
    return -1;
}

int device_manager_remove_device_by_path(device_manager_t *dm, const char *path) {
    if (!dm || !path) return -1;

    for (int i = 0; i < dm->count; i++) {
        if (strcmp(dm->devices[i].path, path) == 0) {
            return device_manager_remove_device_by_fd(dm, dm->devices[i].fd);
        }
    }
    return -1;
}

int device_manager_scan(device_manager_t *dm) {
    if (!dm) return -1;

    DIR *dir = opendir("/dev/input");
    if (!dir) {
        log_error("Failed to open /dev/input: %s", strerror(errno));
        return -1;
    }

    struct dirent *entry;
    int added = 0;

    while ((entry = readdir(dir)) != NULL) {
        if (strncmp(entry->d_name, "event", 5) == 0) {
            char path[PATH_MAX];
            snprintf(path, sizeof(path), "/dev/input/%s", entry->d_name);
            if (device_manager_add_device(dm, path, STATE_UNLOCKED) >= 0) {
                added++;
            }
        }
    }

    closedir(dir);
    log_info("Discovered %d active keyboard device(s)", dm->count);
    return added;
}

int device_manager_grab_all(device_manager_t *dm) {
    if (!dm) return -1;
    int success = 0;

    for (int i = 0; i < dm->count; i++) {
        if (!dm->devices[i].grabbed) {
            if (ioctl(dm->devices[i].fd, EVIOCGRAB, 1) == 0) {
                dm->devices[i].grabbed = true;
                success++;
                log_debug("Grabbed keyboard: %s", dm->devices[i].name);
            } else {
                log_error("Failed to grab keyboard '%s' (%s): %s",
                          dm->devices[i].name, dm->devices[i].path, strerror(errno));
            }
        }
    }

    log_info("Keyboard lock ENGAGED: Grabbed %d/%d keyboard device(s)", success, dm->count);
    return success;
}

int device_manager_ungrab_all(device_manager_t *dm) {
    if (!dm) return -1;
    int success = 0;

    for (int i = 0; i < dm->count; i++) {
        if (dm->devices[i].grabbed) {
            if (ioctl(dm->devices[i].fd, EVIOCGRAB, 0) == 0) {
                dm->devices[i].grabbed = false;
                success++;
                log_debug("Released grab on keyboard: %s", dm->devices[i].name);
            } else {
                log_warn("Failed to release grab on '%s': %s",
                         dm->devices[i].name, strerror(errno));
            }
        }
    }

    log_info("Keyboard lock RELEASED: Ungrabbed %d/%d keyboard device(s)", success, dm->count);
    return success;
}

int device_manager_handle_inotify(device_manager_t *dm, lock_state_t current_state) {
    if (!dm || dm->inotify_fd < 0) return 0;

    char buffer[4096] __attribute__ ((aligned(__alignof__(struct inotify_event))));
    ssize_t len = read(dm->inotify_fd, buffer, sizeof(buffer));
    if (len <= 0) return 0;

    const struct inotify_event *event;
    for (char *ptr = buffer; ptr < buffer + len;
         ptr += sizeof(struct inotify_event) + event->len) {

        event = (const struct inotify_event *)ptr;
        if (event->len > 0 && strncmp(event->name, "event", 5) == 0) {
            char path[PATH_MAX];
            snprintf(path, sizeof(path), "/dev/input/%s", event->name);

            if (event->mask & IN_CREATE) {
                log_debug("inotify: Detected new device node '%s'", path);
                /* Small delay to let kernel / udev finish device setup */
                usleep(50000); /* 50ms */
                device_manager_add_device(dm, path, current_state);
            } else if (event->mask & IN_DELETE) {
                log_debug("inotify: Detected removed device node '%s'", path);
                device_manager_remove_device_by_path(dm, path);
            }
        }
    }

    return 0;
}

void device_manager_cleanup(device_manager_t *dm) {
    if (!dm) return;

    /* Always release all grabs first */
    device_manager_ungrab_all(dm);

    for (int i = 0; i < dm->count; i++) {
        if (dm->epoll_fd >= 0) {
            remove_fd_from_epoll(dm->epoll_fd, dm->devices[i].fd);
        }
        close(dm->devices[i].fd);
    }
    dm->count = 0;

    if (dm->inotify_fd >= 0) {
        if (dm->inotify_wd >= 0) {
            inotify_rm_watch(dm->inotify_fd, dm->inotify_wd);
        }
        if (dm->epoll_fd >= 0) {
            remove_fd_from_epoll(dm->epoll_fd, dm->inotify_fd);
        }
        close(dm->inotify_fd);
        dm->inotify_fd = -1;
    }
}

int device_manager_get_count(const device_manager_t *dm) {
    return dm ? dm->count : 0;
}
