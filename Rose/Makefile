CC = gcc
CFLAGS = -Wall -Wextra -O2 -pthread -Icore -Ithird_party/vosk
LDFLAGS = -Lthird_party/vosk -lvosk -Wl,-rpath,'$$ORIGIN/../third_party/vosk' -lm -lpthread

SRCS_CORE = core/event_bus.c
SRCS_BRICKS = bricks/tone_player/tone_player.c \
              bricks/audio_capture/audio_capture.c \
              bricks/transcription/transcription.c \
              bricks/wake_word/wake_word.c \
              bricks/command_router/command_router.c
SRCS_SUBSYSTEMS = subsystems/timer/duration_parser.c \
                  subsystems/timer/countdown_engine.c \
                  subsystems/timer/timer_subsystem.c

OBJS_ALL = $(SRCS_CORE:.c=.o) $(SRCS_BRICKS:.c=.o) $(SRCS_SUBSYSTEMS:.c=.o)

BIN_DIR = bin
TARGET = $(BIN_DIR)/rose
SIM_TARGET = $(BIN_DIR)/rose_sim

all: $(TARGET) $(SIM_TARGET)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

$(TARGET): $(BIN_DIR) $(OBJS_ALL) core/main.o
	$(CC) $(OBJS_ALL) core/main.o -o $(TARGET) $(LDFLAGS)

$(SIM_TARGET): $(BIN_DIR) $(OBJS_ALL) core/sim_main.o
	$(CC) $(OBJS_ALL) core/sim_main.o -o $(SIM_TARGET) $(LDFLAGS)

test_step1: $(BIN_DIR) $(OBJS_ALL) tests/test_tone_player.o
	$(CC) core/event_bus.o bricks/tone_player/tone_player.o tests/test_tone_player.o -o $(BIN_DIR)/test_step1_tone -lm -lpthread
	./$(BIN_DIR)/test_step1_tone

test_step2: $(BIN_DIR) $(OBJS_ALL) tests/test_countdown_engine.o
	$(CC) core/event_bus.o bricks/tone_player/tone_player.o subsystems/timer/countdown_engine.o tests/test_countdown_engine.o -o $(BIN_DIR)/test_step2_countdown -lm -lpthread
	./$(BIN_DIR)/test_step2_countdown

test_step3: $(BIN_DIR) subsystems/timer/duration_parser.o tests/test_duration_parser.o
	$(CC) subsystems/timer/duration_parser.o tests/test_duration_parser.o -o $(BIN_DIR)/test_step3_duration
	./$(BIN_DIR)/test_step3_duration

test_step4: $(BIN_DIR) $(OBJS_ALL) tests/test_step4_timer_subsystem.o
	$(CC) core/event_bus.o bricks/tone_player/tone_player.o subsystems/timer/countdown_engine.o subsystems/timer/duration_parser.o subsystems/timer/timer_subsystem.o tests/test_step4_timer_subsystem.o -o $(BIN_DIR)/test_step4_subsystem -lm -lpthread
	./$(BIN_DIR)/test_step4_subsystem "2 seconds"

test_step5: $(BIN_DIR) $(OBJS_ALL) tests/test_audio_capture.o
	$(CC) core/event_bus.o bricks/audio_capture/audio_capture.o tests/test_audio_capture.o -o $(BIN_DIR)/test_step5_audio -lm -lpthread
	./$(BIN_DIR)/test_step5_audio

test_step6: $(BIN_DIR) $(OBJS_ALL) tests/test_transcription.o
	$(CC) core/event_bus.o bricks/transcription/transcription.o tests/test_transcription.o -o $(BIN_DIR)/test_step6_transcription $(LDFLAGS)
	./$(BIN_DIR)/test_step6_transcription

test_step7: $(BIN_DIR) $(OBJS_ALL) tests/test_wake_word.o
	$(CC) core/event_bus.o bricks/wake_word/wake_word.o tests/test_wake_word.o -o $(BIN_DIR)/test_step7_wake_word -lm -lpthread
	./$(BIN_DIR)/test_step7_wake_word

test_step8: $(BIN_DIR) $(OBJS_ALL) tests/test_command_router.o
	$(CC) core/event_bus.o subsystems/timer/duration_parser.o bricks/command_router/command_router.o tests/test_command_router.o -o $(BIN_DIR)/test_step8_command_router -lm -lpthread
	./$(BIN_DIR)/test_step8_command_router

test: test_step1 test_step2 test_step3 test_step4 test_step5 test_step6 test_step7 test_step8
	@echo "=========================================="
	@echo "ALL 8 ISOLATED TESTS PASSED SUCCESSFULLY!"
	@echo "=========================================="

clean:
	rm -f $(OBJS_ALL) core/main.o core/sim_main.o tests/*.o
	rm -rf $(BIN_DIR)

.PHONY: all test test_step1 test_step2 test_step3 test_step4 test_step5 test_step6 test_step7 test_step8 clean
