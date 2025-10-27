package org.goodee.startup_BE.employee.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.LoginHistory;
import org.goodee.startup_BE.employee.repository.LoginHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {
    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    // 로그인실패시 exception을 일으켜 로그인 기록도 함께 롤백되는걸 방지하기 위해 로그인 기록은 새로운 트랜잭션으로 작동하도록 설정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginHistory recodeLoginHistory(String username, String ipAddress, String userAgent, CommonCode status) {
        return loginHistoryRepository.save(
                LoginHistory.createLoginHistory(
                        username
                        , ipAddress
                        , userAgent
                        , status
                )
        );
    }
}
