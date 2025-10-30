package org.goodee.startup_BE.attendance.exception;

public class DuplicateAttendanceException extends AttendanceException{
    public DuplicateAttendanceException(String message) {
        super(message);
    }
}
