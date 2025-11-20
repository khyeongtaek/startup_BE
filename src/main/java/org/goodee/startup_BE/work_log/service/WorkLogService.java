package org.goodee.startup_BE.work_log.service;

import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogCodeListDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

public interface WorkLogService {
    // 업무일지 작성
    WorkLogResponseDTO saveWorkLog(WorkLogRequestDTO workLogDTO, String username);
    
    // 업무일지 수정
    WorkLogResponseDTO updateWorkLog(WorkLogRequestDTO workLogDTO, String username) throws AccessDeniedException;
    
    // 업무일지 삭제
    void deleteWorkLog(List<Long> id, String username) throws AccessDeniedException;
    
    // 업무일지 조회(상세 페이지)
    WorkLogResponseDTO getWorkLogDetail(Long id, String username);
    
    // 업무일지 조회(리스트)
    Page<WorkLogResponseDTO> getWorkLogList(String username, String type, int page, int size);
    
    // 업무일지 코드 조회
    WorkLogCodeListDTO getWorkLogCodes();
}
