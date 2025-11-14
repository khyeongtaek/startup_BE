package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.chat.dto.ChatMessageResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomListResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomResponseDTO;
import org.goodee.startup_BE.chat.dto.TotalUnreadCountResponseDTO;
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
public class ChatServiceImpl implements ChatService {

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
        // 모든 참여자 목록: 채팅방 생성자 + 초대 대상
        List<Employee> allParticipants = new ArrayList<>();
        allParticipants.add(creator);
        allParticipants.addAll(invitees);

        // 팀 채팅방 여부 ( invitees.size() == 0이면 false = 1:1 채팅방, invitees.size() >= 1이면 true = 팀 채팅방)
        boolean isTeamChat = invitees.size() >= 2; // 초대 대상이 2명 이상이면 팀방

        if (!isTeamChat && invitees.size() == 1) {
            Employee invitee = invitees.get(0); // 1:1 채팅의 상대방

            Optional<ChatRoom> existingRoomOpt = chatRoomRepository.findExistingOneOnOneRoom(creator.getEmployeeId(), invitee.getEmployeeId());

            if (existingRoomOpt.isPresent()) {
                ChatRoom existingRoom = existingRoomOpt.get();
                Long existingRoomId = existingRoom.getChatRoomId();

                // 두 참가자의 ChatEmployee 정보를 모두 가져와서 재활성화
                List<ChatEmployee> members = chatEmployeeRepository.findAllByChatRoomChatRoomId(existingRoomId);
                ChatMessage lastMessageForNotify = null;

                for (ChatEmployee member : members) {
                    if (Boolean.TRUE.equals(member.getIsLeft())) {
                        member.rejoinChatRoom(); // isLeft = false, joinedAt = now()
                        chatEmployeeRepository.save(member);
                    }
                    // 알림에 사용할 마지막 메시지를 찾기 위해
                    lastMessageForNotify = member.getLastReadMessage();
                }

                // 프론트엔드가 이 방으로 이동할 수 있도록 DTO 반환
                ChatRoomResponseDTO roomDTO = ChatRoomResponseDTO.toDTO(existingRoom);

                // 두 참가자에게 기존 방이 재활성화되었음을 알림 (채팅 목록 갱신)
                // (findRoomsByUsername의 로직과 유사하게 DTO를 만들어 전송)
                final ChatMessage finalLastMessage = lastMessageForNotify;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // notifyParticipantsOfNewMessage를 직접 호출하기보다,
                        // createRoom의 응답을 받은 프론트가 해당 방으로 이동할 것을 기대함.
                        // 다만, 상대방(invitee)의 목록 갱신을 위해 알림을 보냄.
                        notifyParticipantsOfNewMessage(existingRoom, finalLastMessage, creator.getEmployeeId());
                    }
                });

                return roomDTO; // 새 방을 만들지 않고 기존 방 DTO를 반환
            }
        }

        // chatRoom에 저장
        ChatRoom chatRoom = ChatRoom.createChatRoom(creator, roomName, isTeamChat);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        Long chatRoomId = savedChatRoom.getChatRoomId();

        // "채팅방이 생성되었습니다" 시스템 메시지 생성
        String systemMessageContent = String.format("%s님이 채팅방을 생성 했습니다.", creator.getName());
        ChatMessage initialMessage = ChatMessage.createChatMessage(savedChatRoom, null, systemMessageContent);
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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + chatRoomId, messageDTO);

                notifyParticipantsOfNewMessage(savedChatRoom, savedInitialMessage, null);
            }
        });
        // TEAM 채팅방 이면, 초대 대상에게 알림 받기
        if (isTeamChat) {
            try {
                CommonCode chatInviteCode = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("요청 하신 CommonCOde 또는 KeyWord 를 찾을 수 없습니다"));

                Long chatInviteCodeId = chatInviteCode.getCommonCodeId();

                // 알림 전송 로직 호출
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
                ChatMessage.createChatMessage(room, null, inviter.getName() + "님이 " + names + "님을 초대했습니다.")
        );

        List<ChatEmployee> newLinks = newInvitees.stream()
                .map(emp -> ChatEmployee.createChatEmployee(emp, room, room.getName(), systemMsg))
                .toList();

        chatEmployeeRepository.saveAll(newLinks);
        chatEmployeeRepository.saveAll(rejoiningMembers);

        Long finalRoomId = room.getChatRoomId();


        // 6) 커밋 후 알림
        List<Employee> notificationTargets = allAffectedInvitees;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + finalRoomId,
                        ChatMessageResponseDTO.toDTO(systemMsg));

                notifyParticipantsOfNewMessage(room, systemMsg, null);

                try {
                    Long codeId = commonCodeRepository
                            .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name())
                            .stream().findFirst()
                            .orElseThrow(() -> new EntityNotFoundException("채팅 초대 CommonCode 없음"))
                            .getCommonCodeId();

                    for (Employee target : notificationTargets) {
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

        if (Boolean.FALSE.equals(room.getIsTeam())) {
            // 나간 사용자를 포함하여 모든 멤버를 조회합니다.
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(roomId);

            for (ChatEmployee member : allMembers) {
                // 상대방이 나간 상태라면
                if (!member.getEmployee().getEmployeeId().equals(sender.getEmployeeId()) && Boolean.TRUE.equals(member.getIsLeft())) {
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
                .findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                        roomId,
                        membership.getJoinedAt(),
                        pageable
                );

        return page.map(ChatMessageResponseDTO::toDTO);
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
                    .findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(
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

        if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(finalMessageToRead.getCreatedAt())) {
            return; // 이미 더 최신 메시지를 읽었으므로 갱신 안 함
        }

        membership.updateLastReadMessage(finalMessageToRead);
        chatEmployeeRepository.save(membership);

        // 읽음 브로드캐스트: 프론트는 해당 방의 메시지들 중 msg.id 이하의 미읽음 카운트만 감소
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId + "/reads",
                        Map.of("lastMessageId", membership.getEmployee().getEmployeeId(),
                                "readUpToMessageId", finalMessageToRead.getChatMessageId()));

                calculateAndSendTotalUnreadCount(reader.getEmployeeId(), reader.getUsername());
            }
        });
    }

    /**
     * 사용자가 속한 채팅방 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    // 1. 반환 타입 변경
    public List<ChatRoomListResponseDTO> findRoomsByUsername(String username) {

        Employee user = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사원 입니다"));

        List<ChatEmployee> memberships = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user);

        // 2. DTO 리스트 타입 변경
        List<ChatRoomListResponseDTO> dtos = new ArrayList<>();

        for (ChatEmployee membership : memberships) {
            ChatRoom room = membership.getChatRoom();

            Optional<ChatMessage> optLastMessage = chatMessageRepository
                    .findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(room, membership.getJoinedAt());

            long unreadCount = 0;
            if (membership.getLastReadMessage() != null &&
                membership.getLastReadMessage().getCreatedAt().isAfter(membership.getJoinedAt())) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                        room,
                        membership.getLastReadMessage().getChatMessageId()
                );
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtAfterAndEmployeeIsNotNull(room, membership.getJoinedAt());
            }

            // 3. 서비스 로직을 DTO 팩토리 메소드 호출로 대체 (하드코딩 제거)
            if (Boolean.FALSE.equals(room.getIsTeam())) {
                // 1:1 채팅방
                List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room.getChatRoomId());

                Optional<Employee> otherUserOpt = allMembers.stream()
                        .map(ChatEmployee::getEmployee)
                        .filter(emp -> !emp.getEmployeeId().equals(user.getEmployeeId()))
                        .findFirst();

                if (otherUserOpt.isPresent()) {
                    dtos.add(ChatRoomListResponseDTO.toDTO(
                            room, otherUserOpt.get(), unreadCount, optLastMessage
                    ));
                } else {
                    // (예외 처리: 상대방이 나갔거나 데이터가 없는 1:1방)
                    // 필요시 ofLeftUserRoom() 같은 정적 메소드 DTO에 추가
                }
            } else {
                // 팀 채팅방
                dtos.add(ChatRoomListResponseDTO.toDTO(
                        room, unreadCount, optLastMessage
                ));
            }
        }

        // 4. 정렬 로직 (DTO 필드명 확인 필요)
        dtos.sort((dto1, dto2) -> {
            if (dto1.getLastMessage() == null || dto2.getLastMessage() == null) return 0;
            return dto2.getLastMessage().compareTo(dto1.getLastMessage());
        });

        return dtos;
    }

    // 알림 전송을 위한 헬퍼 메소드 추가

    /**
     * 새 메시지 발생 시, 관련자들에게 개인 큐로 전송
     * 1. 채팅방 목록 업데이트 (/user/queue/chat-list-update)
     * 2. 총 안 읽은 개수 업데이트 (/user/queue/unread-count)
     *
     * @param room     발생한 채팅방
     * @param message  발생한 메시지 (LastMessage용)
     * @param senderId 메시지 전송자 ID (알림에서 제외하기 위함, 시스템 메시지면 null)
     */
    private void notifyParticipantsOfNewMessage(ChatRoom room, ChatMessage message, Long senderId) {

        // 채팅방에 참여중인 모든 멤버 조회
        List<ChatEmployee> participants = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room.getChatRoomId());

        // 1:1 채팅방인 경우
        Employee otherUserFor1to1 = null;
        if (Boolean.FALSE.equals(room.getIsTeam()) && senderId != null) {
            otherUserFor1to1 = employeeRepository.findById(senderId)
                    .orElse(null);
        }
        for (ChatEmployee participant : participants) {
            Employee recipient = participant.getEmployee();

            // 채팅방 목록 DTO
            long unreadCount = 0;
            if (participant.getLastReadMessage() != null &&
                participant.getLastReadMessage().getCreatedAt().isAfter(participant.getJoinedAt())) {
                unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                        room,
                        participant.getLastReadMessage().getChatMessageId()
                );
            } else {
                unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtAfterAndEmployeeIsNotNull(room, participant.getJoinedAt());
            }

            ChatRoomListResponseDTO listDto;
            if (Boolean.FALSE.equals(room.getIsTeam())) {
                // 1:1 방일 때
                Employee otherUser = (otherUserFor1to1 != null) ? otherUserFor1to1 :
                        participants.stream()
                                .map(ChatEmployee::getEmployee)
                                .filter(emp -> !emp.getEmployeeId().equals(recipient.getEmployeeId()))
                                .findFirst().orElse(null);

                if (otherUser == null) continue;

                listDto = ChatRoomListResponseDTO.toDTO(
                        room, otherUser, unreadCount, Optional.of(message)
                );
            } else {
                // 팀 채팅방일 때
                listDto = ChatRoomListResponseDTO.toDTO(
                        room, unreadCount, Optional.of(message)
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
     *
     * @param employeeId 대상 직원 ID
     * @param username   대상 직원 username (STOMP 전송용)
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
            chatEmployeeRepository.save(membership);
        }

        ChatRoom room = membership.getChatRoom();

        // findRoomsByUsername와 동일한 로직 수행 (마지막 메시지)
        Optional<ChatMessage> optLastMessage = chatMessageRepository
                .findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(room, membership.getJoinedAt());

        // findRoomsByUsername와 동일한 로직 수행 (안 읽은 개수)
        long unreadCount = 0;
        if (membership.getLastReadMessage() != null &&
            membership.getLastReadMessage().getCreatedAt().isAfter(membership.getJoinedAt())) {
            unreadCount = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                    room,
                    membership.getLastReadMessage().getChatMessageId()
            );
        } else {
            unreadCount = chatMessageRepository.countByChatRoomAndCreatedAtAfterAndEmployeeIsNotNull(room, membership.getJoinedAt());
        }

        // findRoomsByUsername와 동일한 DTO 변환 로직 수행
        if (Boolean.FALSE.equals(room.getIsTeam())) {
            // 1:1 채팅방: 상대방 정보 필요
            List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room.getChatRoomId());

            Optional<Employee> otherUserOpt = allMembers.stream()
                    .map(ChatEmployee::getEmployee)
                    .filter(emp -> !emp.getEmployeeId().equals(user.getEmployeeId()))
                    .findFirst();

            if (otherUserOpt.isPresent()) {
                return ChatRoomListResponseDTO.toDTO(
                        room, otherUserOpt.get(), unreadCount, optLastMessage
                );
            } else {
                // (예외: 상대방이 나간 1:1 방) - findRoomsByUsername의 로직을 그대로 따름
                // DTO에 정적 메소드 (ofLeftUserRoom 등)가 있다면 그것을 사용
                throw new EntityNotFoundException("1:1 채팅방의 상대방 정보를 찾을 수 없습니다.");
            }
        } else {
            // 팀 채팅방
            return ChatRoomListResponseDTO.toDTO(
                    room, unreadCount, optLastMessage
            );
        }
    }
}
