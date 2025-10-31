package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.chat.dto.ChatMessageResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomResponseDTO;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.chat.repository.ChatEmployeeRepository;
import org.goodee.startup_BE.chat.repository.ChatMessageRepository;
import org.goodee.startup_BE.chat.repository.ChatRoomRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService{

    // 필요한 Repository 주입
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEmployeeRepository chatEmployeeRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;

    // Service 및 Util
    private final NotificationService notificationService;
    private final SimpMessagingTemplate simpMessagingTemplate;


    // 채팅방 생성
    @Override
    public ChatRoomResponseDTO createRoom(String creatorUsername, String roomName, List<Long> inviteeEmployeeIds) {

        // 채팅방 생성자 조회
        Employee creator = employeeRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재 하지 않은 사원 입니다"));

        // 초대 대상 Employee 체크
        Set<Long> inviteeEmployeeId = new LinkedHashSet<>(inviteeEmployeeIds);
        if(inviteeEmployeeId.isEmpty()) {
            throw new IllegalArgumentException("최소 한 명 이상 초대해야 합니다");
        }
        if(inviteeEmployeeId.contains(creator.getEmployeeId())) {
            throw new IllegalArgumentException("초대할 수 있는 대상이 아닙니다");
        }

        // 초대 할 사원 조회 & 유효성 검사
        List<Employee> invitees = employeeRepository.findAllById(inviteeEmployeeId);
        if(invitees.size() != inviteeEmployeeId.size()) {
            throw new EntityNotFoundException("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }
        // 모든 참여자 목록: 채팅방 생성자 + 초대 대상
        List<Employee> allParticipants = new ArrayList<>();
        allParticipants.add(creator);
        allParticipants.addAll(invitees);

        // 팀 채팅방 여부 ( invitees.size() == 0이면 false = 1:1 채팅방, invitees.size() >= 1이면 true = 팀 채팅방)
        boolean isTeamChat = invitees.size() >= 2; // 초대 대상이 2명 이상이면 팀방

        // chatRoom에 저장
        ChatRoom chatRoom = ChatRoom.createChatRoom(creator, roomName, isTeamChat);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        Long chatRoomId = savedChatRoom.getChatRoomId();

        // "채팅방이 생성되었습니다" 시스템 메시지 생성
        String systemMessageContent = String.format("%s님이 채팅방을 생성 했습니다.", creator.getName());
        ChatMessage initialMessage = ChatMessage.createChatMessage(savedChatRoom,null,systemMessageContent);
        ChatMessage savedInitialMessage = chatMessageRepository.save(initialMessage);

        // 채팅방 생성자 및 초대 대상 ChatEmployee 저장
        List<ChatEmployee> chatEmployees = allParticipants.stream()
                .map(emp -> ChatEmployee.createChatEmployee(
                        emp,
                        savedChatRoom,
                        roomName,
                        savedInitialMessage
                ))
                .collect(Collectors.toList());

        chatEmployeeRepository.saveAll(chatEmployees);

        // WebSocket 브로드캐스트 (/topic/rooms/{roomId})
        ChatMessageResponseDTO messageDTO = ChatMessageResponseDTO.toDTO(savedInitialMessage);
        simpMessagingTemplate.convertAndSend("/topic/rooms/" + chatRoomId, messageDTO);

        // TEAM 채팅방 이면, 초대 대상에게 알림 받기
        if(isTeamChat) {
            CommonCode chatInviteCode = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("요청 하신 CommonCOde 또는 KeyWord 를 찾을 수 없습니다"));

            Long chatInviteCodeId = chatInviteCode.getCommonCodeId();

            // 알림 전송 로직 호출
            for(Employee recipient : invitees) {
                NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                        .employeeId(recipient.getEmployeeId())
                        .ownerTypeCommonCodeId(chatInviteCodeId)
                        .url("/chat/rooms/" + chatRoomId)
                        .title(roomName + "채팅방에 초대 되었습니다 ")
                        .content(String.format("%s님이 채팅방에 초대 하였습니다.", creator.getName()))
                        .build();
                notificationService.create(notificationRequestDTO);
            }
        }
        return ChatRoomResponseDTO.toDTO(savedChatRoom);
    }

    // 채팅방에 사용자 초대
    @Transactional
    @Override
    public void inviteToRoom(String inviterUsername, Long chatRoomId, List<Long> inviteeEmployeeIds) {

        Employee inviter = employeeRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원입니다."));

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        // 초대자 구성원 여부 체크
        if (!chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(chatRoomId, inviter.getEmployeeId())) {
            throw new AccessDeniedException("채팅방 구성원이 아니면 초대할 수 없습니다.");
        }

        // 1) ID 중복 제거 + 자기 자신 제거
        Set<Long> idSet = new LinkedHashSet<>(inviteeEmployeeIds);
        idSet.remove(inviter.getEmployeeId());
        if (idSet.isEmpty()) return;

        // 2) ID로 사용자 일괄 조회
        List<Employee> candidates = employeeRepository.findAllById(idSet);

        // 존재 여부 검증
        if (candidates.size() != idSet.size()) {
            throw new EntityNotFoundException("초대 대상 중 존재하지 않는 사원 ID가 있습니다.");
        }

        // 3) 이미 멤버인 인원 제외
        Set<Long> existing = new HashSet<>(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(chatRoomId));
        List<Employee> newInvitees = candidates.stream()
                .filter(e -> !existing.contains(e.getEmployeeId()))
                .toList();
        if (newInvitees.isEmpty()) return;

        // 4) 1:1 → 팀 채팅방 변경
        if (Boolean.FALSE.equals(room.getIsTeam()) && !newInvitees.isEmpty()) {
            room.updateToTeamRoom();
        }

        // 5) 시스템 메시지 + 멤버 추가
        String names = newInvitees.stream().map(Employee::getName).collect(Collectors.joining(", "));
        ChatMessage systemMsg = chatMessageRepository.save(
                ChatMessage.createChatMessage(room, null, inviter.getName() + "님이 " + names + "님을 초대했습니다.")
        );

        List<ChatEmployee> links = newInvitees.stream()
                .map(emp -> ChatEmployee.createChatEmployee(emp, room, room.getName(), systemMsg))
                .toList();
        chatEmployeeRepository.saveAll(links);

        Long finalRoomId = room.getChatRoomId();

        // 6) 커밋 후 알림
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/rooms/" + finalRoomId,
                        ChatMessageResponseDTO.toDTO(systemMsg));

                try {
                    Long codeId = commonCodeRepository
                            .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                            .stream().findFirst()
                            .orElseThrow(() -> new EntityNotFoundException("채팅 초대 CommonCode 없음"))
                            .getCommonCodeId();

                    for (Employee target : newInvitees) {
                        notificationService.create(
                                NotificationRequestDTO.builder()
                                        .employeeId(target.getEmployeeId())
                                        .ownerTypeCommonCodeId(codeId)
                                        .url("/chat/rooms/" + finalRoomId)
                                        .title(room.getName() + " 채팅방에 초대되었습니다.")
                                        .content(inviter.getName() + "님이 채팅방에 초대했습니다.")
                                        .build()
                        );
                    }
                } catch (Exception e) {
                    log.warn("Invite notification failed. roomId={}, error={}", finalRoomId, e.getMessage());
                }
            }
        });
    }
    // 채팅방 나가기
    @Override
    public void leaveRoom(String username, Long roomId) {
        // 사용자, 채팅방 조회
        Employee leaver = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));
        // 채팅방 멤버 조회
        ChatEmployee chatEmployee = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, leaver.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방의 멤버가 아닙니다."));

        // 팀 채팅방 여부 선확인
        final boolean isTeamRoom = Boolean.TRUE.equals(room.getIsTeam());

        // 채팅방 나가기 처리
        chatEmployee.leftChatRoom();
        chatEmployeeRepository.save(chatEmployee);

        // 팀 채팅방인 경우에만 시스템 메시지 생성/브로드캐스트
        final ChatMessage systemMessage = isTeamRoom
                ? chatMessageRepository.save(
                        ChatMessage.createChatMessage(room, null, leaver.getName() + "님이 채팅방에서 나가셨습니다."))
                : null;

        // 남은 인원 수 확인(0명이면 채팅방 삭제)
        long remainingMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(roomId);
        if (remainingMemberCount == 0) {
            room.deleteRoom();
            chatRoomRepository.save(room);
        }
        Long finalRoomId = room.getChatRoomId();

        // WebSocket 브로드캐스트 (/topic/rooms/{roomId})
        if (systemMessage != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/rooms/" + finalRoomId,
                            ChatMessageResponseDTO.toDTO(systemMessage));
                }
            });
        }
    }

    // 채팅방에 메시지 전송
    @Override
    public ChatMessageResponseDTO sendMessage(String senderUsername, Long roomId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
        }

        Employee sender = employeeRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, senderUsername)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.createChatMessage(room, sender, content.trim())
        );

        // 본인 메시지는 바로 읽음 처리
        membership.updateLastReadMessage(message);
        chatEmployeeRepository.save(membership);

        // 팀 채팅방이면 미읽음 수 계산 (본인 제외)
        long unread = 0;
        if (Boolean.TRUE.equals(room.getIsTeam())) {
            unread = chatEmployeeRepository.countUnreadForMessage(
                    room.getChatRoomId(),
                    sender.getEmployeeId(),
                    message.getCreatedAt()
            );
        }

        Long finalRoomId = room.getChatRoomId();
        ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(message);
        dto.setUnreadCount(unread);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/rooms/" + finalRoomId, dto);
            }
        });

        return dto;
    }

    @Override
    public Page<ChatMessageResponseDTO> getMessages(String username, Long roomId, Pageable pageable) {
        // 멤버십 및 참여 시각 확인
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, username)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        Page<ChatMessage> page = chatMessageRepository
                .findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                        roomId,
                        membership.getJoinedAt(),
                        pageable
                );

        return page.map(ChatMessageResponseDTO::toDTO);
    }

    @Override
    public void updateLastReadMessageId(String username, Long roomId, Long lastMessageId) {
        if (lastMessageId == null) {
            throw new IllegalArgumentException("lastMessageId는 필수입니다.");
        }

        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, username)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        ChatMessage message = chatMessageRepository.findById(lastMessageId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 메시지입니다."));

        if (!Objects.equals(message.getChatRoom().getChatRoomId(), roomId)) {
            throw new AccessDeniedException("해당 채팅방의 메시지가 아닙니다.");
        }

        membership.updateLastReadMessage(message);
        chatEmployeeRepository.save(membership);

        // 읽음 브로드캐스트: 프론트는 해당 방의 메시지들 중 msg.id 이하의 미읽음 카운트만 감소
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/reads",
                        Map.of("lastMessageId", membership.getEmployee().getEmployeeId(),
                        "readUpToMessageId", message.getChatMessageId()));
            }
        });
    }
}
