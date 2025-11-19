package org.goodee.startup_BE.employee.service;


import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
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
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttachmentFileService attachmentFileService;
    private final EmployeeHistoryService employeeHistoryService;

    @Override
    public EmployeeResponseDTO updateEmployeeByUser(String username, EmployeeRequestDTO request) {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("회원 정보 수정 권한이 없습니다."));

        CommonCode ownerCode = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.EMPLOYEE.name())
                .get(0);


        employee.updatePhoneNumber(request.getPhoneNumber(), employee);

        //이미지를 실제 업로드 후 이미지경로만 가져옴
        if (request.getMultipartFile() != null) {
            employee.updateProfileImg(
                    attachmentFileService
                            .uploadFiles(request.getMultipartFile(), ownerCode.getCommonCodeId(), employee.getEmployeeId())
                            .get(0)
                            .getStoragePath()
                    , employee
            );
        }

        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO updateEmployeeByAdmin(String username, EmployeeRequestDTO request) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        // --- 변경 전 값 저장 ---
        String oldStatus = employee.getStatus().getValue2();
        String oldRole = employee.getRole().getValue2();
        String oldDepartment = employee.getDepartment().getValue1();
        String oldPosition = employee.getPosition().getValue1();

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


        employee.updateStatus(statusCode, admin);
        employee.updateRole(roleCode, admin);
        employee.updateDepartment(departmentCode, admin);
        employee.updatePosition(positionCode, admin);

        // --- 이력 기록 ---
        employeeHistoryService.logHistory(employee, admin, "재직상태", oldStatus, statusCode.getValue2());
        employeeHistoryService.logHistory(employee, admin, "권한", oldRole, roleCode.getValue2());
        employeeHistoryService.logHistory(employee, admin, "부서", oldDepartment, departmentCode.getValue1());
        employeeHistoryService.logHistory(employee, admin, "직급", oldPosition, positionCode.getValue1());

        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO initPassword(String username, EmployeeRequestDTO request) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        employee.updateInitPassword(passwordEncoder.encode(employee.getUsername()), admin);
        employeeHistoryService.logHistory(employee, admin, "비밀번호", null, null);

        return EmployeeResponseDTO.toDTO(employee);
    }

    @Override
    public EmployeeResponseDTO getEmployee(Long employeeId) {
        return EmployeeResponseDTO.toDTO(
                employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."))
        );
    }

    @Override
    public EmployeeResponseDTO getEmployee(String username) {
        return EmployeeResponseDTO.toDTO(
                employeeRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."))
        );
    }


    @Override
    public List<EmployeeResponseDTO> getDepartmentMembers(Long departmentId) {
        return employeeRepository.findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(departmentId)
                .stream()
                .map(EmployeeResponseDTO::toDTO)
                .toList();
    }

    @Override
    public void deleteEmployee(Long employeeId) {
        employeeRepository.deleteById(employeeId);
    }

}
