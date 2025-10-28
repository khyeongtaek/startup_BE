package org.goodee.startup_BE.schedule.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.goodee.startup_BE.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements  ScheduleService{

    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;

    // 일정 등록
    @Override
    public ScheduleResponseDTO createSchedule(ScheduleRequestDTO request) {

        // 직원 조회
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));

        // 일정 카테고리 조회
        List<CommonCode> categoryList = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("SC", request.getCategoryCode());
        if (categoryList.isEmpty()){
            throw new IllegalArgumentException("유효하지 않은 일정 카테고리 코드입니다.");
        }
        CommonCode category = categoryList.get(0);

        // 색상 조회
        List<CommonCode> colorList = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("CL", request.getColorCode());
        if (colorList.isEmpty()){
            throw new IllegalArgumentException("유효하지 않은 색상 코드입니다");
        }
        CommonCode color = colorList.get(0);

        // DTO -> Entity
        Schedule schedule = request.toEntity(employee, category, color);

        // 저장
        Schedule saved = scheduleRepository.save(schedule);


        return ScheduleResponseDTO.toDTO(saved);
    }


    // 전체 일정 조회
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> getAllSchedule() {
        return scheduleRepository.findByIsDeletedFalse()
                .stream()
                .map(ScheduleResponseDTO :: toDTO)
                .toList();
    }


    // 단일 일정 조회
    @Override
    @Transactional(readOnly = true)
    public ScheduleResponseDTO getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다"));
        return ScheduleResponseDTO.toDTO(schedule);
    }


    // 기간별 일정 조회
    @Override
    public List<ScheduleResponseDTO> getAllScheduleByPeriod(LocalDate start, LocalDate end) {

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이후여야 합니다.");
        }

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(23, 59, 59);


        return scheduleRepository.findByStartTimeBetweenAndIsDeletedFalse(startTime,endTime)
                .stream()
                .map(ScheduleResponseDTO::toDTO)
                .toList();
    }
}
