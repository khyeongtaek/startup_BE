package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션, 최신순)
     * 사용자가 방에 참여한 시간(joinedAt) 이후의 메시지만 조회합니다.
     * @param chatRoomId 채팅방 ID
     * @param joinedAt 사용자의 채팅방 참여 시각
     * @param pageable 페이지 정보
     * @return 메시지 페이지
     */
    Page<ChatMessage> findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
            Long chatRoomId, LocalDateTime joinedAt, Pageable pageable);
}
