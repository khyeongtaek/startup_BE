package org.goodee.startup_BE.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.notification.dto.NotificationPageDTO;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * NotificationController
 * 통신 프로토콜 : HTTP (단방향 통신)
 * 주요 역할 : Pull 방식 데이터 조회 및 단발성 상태 변경
 * 주요 어노테이션 : @PostMapping, @GetMapping, @DeleteMapping
 * 주요 기능  - 알림 목록 로딩
 * - 읽지 않은 개수 조회
 * - 클릭 후 URL 이동
 * - 모든 알림 읽기
 * - 단일 알림 삭제 (소프트 삭제)
 * - 전체 알림 삭제 (소프트 삭제)
 */

@Tag(name = "Notification API", description = "알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    // 알림 목록 조회
    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ResponseEntity<NotificationPageDTO> getNotificationList(
            Authentication authentication,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponseDTO> pageData = notificationService.list(authentication.getName(), pageable);

        NotificationPageDTO responseDTO = new NotificationPageDTO(
                pageData.getContent(),
                pageData.isLast()
        );

        return ResponseEntity.ok(responseDTO);
    }

    // 읽지 않은 알림 개수 조회
    @Operation(summary = "읽지 않은 알림 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadNotiCount(Authentication authentication) {
        Long count = notificationService.getUnreadNotiCount(authentication.getName());
        return ResponseEntity.ok(count);
    }

    // 알림을 읽음 처리 하고 URL 반환
    @Operation(summary = "알림을 읽음 처리 하고 URL 반환")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<String> readNotification(@PathVariable Long notificationId, Authentication authentication) {
        String url = notificationService.getUrl(notificationId, authentication.getName());
        return ResponseEntity.ok(url);
    }

    // 모든 알림 읽음 처리
    @Operation(summary = "모든 알림 읽음 처리")
    @PatchMapping("/read-all")  // 상태 변경이므로 PUT/PATCH 사용
    public ResponseEntity<Void> readAll(Authentication authentication) {
        notificationService.readAll(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // 단일 알림 삭제 (소프트 삭제)
    @Operation(summary = "단일 알림 삭제 (소프트 삭제)")
    @PatchMapping("/{notificationId}/delete")
    public ResponseEntity<Void> softDelete(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.softDelete(notificationId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // 전체 알림 삭제 (소프트 삭제)
    @Operation(summary = "전체 알림 삭제 (소프트 삭제)")
    @PatchMapping("/delete-all")
    public ResponseEntity<Void> softDeleteAll(Authentication authentication) {
        notificationService.softDeleteAll(authentication.getName());
        return ResponseEntity.noContent().build();
    }

/*   ! 테스트 용 ! // 알림 생성 (이 기능은 Service 내부에서 처리 하거나, 다른 서비스에서 Service를 직접 호출 하도록 유지)
    @Operation(summary = "알림 생성 테스트 API")
    @PostMapping("/send-test")
    public ResponseEntity<NotificationResponseDTO> createNotification(
            // Principal: 요청을 보낸 사용자의 정보 (여기서는 사용하지 않고 DTO의 recipientUsername을 사용)
            Authentication authentication,
            @RequestBody NotificationRequestDTO requestDTO) {

        // DTO에서 수신자 username을 추출
        String username = authentication.getName();

        NotificationResponseDTO responseDTO = notificationService.create(username, requestDTO);

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }*/

}
