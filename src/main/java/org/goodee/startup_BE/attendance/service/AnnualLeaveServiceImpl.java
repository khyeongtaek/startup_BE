package org.goodee.startup_BE.attendance.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.goodee.startup_BE.attendance.repository.AnnualLeaveRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnualLeaveServiceImpl implements AnnualLeaveService {

    private final AnnualLeaveRepository annualLeaveRepository;
    private final EmployeeRepository employeeRepository;

    //  직원 연차 조회 (없으면 자동 생성)
    @Override
    @Transactional(readOnly = true)
    public AnnualLeave getAnnualLeave(Long employeeId) {
        Long currentYear = (long) LocalDate.now().getYear();
        return annualLeaveRepository
                .findByEmployeeEmployeeIdAndYear(employeeId, currentYear)
                .orElse(null);
    }

    //  직원 연차 자동 생성 (없을 경우)
    @Override
    public AnnualLeave createIfNotExists(Long employeeId) {
        Long currentYear = (long) LocalDate.now().getYear();

        return annualLeaveRepository.findByEmployeeEmployeeIdAndYear(employeeId, currentYear)
                .orElseGet(() -> {
                    Employee employee = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("직원 정보를 찾을 수 없습니다."));
                    AnnualLeave newLeave = AnnualLeave.createInitialLeave(employee);
                    return annualLeaveRepository.save(newLeave);
                });
    }

    //  연차 사용
    @Override
    public AnnualLeave useAnnualLeave(Long employeeId, Double days) {
        AnnualLeave leave = getAnnualLeave(employeeId);
        leave.useDays(days);
        return annualLeaveRepository.save(leave);
    }

    //  연차 사용 취소
    @Override
    public AnnualLeave refundAnnualLeave(Long employeeId, Double days) {
        AnnualLeave leave = getAnnualLeave(employeeId);
        leave.refundDays(days);
        return annualLeaveRepository.save(leave);
    }

    //  관리자 전체 조회
    @Override
    @Transactional(readOnly = true)
    public List<AnnualLeave> getAllAnnualLeaves() {
        return annualLeaveRepository.findAll();
    }

}