package org.goodee.startup_BE.schedule.service;

import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    // 일정 등록
    ScheduleResponseDTO createSchedule(ScheduleRequestDTO request);

    // 전체 일정 조회
    List<ScheduleResponseDTO> getAllSchedule();

    // 일정 상세 조회
    ScheduleResponseDTO getSchedule(Long scheduleId);


    // 기간별 일정 조회
    List<ScheduleResponseDTO> getAllScheduleByPeriod(LocalDate start, LocalDate end);

    // 일정 삭제
    public void deleteSchedule(Long scheduleId);

    ScheduleResponseDTO updateSchedule(Long scheduleId, ScheduleRequestDTO request);
}