package org.goodee.startup_BE.chat.service;

import org.goodee.startup_BE.chat.dto.ChatMessageResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomListResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomResponseDTO;
import org.goodee.startup_BE.chat.dto.MessageSendPayloadDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {

    /**
     * 채팅방 생성
     * @param creatorUsername 생성자 username
     * @param roomName 채팅방 이름
     * @param inviteeEmployeeIds 초기 초대 대상 username 목록 (생성자 제외)
     * @return 생성된 채팅방 정보 DTO
     */
    ChatRoomResponseDTO createRoom(String creatorUsername, String roomName, List<Long> inviteeEmployeeIds);

    /**
     * 채팅방에 사용자 초대
     * @param inviterUsername 초대자 username (방 멤버여야 함)
     * @param roomId 대상 채팅방 ID
     * @param inviteeEmployeeIds 초대 대상 username 목록
     */
    void inviteToRoom(String inviterUsername, Long roomId, List<Long> inviteeEmployeeIds);

    /**
     * 채팅방 나가기
     * @param username 나가려는 사용자 username
     * @param roomId 대상 채팅방 ID
     */
    void leaveRoom(String username, Long roomId);

    /**
     * 메시지 전송
     * @param senderUsername 메시지 발신자 username
     * @param roomId 대상 채팅방 ID
     * @param payload 메시지 DTO (텍스트 전용)
     * @return 전송된 메시지 정보 DTO
     */
    ChatMessageResponseDTO sendMessage(String senderUsername, Long roomId, MessageSendPayloadDTO payload);

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션)
     * @param username 조회 요청자 username (참여 시각 기준으로 메시지 필터링)
     * @param roomId 대상 채팅방 ID
     * @param pageable 페이지 정보
     * @return 메시지 목록 페이지 DTO
     */
    Page<ChatMessageResponseDTO> getMessages(String username, Long roomId, Pageable pageable);

    /**
     * 마지막 읽은 메시지 ID 업데이트
     * @param username 사용자 username
     * @param roomId 대상 채팅방 ID
     * @param lastMessageId 마지막으로 읽은 메시지 ID
     */
    void updateLastReadMessageId(String username, Long roomId, Long lastMessageId);

    /**
     * 사용자가 속한 채팅방 목록을 조회 한다.
     * @param username 사용자 username
     * @return 해당 사용자가 속한 채팅방 DTO 목록
     */
    List<ChatRoomListResponseDTO> findRoomsByUsername(String username);

    /**
     * ID로 특정 채팅방 정보 조회 (알림 클릭 시 사용)
     *
     * @param username 조회 요청한 사용자 (권한 확인용)
     * @param roomId 조회할 채팅방 ID
     * @return 채팅방 상세 정보 DTO
     */
    ChatRoomListResponseDTO getRoomById(String username, Long roomId);

    /**
     * HTTP를 통한 파일 첨부 메시지 전송
     * @param senderUsername 메시지 발신자 username
     * @param roomId 대상 채팅방 ID
     * @param content 텍스트 내용 (파일만 보낼 시 "" or null)
     * @param files 업로드할 파일 목록
     */
    void sendMessageWithFiles(String senderUsername, Long roomId, String content, List<MultipartFile> files);
}
