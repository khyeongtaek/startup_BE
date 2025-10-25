package org.goodee.startup_BE.employee.service;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.LoginHistory;

public interface LoginHistoryService {
    LoginHistory recodeLoginHistory(String username, String ipAddress, String userAgent, CommonCode status);
}
