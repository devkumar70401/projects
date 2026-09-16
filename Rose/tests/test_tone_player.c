#include "../bricks/tone_player/tone_player.h"
#include <stdio.h>
#include <unistd.h>

int main(void) {
    printf("============================================\n");
    printf("ROSE STEP 1 VERIFICATION: Tone Player Brick \n");
    printf("============================================\n");

    if (tone_player_init() != 0) {
        fprintf(stderr, "Failed to initialize Tone Player.\n");
        return 1;
    }

    printf("[test] Playing chime (880 Hz)...\n");
    tone_player_play_tone(880, 500);

    printf("[test] Playing second chime (1046 Hz)...\n");
    tone_player_play_tone(1046, 500);

    tone_player_shutdown();
    printf("STEP 1 TEST COMPLETED SUCCESSFULLY.\n");
    return 0;
}
