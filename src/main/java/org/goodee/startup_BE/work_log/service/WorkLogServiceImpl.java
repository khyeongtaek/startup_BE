package org.goodee.startup_BE.work_log.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.work_log.dto.WorkLogDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.repository.WorkLogReadRepository;
import org.goodee.startup_BE.work_log.repository.WorkLogRepository;
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

    // 업무일지 작성
    @Override
    public void saveWorkLog(WorkLogDTO workLogDTO) {
        Employee employee = employeeRepository.findById(workLogDTO.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("직원이 존재하지 않습니다."));
        CommonCode workType = commonCodeRepository.findByCode(workLogDTO.getWorkType())
                .orElseThrow(() -> new IllegalArgumentException("업무 구분 코드가 존재하지 않습니다."));
        CommonCode workOption = commonCodeRepository.findByCode(workLogDTO.getWorkOption())
                .orElseThrow(() -> new IllegalArgumentException("업무 옵션 코드가 존재하지 않습니다."));

        workLogRepository.save(workLogDTO.toEntity(employee, workType, workOption));
    }

    // 업무일지 수정
    @Override
    public void updateWorkLog(Long id, WorkLogDTO workLogDTO) {
        // 수정할 업무일지 조회
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("업무일지가 존재하지 않습니다."));
        CommonCode workType = commonCodeRepository.findByCode(workLogDTO.getWorkType())
                .orElseThrow(() -> new IllegalArgumentException("업무 구분 코드가 존재하지 않습니다."));
        CommonCode workOption = commonCodeRepository.findByCode(workLogDTO.getWorkOption())
                .orElseThrow(() -> new IllegalArgumentException("업무 옵션 코드가 존재하지 않습니다."));

        workLog.updateWorkLog(workType, workOption, workLogDTO.getWorkDate(), workLogDTO.getTitle(), workLogDTO.getContent());
    }

    // 업무일지 삭제 (소프트 삭제)
    @Override
    public void deleteWorkLog(Long id) {
        WorkLog workLog = workLogRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("업무일지가 존재하지 않습니다."));
        workLog.deleteWorkLog();    // 엔티티에서 isDeleted를 true 처리 (소프트 삭제)
    }

    @Override
    @Transactional(readOnly = true)
    public WorkLogDTO getWorkLogDetail(Long id) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("업무일지가 존재하지 않습니다."));

        return WorkLogDTO.fromEntity(workLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkLogDTO> getWorkLogList() {
        return List.of();
    }
}
