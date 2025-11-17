package org.goodee.startup_BE.notification.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationCountDTO;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.goodee.startup_BE.notification.entity.Notification;
import org.goodee.startup_BE.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final CommonCodeRepository commonCodeRepository;

    // 알림 생성
    @Override
    public NotificationResponseDTO create(NotificationRequestDTO requestDTO) {

        // 사원 조회
        Employee employee = employeeRepository.findById(requestDTO.getEmployeeId())
                .orElseThrow(() -> new UsernameNotFoundException("존재 하지 않는 사원 입니다"));

        // OwnerType CommonCode 조회
        // DTO에서 받은 code를 사용하여 CommonCode Entity를 조회 하기
        CommonCode ownerType = commonCodeRepository.findById(requestDTO.getOwnerTypeCommonCodeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "유효하지 않은 알림 출처 CommonCode Id 입니다 ID: " + requestDTO.getOwnerTypeCommonCodeId()
                ));

        // 알림 생성
        Notification notification =
                Notification
                        .createNotification(
                                employee,
                                ownerType,
                                requestDTO.getUrl(),
                                requestDTO.getTitle(),
                                requestDTO.getContent()
                        );
        // 저장
        NotificationResponseDTO dto = NotificationResponseDTO.toDTO(notificationRepository.save(notification));

        // Websocket 푸쉬
        // 공통 메소드 호출
        sendNotificationCounts(employee.getUsername());

        // 생성된 알림 객체 전송
        simpMessagingTemplate.convertAndSendToUser(employee.getUsername(), "/queue/new-notifications", dto);

        return dto;
    }

    // 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> list(String username, Pageable pageable) {
        // username을 이용해서 employeeId를 찾기
        Long employeeId = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(" 존재 하지 않는 사원 입니다" + username))
                .getEmployeeId();
        return notificationRepository.findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(employeeId, pageable)
                .map(NotificationResponseDTO::toDTO);
    }

    // 알림을 읽음 처리 하고 URL 반환
    @Override
    public String getUrl(Long notificationId, String username) {

        // 알림 조회 및 권한 확인
        Notification notification = checkRole(notificationId, username);

        // 알림 읽음 처리
        notification.readNotification();

        // WebSocket 푸쉬
        sendNotificationCounts(username);

        // 링크 반환
        return notification.getUrl();
    }

    // 알림 삭제 (소프트 삭제)
    @Override
    public void softDelete(Long notificationId, String username) {

        // 알림 조회 및 권한 확인
        Notification notification = checkRole(notificationId, username);

        // 엔티티 삭제 메소드 호출
        notification.deleteNotification();

        // WebSocket 푸쉬
        sendNotificationCounts(username);
    }

    // 읽지 않은 알림 개수
    @Override
    public long getUnreadNotiCount(String username) {
        // 유효성 검사
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다.: " + username));
        return notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(employee.getUsername());
    }

    // 모든 알림 읽음 처리
    @Override
    public void readAll(String username) {

        // 해당 사용자의 읽지 않은 알림만 조회 후 모든 알림 읽음 처리
        List<Notification> notifications = notificationRepository.findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(username);
        notifications.forEach(Notification::readNotification);

        // WebSocket 푸쉬
        sendNotificationCounts(username);

    }

    // 모든 알림 삭제
    @Override
    public void softDeleteAll(String username) {

        // 해당 사용자의 읽지 않은 알림만 조회 후 모든 알림 삭제 (소프트 딜리트)
        List<Notification> notifications = notificationRepository.findByEmployeeUsernameAndIsDeletedFalse(username);
        notifications.forEach(Notification::deleteNotification);

        // WebSocket 푸쉬
        sendNotificationCounts(username);

    }

    /**
     * [공통 메서드] 알림 조회 및 권한 확인
     *
     * @return 조회된 Notification 엔티티
     */
    private Notification checkRole(Long notificationId, String username) {
        // 알림 ID로 조회
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 알림입니다. ID: " + notificationId));

        // 본인의 알림이 맞는지 확인
        if (!notification.getEmployee().getUsername().equals(username)) {
            throw new AccessDeniedException("해당 알림에 접근할 권한이 없습니다.");
        }

        // 이미 삭제된 알림인지 확인
        if (notification.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 알림입니다.");
        }
        return notification;
    }

    /**
     * [공통 메서드] 최신 알림 개수 전송
     *
     * @param username
     * @return CountDTO
     */
    @Override
    public void sendNotificationCounts(String username) {

        long unreadCount = getUnreadNotiCount(username);

        NotificationCountDTO countDTO = new NotificationCountDTO(unreadCount);

        simpMessagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                countDTO
        );
    }
}
