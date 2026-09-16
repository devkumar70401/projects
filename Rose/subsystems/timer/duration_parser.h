#ifndef ROSE_DURATION_PARSER_H
#define ROSE_DURATION_PARSER_H

/**
 * @brief Parses a human-readable duration string into total seconds.
 * 
 * Supports phrases like:
 *   "10 minutes", "10 min", "10 mins"
 *   "30 seconds", "30 sec", "30 s"
 *   "2 hours", "1 hour", "1 hr"
 *   "start timer of 10 minutes" (or with embedded duration)
 * 
 * @param text The input string to parse.
 * @return Total duration in seconds, or -1 if no valid duration found.
 */
int parse_duration_to_seconds(const char *text);

#endif /* ROSE_DURATION_PARSER_H */
