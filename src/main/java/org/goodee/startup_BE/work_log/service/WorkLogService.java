package org.goodee.startup_BE.work_log.service;

import org.goodee.startup_BE.work_log.dto.WorkLogDTO;

import java.util.List;

public interface WorkLogService {
    void saveWorkLog(WorkLogDTO workLogDTO);      // 업무일지 작성
    void updateWorkLog(Long id, WorkLogDTO workLogDTO);
    void deleteWorkLog(Long id);                  // 업무일지 삭제
    WorkLogDTO getWorkLogDetail(Long id);         // 업무일지 조회(상세 페이지)
    List<WorkLogDTO> getWorkLogList();            // 업무일지 조회(리스트)
}
