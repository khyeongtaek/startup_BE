package org.goodee.startup_BE.employee.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.dto.EmployeeHistoryResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.entity.EmployeeHistory;
import org.goodee.startup_BE.employee.repository.EmployeeHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeHistoryServiceImpl implements EmployeeHistoryService {

    private final EmployeeHistoryRepository employeeHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeHistoryResponseDTO> getEmployeeHistories(Long employeeId, Pageable pageable) {
        Page<EmployeeHistory> historyPage = employeeHistoryRepository.findByEmployeeEmployeeId(employeeId, pageable);
        return historyPage.map(EmployeeHistoryResponseDTO::toDTO);
    }

    @Override
    public void logHistory(Employee employee, Employee updater, String fieldName, String oldValue, String newValue) {
        if ((oldValue != null && newValue != null) && Objects.equals(oldValue, newValue)) {
            return;
        }

        EmployeeHistory history = EmployeeHistory.createHistory(
                employee,
                updater,
                fieldName,
                oldValue,
                newValue
        );
        employeeHistoryRepository.save(history);
    }
}