package org.goodee.startup_BE.notification.service;

import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /* 알림 생성 */
    NotificationResponseDTO create(NotificationRequestDTO requestDTO);

    /* 목록(직원별, 미삭제, 최신순) */
    Page<NotificationResponseDTO> list(String username, Pageable pageable);

    /* 알림을 읽음 처리 하고 URL 반환 */
    String getUrl(Long notificationId, String username);

    /* 알림 삭제 (소프트 삭제) */
    void softDelete(Long notificationId, String username);

    /* 읽지 않은 알림 개수 */
    long getUnreadNotiCount(String username);

    /* 모든 알림 읽음 처리 */
    void readAll(String username);

    /* 모든 알림 삭제 */
    void softDeleteAll(String username);

    /* 사용자에게 최신 알림 개수를 WebSocket으로 전송하는 공통 메소드 */
    void sendNotificationCounts(String username);

}
