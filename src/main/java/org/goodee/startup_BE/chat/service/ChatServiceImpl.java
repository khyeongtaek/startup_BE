package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.chat.dto.*;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.chat.repository.ChatEmployeeRepository;
import org.goodee.startup_BE.chat.repository.ChatMessageRepository;
import org.goodee.startup_BE.chat.repository.ChatRoomRepository;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEmployeeRepository chatEmployeeRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final AttachmentFileRepository attachmentFileRepository;

    private final NotificationService notificationService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AttachmentFileService attachmentFileService;

    private static final String FILE_UPLOAD_MESSAGE = "파일을 전송 했습니다.";

    @Override
    public ChatRoomResponseDTO createRoom(String creatorUsername, String roomName, List<Long> inviteeEmployeeIds) {
        Employee creator = employeeRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재 하지 않은 사원 입니다"));

        Set<Long> inviteeEmployeeId = new LinkedHashSet<>(inviteeEmployeeIds);
        if (inviteeEmployeeId.isEmpty()) {
            throw new IllegalArgumentException("최소 한 명 이상 초대해야 합니다");
        }
        if (inviteeEmployeeId.contains(creator.getEmployeeId())) {
            throw new IllegalArgumentException("초대할 수 있는 대상이 아닙니다");
        }

        List<Employee> invitees = employeeRepository.findAllById(inviteeEmployeeId);
        if (invitees.size() != inviteeEmployeeId.size()) {
            throw new EntityNotFoundException("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }

        List<Employee> allParticipants = new ArrayList<>();
        allParticipants.add(creator);
        allParticipants.addAll(invitees);

        boolean isTeamChat = invitees.size() >= 2;

        if (!isTeamChat && invitees.size() == 1) {
            Employee invitee = invitees.get(0);

            List<ChatRoom> existingRoomOpt = chatRoomRepository.findExistingOneOnOneRooms(creator.getEmployeeId(), invitee.getEmployeeId());

            if (!existingRoomOpt.isEmpty()) {
                ChatRoom existingRoom = existingRoomOpt.get(0);
                Long existingRoomId = existingRoom.getChatRoomId();

                List<ChatEmployee> members = chatEmployeeRepository.findAllByChatRoomChatRoomId(existingRoomId);
                ChatMessage lastMessageForNotify = null;

                long currentMemberCount = members.stream().filter(member -> Boolean.FALSE.equals(member.getIsLeft())).count();

                for (ChatEmployee member : members) {
                    if (Boolean.TRUE.equals(member.getIsLeft())) {
                        member.rejoinChatRoom();
                        member.rejoinChatRoom();
                        chatEmployeeRepository.save(member);
                    }
                    lastMessageForNotify = member.getLastReadMessage();
                }

                ChatRoomResponseDTO roomDTO = ChatRoomResponseDTO.toDTO(existingRoom, currentMemberCount);
                String inviteePositionName = (invitee.getPosition() != null) ? invitee.getPosition().getValue1() : "";

                roomDTO = ChatRoomResponseDTO.builder()
                        .chatRoomId(existingRoom.getChatRoomId())
                        .name(existingRoom.getEmployee().getName())
                        .displayName(invitee.getName())
                        .isTeam(false)
                        .createdAt(existingRoom.getCreatedAt())
                        .memberCount(currentMemberCount)
                        .profileImg(invitee.getProfileImg())
                        .positionName(inviteePositionName)
                        .build();

                final ChatMessage finalLastMessage = lastMessageForNotify;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyParticipantsOfNewMessage(existingRoom, finalLastMessage, creator.getEmployeeId());
                    }
                });

                return roomDTO;
            }
        }

        ChatRoom chatRoom = ChatRoom.createChatRoom(creator, roomName, isTeamChat);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        Long chatRoomId = savedChatRoom.getChatRoomId();

        String systemMessageContent = String.format("%s님이 채팅방을 생성 했습니다.", creator.getName());
        ChatMessage initialMessage = ChatMessage.createSystemMessage(savedChatRoom, systemMessageContent);
        ChatMessage savedInitialMessage = chatMessageRepository.save(initialMessage);

        List<ChatEmployee> chatEmployees = allParticipants.stream()
                .map(emp -> ChatEmployee.createChatEmployee(
                        emp,
                        savedChatRoom,
                        roomName,
                        savedInitialMessage
                ))
                .collect(Collectors.toList());

        chatEmployeeRepository.saveAll(chatEmployees);

        ChatMessageResponseDTO messageDTO = ChatMessageResponseDTO.toDTO(savedInitialMessage);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + chatRoomId, messageDTO);
                notifyParticipantsOfNewMessage(savedChatRoom, savedInitialMessage, null);
            }
        });

        if (isTeamChat) {
            try {
                CommonCode chatInviteCode = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("요청 하신 CommonCOde 또는 KeyWord 를 찾을 수 없습니다"));
                Long chatInviteCodeId = chatInviteCode.getCommonCodeId();
                for (Employee recipient : invitees) {
                    NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                            .employeeId(recipient.getEmployeeId())
                            .ownerTypeCommonCodeId(chatInviteCodeId)
                            .url("/chat/rooms/" + chatRoomId)
                            .title(roomName + "채팅방에 초대 되었습니다 ")
                            .content(String.format("%s님이 채팅방에 초대 하였습니다.", creator.getName()))
                            .build();
                    notificationService.create(notificationRequestDTO);
                }
            } catch (Exception e) {
                log.warn("채팅방 생성 알림 전송에 실패 하였습니다.", chatRoomId, e.getMessage());
            }
        }

        Long memberCount = (long) (allParticipants.size());
        ChatRoomResponseDTO responseDTO = ChatRoomResponseDTO.toDTO(savedChatRoom, memberCount);

        if (!isTeamChat && invitees.size() == 1) {
            Employee invitee = invitees.get(0);
            String inviteePositionName = (invitee.getPosition() != null) ? invitee.getPosition().getValue1() : "";

            responseDTO = ChatRoomResponseDTO.builder()
                    .chatRoomId(savedChatRoom.getChatRoomId())
                    .name(savedChatRoom.getEmployee().getName())
                    .displayName(invitee.getName())
                    .isTeam(false)
                    .createdAt(savedChatRoom.getCreatedAt())
                    .memberCount(memberCount)
                    .profileImg(invitee.getProfileImg())
                    .positionName(inviteePositionName)
                    .build();
        }

        return responseDTO;
    }

    @Override
    @Transactional
    public void inviteToRoom(String inviterUsername, Long chatRoomId, List<Long> inviteeEmployeeIds) {
        Employee inviter = employeeRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원입니다."));

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        if (!chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(chatRoomId, inviter.getEmployeeId())) {
            throw new AccessDeniedException("채팅방 구성원이 아니면 초대할 수 없습니다.");
        }

        Set<Long> idSet = new LinkedHashSet<>(inviteeEmployeeIds);
        idSet.remove(inviter.getEmployeeId());
        if (idSet.isEmpty()) return;

        List<Employee> candidates = employeeRepository.findAllById(idSet);
        if (candidates.size() != idSet.size()) {
            throw new EntityNotFoundException("초대 대상 중 존재하지 않는 사원 ID가 있습니다.");
        }

        Map<Long, ChatEmployee> existingMembersMap = chatEmployeeRepository
                .findAllByChatRoomChatRoomId(chatRoomId)
                .stream()
                .collect(Collectors.toMap(ce -> ce.getEmployee().getEmployeeId(), ce -> ce));

        List<Employee> newInvitees = new ArrayList<>();
        List<ChatEmployee> rejoiningMembers = new ArrayList<>();

        for (Employee candidate : candidates) {
            ChatEmployee existingMember = existingMembersMap.get(candidate.getEmployeeId());
            if (existingMember == null) {
                newInvitees.add(candidate);
            } else if (Boolean.TRUE.equals(existingMember.getIsLeft())) {
                existingMember.rejoinChatRoom();
                existingMember.rejoinChatRoom();
                rejoiningMembers.add(existingMember);
            }
        }

        if (newInvitees.isEmpty() && rejoiningMembers.isEmpty()) return;

        if (Boolean.FALSE.equals(room.getIsTeam()) && !newInvitees.isEmpty()) {
            room.updateToTeamRoom();
        }

        List<Employee> allAffectedInvitees = new ArrayList<>(newInvitees);
        allAffectedInvitees.addAll(rejoiningMembers.stream().map(ChatEmployee::getEmployee).toList());

        String names = allAffectedInvitees.stream().map(Employee::getName).collect(Collectors.joining(", "));
        ChatMessage systemMsg = chatMessageRepository.save(
                ChatMessage.createSystemMessage(room, inviter.getName() + "님이 " + names + "님을 초대했습니다.")
        );

        List<ChatEmployee> newLinks = newInvitees.stream()
                .map(emp -> ChatEmployee.createChatEmployee(emp, room, room.getName(), systemMsg))
                .toList();

        chatEmployeeRepository.saveAll(newLinks);
        chatEmployeeRepository.saveAll(rejoiningMembers);

        Long finalRoomId = room.getChatRoomId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId,
                        ChatMessageResponseDTO.toDTO(systemMsg));

                notifyParticipantsOfNewMessage(room, systemMsg, null);
            }
        });

        try {
            Long codeId = commonCodeRepository
                    .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                    .stream().findFirst().orElseThrow().getCommonCodeId();

            for (Employee target : allAffectedInvitees) {
                notificationService.create(
                        NotificationRequestDTO.builder()
                                .employeeId(target.getEmployeeId())
                                .ownerTypeCommonCodeId(codeId)
                                .url("/chat/rooms/" + finalRoomId)
                                .title(room.getName() + "팀 채팅방에 초대되었습니다.")
                                .content(inviter.getName() + "님이 채팅방에 초대했습니다.")
                                .build()
                );
            }
        } catch (Exception e) {
            log.warn("Invite notification failed. roomId={}, error={}", finalRoomId, e.getMessage());
        }
    }

    @Override
    public void leaveRoom(String username, Long roomId) {
        Employee leaver = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));
        ChatEmployee chatEmployee = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, leaver.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방의 멤버가 아닙니다."));

        final boolean isTeamRoom = Boolean.TRUE.equals(room.getIsTeam());
        chatEmployee.leftChatRoom();
        chatEmployeeRepository.save(chatEmployee);

        final ChatMessage systemMessage = isTeamRoom
                ? chatMessageRepository.save(
                ChatMessage.createSystemMessage(room, leaver.getName() + "님이 채팅방에서 나가셨습니다."))
                : null;

        long remainingMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(roomId);
        if (remainingMemberCount == 0) {
            room.deleteRoom();
            chatRoomRepository.save(room);
        }
        Long finalRoomId = room.getChatRoomId();

        if (systemMessage != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId,
                            ChatMessageResponseDTO.toDTO(systemMessage));
                    notifyParticipantsOfNewMessage(room, systemMessage, null);
                }
            });
        }
    }

    @Override
    public ChatMessageResponseDTO sendMessage(String senderUsername, Long roomId, MessageSendPayloadDTO payload) {
        String content = payload.getContent();
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

        if (Boolean.FALSE.equals(room.getIsTeam())) {
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(roomId);

            for (ChatEmployee member : allMembers) {
                if (!member.getEmployee().getEmployeeId().equals(sender.getEmployeeId()) && Boolean.TRUE.equals(member.getIsLeft())) {
                    member.rejoinChatRoom();
                    chatEmployeeRepository.save(member);
                }
            }
        }

        ChatMessage message = chatMessageRepository.save(
                ChatMessage.createChatMessage(room, sender, content.trim())
        );

        membership.updateLastReadMessage(message);
        chatEmployeeRepository.save(membership);

        long unread = 0;
        if (Boolean.TRUE.equals(room.getIsTeam())) {
            unread = chatEmployeeRepository.countUnreadForMessage(
                    room.getChatRoomId(),
                    sender.getEmployeeId(),
                    message.getCreatedAt()
            );
        } else {
            unread = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                    room.getChatRoomId(),
                    sender.getEmployeeId()
            );
        }

        Long finalRoomId = room.getChatRoomId();
        ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(message);
        dto.setUnreadCount(unread);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId, dto);
                notifyParticipantsOfNewMessage(room, message, sender.getEmployeeId());
            }
        });

        return dto;
    }

    @Override
    public Page<ChatMessageResponseDTO> getMessages(String username, Long roomId, Pageable pageable) {
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, username)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        // joinedAt 이후의 메시지만 가져옵니다
        Page<ChatMessage> page = chatMessageRepository
                .findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                        roomId,
                        membership.getJoinedAt(),
                        pageable
                );

        Page<ChatMessage> globalLatestPage = chatMessageRepository
                .findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                        roomId,
                        LocalDateTime.of(1900, 1, 1, 0, 0),
                        Pageable.ofSize(1)
                );

        if (globalLatestPage.hasContent()) {
            ChatMessage globalLatestMessage = globalLatestPage.getContent().get(0);
            ChatMessage currentLastRead = membership.getLastReadMessage();

            boolean needUpdate = false;

            if (currentLastRead == null) {
                needUpdate = true;
            } else {
                if (!currentLastRead.getChatMessageId().equals(globalLatestMessage.getChatMessageId())
                        && globalLatestMessage.getCreatedAt().isAfter(currentLastRead.getCreatedAt())) {
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                LocalDateTime start = (currentLastRead != null)
                        ? currentLastRead.getCreatedAt()
                        : LocalDateTime.of(1900, 1, 1, 0, 0);
                LocalDateTime end = globalLatestMessage.getCreatedAt();

                List<ChatMessage> newlyReadMessages = chatMessageRepository
                        .findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                                roomId, start, end
                        );

                // DB 업데이트: 사용자의 읽은 위치를 "전체 최신 메시지"로 이동
                membership.updateLastReadMessage(globalLatestMessage);
                chatEmployeeRepository.save(membership);

                List<Map<String, Object>> unreadUpdates = new ArrayList<>();
                for (ChatMessage msg : newlyReadMessages) {
                    if (msg.getEmployee() != null) {
                        long count = chatEmployeeRepository.countUnreadByAllParticipants(roomId, msg.getCreatedAt());
                        unreadUpdates.add(Map.of(
                                "chatMessageId", msg.getChatMessageId(),
                                "newUnreadCount", count
                        ));
                    }
                }

                long currentUnreadCount = 0; // 다 읽었으므로 0

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 상대방 화면의 '1'을 없애기 위해 브로드캐스트
                        if (!unreadUpdates.isEmpty()) {
                            simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId + "/unread-updates",
                                    Map.of("unreadUpdates", unreadUpdates));
                        }
                        calculateAndSendTotalUnreadCount(membership.getEmployee().getEmployeeId(), username);
                        sendRoomListUpdateToUser(membership.getEmployee(), membership.getChatRoom(), currentUnreadCount);
                    }
                });
            }
        }

        // 반환값은 여전히 joinedAt 기준으로 보여줍니다 (삭제된 이력 안 보이게)
        List<Long> messageIds = page.getContent().stream()
                .map(ChatMessage::getChatMessageId)
                .toList();

        if (messageIds.isEmpty()) {
            return page.map(ChatMessageResponseDTO::toDTO);
        }

        CommonCode chatOwnerType = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name())
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Code Not Found"));

        List<AttachmentFile> allAttachments =
                attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(chatOwnerType, messageIds);

        Map<Long, List<AttachmentFileResponseDTO>> attachmentsMap = allAttachments.stream()
                .collect(Collectors.groupingBy(
                        AttachmentFile::getOwnerId,
                        Collectors.mapping(AttachmentFileResponseDTO::toDTO, Collectors.toList())));

        return page.map(message -> {
            ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(message);
            if (message.getEmployee() != null) {
                long currentUnreadCount = chatEmployeeRepository.countUnreadByAllParticipants(roomId, message.getCreatedAt());
                dto.setUnreadCount(currentUnreadCount);
            }
            List<AttachmentFileResponseDTO> attachments = attachmentsMap.get(message.getChatMessageId());
            dto.setAttachments(attachments);
            if (attachments != null && !attachments.isEmpty() && FILE_UPLOAD_MESSAGE.equals(dto.getContent())) {
                dto.setContent(null);
            }
            return dto;
        });
    }

    @Override
    public void updateLastReadMessageId(String username, Long roomId, Long lastMessageId) {
        Employee reader = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, reader.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Access Denied"));

        ChatMessage finalMessageToRead;

        if (lastMessageId == null) {
            finalMessageToRead = chatMessageRepository
                    .findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            membership.getChatRoom(),
                            membership.getJoinedAt()
                    )
                    .orElse(null);
        } else {
            finalMessageToRead = chatMessageRepository.findById(lastMessageId)
                    .orElseThrow(() -> new EntityNotFoundException("Message not found"));
            if (!Objects.equals(finalMessageToRead.getChatRoom().getChatRoomId(), roomId)) {
                throw new AccessDeniedException("Wrong Room");
            }
        }
        if (finalMessageToRead == null) return;

        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getChatMessageId().equals(finalMessageToRead.getChatMessageId())) {
            return;
        }

        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(finalMessageToRead.getCreatedAt())) {
            return;
        }

        ChatMessage oldLastReadMessage = membership.getLastReadMessage();
        LocalDateTime start = (oldLastReadMessage != null)
                ? oldLastReadMessage.getCreatedAt()
                : LocalDateTime.of(1900, 1, 1, 0, 0);

        LocalDateTime end = finalMessageToRead.getCreatedAt();

        membership.updateLastReadMessage(finalMessageToRead);
        chatEmployeeRepository.save(membership);

        List<ChatMessage> messagesJustRead = chatMessageRepository
                .findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                        roomId, start, end
                );

        List<Map<String, Object>> unreadUpdates = new ArrayList<>();
        for (ChatMessage message : messagesJustRead) {
            if (message.getEmployee() != null) {
                long newUnreadCount = chatEmployeeRepository.countUnreadByAllParticipants(roomId, message.getCreatedAt());
                unreadUpdates.add(Map.of(
                        "chatMessageId", message.getChatMessageId(),
                        "newUnreadCount", newUnreadCount
                ));
            }
        }

        long currentUnreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                membership.getChatRoom(),
                finalMessageToRead.getChatMessageId()
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!unreadUpdates.isEmpty()) {
                    simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId + "/unread-updates",
                            Map.of("unreadUpdates", unreadUpdates));
                }
                calculateAndSendTotalUnreadCount(reader.getEmployeeId(), reader.getUsername());
                sendRoomListUpdateToUser(reader, membership.getChatRoom(), currentUnreadCount);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomListResponseDTO> findRoomsByUsername(String username) {
        Employee user = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));

        List<ChatEmployee> memberships = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user);
        List<ChatRoomListResponseDTO> dtos = new ArrayList<>();

        for (ChatEmployee membership : memberships) {
            ChatRoom room = membership.getChatRoom();
            Optional<ChatMessage> optLastMessage = chatMessageRepository
                    .findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(room, membership.getJoinedAt());
            long unreadCount = 0;
            if (membership.getLastReadMessage() != null &&
                    membership.getLastReadMessage().getCreatedAt().isAfter(membership.getJoinedAt())) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                        room, membership.getLastReadMessage().getChatMessageId());
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, membership.getJoinedAt());
            }
            long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

            if (Boolean.FALSE.equals(room.getIsTeam())) {
                List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room.getChatRoomId());
                Optional<Employee> otherUserOpt = allMembers.stream()
                        .map(ChatEmployee::getEmployee)
                        .filter(emp -> !emp.getEmployeeId().equals(user.getEmployeeId()))
                        .findFirst();
                String otherUserName = "정보 없음";
                String otherUserProfileImg = null;
                String otherUserPosition = "";
                if (otherUserOpt.isPresent()) {
                    Employee otherUser = otherUserOpt.get();
                    otherUserName = otherUser.getName();
                    otherUserProfileImg = otherUser.getProfileImg();
                    if (otherUser.getPosition() != null) otherUserPosition = otherUser.getPosition().getValue1();
                }
                dtos.add(ChatRoomListResponseDTO.toDTO(room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, optLastMessage, currentMemberCount));
            } else {
                dtos.add(ChatRoomListResponseDTO.toDTO(room, unreadCount, optLastMessage, currentMemberCount));
            }
        }
        dtos.sort((dto1, dto2) -> {
            if (dto1.getLastMessageCreatedAt() == null) return 1;
            if (dto2.getLastMessageCreatedAt() == null) return -1;
            return dto2.getLastMessageCreatedAt().compareTo(dto1.getLastMessageCreatedAt());
        });
        return dtos;
    }

    private void notifyParticipantsOfNewMessage(ChatRoom room, ChatMessage message, Long senderId) {
        List<ChatEmployee> participants = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());
        for (ChatEmployee participant : participants) {
            Employee recipient = participant.getEmployee();
            if (recipient == null) continue;
            sendRoomListUpdateToUser(recipient, room, null);
            if (senderId == null || !recipient.getEmployeeId().equals(senderId)) {
                calculateAndSendTotalUnreadCount(recipient.getEmployeeId(), recipient.getUsername());
            }
        }
    }

    private void sendRoomListUpdateToUser(Employee recipient, ChatRoom room, Long specificUnreadCount) {
        ChatEmployee participant = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeEmployeeId(room.getChatRoomId(), recipient.getEmployeeId())
                .orElse(null);

        if (participant == null || Boolean.TRUE.equals(participant.getIsLeft())) return;

        long unreadCount;

        if (specificUnreadCount != null) {
            unreadCount = specificUnreadCount;
        } else {
            if (participant.getLastReadMessage() != null &&
                    participant.getLastReadMessage().getCreatedAt().isAfter(participant.getJoinedAt())) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                        room,
                        participant.getLastReadMessage().getChatMessageId()
                );
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, participant.getJoinedAt());
            }
        }

        Optional<ChatMessage> lastMessage = chatMessageRepository
                .findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(room, participant.getJoinedAt());
        long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

        ChatRoomListResponseDTO listDto;
        if (Boolean.FALSE.equals(room.getIsTeam())) {
            String otherUserName;
            String otherUserProfileImg;
            String otherUserPosition = "";
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room.getChatRoomId());
            Employee otherUser = allMembers.stream()
                    .map(ChatEmployee::getEmployee)
                    .filter(emp -> emp != null && !emp.getEmployeeId().equals(recipient.getEmployeeId()))
                    .findFirst()
                    .orElse(null);

            if (otherUser == null) {
                otherUserName = "정보 없음";
                otherUserProfileImg = null;
            } else {
                otherUserName = otherUser.getName();
                otherUserProfileImg = otherUser.getProfileImg();
                if (otherUser.getPosition() != null) {
                    otherUserPosition = otherUser.getPosition().getValue1();
                }
            }
            listDto = ChatRoomListResponseDTO.toDTO(
                    room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, lastMessage, currentMemberCount
            );
        } else {
            listDto = ChatRoomListResponseDTO.toDTO(
                    room, unreadCount, lastMessage, currentMemberCount
            );
        }

        simpMessagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/chat-list-update",
                listDto
        );
    }

    private void calculateAndSendTotalUnreadCount(Long employeeId, String username) {
        long totalUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(employeeId);
        simpMessagingTemplate.convertAndSendToUser(
                username,
                "/queue/unread-count",
                new TotalUnreadCountResponseDTO(totalUnread)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomListResponseDTO getRoomById(String username, Long roomId) {
        Employee user = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, user.getEmployeeId())
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));
        if (Boolean.FALSE.equals(membership.getChatRoom().getIsTeam()) && Boolean.TRUE.equals(membership.getIsLeft())) {
            membership.rejoinChatRoom();
            membership.rejoinChatRoom();
            chatEmployeeRepository.save(membership);
        }
        ChatRoom room = membership.getChatRoom();
        Optional<ChatMessage> optLastMessage = chatMessageRepository
                .findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(room, membership.getJoinedAt());
        long unreadCount = 0;
        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(membership.getJoinedAt())) {
            unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                    room, membership.getLastReadMessage().getChatMessageId());
        } else {
            unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, membership.getJoinedAt());
        }
        long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

        if (Boolean.FALSE.equals(room.getIsTeam())) {
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room.getChatRoomId());
            Optional<Employee> otherUserOpt = allMembers.stream()
                    .map(ChatEmployee::getEmployee)
                    .filter(emp -> !emp.getEmployeeId().equals(user.getEmployeeId()))
                    .findFirst();
            if (otherUserOpt.isPresent()) {
                Employee otherUser = otherUserOpt.get();
                String otherUserName;
                String otherUserProfileImg;
                String otherUserPosition = "";
                if (otherUser == null) {
                    otherUserName = "정보 없음";
                    otherUserProfileImg = null;
                } else {
                    otherUserName = otherUser.getName();
                    otherUserProfileImg = otherUser.getProfileImg();
                    if (otherUser.getPosition() != null) otherUserPosition = otherUser.getPosition().getValue1();
                }
                return ChatRoomListResponseDTO.toDTO(room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, optLastMessage, currentMemberCount);
            } else {
                throw new EntityNotFoundException("1:1 채팅방의 상대방 정보를 찾을 수 없습니다.");
            }
        } else {
            return ChatRoomListResponseDTO.toDTO(room, unreadCount, optLastMessage, currentMemberCount);
        }
    }

    @Transactional
    public void sendMessageWithFiles(String senderUsername, Long roomId, String content, List<MultipartFile> files) {
        Employee sender = employeeRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방 입니다"));
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, senderUsername)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        if (Boolean.FALSE.equals(room.getIsTeam())) {
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(roomId);
            for (ChatEmployee member : allMembers) {
                if (!member.getEmployee().getEmployeeId().equals(sender.getEmployeeId()) && Boolean.TRUE.equals(member.getIsLeft())) {
                    member.rejoinChatRoom();
                    // null 초기화 제거
                    chatEmployeeRepository.save(member);
                }
            }
        }

        String messageContent;
        boolean isPlaceholder = false;
        if (content == null || content.isBlank()) {
            messageContent = FILE_UPLOAD_MESSAGE;
            isPlaceholder = true;
        } else {
            messageContent = content.trim();
        }

        ChatMessage message = ChatMessage.createChatMessage(room, sender, messageContent);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        Long chatMessageId = savedMessage.getChatMessageId();

        CommonCode chatOwnerType = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name())
                .stream().findFirst().orElseThrow(() -> new EntityNotFoundException("Code Not Found"));

        List<AttachmentFileResponseDTO> finalAttachments =
                attachmentFileService.uploadFiles(files, chatOwnerType.getCommonCodeId(), chatMessageId);

        membership.updateLastReadMessage(savedMessage);
        chatEmployeeRepository.save(membership);

        long unread = 0;
        if (Boolean.TRUE.equals(room.getIsTeam())) {
            unread = chatEmployeeRepository.countUnreadForMessage(room.getChatRoomId(), sender.getEmployeeId(), message.getCreatedAt());
        } else {
            unread = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(room.getChatRoomId(), sender.getEmployeeId());
        }
        Long finalRoomId = room.getChatRoomId();
        ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(savedMessage);
        dto.setUnreadCount(unread);
        dto.setAttachments(finalAttachments);
        if (isPlaceholder) {
            dto.setContent(null);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId, dto);
                notifyParticipantsOfNewMessage(room, savedMessage, sender.getEmployeeId());
            }
        });
    }
}