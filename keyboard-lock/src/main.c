/**
 * main.c - Keyboard Lock CLI Entrypoint & Dispatcher
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>
#include "daemon.h"
#include "ipc.h"
#include "hotkey.h"
#include "logger.h"

#define VERSION "1.0.0"

static void print_version(void) {
    printf("keyboard-lock version %s\n", VERSION);
    printf("A minimal, reliable keyboard-only lock utility for Ubuntu Linux.\n");
    printf("Operates at kernel evdev (EVIOCGRAB) and X11 levels.\n");
    printf("Guarantees that ONLY keyboard input is disabled while mouse,\n");
    printf("displays, audio, networking, and applications remain 100%% functional.\n");
}

static void print_help(const char *progname) {
    print_version();
    printf("\nUsage:\n");
    printf("  %s [options]                  Start keyboard-lock in foreground\n", progname);
    printf("  %s --daemon [options]         Start keyboard-lock as background daemon\n", progname);
    printf("  %s --lock                     Lock keyboard (disable input)\n", progname);
    printf("  %s --unlock                   Unlock keyboard (restore input)\n", progname);
    printf("  %s --toggle                   Toggle locked/unlocked state\n", progname);
    printf("  %s --status                   Query status of running instance\n", progname);
    printf("  %s --stop                     Stop running daemon\n", progname);
    printf("\nOptions:\n");
    printf("  -d, --daemon                  Fork into background daemon\n");
    printf("  -b, --backend <evdev|x11|auto> Input interception backend (default: auto/evdev)\n");
    printf("      --hotkey <combo>          Custom hotkey (default: ctrl+alt+k)\n");
    printf("  -v, --verbose                 Enable detailed debug logging\n");
    printf("  -q, --quiet                   Suppress informative output\n");
    printf("      --syslog                  Log output to system syslog / journald\n");
    printf("  -V, --version                 Show version information\n");
    printf("  -h, --help                    Show this help message\n");
    printf("\nDefault Hotkey:\n");
    printf("  Ctrl + Alt + K               Toggles keyboard lock state\n");
}

int main(int argc, char *argv[]) {
    daemon_config_t config;
    memset(&config, 0, sizeof(config));
    config.target_keycode = KEY_K;
    config.backend_type = "auto";
    config.daemonize = false;
    config.verbose = false;
    config.quiet = false;
    config.use_syslog = false;

    enum {
        MODE_RUN_DAEMON = 0,
        MODE_CLI_LOCK,
        MODE_CLI_UNLOCK,
        MODE_CLI_TOGGLE,
        MODE_CLI_STATUS,
        MODE_CLI_STOP
    } mode = MODE_RUN_DAEMON;

    static struct option long_options[] = {
        {"lock",     no_argument,       0, 'l'},
        {"unlock",   no_argument,       0, 'u'},
        {"toggle",   no_argument,       0, 't'},
        {"status",   no_argument,       0, 's'},
        {"stop",     no_argument,       0, 'k'},
        {"daemon",   no_argument,       0, 'd'},
        {"backend",  required_argument, 0, 'b'},
        {"hotkey",   required_argument, 0, 'H'},
        {"verbose",  no_argument,       0, 'v'},
        {"quiet",    no_argument,       0, 'q'},
        {"syslog",   no_argument,       0, 'S'},
        {"version",  no_argument,       0, 'V'},
        {"help",     no_argument,       0, 'h'},
        {0, 0, 0, 0}
    };

    int opt;
    int opt_index = 0;
    while ((opt = getopt_long(argc, argv, "lutskdb:vqVh", long_options, &opt_index)) != -1) {
        switch (opt) {
            case 'l':
                mode = MODE_CLI_LOCK;
                break;
            case 'u':
                mode = MODE_CLI_UNLOCK;
                break;
            case 't':
                mode = MODE_CLI_TOGGLE;
                break;
            case 's':
                mode = MODE_CLI_STATUS;
                break;
            case 'k':
                mode = MODE_CLI_STOP;
                break;
            case 'd':
                config.daemonize = true;
                break;
            case 'b':
                config.backend_type = optarg;
                break;
            case 'H': {
                int kc = hotkey_parse_string(optarg);
                if (kc < 0) {
                    fprintf(stderr, "Error: Invalid hotkey '%s'. Expected format like 'ctrl+alt+k'\n", optarg);
                    return 1;
                }
                config.target_keycode = kc;
                break;
            }
            case 'v':
                config.verbose = true;
                break;
            case 'q':
                config.quiet = true;
                break;
            case 'S':
                config.use_syslog = true;
                break;
            case 'V':
                print_version();
                return 0;
            case 'h':
                print_help(argv[0]);
                return 0;
            default:
                print_help(argv[0]);
                return 1;
        }
    }

    /* Handle client commands */
    char response[512];
    memset(response, 0, sizeof(response));

    switch (mode) {
        case MODE_CLI_LOCK:
            if (ipc_client_send_command("LOCK", response, sizeof(response)) == 0) {
                printf("%s\n", response);
                return 0;
            } else {
                fprintf(stderr, "Error: keyboard-lock daemon is not running.\n");
                return 1;
            }
        case MODE_CLI_UNLOCK:
            if (ipc_client_send_command("UNLOCK", response, sizeof(response)) == 0) {
                printf("%s\n", response);
                return 0;
            } else {
                fprintf(stderr, "Error: keyboard-lock daemon is not running.\n");
                return 1;
            }
        case MODE_CLI_TOGGLE:
            if (ipc_client_send_command("TOGGLE", response, sizeof(response)) == 0) {
                printf("%s\n", response);
                return 0;
            } else {
                fprintf(stderr, "Error: keyboard-lock daemon is not running.\n");
                return 1;
            }
        case MODE_CLI_STATUS:
            if (ipc_client_send_command("STATUS", response, sizeof(response)) == 0) {
                printf("%s\n", response);
                return 0;
            } else {
                printf("keyboard-lock daemon is NOT running.\n");
                return 3;
            }
        case MODE_CLI_STOP:
            if (ipc_client_send_command("STOP", response, sizeof(response)) == 0) {
                printf("%s\n", response);
                return 0;
            } else {
                fprintf(stderr, "Error: keyboard-lock daemon is not running.\n");
                return 1;
            }
        case MODE_RUN_DAEMON:
        default:
            /* Run daemon */
            return daemon_run(&config);
    }
}
