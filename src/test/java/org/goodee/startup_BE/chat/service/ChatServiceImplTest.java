package org.goodee.startup_BE.chat.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatEmployeeRepository chatEmployeeRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CommonCodeRepository commonCodeRepository;
    @Mock private AttachmentFileRepository attachmentFileRepository;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate simpMessagingTemplate;
    @Mock private AttachmentFileService attachmentFileService;

    // --- Mock Data ---
    private Employee creator, invitee1, invitee2;
    private CommonCode posCreator, posInvitee;
    private ChatRoom chatRoom;
    private ChatMessage chatMessage;
    private ChatEmployee chatEmployeeCreator, chatEmployeeInvitee1;
    private CommonCode commonCodeTeamNoti, commonCodeChat;

    // --- Captors ---
    @Captor private ArgumentCaptor<TransactionSynchronization> syncCaptor;
    @Captor private ArgumentCaptor<List<ChatEmployee>> chatEmployeesCaptor;
    @Captor private ArgumentCaptor<NotificationRequestDTO> notificationCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> unreadUpdateCaptor;
    @Captor private ArgumentCaptor<ChatMessage> messageCaptor;
    @Captor private ArgumentCaptor<Object> payloadCaptor;

    @BeforeEach
    void setUp() {
        // 직급 Mock
        posCreator = mock(CommonCode.class);
        posInvitee = mock(CommonCode.class);
        lenient().when(posCreator.getValue1()).thenReturn("Manager");
        lenient().when(posInvitee.getValue1()).thenReturn("Staff");

        // Employee Mock
        creator = mock(Employee.class);
        lenient().when(creator.getEmployeeId()).thenReturn(1L);
        lenient().when(creator.getUsername()).thenReturn("creator");
        lenient().when(creator.getName()).thenReturn("Creator Name");
        lenient().when(creator.getPosition()).thenReturn(posCreator);
        lenient().when(creator.getProfileImg()).thenReturn("creator.png");

        invitee1 = mock(Employee.class);
        lenient().when(invitee1.getEmployeeId()).thenReturn(2L);
        lenient().when(invitee1.getUsername()).thenReturn("invitee1");
        lenient().when(invitee1.getName()).thenReturn("Invitee1 Name");
        lenient().when(invitee1.getPosition()).thenReturn(posInvitee);
        lenient().when(invitee1.getProfileImg()).thenReturn("invitee1.png");

        invitee2 = mock(Employee.class);
        lenient().when(invitee2.getEmployeeId()).thenReturn(3L);
        lenient().when(invitee2.getUsername()).thenReturn("invitee2");
        lenient().when(invitee2.getName()).thenReturn("Invitee2 Name");

        // ChatRoom Mock
        chatRoom = mock(ChatRoom.class);
        lenient().when(chatRoom.getChatRoomId()).thenReturn(100L);
        lenient().when(chatRoom.getName()).thenReturn("Test Room");
        lenient().when(chatRoom.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(chatRoom.getEmployee()).thenReturn(creator);
        lenient().when(chatRoom.getIsTeam()).thenReturn(false);

        // ChatMessage Mock (ID가 있는 상태 가정)
        chatMessage = mock(ChatMessage.class);
        lenient().when(chatMessage.getChatMessageId()).thenReturn(1000L);
        lenient().when(chatMessage.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatMessage.getEmployee()).thenReturn(creator);
        lenient().when(chatMessage.getContent()).thenReturn("Test Message");
        lenient().when(chatMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(chatMessage.getMessageType()).thenReturn(OwnerType.CHAT_USER);

        // ChatEmployee Mock
        chatEmployeeCreator = mock(ChatEmployee.class);
        lenient().when(chatEmployeeCreator.getEmployee()).thenReturn(creator);
        lenient().when(chatEmployeeCreator.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatEmployeeCreator.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));
        lenient().when(chatEmployeeCreator.getIsLeft()).thenReturn(false);

        chatEmployeeInvitee1 = mock(ChatEmployee.class);
        lenient().when(chatEmployeeInvitee1.getEmployee()).thenReturn(invitee1);
        lenient().when(chatEmployeeInvitee1.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatEmployeeInvitee1.getIsLeft()).thenReturn(false);

        // CommonCode Mock
        commonCodeTeamNoti = mock(CommonCode.class);
        lenient().when(commonCodeTeamNoti.getCommonCodeId()).thenReturn(500L);

        commonCodeChat = mock(CommonCode.class);
        lenient().when(commonCodeChat.getCommonCodeId()).thenReturn(600L);
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        @Test
        @DisplayName("성공 - 1:1 채팅방 신규 생성")
        void createRoom_Success_New_1on1() {
            // given
            List<Long> invitees = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(Collections.emptyList());

            // 저장 로직
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "New Room", invitees);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getDisplayName()).isEqualTo("Invitee1 Name"); // 상대방 이름
                verify(chatRoomRepository).save(any(ChatRoom.class));
                verify(chatEmployeeRepository).saveAll(chatEmployeesCaptor.capture());
                assertThat(chatEmployeesCaptor.getValue()).hasSize(2);

                // STOMP 전송 확인 (After Commit)
                syncCaptor.getValue().afterCommit();
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 기존 방 재활성화 (DTO 즉시 반환)")
        void createRoom_Success_Rejoin_1on1() {
            // given
            List<Long> invitees = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));

            // 기존 방 존재
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(List.of(chatRoom));

            // Invitee1이 나간 상태
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true);
            given(chatEmployeeCreator.getLastReadMessage()).willReturn(chatMessage);

            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "Rejoin Room", invitees);

                // then
                verify(chatRoomRepository, never()).save(any(ChatRoom.class)); // 새 방 생성 안함
                verify(chatEmployeeInvitee1, atLeastOnce()).rejoinChatRoom(); // 재입장 확인
                verify(chatEmployeeRepository).save(chatEmployeeInvitee1);

                assertThat(result.getChatRoomId()).isEqualTo(100L);
                assertThat(result.getDisplayName()).isEqualTo("Invitee1 Name");

                // After Commit
                syncCaptor.getValue().afterCommit();
                verify(chatEmployeeRepository, atLeastOnce()).findAllByChatRoomChatRoomIdAndIsLeftFalse(100L);
            }
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 생성")
        void createRoom_Success_Team() {
            // given
            List<Long> invitees = List.of(2L, 3L); // 2명 초대 -> 팀방
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1, invitee2));

            // Team Room Mock
            ChatRoom teamRoom = mock(ChatRoom.class);
            when(teamRoom.getChatRoomId()).thenReturn(200L);
            when(teamRoom.getName()).thenReturn("Team Room");
            when(teamRoom.getIsTeam()).thenReturn(true);
            when(teamRoom.getEmployee()).thenReturn(creator);

            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(teamRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // CommonCode
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(commonCodeTeamNoti));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "Team Room", invitees);

                // then
                assertThat(result.getIsTeam()).isTrue();
                verify(notificationService, times(2)).create(notificationCaptor.capture());
            }
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoom {

        @Test
        @DisplayName("성공 - 1:1 방에서 새로운 멤버 초대 시 팀 방으로 승격")
        void inviteToRoom_Success_UpgradeToTeam() {
            // given
            List<Long> inviteeIds = List.of(3L); // invitee2
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

            // 멤버십 체크
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L))
                    .willReturn(true);

            // 초대 대상 조회
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee2));

            // 기존 멤버 (creator, invitee1)
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            given(chatRoom.getIsTeam()).willReturn(false);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(commonCodeTeamNoti));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.inviteToRoom("creator", 100L, inviteeIds);

                // then
                verify(chatRoom).updateToTeamRoom(); // 승격
                verify(chatEmployeeRepository, atLeastOnce()).saveAll(anyList());
                verify(notificationService).create(any(NotificationRequestDTO.class));
            }
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        @Test
        @DisplayName("성공 - 1:1 방 상대방이 나갔으면 자동 재입장")
        void sendMessage_Success_Rejoin_Opponent() {
            // given
            MessageSendPayloadDTO payload = new MessageSendPayloadDTO("Hello", null);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            given(chatRoom.getIsTeam()).willReturn(false);

            // 상대방 나감
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // 메시지 저장 시 ID 부여 (Answer)
            given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(inv -> {
                ChatMessage msg = inv.getArgument(0);
                ReflectionTestUtils.setField(msg, "chatMessageId", 1000L);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.sendMessage("creator", 100L, payload);

                // then
                verify(chatEmployeeInvitee1, atLeastOnce()).rejoinChatRoom(); // 재입장
                verify(chatEmployeeRepository).save(chatEmployeeInvitee1);
                verify(chatMessageRepository).save(any(ChatMessage.class));
            }
        }
    }

    @Nested
    @DisplayName("sendMessageWithFiles (파일 전송)")
    class SendMessageWithFiles {

        @Test
        @DisplayName("성공 - 내용 없이 파일만 보낼 때 FILE_UPLOAD_MESSAGE 대체")
        void sendMessageWithFiles_Placeholder_Success() {
            // given
            List<MultipartFile> files = List.of(mock(MultipartFile.class));
            String emptyContent = "";

            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // [중요] save 호출 시 반환되는 객체에 ID를 설정해야 uploadFiles의 매칭이 성공함
            given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
                ChatMessage msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "chatMessageId", 1000L);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name()))
                    .willReturn(List.of(commonCodeChat));

            // 1000L ID가 정상적으로 전달되는지 eq(1000L)로 확인
            given(attachmentFileService.uploadFiles(anyList(), eq(600L), eq(1000L)))
                    .willReturn(List.of(new AttachmentFileResponseDTO()));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.sendMessageWithFiles("creator", 100L, emptyContent, files);

                // then
                verify(chatMessageRepository).save(messageCaptor.capture());
                ChatMessage savedMsg = messageCaptor.getValue();
                assertThat(savedMsg.getContent()).isEqualTo("파일을 전송 했습니다.");

                verify(attachmentFileService).uploadFiles(anyList(), eq(600L), eq(1000L));
            }
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (읽음 처리)")
    class UpdateLastReadMessageId {

        @Test
        @DisplayName("성공 - 읽음 처리 및 STOMP 전송")
        void updateLastReadMessageId_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));
            given(chatMessageRepository.findById(1000L)).willReturn(Optional.of(chatMessage));

            // 과거에 읽은 메시지가 있다고 가정
            ChatMessage oldMsg = mock(ChatMessage.class);
            given(oldMsg.getCreatedAt()).willReturn(LocalDateTime.now().minusHours(1));
            given(chatEmployeeCreator.getLastReadMessage()).willReturn(oldMsg);

            // 방금 읽은 메시지 리스트
            given(chatMessageRepository.findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    anyLong(), any(), any()
            )).willReturn(List.of(chatMessage));

            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(1L)).willReturn(5L);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                chatService.updateLastReadMessageId("creator", 100L, 1000L);

                // then
                verify(chatEmployeeCreator).updateLastReadMessage(chatMessage);

                // After Commit
                syncCaptor.getValue().afterCommit();

                // 1. Unread Updates Broadcast
                verify(simpMessagingTemplate).convertAndSend(
                        eq("/topic/chat/rooms/100/unread-updates"),
                        unreadUpdateCaptor.capture()
                );
                assertThat(unreadUpdateCaptor.getValue()).containsKey("unreadUpdates");

                // 2. Total Count to User
                verify(simpMessagingTemplate).convertAndSendToUser(
                        eq("creator"),
                        eq("/queue/unread-count"),
                        payloadCaptor.capture()
                );
                assertThat(payloadCaptor.getValue()).isInstanceOf(TotalUnreadCountResponseDTO.class);
            }
        }
    }

    @Nested
    @DisplayName("leaveRoom (채팅방 나가기)")
    class LeaveRoom {
        @Test
        @DisplayName("성공 - 마지막 멤버가 나가면 방 삭제")
        void leaveRoom_Delete_If_Empty() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 팀방 True
            given(chatRoom.getIsTeam()).willReturn(true);
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(0L);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.leaveRoom("creator", 100L);

                // then
                verify(chatEmployeeCreator).leftChatRoom();
                verify(chatRoom).deleteRoom();
                verify(chatRoomRepository).save(chatRoom);
            }
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {
        @Test
        @DisplayName("성공 - 첨부파일 매핑 및 PLACEHOLDER 처리")
        void getMessages_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 메시지 내용이 "파일을 전송 했습니다."
            when(chatMessage.getContent()).thenReturn("파일을 전송 했습니다.");

            Page<ChatMessage> page = new PageImpl<>(List.of(chatMessage));
            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(100L), any(), eq(pageable)
            )).willReturn(page);

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name()))
                    .willReturn(List.of(commonCodeChat));

            // 첨부파일 존재 설정
            AttachmentFile attachment = mock(AttachmentFile.class);
            when(attachment.getOwnerId()).thenReturn(1000L);
            given(attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(any(), anyList()))
                    .willReturn(List.of(attachment));

            // when
            Page<ChatMessageResponseDTO> result = chatService.getMessages("creator", 100L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            ChatMessageResponseDTO dto = result.getContent().get(0);

            // 첨부파일이 있고 내용이 PLACEHOLDER와 같으면 content는 null이어야 함
            assertThat(dto.getContent()).isNull();
            assertThat(dto.getAttachments()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findRoomsByUsername (채팅방 목록)")
    class FindRoomsByUsername {
        @Test
        @DisplayName("성공 - 목록 조회 및 1:1 상대방 매핑")
        void findRoomsByUsername_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(creator))
                    .willReturn(List.of(chatEmployeeCreator));

            given(chatRoom.getIsTeam()).willReturn(false);

            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // when
            List<ChatRoomListResponseDTO> result = chatService.findRoomsByUsername("creator");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Invitee1 Name");
        }
    }
}