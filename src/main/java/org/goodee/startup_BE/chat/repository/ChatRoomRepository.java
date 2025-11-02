package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

}
