package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션, 최신순)
     * 사용자가 방에 참여한 시간(joinedAt) 이후의 메시지만 조회합니다.
     * 채팅방 안에 들어갔을 때 사용자가 볼 실제 대화 내용 목록을 불러옵니다.
     * @param chatRoomId 채팅방 ID
     * @param joinedAt 사용자의 채팅방 참여 시각
     * @param pageable 페이지 정보
     * @return 메시지 페이지
     */
    Page<ChatMessage> findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
            Long chatRoomId, LocalDateTime joinedAt, Pageable pageable);

    /**
     * 특정 채팅방에서, 특정 시각(사용자 참여 시각) 이후에 생성된
     * 가장 최신의 메시지 1개를 조회합니다. (채팅방 목록의 마지막 메시지 표시용)
     * 채팅방 목록 화면에서 각 방의 마지막 대화 내용을 미리 보여줄 때 사용합니다.
     * @param chatRoom  채팅방 Entity
     * @param joinedAt  사용자가 채팅방에 참여한 시각
     * @return 최신 메시지 (Optional)
     */
    Optional<ChatMessage> findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            ChatRoom chatRoom,
            LocalDateTime joinedAt
    );

    /** ID 기준 카운트
     * 특정 채팅방에서, 특정 메시지 ID(마지막 읽은 메시지 ID)보다
     * 최신인 메시지의 개수를 카운트합니다. (안 읽은 메시지 수 계산용)
     * 사용자가 이미 한 번 이상 읽은 채팅방의 안 읽은 메시지 개수를 셀 때 사용합니다.
     * @param chatRoom            채팅방 Entity
     * @param lastReadMessageId   마지막으로 읽은 메시지의 ID
     * @return 안 읽은 메시지 개수
     */
    long countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
            ChatRoom chatRoom,
            Long lastReadMessageId
    );

    /** 시간 기준 카운트
     * 특정 채팅방에서, 특정 시각(사용자 참여 시각) 이후에 생성된
     * 메시지의 총 개수를 카운트합니다.
     * 사용자가 방에 참여한 후 아직 한 번도 읽지 않은 채팅방의 안 읽은 메시지 개수를 셀 때 사용합니다.
     * @param chatRoom  채팅방 Entity
     * @param joinedAt  사용자가 채팅방에 참여한 시각
     * @return 메시지 개수
     */
    long countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(
            ChatRoom chatRoom,
            LocalDateTime joinedAt
    );

    /**
     * 특정 기간 사이의 모든 메시지를 조회합니다. (시간 오름차순)
     * updateLastReadMessageId에서 사용자가 '방금 읽은' 메시지 목록을 가져올 때 사용합니다.
     */
    List<ChatMessage> findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            Long roomId,
            LocalDateTime afterTime,
            LocalDateTime untilTime
    );
}
