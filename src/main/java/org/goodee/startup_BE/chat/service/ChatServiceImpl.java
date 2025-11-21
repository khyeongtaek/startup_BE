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

    // 필요한 Repository 주입
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEmployeeRepository chatEmployeeRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final AttachmentFileRepository attachmentFileRepository;

    // Service 및 Util
    private final NotificationService notificationService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AttachmentFileService attachmentFileService;

    // 파일 전송 시 사용할 대체 텍스트 상수 정의
    private static final String FILE_UPLOAD_MESSAGE = "파일을 전송 했습니다.";

    // 채팅방 생성
    @Override
    public ChatRoomResponseDTO createRoom(String creatorUsername, String roomName, List<Long> inviteeEmployeeIds) {

        // 채팅방 생성자 조회
        Employee creator = employeeRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재 하지 않은 사원 입니다"));

        // 초대 대상 Employee 체크
        Set<Long> inviteeEmployeeId = new LinkedHashSet<>(inviteeEmployeeIds);
        if (inviteeEmployeeId.isEmpty()) {
            throw new IllegalArgumentException("최소 한 명 이상 초대해야 합니다");
        }
        if (inviteeEmployeeId.contains(creator.getEmployeeId())) {
            throw new IllegalArgumentException("초대할 수 있는 대상이 아닙니다");
        }

        // 초대 할 사원 조회 & 유효성 검사
        List<Employee> invitees = employeeRepository.findAllById(inviteeEmployeeId);
        if (invitees.size() != inviteeEmployeeId.size()) {
            throw new EntityNotFoundException("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }

        // 모든 참여자 목록
        List<Employee> allParticipants = new ArrayList<>();
        allParticipants.add(creator);
        allParticipants.addAll(invitees);

        // 팀 채팅방 여부 확인
        boolean isTeamChat = invitees.size() >= 2;

        // 1:1 채팅방인 경우 기존 방 검색 로직
        if (!isTeamChat && invitees.size() == 1) {
            Employee invitee = invitees.get(0); // 1:1 채팅의 상대방

            List<ChatRoom> existingRoomOpt = chatRoomRepository.findExistingOneOnOneRooms(creator.getEmployeeId(), invitee.getEmployeeId());

            if (!existingRoomOpt.isEmpty()) {
                ChatRoom existingRoom = existingRoomOpt.get(0);
                Long existingRoomId = existingRoom.getChatRoomId();

                // 멤버 재활성화 로직
                List<ChatEmployee> members = chatEmployeeRepository.findAllByChatRoomChatRoomId(existingRoomId);
                ChatMessage lastMessageForNotify = null;

                long currentMemberCount = members.stream().filter(member -> Boolean.FALSE.equals(member.getIsLeft())).count();

                for (ChatEmployee member : members) {
                    if (Boolean.TRUE.equals(member.getIsLeft())) {
                        member.rejoinChatRoom();
                        // 재입장 시 참여 시간을 현재로 갱신하여 과거 시스템 메시지(방 생성 등) 노출 방지
                        member.rejoinChatRoom();
                        chatEmployeeRepository.save(member);
                    }
                    lastMessageForNotify = member.getLastReadMessage();
                }

                // 기존 방 반환 시, 1:1 채팅방이면 상대방 정보로 DTO 구성 (여기선 기존 ChatRoomResponseDTO 사용)
                ChatRoomResponseDTO roomDTO = ChatRoomResponseDTO.toDTO(existingRoom, currentMemberCount);

                // 상대방 정보로 덮어쓰기 (1:1인 경우)
                String inviteePositionName = (invitee.getPosition() != null) ? invitee.getPosition().getValue1() : "";

                roomDTO = ChatRoomResponseDTO.builder()
                        .chatRoomId(existingRoom.getChatRoomId())
                        .name(existingRoom.getEmployee().getName()) // 방 생성자(본인) 이름 유지
                        .displayName(invitee.getName())             // 방 제목 -> 상대방 이름
                        .isTeam(false)
                        .createdAt(existingRoom.getCreatedAt())
                        .memberCount(currentMemberCount)
                        .profileImg(invitee.getProfileImg())        // 프로필 -> 상대방 이미지
                        .positionName(inviteePositionName)          // 직급 -> 상대방 직급
                        .build();


                // 알림 전송 (기존 로직 유지)
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

        // 새 채팅방 생성 및 저장
        ChatRoom chatRoom = ChatRoom.createChatRoom(creator, roomName, isTeamChat);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        Long chatRoomId = savedChatRoom.getChatRoomId();

        // 시스템 메시지 생성
        String systemMessageContent = String.format("%s님이 채팅방을 생성 했습니다.", creator.getName());
        ChatMessage initialMessage = ChatMessage.createSystemMessage(savedChatRoom, systemMessageContent);
        ChatMessage savedInitialMessage = chatMessageRepository.save(initialMessage);

        // ChatEmployee 저장
        List<ChatEmployee> chatEmployees = allParticipants.stream()
                .map(emp -> ChatEmployee.createChatEmployee(
                        emp,
                        savedChatRoom,
                        roomName,
                        savedInitialMessage
                ))
                .collect(Collectors.toList());

        chatEmployeeRepository.saveAll(chatEmployees);

        // WebSocket 브로드캐스트
        ChatMessageResponseDTO messageDTO = ChatMessageResponseDTO.toDTO(savedInitialMessage);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + chatRoomId, messageDTO);
                notifyParticipantsOfNewMessage(savedChatRoom, savedInitialMessage, null);
            }
        });

        // 팀 채팅방 알림 전송 로직 (기존과 동일)
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

        // 새 방 반환 시, 1:1 채팅방이면 상대방 정보로 DTO 구성
        ChatRoomResponseDTO responseDTO = ChatRoomResponseDTO.toDTO(savedChatRoom, memberCount);

        if (!isTeamChat && invitees.size() == 1) {
            Employee invitee = invitees.get(0);

            String inviteePositionName = (invitee.getPosition() != null) ? invitee.getPosition().getValue1() : "";

            responseDTO = ChatRoomResponseDTO.builder()
                    .chatRoomId(savedChatRoom.getChatRoomId())
                    .name(savedChatRoom.getEmployee().getName()) // 방 생성자
                    .displayName(invitee.getName())              // 상대방 이름
                    .isTeam(false)
                    .createdAt(savedChatRoom.getCreatedAt())
                    .memberCount(memberCount)
                    .profileImg(invitee.getProfileImg())         // 상대방 이미지
                    .positionName(inviteePositionName)           // 상대방 직급
                    .build();
        }

        return responseDTO;
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

        Map<Long, ChatEmployee> existingMembersMap = chatEmployeeRepository
                .findAllByChatRoomChatRoomId(chatRoomId)
                .stream()
                .collect(Collectors.toMap(ce -> ce.getEmployee().getEmployeeId(), ce -> ce));

        List<Employee> newInvitees = new ArrayList<>();  // 진짜 새로운 사원
        List<ChatEmployee> rejoiningMembers = new ArrayList<>();  // 재참여 사원

        for (Employee candidate : candidates) {
            ChatEmployee existingMember = existingMembersMap.get(candidate.getEmployeeId());

            if (existingMember == null) {
                // 새로운 초대
                newInvitees.add(candidate);
            } else if (Boolean.TRUE.equals(existingMember.getIsLeft())) {
                // 재 참여
                existingMember.rejoinChatRoom();
                // 재참여 시 joinedAt을 갱신하여 이전 기록 노출 방지
                existingMember.rejoinChatRoom();
                rejoiningMembers.add(existingMember);
            }
            // 이미 방에 있는 사원이라 자동으로 걸러짐
        }

        // 초대할 대상이 아무도 없으면 종료
        if (newInvitees.isEmpty() && rejoiningMembers.isEmpty()) return;


        // 4) 1:1 → 팀 채팅방 변경
        if (Boolean.FALSE.equals(room.getIsTeam()) && !newInvitees.isEmpty()) {
            room.updateToTeamRoom();
        }

        // 5) 시스템 메시지 + (신규 + 재 참여 모두 포함)
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


        // 6) 커밋 후 알림
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
                    .stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("채팅 초대 CommonCode 없음"))
                    .getCommonCodeId();

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
                ChatMessage.createSystemMessage(room, leaver.getName() + "님이 채팅방에서 나가셨습니다."))
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
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId,
                            ChatMessageResponseDTO.toDTO(systemMessage));
                }
            });
        }
    }

    // 채팅방에 메시지 전송
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
            // 나간 사용자를 포함하여 모든 멤버를 조회합니다.
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(roomId);

            for (ChatEmployee member : allMembers) {
                // 상대방이 나간 상태라면
                if (!member.getEmployee().getEmployeeId().equals(sender.getEmployeeId()) && Boolean.TRUE.equals(member.getIsLeft())) {
                    member.rejoinChatRoom();
                    // 재입장 시 joinedAt을 현재로 갱신하여, 과거 '채팅방 생성' 시스템 메시지 등이 안 보이도록 처리
                    member.rejoinChatRoom();
                    chatEmployeeRepository.save(member);
                }
            }
        }

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
        // 멤버십 및 참여 시각 확인
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, username)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        Page<ChatMessage> page = chatMessageRepository
                .findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                        roomId,
                        membership.getJoinedAt(),
                        pageable
                );

        // 메시지 ID 목록 추출
        List<Long> messageIds = page.getContent().stream()
                .map(ChatMessage::getChatMessageId)
                .toList();

        if (messageIds.isEmpty()) {
            return page.map(message -> ChatMessageResponseDTO.toDTO(message));
        }

        // CHAT 공통 코드 조회
        CommonCode chatOwnerType = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name())
                .stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("CHAT 공통 코드를 찾을 수 없습니다"));

        List<AttachmentFile> allAttachments =
                attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(chatOwnerType, messageIds);

        Map<Long, List<AttachmentFileResponseDTO>> attachmentsMap = allAttachments.stream()
                .collect(Collectors.groupingBy(
                        AttachmentFile::getOwnerId,
                        Collectors.mapping(AttachmentFileResponseDTO::toDTO, Collectors.toList())));

        return page.map(message -> {
            ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(message);

            if (message.getEmployee() != null) {
                long currentUnreadCount = chatEmployeeRepository.countUnreadByAllParticipants(
                        roomId,
                        message.getCreatedAt()
                );
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
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));

        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, reader.getUsername())
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

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
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 메시지입니다."));

            if (!Objects.equals(finalMessageToRead.getChatRoom().getChatRoomId(), roomId)) {
                throw new AccessDeniedException("해당 채팅방의 메시지가 아닙니다.");
            }
        }
        if (finalMessageToRead == null) return;

        // 사용자의 이전 마지막 읽은 메시지 정보를 가져옴
        ChatMessage oldLastReadMessage = membership.getLastReadMessage();
        LocalDateTime readAfterTime = (oldLastReadMessage != null && oldLastReadMessage.getCreatedAt().isAfter(membership.getJoinedAt()))
                ? oldLastReadMessage.getCreatedAt()
                : membership.getJoinedAt();

        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(finalMessageToRead.getCreatedAt())) {
            return; // 이미 더 최신 메시지를 읽었으므로 갱신 안 함
        }

        membership.updateLastReadMessage(finalMessageToRead);
        chatEmployeeRepository.save(membership);

        // 방금 읽은 메시지 목록 조회
        List<ChatMessage> messagesJustRead = chatMessageRepository
                .findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                        roomId,
                        readAfterTime,
                        finalMessageToRead.getCreatedAt()
                );

        // 각 메시지의 새로운 안 읽음 카운트를 계산
        List<Map<String, Object>> unreadUpdates = new ArrayList<>();
        for (ChatMessage message : messagesJustRead) {
            // 시스템 메시지(employee=null)는 카운트 계산에서 제외
            if (message.getEmployee() != null) {
                // ChatEmployeeRepository에 추가한 새 메소드 사용
                long newUnreadCount = chatEmployeeRepository.countUnreadByAllParticipants(
                        roomId,
                        message.getCreatedAt()
                );
                unreadUpdates.add(Map.of(
                        "chatMessageId", message.getChatMessageId(),
                        "newUnreadCount", newUnreadCount
                ));
            }
        }

        // 읽음 브로드캐스트: 프론트는 해당 방의 메시지들 중 msg.id 이하의 미읽음 카운트만 감소
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!unreadUpdates.isEmpty()) {
                    simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId + "/unread-updates",
                            Map.of("unreadUpdates", unreadUpdates));
                }

                calculateAndSendTotalUnreadCount(reader.getEmployeeId(), reader.getUsername());
            }
        });
    }

    /**
     * 사용자가 속한 채팅방 목록 조회
     */
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
                        room,
                        membership.getLastReadMessage().getChatMessageId()
                );
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, membership.getJoinedAt());
            }
            long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

            // DTO 팩토리 메소드 호출 (직급 포함)
            if (Boolean.FALSE.equals(room.getIsTeam())) {
                // 1:1 채팅방
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
                    // 화면 표시용 이름 (username -> name 권장)
                    otherUserName = otherUser.getName();
                    otherUserProfileImg = otherUser.getProfileImg();

                    if (otherUser.getPosition() != null) {
                        otherUserPosition = otherUser.getPosition().getValue1();
                    }
                }

                dtos.add(ChatRoomListResponseDTO.toDTO(
                        room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, optLastMessage, currentMemberCount
                ));
            } else {
                // 팀 채팅방
                dtos.add(ChatRoomListResponseDTO.toDTO(
                        room, unreadCount, optLastMessage, currentMemberCount
                ));
            }
        }

        // 정렬 로직
        dtos.sort((dto1, dto2) -> {
            if (dto1.getLastMessageCreatedAt() == null) return 1;
            if (dto2.getLastMessageCreatedAt() == null) return -1;
            return dto2.getLastMessageCreatedAt().compareTo(dto1.getLastMessageCreatedAt());
        });

        return dtos;
    }

    // 알림 전송을 위한 헬퍼 메소드

    /**
     * 새 메시지 발생 시, 관련자들에게 개인 큐로 전송
     */
    private void notifyParticipantsOfNewMessage(ChatRoom room, ChatMessage message, Long senderId) {

        // 채팅방에 참여중인 모든 멤버 조회
        List<ChatEmployee> participants = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

        // 1:1 채팅방인 경우 전송자(상대방) 정보를 미리 조회
        Employee sender = null;
        if (senderId != null) {
            sender = employeeRepository.findById(senderId)
                    .orElse(null);
        }

        for (ChatEmployee participant : participants) {
            Employee recipient = participant.getEmployee();

            long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

            if (recipient == null) continue;

            // 안 읽은 갯수 계산
            long unreadCount = 0;
            if (participant.getLastReadMessage() != null &&
                    participant.getLastReadMessage().getCreatedAt().isAfter(participant.getJoinedAt())) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                        room,
                        participant.getLastReadMessage().getChatMessageId()
                );
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, participant.getJoinedAt());
            }

            ChatRoomListResponseDTO listDto;
            if (Boolean.FALSE.equals(room.getIsTeam())) {
                // 1:1 방일 때
                String otherUserName;
                String otherUserProfileImg;
                String otherUserPosition = "";

                Employee otherUser = null;
                if (senderId != null) {
                    otherUser = sender; // 메시지 보낸 사람이 상대방
                } else {
                    // 시스템 메시지 등 senderId가 없는 경우, 참가자 목록에서 본인 제외하고 찾기
                    otherUser = participants.stream()
                            .map(ChatEmployee::getEmployee)
                            .filter(emp -> emp != null && !emp.getEmployeeId().equals(recipient.getEmployeeId()))
                            .findFirst()
                            .orElse(null);
                }

                if (otherUser == null) {
                    otherUserName = "정보 없음";
                    otherUserProfileImg = null;
                } else {
                    otherUserName = otherUser.getName(); // 화면 표시용 이름
                    otherUserProfileImg = otherUser.getProfileImg();
                    if (otherUser.getPosition() != null) {
                        otherUserPosition = otherUser.getPosition().getValue1();
                    }
                }

                listDto = ChatRoomListResponseDTO.toDTO(
                        room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, Optional.of(message), currentMemberCount
                );
            } else {
                // 팀 채팅방일 때
                listDto = ChatRoomListResponseDTO.toDTO(
                        room, unreadCount, Optional.of(message), currentMemberCount
                );
            }
            // 채팅방 목록 업데이트 알림 전송
            simpMessagingTemplate.convertAndSendToUser(
                    recipient.getUsername(),
                    "/queue/chat-list-update",
                    listDto
            );

            if (senderId == null || !recipient.getEmployeeId().equals(senderId)) {
                // 총 안 읽은 메시지 개수 전송
                calculateAndSendTotalUnreadCount(recipient.getEmployeeId(), recipient.getUsername());
            }
        }
    }

    /**
     * 특정 사용자의 총 안 읽은 메시지 개수를 계산하여 개인 큐로 전송합니다.
     */
    private void calculateAndSendTotalUnreadCount(Long employeeId, String username) {
        long totalUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(employeeId);
        simpMessagingTemplate.convertAndSendToUser(
                username,
                "/queue/unread-count",
                new TotalUnreadCountResponseDTO(totalUnread)
        );
    }

    /**
     * ID로 특정 채팅방 정보 조회 (알림 클릭 시 사용)
     */
    @Override
    @Transactional(readOnly = true)
    public ChatRoomListResponseDTO getRoomById(String username, Long roomId) {
        // 사용자 조회
        Employee user = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));

        // 채팅방 멤버십 확인 (권한 확인)
        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, user.getEmployeeId())
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        // 1:1 채팅방이고, A가 '나간' 상태에서 알림을 클릭한 경우, 방에 입장하는 즉시 '활성'으로 변경
        if (Boolean.FALSE.equals(membership.getChatRoom().getIsTeam()) && Boolean.TRUE.equals(membership.getIsLeft())) {
            membership.rejoinChatRoom();
            // 방에 다시 들어왔을 때도 joinedAt 갱신 (선택사항이나 일관성을 위해 추가 추천)
            membership.rejoinChatRoom();
            chatEmployeeRepository.save(membership);
        }

        ChatRoom room = membership.getChatRoom();

        // 마지막 메시지 조회
        Optional<ChatMessage> optLastMessage = chatMessageRepository
                .findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(room, membership.getJoinedAt());

        // 안 읽은 개수
        long unreadCount = 0;
        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(membership.getJoinedAt())) {
            unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                    room,
                    membership.getLastReadMessage().getChatMessageId()
            );
        } else {
            unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(room, membership.getJoinedAt());
        }

        long currentMemberCount = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

        // DTO 생성 로직
        if (Boolean.FALSE.equals(room.getIsTeam())) {
            // 1:1 채팅방: 상대방 정보 필요
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
                    otherUserName = otherUser.getName(); // 화면 표시용 이름
                    otherUserProfileImg = otherUser.getProfileImg();
                    if (otherUser.getPosition() != null) {
                        otherUserPosition = otherUser.getPosition().getValue1();
                    }
                }

                return ChatRoomListResponseDTO.toDTO(
                        room, otherUserName, otherUserProfileImg, otherUserPosition, unreadCount, optLastMessage, currentMemberCount
                );
            } else {
                // (예외: 상대방이 나간 1:1 방 등)
                throw new EntityNotFoundException("1:1 채팅방의 상대방 정보를 찾을 수 없습니다.");
            }
        } else {
            // 팀 채팅방
            return ChatRoomListResponseDTO.toDTO(
                    room, unreadCount, optLastMessage, currentMemberCount
            );
        }
    }

    /**
     * 파일 첨부 메시지 전송 (HTTP)
     */
    @Transactional
    public void sendMessageWithFiles(String senderUsername, Long roomId, String content, List<MultipartFile> files) {

        // 사용자 및 채팅방 조회
        Employee sender = employeeRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방 입니다"));

        ChatEmployee membership = chatEmployeeRepository
                .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, senderUsername)
                .orElseThrow(() -> new AccessDeniedException("채팅방 멤버가 아니거나 이미 나간 사용자입니다."));

        if (Boolean.FALSE.equals(room.getIsTeam())) {
            // 나간 사용자를 포함하여 모든 멤버를 조회합니다.
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(roomId);

            for (ChatEmployee member : allMembers) {
                // 상대방이 나간 상태라면
                if (!member.getEmployee().getEmployeeId().equals(sender.getEmployeeId()) && Boolean.TRUE.equals(member.getIsLeft())) {
                    member.rejoinChatRoom();
                    // 재입장 시 joinedAt을 현재로 갱신하여 이전 시스템 메시지 노출 방지
                    member.rejoinChatRoom();
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

        // 메시지 엔티티 먼저 저장
        ChatMessage message = ChatMessage.createChatMessage(room, sender, messageContent);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        Long chatMessageId = savedMessage.getChatMessageId();

        // 공통 코드 조회
        CommonCode chatOwnerType = commonCodeRepository
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name())
                .stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("CHAT 공통 코드를 찾을 수 없습니다"));


        // 파일 저장
        List<AttachmentFileResponseDTO> finalAttachments =
                attachmentFileService.uploadFiles(files, chatOwnerType.getCommonCodeId(), chatMessageId);

        // 본인 메시지 읽음 처리
        membership.updateLastReadMessage(savedMessage);
        chatEmployeeRepository.save(membership);

        // 미 읽음 수 계산
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
        // 브로드캐스트 준비
        Long finalRoomId = room.getChatRoomId();
        ChatMessageResponseDTO dto = ChatMessageResponseDTO.toDTO(savedMessage);
        dto.setUnreadCount(unread);
        dto.setAttachments(finalAttachments);

        if (isPlaceholder) {
            dto.setContent(null);
        }

        // STOMP로 브로드 캐스트 (HTTP로 받고 전파는 STOMP)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId, dto);
                notifyParticipantsOfNewMessage(room, savedMessage, sender.getEmployeeId());
            }
        });
    }
}