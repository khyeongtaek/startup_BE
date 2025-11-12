package org.goodee.startup_BE.schedule.service;

import org.goodee.startup_BE.schedule.dto.ScheduleParticipantResponseDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;

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
    void deleteSchedule(Long scheduleId);

    // 일정 변경
    ScheduleResponseDTO updateSchedule(Long scheduleId, ScheduleRequestDTO request);

    //  일정 참여자 초대
    void inviteParticipants(Long scheduleId, List<Long> employeeIds);

    //   일정 참여자 조회
    List<ScheduleParticipantResponseDTO> getParticipants(Long scheduleId);

    List<ScheduleResponseDTO> getVisibleSchedules(Long employeeId);

    // 일점 참여자 참여 상태 변경
    void updateParticipantStatus(Long scheduleId, Long employeeId, String value1);

}