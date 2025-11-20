package org.goodee.startup_BE.schedule.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.entity.Notification;
import org.goodee.startup_BE.notification.repository.NotificationRepository;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.goodee.startup_BE.schedule.dto.ScheduleParticipantResponseDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleRequestDTO;
import org.goodee.startup_BE.schedule.dto.ScheduleResponseDTO;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.goodee.startup_BE.schedule.entity.ScheduleParticipant;
import org.goodee.startup_BE.schedule.exception.InvalidScheduleArgumentException;
import org.goodee.startup_BE.schedule.exception.ScheduleNotFoundException;
import org.goodee.startup_BE.schedule.repository.ScheduleParticipantRepository;
import org.goodee.startup_BE.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Builder
@Slf4j
public class ScheduleServiceImpl implements  ScheduleService{

    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    // 일정 등록
    @Override
    public ScheduleResponseDTO createSchedule(ScheduleRequestDTO request) {

        // 직원 조회
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("사원 정보를 찾을 수 없습니다."));

        // 일정 카테고리 조회
        List<CommonCode> categoryList = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("SC", request.getCategoryCode());
        if (categoryList.isEmpty()){
            throw new InvalidScheduleArgumentException("유효하지 않은 일정 카테고리 코드입니다.");
        }
        CommonCode category = categoryList.get(0);


        // DTO -> Entity
        Schedule schedule = request.toEntity(employee, category);

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
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new ScheduleNotFoundException("해당 일정을 찾을 수 없습니다"));
        return ScheduleResponseDTO.toDTO(schedule);
    }


    // 기간별 일정 조회
    @Override
    public List<ScheduleResponseDTO> getAllScheduleByPeriod(LocalDate start, LocalDate end) {

        if (end.isBefore(start)) {
            throw new InvalidScheduleArgumentException("종료일은 시작일보다 이후여야 합니다.");
        }

        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.atTime(23, 59, 59);


        return scheduleRepository.findByStartTimeBetweenAndIsDeletedFalse(startTime,endTime)
                .stream()
                .map(ScheduleResponseDTO::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public ScheduleResponseDTO updateSchedule(Long scheduleId, ScheduleRequestDTO request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("해당 일정을 찾을 수 없습니다."));

        //  카테고리 처리 추가
        CommonCode category = null;
        if (request.getCategoryCode() != null && !request.getCategoryCode().isBlank()) {
            List<CommonCode> categoryList =
                    commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("SC", request.getCategoryCode());
            if (categoryList.isEmpty()) {
                throw new InvalidScheduleArgumentException("유효하지 않은 일정 카테고리 코드입니다.");
            }
            category = categoryList.get(0);
        }

        //  카테고리를 포함한 update 호출
        schedule.update(
                request.getTitle(),
                request.getContent(),
                request.getStartTime(),
                request.getEndTime(),
                category
        );

        scheduleRepository.save(schedule);
        return ScheduleResponseDTO.toDTO(schedule);
    }

    //  일정 삭제
    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));

        schedule.delete(); // soft delete
        scheduleRepository.save(schedule);
    }

    // 로그인한 사용자가 만든 일정 + 초대받은 일정 조회
    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO> getVisibleSchedules(Long employeeId) {

        List<Schedule> owned   = scheduleRepository.findOwnedVisible(employeeId);
        List<Schedule> invited = scheduleRepository.findInvitedVisible(employeeId);

        // 중복 제거(작성자이면서 초대도 받은 경우 등)
        java.util.Map<Long, Schedule> map = new java.util.LinkedHashMap<>();
        owned.forEach(s -> map.put(s.getScheduleId(), s));
        invited.forEach(s -> map.putIfAbsent(s.getScheduleId(), s));

        return map.values().stream()
                .map(ScheduleResponseDTO::toDTO) // 네가 쓰는 DTO 변환기
                .toList();
    }

    // 일정 초대
    @Override
    @Transactional
    public void inviteParticipants(Long scheduleId, List<Long> employeeIds) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("해당 일정을 찾을 수 없습니다."));

        CommonCode pendingStatus = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues("SP", "PENDING")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("기본 참여 상태 코드 'PENDING'을 찾을 수 없습니다."));

        CommonCode inviteCode = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.SCHEDULEINVITE.name())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("일정 초대 관련 공통 코드를 찾을 수 없습니다."));

        List<Employee> candidates = employeeRepository.findAllById(employeeIds);
        List<Employee> newInvitees = new ArrayList<>();

        for (Employee emp : candidates) {
            boolean alreadyInvited = scheduleParticipantRepository.findByScheduleScheduleId(scheduleId)
                    .stream()
                    .anyMatch(p -> p.getParticipant().getEmployeeId().equals(emp.getEmployeeId()));

            if (!alreadyInvited) {
                ScheduleParticipant participant =
                        ScheduleParticipant.createScheduleParticipant(schedule, emp, pendingStatus);
                scheduleParticipantRepository.save(participant);
                newInvitees.add(emp);
            }
        }

        //  트랜잭션 안에서 바로 알림 전송 (ChatServiceImpl 방식)
        if (!newInvitees.isEmpty()) {
            Long inviteCodeId = inviteCode.getCommonCodeId();
            String title = schedule.getTitle() + " 일정에 초대되었습니다.";
            String content = schedule.getEmployee().getName() + "님이 일정을 공유했습니다.";

            for (Employee recipient : newInvitees) {
                NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                        .employeeId(recipient.getEmployeeId())
                        .ownerTypeCommonCodeId(inviteCodeId)
                        .url("/schedule?modal=edit&id=" + scheduleId)
                        .title(title)
                        .content(content)
                        .build();

                notificationService.create(notificationRequestDTO);
            }
        }
    }

    // 일정 참여 상태 변경 (PENDING, ATTEND, REJECT)
    @Override
    public void updateParticipantStatus(Long scheduleId, Long employeeId, String value1) {
        // 참여자 찾기
        ScheduleParticipant participant = scheduleParticipantRepository
                .findBySchedule_ScheduleIdAndParticipant_EmployeeId(scheduleId,employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("참여자 정보를 찾을 수 없습니다."));

        // Common Code 조회
        CommonCode newStatus = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues("SP", value1)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("해당 코드가 존재하지 않습니다."));

        participant.updateStatus(newStatus);
        scheduleParticipantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleParticipantResponseDTO> getParticipants(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("해당 일정을 찾을 수 없습니다."));

        //  초대된 참여자 목록 조회
        List<ScheduleParticipant> participants = scheduleParticipantRepository.findByScheduleScheduleId(scheduleId)
                .stream()
                .filter(p -> p.getIs_deleted() == null || !p.getIs_deleted())
                .toList();

        //  DTO 변환
        List<ScheduleParticipantResponseDTO> dtos = participants.stream()
                .map(ScheduleParticipantResponseDTO::toDTO)
                .collect(java.util.stream.Collectors.toList());

        //   작성자(일정 생성자)가 목록에 없는 경우 수동 추가 (null-safe)
        boolean alreadyIncluded = dtos.stream()
                .anyMatch(dto -> java.util.Objects.equals(
                        dto.getParticipantEmployeeId(),
                        schedule.getEmployee().getEmployeeId()
                ));

        if (!alreadyIncluded) { //   작성자 추가 로직
            dtos.add(ScheduleParticipantResponseDTO.builder()
                    .participantId(null) // 작성자는 ScheduleParticipant 엔티티에 없으므로 null
                    .scheduleId(schedule.getScheduleId())
                    .participantEmployeeId(schedule.getEmployee().getEmployeeId())
                    .participantName(schedule.getEmployee().getName())
                    .participantStatusName("주최자")
                    .createdAt(schedule.getCreatedAt())
                    .updatedAt(schedule.getUpdatedAt())
                    .build());
        }

        return dtos;
    }
}
