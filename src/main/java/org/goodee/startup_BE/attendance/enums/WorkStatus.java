package org.goodee.startup_BE.attendance.enums;

public enum WorkStatus {
    NORMAL,
    LATE,
    EARLY_LEAVE,
    ABSENT,
    VACATION,
    OUT_ON_BUSINESS,
    CLOCK_OUT,
    MORNING_HALF,
    AFTERNOON_HALF;

    public static final String PREFIX = "WS";
}
