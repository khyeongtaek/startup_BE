package org.goodee.startup_BE.employee.enums;

public enum Position {

    STAFF,              // PS1: 사원
    SENIOR_STAFF,       // PS2: 주임
    ASSISTANT_MANAGER,  // PS3: 대리
    MANAGER,            // PS4: 과장
    SENIOR_MANAGER,     // PS5: 차장
    GENERAL_MANAGER,    // PS6: 부장
    DIRECTOR,           // PS7: 이사
    CEO;                // PS8: 대표이사

    public static final String PREFIX = "PS";
}