package org.goodee.startup_BE.schedule.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.schedule.controller.ScheduleEmployeeController.EmployeeInfoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleEmployeeServiceImpl implements ScheduleEmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeInfoDTO> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();

        return employees.stream()
                .map(emp -> new EmployeeInfoDTO(
                        emp.getEmployeeId(),
                        emp.getName()
                ))
                .toList();
    }
}