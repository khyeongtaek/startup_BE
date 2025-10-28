package org.goodee.startup_BE.work_log.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.entity.WorkLogRead;
import org.goodee.startup_BE.work_log.repository.WorkLogReadRepository;
import org.goodee.startup_BE.work_log.repository.WorkLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkLogServiceImpl implements WorkLogService {
    private final WorkLogRepository workLogRepository;
    private final WorkLogReadRepository workLogReadRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;

    // 업무일지 등록
    @Override
    public WorkLogResponseDTO saveWorkLog(WorkLogRequestDTO workLogDTO, String username) {
        Employee employee = employeeRepository.findByUsername(username)
                              .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
        CommonCode workType = commonCodeRepository.findById(workLogDTO.getWorkTypeId())
                                .orElseThrow(() -> new ResourceNotFoundException("업무분류 코드가 존재하지 않습니다."));
        CommonCode workOption = commonCodeRepository.findById(workLogDTO.getWorkOptionId())
                                  .orElseThrow(() -> new ResourceNotFoundException("업무옵션 코드가 존재하지 않습니다."));

        WorkLog workLog = workLogRepository.save(workLogDTO.toEntity(employee, workType, workOption));
        return WorkLogResponseDTO.toDTO(workLog);
    }

    // 업무일지 수정
    @Override
    public WorkLogResponseDTO updateWorkLog(WorkLogRequestDTO workLogDTO,String username) throws AccessDeniedException {
        // 수정할 업무일지 조회
        WorkLog workLog = workLogRepository.findById(workLogDTO.getWorkLogId())
                 .orElseThrow(() -> new ResourceNotFoundException("업무일지가 존재하지 않습니다."));
        if(!workLog.getEmployee().getUsername().equals(username)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }
        
        CommonCode workType = commonCodeRepository.findById(workLogDTO.getWorkTypeId())
                  .orElseThrow(() -> new ResourceNotFoundException("업무구분 코드가 존재하지 않습니다."));
        CommonCode workOption = commonCodeRepository.findById(workLogDTO.getWorkOptionId())
                  .orElseThrow(() -> new ResourceNotFoundException("업무옵션 코드가 존재하지 않습니다."));

        workLog.updateWorkLog(workType, workOption, workLogDTO.getWorkDate(), workLogDTO.getTitle(), workLogDTO.getContent());
        
        return WorkLogResponseDTO.toDTO(workLog);
    }

    // 업무일지 삭제 (소프트 삭제)
    @Override
    public void deleteWorkLog(Long workLogId, String username) throws AccessDeniedException{
        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new ResourceNotFoundException("업무일지가 존재하지 않습니다."));
        if(!workLog.getEmployee().getUsername().equals(username)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }
        workLog.deleteWorkLog();    // 엔티티에서 isDeleted를 true 처리 (소프트 삭제)
    }

    // 업무일지 조회 (상세보기)
    @Override
    public WorkLogResponseDTO getWorkLogDetail(Long id, String username) {
        var employee = employeeRepository.findByUsername(username)
                         .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
        var workLog = workLogRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("업무일지가 존재하지 않습니다."));
        
        // 읽음 upsert (중복 삽입 경쟁 대비)
        boolean already = workLogReadRepository.existsByWorkLogAndEmployee(workLog, employee);
        if (!already) {
            try {
                workLogReadRepository.save(WorkLogRead.createWorkLogRead(workLog, employee));
            } catch (org.springframework.dao.DataIntegrityViolationException ignore) {
                // UNIQUE (work_log_id, employee_id) 충돌 시 이미 읽음으로 간주
            }
        }
        
        var dto = WorkLogResponseDTO.toDTO(workLog);
        dto.setIsRead(true); // 상세는 항상 읽음
        return dto;
    }
    
    // 업무일지 조회 (리스트)
    @Override
    @Transactional(readOnly = true)
    public Page<WorkLogResponseDTO> getWorkLogList(String username, String type, int page, int size) {
        var employee = employeeRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "workLogId"));
        
        return switch (type == null ? "my" : type.toLowerCase()) {
            case "all"  -> workLogRepository.findWithRead(employee.getEmployeeId(), null, false, pageable);
            case "dept" -> workLogRepository.findWithRead(employee.getEmployeeId(), employee.getDepartment().getCommonCodeId(), false, pageable); // 또는 deptCode
            default     -> workLogRepository.findWithRead(employee.getEmployeeId(), null, true,  pageable);
        };
    }
}
