package org.goodee.startup_BE.employee.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.dto.EmployeeHistoryResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.entity.EmployeeHistory;
import org.goodee.startup_BE.employee.repository.EmployeeHistoryRepository;
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
    public List<EmployeeHistoryResponseDTO> getEmployeeHistories(Long employeeId) {
        return employeeHistoryRepository.findByEmployeeEmployeeIdOrderByChangedAtDesc(employeeId)
                .stream()
                .map(EmployeeHistoryResponseDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void logHistory(Employee employee, Employee updater, String fieldName, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
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