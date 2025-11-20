package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ChatEmployeeRepository extends JpaRepository<ChatEmployee, Long> {

    /**
     * 특정 사용자가 속한 (isLeft=false) 모든 채팅방 정보를 조회합니다.
     * 채팅방 목록 조회 시 사용됩니다.
     *
     * @param employee 사용자 Entity
     * @return ChatEmployee 목록
     */
    List<ChatEmployee> findAllByEmployeeAndIsLeftFalse(Employee employee);

    /**
     * 특정 채팅방의 (isLeft=false) 모든 정보를 조회합니다.
     * 1:1 채팅방에서 상대방 정보를 찾을 때 사용됩니다.
     *
     * @param roomId 채팅방 ID
     * @return ChatEmployee 목록
     */
    List<ChatEmployee> findAllByChatRoomChatRoomIdAndIsLeftFalse(Long roomId);

    /**
     * 특정 채팅방의 모든 (isLeft 무관) 멤버 정보를 조회합니다.
     * 1:1 채팅방에서 '나간' 상대방을 찾거나 재활성화할 때 사용됩니다.
     *
     * @param roomId 채팅방 ID
     * @return ChatEmployee 목록
     */
    List<ChatEmployee> findAllByChatRoomChatRoomId(Long roomId);

    /**
     * 특정 채팅방과 특정 사원ID 참여 정보 조회
     * 권한 확인, 상태 업데이트 등에 사용됩니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param employeeId 직원 ID
     * @return 참여 정보 (Optional)
     */
    Optional<ChatEmployee> findByChatRoomChatRoomIdAndEmployeeEmployeeId(Long chatRoomId, Long employeeId);

    /**
     * 특정 채팅방과 특정 사원 username으로 참여 정보 조회 (isLeft=false 조건 포함)
     * 권한 확인 등에 사용됩니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param username   직원 username
     * @return 참여 정보 (Optional)
     */
    Optional<ChatEmployee> findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(Long chatRoomId, String username);

    /**
     * 특정 채팅방(roomId)에 특정 직원(employeeId)이 참여하고 있는지 (isDeleted=false, isLeft=무관) 확인합니다.
     * 초대 권한 체크 등에서 사용됩니다.
     *
     * @param chatRoomId 채팅방 ID
     * @param employeeId 확인할 직원 ID
     * @return 참여 중이면 true, 아니면 false
     */
    boolean existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(Long chatRoomId, Long employeeId);

    /**
     * 특정 채팅방(roomId)의 활성(isLeft=false) 멤버들의 Employee ID 목록만 조회합니다.
     * 초대 시 중복 방지, 멤버십 집계 등에 사용됩니다.
     *
     * @param roomId 채팅방 ID
     * @return 활성 멤버들의 Employee ID Set
     */
    @Query("""
                        SELECT ce.employee.employeeId
                        FROM ChatEmployee ce
                        WHERE ce.chatRoom.chatRoomId = :roomId
                        AND ce.isLeft = false
            """)
    Set<Long> findActiveEmployeeIdsByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 채팅방(chatRoomId)의 활성(isLeft=false) 멤버 수를 조회합니다.
     * 잔여 인원 0명 시 방 삭제 등 판단에 사용됩니다.
     * 팀 채팅방 헤더에 인원 수를 표시할 때 사용됩니다.
     * @param chatRoomId 채팅방 ID
     * @return 활성 멤버 수
     */
    long countByChatRoomChatRoomIdAndIsLeftFalse(Long chatRoomId);

    @Query("""
               SELECT COUNT(ce) FROM ChatEmployee ce
               LEFT JOIN ce.lastReadMessage lrm
               WHERE ce.chatRoom.chatRoomId = :roomId
                 AND ce.isLeft = false
                 AND ce.employee.employeeId <> :senderId
                 AND ce.joinedAt <= :messageCreatedAt
                 AND (lrm IS NULL OR lrm.createdAt < :messageCreatedAt)
            """)
    long countUnreadForMessage(@Param("roomId") Long roomId,
                               @Param("senderId") Long senderId,
                               @Param("messageCreatedAt") LocalDateTime messageCreatedAt);

    /**
     * 특정 사용자의 모든 채팅방에서 안 읽은 메시지의 총 개수를 합산
     * ChatEmployee의 lastReadMessage ID와 ChatMessage의 ID를 비교하여 계산
     * 메시지 발신자가 삭제되지 않은 경우만 카운트
     *
     * @param employeeId 안 읽은 개수를 계산할 직원의 ID
     * @return 총 안 읽은 메시지 개수
     */
    @Query("""
        SELECT COALESCE(SUM(
            (SELECT COUNT(m.chatMessageId)
             FROM ChatMessage m
             WHERE m.chatRoom = ce.chatRoom
             AND m.chatMessageId > ce.lastReadMessage.chatMessageId
             AND m.employee IS NOT NULL AND m.employee.employeeId <> :employeeId
             AND m.employee IS NOT NULL)
        ), 0)
        FROM ChatEmployee ce
        WHERE ce.employee.employeeId = :employeeId
        AND ce.isLeft = false
    """)
    long sumTotalUnreadMessagesByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 1:1 채팅방에서 메시지를 보낸 사람 외에 나가지 않은 참여자 수 Count (무조건 0 또는 1)
     * sendMessage에서 1:1 방의 unreadCount를 1로 설정할 때 사용
     */
    long countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(Long rommId, Long employeeId);

    /**
     * 특정 메시지보다 이전에 마지막으로 읽은, 나가지 않은 참여자 수 Count
     * updateLastReadMessageId에서 '새로운 안 읽음 카운트'를 계산할 때 사용
     * (기존 countUnreadForMessage와 달리 employeeId를 제외하지 않습니다)
     */
    @Query("""
           SELECT COUNT(ce) FROM ChatEmployee ce
           LEFT JOIN ce.lastReadMessage lrm
           WHERE ce.chatRoom.chatRoomId = :roomId
             AND ce.isLeft = false
             AND ce.joinedAt <= :messageCreatedAt
             AND (lrm IS NULL OR lrm.createdAt < :messageCreatedAt)
        """)
    long countUnreadByAllParticipants(
            @Param("roomId") Long roomId,
            @Param("messageCreatedAt") LocalDateTime messageCreatedAt
    );
}
