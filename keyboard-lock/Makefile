# Makefile for keyboard-lock utility

CC ?= gcc
CFLAGS ?= -Wall -Wextra -O2 -pthread -D_GNU_SOURCE
LDFLAGS ?= -pthread -ldl

PREFIX ?= /usr/local
BINDIR ?= $(PREFIX)/bin
SYSTEMD_USER_DIR ?= $(HOME)/.config/systemd/user
UDEV_DIR ?= /etc/udev/rules.d

SRC_DIR = src
TEST_DIR = tests
BIN_DIR = bin
BUILD_DIR = build

SRCS = $(SRC_DIR)/state_machine.c \
       $(SRC_DIR)/hotkey.c \
       $(SRC_DIR)/logger.c \
       $(SRC_DIR)/device_manager.c \
       $(SRC_DIR)/ipc.c \
       $(SRC_DIR)/backend_evdev.c \
       $(SRC_DIR)/backend_x11.c \
       $(SRC_DIR)/daemon.c \
       $(SRC_DIR)/main.c

OBJS = $(patsubst $(SRC_DIR)/%.c, $(BUILD_DIR)/%.o, $(SRCS))
TARGET = $(BIN_DIR)/keyboard-lock

.PHONY: all clean test install uninstall

all: $(TARGET)

$(TARGET): $(OBJS) | $(BIN_DIR)
	$(CC) $(OBJS) $(LDFLAGS) -o $@

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c | $(BUILD_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(BIN_DIR) $(BUILD_DIR):
	mkdir -p $@

test: $(TARGET)
	@echo "=== Building and Running Unit Tests ==="
	@bash $(TEST_DIR)/run_tests.sh
	$(CC) $(CFLAGS) $(SRC_DIR)/ipc.c $(SRC_DIR)/state_machine.c $(SRC_DIR)/logger.c $(TEST_DIR)/test_ipc.c $(LDFLAGS) -o $(BIN_DIR)/test_ipc
	$(BIN_DIR)/test_ipc

install: $(TARGET)
	install -d $(DESTDIR)$(BINDIR)
	install -m 755 $(TARGET) $(DESTDIR)$(BINDIR)/keyboard-lock
	install -d $(DESTDIR)$(SYSTEMD_USER_DIR)
	install -m 644 systemd/keyboard-lock.service $(DESTDIR)$(SYSTEMD_USER_DIR)/keyboard-lock.service
	@echo "Installed binary to $(DESTDIR)$(BINDIR)/keyboard-lock"
	@echo "Installed systemd user service to $(DESTDIR)$(SYSTEMD_USER_DIR)/keyboard-lock.service"

uninstall:
	rm -f $(DESTDIR)$(BINDIR)/keyboard-lock
	rm -f $(DESTDIR)$(SYSTEMD_USER_DIR)/keyboard-lock.service
	@echo "Removed keyboard-lock binary and systemd service"

clean:
	rm -rf $(BUILD_DIR) $(BIN_DIR)
