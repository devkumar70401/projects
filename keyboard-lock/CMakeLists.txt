cmake_minimum_required(VERSION 3.16)
project(keyboard-lock VERSION 1.0.0 LANGUAGES C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
add_definitions(-D_GNU_SOURCE)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -O2")

find_package(Threads REQUIRED)

set(SOURCES
    src/state_machine.c
    src/hotkey.c
    src/logger.c
    src/device_manager.c
    src/ipc.c
    src/backend_evdev.c
    src/backend_x11.c
    src/daemon.c
    src/main.c
)

add_executable(keyboard-lock ${SOURCES})
target_link_libraries(keyboard-lock PRIVATE Threads::Threads ${CMAKE_DL_LIBS})

# Unit tests
enable_testing()

add_executable(test_state_machine src/state_machine.c tests/test_state_machine.c)
add_test(NAME state_machine_test COMMAND test_state_machine)

add_executable(test_ipc src/ipc.c src/state_machine.c src/logger.c tests/test_ipc.c)
target_link_libraries(test_ipc PRIVATE Threads::Threads)
add_test(NAME ipc_test COMMAND test_ipc)

install(TARGETS keyboard-lock DESTINATION bin)
install(FILES systemd/keyboard-lock.service DESTINATION share/systemd/user OPTIONAL)
