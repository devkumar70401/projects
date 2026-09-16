#ifndef ROSE_BRICK_H
#define ROSE_BRICK_H

typedef struct {
    const char *brick_id;
    int         version;
    int  (*init)(void);
    void (*process)(void);   /* optional, called each main loop tick */
    void (*shutdown)(void);
} Brick;

#endif /* ROSE_BRICK_H */
