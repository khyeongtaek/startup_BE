package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 사용자 ID를 기준으로 1:1 채팅방(isTeam = false)이 존재하는지 확인합니다.
     * 두 사용자 모두 활성 상태(isDeleted=false)인 경우만 확인합니다.
     */
    @Query("SELECT ce1.chatRoom FROM ChatEmployee ce1 " +
            "JOIN ChatEmployee ce2 ON ce1.chatRoom = ce2.chatRoom " +
            "WHERE ce1.employee.employeeId = :userId1 " +
            "AND ce2.employee.employeeId = :userId2 " +
            "AND ce1.chatRoom.isTeam = false " +
            "AND ce1.employee IS NOT NULL " +
            "AND ce2.employee IS NOT NULL " +
            "ORDER BY ce1.chatRoom.createdAt DESC")
    List<ChatRoom> findExistingOneOnOneRooms(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

}
