#include "../subsystems/timer/duration_parser.h"
#include <stdio.h>
#include <assert.h>

typedef struct {
    const char *input;
    int expected_seconds;
} TestCase;

int main(void) {
    printf("===================================================\n");
    printf("ROSE STEP 3 VERIFICATION: Duration Parser Brick    \n");
    printf("===================================================\n");

    TestCase tests[] = {
        {"10 minutes", 600},
        {"10 min", 600},
        {"10 mins", 600},
        {"1 minute", 60},
        {"30 seconds", 30},
        {"45 sec", 45},
        {"1 hour", 3600},
        {"2 hours", 7200},
        {"start timer of 10 minutes", 600},
        {"five minutes", 300},
        {"invalid phrase", -1},
        {NULL, 0}
    };

    int passed = 0;
    int total = 0;

    for (int i = 0; tests[i].input != NULL; i++) {
        total++;
        int result = parse_duration_to_seconds(tests[i].input);
        if (result == tests[i].expected_seconds) {
            printf("  [PASS] '%s' -> %d seconds\n", tests[i].input, result);
            passed++;
        } else {
            printf("  [FAIL] '%s' -> got %d, expected %d\n", tests[i].input, result, tests[i].expected_seconds);
        }
    }

    printf("\nResults: %d/%d tests passed.\n", passed, total);
    if (passed == total) {
        printf("STEP 3 TEST COMPLETED SUCCESSFULLY.\n");
        return 0;
    }
    return 1;
}
