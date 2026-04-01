package com.ya.recallio.routine.model;

/**
 * Defines whether a routine is flexible, tied to a single time, or expected inside a time window.
 */
public enum RoutineTimingMode {
    ANYTIME,
    AT_TIME,
    TIME_WINDOW
}
