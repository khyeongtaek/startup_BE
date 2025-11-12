package org.goodee.startup_BE.attendance.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.attendance.repository.AttendanceWorkHistoryRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceWorkHistoryServiceImpl implements AttendanceWorkHistoryService {

    private final AttendanceWorkHistoryRepository historyRepository;
    private final CommonCodeRepository commonCodeRepository;

    @Override
    public void recordHistory(Attendance attendance, Employee employee, String actionCode) {
        // WS 그룹에서 value1이 일치하는 코드 가져오기
        List<CommonCode> codes = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("WS", actionCode);

        if (codes.isEmpty()) {
            throw new ResourceNotFoundException("유효하지 않은 근무 상태 코드입니다: " + actionCode);
        }

        CommonCode code = codes.get(0); // 첫 번째 결과 사용

        AttendanceWorkHistory history = new AttendanceWorkHistory(
                null,
                attendance,
                employee,
                code,
                LocalDateTime.now()
        );

        historyRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceWorkHistory> getHistoryByEmployee(Long employeeId) {
        return historyRepository.findByEmployeeEmployeeIdOrderByActionTimeDesc(employeeId);
    }
}