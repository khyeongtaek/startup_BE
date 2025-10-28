package org.goodee.startup_BE.notification.repository;

import org.goodee.startup_BE.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 목록(최신순)
    Page<Notification> findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(Long empolyeeId, Pageable pageable);

    // 읽지 않은 알림의 개수
    long countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(String username);

    // 삭제되지 않은 모든 알림 조회
    List<Notification> findByEmployeeUsernameAndIsDeletedFalse(String username);

    // 읽지 않은 모든 알림 조회
    List<Notification> findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(String username);
}
