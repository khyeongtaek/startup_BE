package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface ChatEmployeeRepository extends JpaRepository<ChatEmployee, Long> {

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
     *
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
}
