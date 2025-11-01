package org.goodee.startup_BE.employee.service;


import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService{
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public EmployeeResponseDTO updateEmployeeByUser(String username, EmployeeRequestDTO request) {
        Employee employee = employeeRepository.findByUsername(request.getUsername())
                .orElse(null);

        if(employee == null || !username.equals(employee.getUsername())){
            throw new BadCredentialsException("회원 정보 수정 권한이 없습니다.");
        }

        employee.updatePhoneNumber(request.getPhoneNumber(),employee);
        return EmployeeResponseDTO.toDTO(employee);
    }

    // 첨부 기능이 없어서 미구현
    @Override
    public EmployeeResponseDTO updateEmployeeProfileImg(String username, EmployeeRequestDTO request) {
        return null;
    }

    @Override
    public EmployeeResponseDTO updateEmployeePassword(String username, EmployeeRequestDTO request) {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        employee.updatePassword(passwordEncoder.encode(request.getPassword()),employee);
        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO updateEmployeeByAdmin(String username, EmployeeRequestDTO request) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        CommonCode statusCode = commonCodeRepository
                .findById(request.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("status code: " + request.getStatus() + " 를 찾을 수 없습니다."));

        CommonCode roleCode = commonCodeRepository
                .findById(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("role code: " + request.getRole() + " 를 찾을 수 없습니다."));

        CommonCode departmentCode = commonCodeRepository
                .findById(request.getDepartment())
                .orElseThrow(() -> new ResourceNotFoundException("department code: " + request.getDepartment() + " 를 찾을 수 없습니다."));

        CommonCode positionCode = commonCodeRepository
                .findById(request.getPosition())
                .orElseThrow(() -> new ResourceNotFoundException("position code: " + request.getPosition() + " 를 찾을 수 없습니다."));


        employee.updateStatus(statusCode,admin);
        employee.updateRole(roleCode,admin);
        employee.updateDepartment(departmentCode,admin);
        employee.updatePosition(positionCode,admin);

        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO initPassword(String username, EmployeeRequestDTO request) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        employee.updateInitPassword(passwordEncoder.encode(employee.getUsername()), admin);

        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO getEmployee(Long employeeId) {
        return EmployeeResponseDTO.toDTO(
                employeeRepository.findById(employeeId)
                        .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."))
        );
    }

    @Override
    public EmployeeResponseDTO getEmployee(String username) {
        return EmployeeResponseDTO.toDTO(
                employeeRepository.findByUsername(username)
                        .orElseThrow(()-> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."))
        );
    }


    @Override
    public List<EmployeeResponseDTO> getDepartmentMembers(Long departmentId) {
        return employeeRepository.findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(departmentId)
                .stream()
                .map(EmployeeResponseDTO::toDTO)
                .toList();
    }

}
