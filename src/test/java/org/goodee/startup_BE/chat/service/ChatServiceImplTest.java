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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    // Test Data Mocks
    private Employee creator, invitee1, invitee2;
    private ChatRoom chatRoom;
    private ChatMessage chatMessage;
    private ChatEmployee chatEmployeeCreator, chatEmployeeInvitee1;
    private CommonCode commonCodeTeamNoti, commonCodeChat;

    @Captor private ArgumentCaptor<TransactionSynchronization> syncCaptor;
    @Captor private ArgumentCaptor<List<ChatEmployee>> chatEmployeesCaptor;
    @Captor private ArgumentCaptor<NotificationRequestDTO> notificationCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> unreadUpdateCaptor;

    @BeforeEach
    void setUp() {
        // 기본 Employee 설정
        creator = mock(Employee.class);
        invitee1 = mock(Employee.class);
        invitee2 = mock(Employee.class);
        lenient().when(creator.getEmployeeId()).thenReturn(1L);
        lenient().when(creator.getUsername()).thenReturn("creator");
        lenient().when(creator.getName()).thenReturn("Creator Name");

        lenient().when(invitee1.getEmployeeId()).thenReturn(2L);
        lenient().when(invitee1.getUsername()).thenReturn("invitee1");
        lenient().when(invitee1.getName()).thenReturn("Invitee1 Name");

        lenient().when(invitee2.getEmployeeId()).thenReturn(3L);
        lenient().when(invitee2.getUsername()).thenReturn("invitee2");

        // 기본 ChatRoom 설정
        chatRoom = mock(ChatRoom.class);
        lenient().when(chatRoom.getChatRoomId()).thenReturn(100L);
        lenient().when(chatRoom.getName()).thenReturn("Test Room");

        // 기본 ChatMessage 설정 (toDTO 변환 시 NPE 방지를 위해 상세 설정)
        chatMessage = mock(ChatMessage.class);
        lenient().when(chatMessage.getChatMessageId()).thenReturn(1000L);
        lenient().when(chatMessage.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatMessage.getEmployee()).thenReturn(creator); // 보낸 사람 설정
        lenient().when(chatMessage.getContent()).thenReturn("Test Message");
        lenient().when(chatMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        // MessageType은 기본적으로 null, toDTO 로직상 null이면 일반 메시지로 처리됨

        // 기본 ChatEmployee 설정
        chatEmployeeCreator = mock(ChatEmployee.class);
        lenient().when(chatEmployeeCreator.getEmployee()).thenReturn(creator);
        lenient().when(chatEmployeeCreator.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatEmployeeCreator.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));

        chatEmployeeInvitee1 = mock(ChatEmployee.class);
        lenient().when(chatEmployeeInvitee1.getEmployee()).thenReturn(invitee1);
        lenient().when(chatEmployeeInvitee1.getChatRoom()).thenReturn(chatRoom);

        // CommonCode 설정
        commonCodeTeamNoti = mock(CommonCode.class);
        lenient().when(commonCodeTeamNoti.getCommonCodeId()).thenReturn(500L);

        commonCodeChat = mock(CommonCode.class);
        lenient().when(commonCodeChat.getCommonCodeId()).thenReturn(600L);
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        @Test
        @DisplayName("성공: 1:1 채팅방 신규 생성")
        void createRoom_Success_New_1on1() {
            // given
            List<Long> invitees = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));

            // 기존 방 없음
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(Collections.emptyList());

            // 저장 로직 Mock
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // ChatRoomResponseDTO의 builder() 사용을 위해 CALLS_REAL_METHODS 사용
            try (MockedStatic<ChatRoomResponseDTO> dtoMock = mockStatic(ChatRoomResponseDTO.class, Mockito.CALLS_REAL_METHODS);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                ChatRoomResponseDTO expectedDTO = ChatRoomResponseDTO.builder().chatRoomId(100L).build();
                dtoMock.when(() -> ChatRoomResponseDTO.toDTO(chatRoom, 2L)).thenReturn(expectedDTO);

                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "New Room", invitees);

                // then
                assertThat(result).isEqualTo(expectedDTO);
                verify(chatRoomRepository).save(any(ChatRoom.class));
                verify(chatEmployeeRepository).saveAll(chatEmployeesCaptor.capture());
                assertThat(chatEmployeesCaptor.getValue()).hasSize(2); // Creator + Invitee1

                // After Commit 로직 검증 (시스템 메시지 전송)
                syncCaptor.getValue().afterCommit();
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }

        @Test
        @DisplayName("성공: 1:1 채팅방 기존 방 재활성화 (Rejoin)")
        void createRoom_Success_Rejoin_1on1() {
            // given
            List<Long> invitees = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));

            // 기존 방 있음
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(List.of(chatRoom));

            // 멤버 정보 (Invitee1은 나간 상태 가정)
            given(chatEmployeeCreator.getIsLeft()).willReturn(false);
            given(chatEmployeeCreator.getLastReadMessage()).willReturn(chatMessage);
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true); // 나감
            given(chatEmployeeInvitee1.getLastReadMessage()).willReturn(chatMessage);

            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            try (MockedStatic<ChatRoomResponseDTO> dtoMock = mockStatic(ChatRoomResponseDTO.class, Mockito.CALLS_REAL_METHODS);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                ChatRoomResponseDTO expectedDTO = ChatRoomResponseDTO.builder().chatRoomId(100L).build();
                // 현재 멤버 수 카운트 로직
                dtoMock.when(() -> ChatRoomResponseDTO.toDTO(any(ChatRoom.class), anyLong())).thenReturn(expectedDTO);

                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "Rejoin Room", invitees);

                // then
                assertThat(result).isEqualTo(expectedDTO);
                verify(chatRoomRepository, never()).save(any(ChatRoom.class)); // 새 방 생성 X
                verify(chatEmployeeInvitee1).rejoinChatRoom(); // 나간 사람 재입장 호출 확인
                verify(chatEmployeeRepository).save(chatEmployeeInvitee1); // 업데이트 저장

                // After Commit 검증
                syncCaptor.getValue().afterCommit();
                // 기존 방 정보로 알림 전송 확인
                verify(chatEmployeeRepository, atLeastOnce()).findAllByChatRoomChatRoomIdAndIsLeftFalse(100L);
            }
        }

        @Test
        @DisplayName("성공: 팀 채팅방 생성 및 알림 발송")
        void createRoom_Success_Team() {
            // given
            List<Long> invitees = List.of(2L, 3L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1, invitee2));

            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // CommonCode (팀 채팅 알림용)
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(commonCodeTeamNoti));

            try (MockedStatic<ChatRoomResponseDTO> dtoMock = mockStatic(ChatRoomResponseDTO.class, Mockito.CALLS_REAL_METHODS);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                ChatRoomResponseDTO expectedDTO = ChatRoomResponseDTO.builder().chatRoomId(100L).build();
                dtoMock.when(() -> ChatRoomResponseDTO.toDTO(chatRoom, 3L)).thenReturn(expectedDTO);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.createRoom("creator", "Team Room", invitees);

                // then
                verify(chatEmployeeRepository).saveAll(chatEmployeesCaptor.capture());
                assertThat(chatEmployeesCaptor.getValue()).hasSize(3); // Creator + Invitee1 + Invitee2

                // 알림 서비스 호출 검증
                verify(notificationService, times(2)).create(notificationCaptor.capture());
                List<NotificationRequestDTO> notis = notificationCaptor.getAllValues();
                assertThat(notis).extracting("employeeId").containsExactlyInAnyOrder(2L, 3L);
            }
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoom {

        @Test
        @DisplayName("성공: 새로운 멤버 초대 및 1:1 방에서 팀 방으로 승격")
        void inviteToRoom_Success_UpgradeToTeam() {
            // given
            List<Long> inviteeIds = List.of(3L); // invitee2 초대
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

            // 초대 권한 확인 (생성자는 방에 있음)
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L)).willReturn(true);

            // 초대 대상 조회
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee2));

            // 기존 멤버 조회 (현재 creator, invitee1 있다고 가정 - 1:1 방)
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L)).willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // 방 타입: 1:1 -> 팀 방 변경 필요
            given(chatRoom.getIsTeam()).willReturn(false);

            // 시스템 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // CommonCode for Notification
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(anyString(), anyString()))
                    .willReturn(List.of(commonCodeTeamNoti));

            // when
            chatService.inviteToRoom("creator", 100L, inviteeIds);

            // then
            verify(chatRoom).updateToTeamRoom(); // 팀 방 승격 호출 확인

            // [수정] saveAll이 2번 호출됨 (신규 멤버, 재입장 멤버). 첫 번째 호출(신규)을 검증
            verify(chatEmployeeRepository, times(2)).saveAll(chatEmployeesCaptor.capture());

            // 새로 저장된 멤버 확인 (Invitee2)
            // capture된 값의 첫 번째 요소(첫 번째 호출) 확인
            List<ChatEmployee> savedEmployees = chatEmployeesCaptor.getAllValues().get(0);
            assertThat(savedEmployees).hasSize(1);
            assertThat(savedEmployees.get(0).getEmployee()).isEqualTo(invitee2);

            verify(notificationService, atLeastOnce()).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("실패: 초대자가 방 멤버가 아님")
        void inviteToRoom_Fail_AccessDenied() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatService.inviteToRoom("creator", 100L, List.of(2L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("구성원이 아니면");
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        @Test
        @DisplayName("성공: 1:1 방에서 상대방이 나간 경우 재입장 처리 후 전송")
        void sendMessage_Success_1on1_Rejoin() {
            // given
            String content = "Hello";
            MessageSendPayloadDTO payload = new MessageSendPayloadDTO(content, null);

            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

            // 보낸 사람 멤버십 확인
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 1:1 방 설정
            given(chatRoom.getIsTeam()).willReturn(false);

            // 전체 멤버 조회 (상대방이 나간 상태)
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // 미읽음 수 계산 (상대방이 있으므로 1)
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(100L, 1L))
                    .willReturn(1L);

            // [수정] ChatMessageResponseDTO Mocking 제거 -> Real Method 사용
            // chatMessage Mock이 잘 설정되어 있으므로 실제 toDTO 사용해도 안전함
            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                chatService.sendMessage("creator", 100L, payload);

                // then
                verify(chatEmployeeInvitee1).rejoinChatRoom(); // 재입장 검증
                verify(chatEmployeeRepository).save(chatEmployeeInvitee1); // 저장 검증
                verify(chatMessageRepository).save(any(ChatMessage.class));

                // 읽음 처리 업데이트 검증
                verify(chatEmployeeCreator).updateLastReadMessage(chatMessage);

                // After Commit STOMP 전송
                syncCaptor.getValue().afterCommit();
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }
    }

    @Nested
    @DisplayName("sendMessageWithFiles (파일 전송)")
    class SendMessageWithFiles {

        @Test
        @DisplayName("성공: 파일 업로드 및 메시지 저장")
        void sendMessageWithFiles_Success() {
            // given
            List<MultipartFile> files = List.of(mock(MultipartFile.class));
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // CommonCode CHAT
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name()))
                    .willReturn(List.of(commonCodeChat));

            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            // File Service Mock
            given(attachmentFileService.uploadFiles(anyList(), anyLong(), anyLong()))
                    .willReturn(new ArrayList<>()); // 빈 리스트 반환 가정

            // [수정] ChatMessageResponseDTO.class에 대한 MockStatic 제거
            // Mocking 충돌(UnfinishedStubbingException) 방지 및 Real DTO 변환 로직 사용
            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture())).then(invocation -> null);

                // when
                chatService.sendMessageWithFiles("creator", 100L, "File Msg", files);

                // then
                verify(attachmentFileService).uploadFiles(eq(files), eq(600L), eq(1000L));
                verify(chatMessageRepository).save(any(ChatMessage.class));

                syncCaptor.getValue().afterCommit();
                // Real DTO가 생성되어 전송되었는지 확인
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {

        @Test
        @DisplayName("성공: 메시지 목록과 첨부파일 조회")
        void getMessages_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // Page Mock
            Page<ChatMessage> page = new PageImpl<>(List.of(chatMessage));
            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(100L), any(), eq(pageable))).willReturn(page);

            // CommonCode & Attachments
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(anyString(), anyString()))
                    .willReturn(List.of(commonCodeChat));

            AttachmentFile attachment = mock(AttachmentFile.class);
            given(attachment.getOwnerId()).willReturn(1000L); // messageId와 일치
            given(attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(any(), anyList()))
                    .willReturn(List.of(attachment));

            // unread count
            given(chatMessage.getEmployee()).willReturn(creator);
            given(chatEmployeeRepository.countUnreadByAllParticipants(anyLong(), any())).willReturn(0L);

            // AttachmentFileResponseDTO도 Real Method 사용 (단순 매핑이므로)
            try (MockedStatic<AttachmentFileResponseDTO> fileDtoMock = mockStatic(AttachmentFileResponseDTO.class, Mockito.CALLS_REAL_METHODS)) {

                // AttachmentFile Mock에 필요한 Getter 설정 (NPE 방지)
                lenient().when(attachment.getFileId()).thenReturn(1L);
                lenient().when(attachment.getOriginalName()).thenReturn("file.txt");
                lenient().when(attachment.getStoragePath()).thenReturn("http://url");

                // when
                Page<ChatMessageResponseDTO> result = chatService.getMessages("creator", 100L, pageable);

                // then
                assertThat(result.getContent()).hasSize(1);
                verify(attachmentFileRepository).findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(any(), anyList());
                // DTO에 첨부파일 설정 확인
                assertThat(result.getContent().get(0).getAttachments()).hasSize(1);
            }
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (읽음 처리)")
    class UpdateLastReadMessageId {

        @Test
        @DisplayName("성공: 최신 메시지 읽음 처리 및 카운트 갱신 브로드캐스트")
        void updateLastReadMessageId_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 타겟 메시지 조회
            given(chatMessageRepository.findById(1000L)).willReturn(Optional.of(chatMessage));
            // 기존 마지막 읽은 메시지가 타겟보다 과거라고 가정
            ChatMessage oldMsg = mock(ChatMessage.class);
            given(oldMsg.getCreatedAt()).willReturn(LocalDateTime.now().minusHours(1));
            given(chatEmployeeCreator.getLastReadMessage()).willReturn(oldMsg);

            // 방금 읽은 메시지 목록 조회 (1개라고 가정)
            given(chatMessageRepository.findAllByChatRoomChatRoomIdAndCreatedAtAfterAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    anyLong(), any(), any())).willReturn(List.of(chatMessage));

            given(chatMessage.getEmployee()).willReturn(invitee1); // 보낸 사람이 있음
            given(chatEmployeeRepository.countUnreadByAllParticipants(anyLong(), any())).willReturn(0L);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                chatService.updateLastReadMessageId("creator", 100L, 1000L);

                // then
                verify(chatEmployeeCreator).updateLastReadMessage(chatMessage);
                verify(chatEmployeeRepository).save(chatEmployeeCreator);

                // After Commit
                syncCaptor.getValue().afterCommit();

                // STOMP /unread-updates 검증
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100/unread-updates"), unreadUpdateCaptor.capture());
                Map<String, Object> payload = unreadUpdateCaptor.getValue();
                assertThat(payload).containsKey("unreadUpdates");

                // STOMP /queue/unread-count 검증
                verify(chatEmployeeRepository).sumTotalUnreadMessagesByEmployeeId(1L);
                verify(simpMessagingTemplate).convertAndSendToUser(eq("creator"), eq("/queue/unread-count"), any(TotalUnreadCountResponseDTO.class));
            }
        }
    }

    @Nested
    @DisplayName("findRoomsByUsername (채팅방 목록 조회)")
    class FindRoomsByUsername {
        @Test
        @DisplayName("성공: 1:1 방 목록 조회 및 상대방 정보 매핑")
        void findRoomsByUsername_Success_1on1() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(creator))
                    .willReturn(List.of(chatEmployeeCreator));

            given(chatRoom.getIsTeam()).willReturn(false);
            given(chatEmployeeCreator.getChatRoom()).willReturn(chatRoom);
            given(chatEmployeeCreator.getJoinedAt()).willReturn(LocalDateTime.now());

            // 마지막 메시지
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                    .willReturn(Optional.of(chatMessage));

            // 미읽음 수
            given(chatMessageRepository.countByChatRoomAndCreatedAtAfterAndEmployeeIsNotNull(any(), any()))
                    .willReturn(5L);

            // 멤버 수
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(2L);

            // 1:1 상대방 찾기
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // Mockito.CALLS_REAL_METHODS 추가
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class, Mockito.CALLS_REAL_METHODS)) {
                ChatRoomListResponseDTO listDTO = ChatRoomListResponseDTO.builder().chatRoomId(100L).build();
                // toDTO 호출 시 상대방 이름(invitee1)이 들어가는지 확인
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(
                        eq(chatRoom), eq("invitee1"), any(), eq(5L), any(), eq(2L)
                )).thenReturn(listDTO);

                // when
                List<ChatRoomListResponseDTO> result = chatService.findRoomsByUsername("creator");

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0)).isEqualTo(listDTO);
            }
        }
    }

    @Nested
    @DisplayName("getRoomById (단일 채팅방 조회)")
    class GetRoomById {
        @Test
        @DisplayName("성공: 1:1 방 나간 후 재입장 처리")
        void getRoomById_Success_Rejoin() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.of(chatEmployeeCreator));

            given(chatEmployeeCreator.getChatRoom()).willReturn(chatRoom);
            given(chatRoom.getIsTeam()).willReturn(false);
            given(chatEmployeeCreator.getIsLeft()).willReturn(true); // 나감

            // 기타 필요한 Mock (목록 조회 로직과 동일)
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // Mockito.CALLS_REAL_METHODS 추가
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class, Mockito.CALLS_REAL_METHODS)) {
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(any(), any(), any(), anyLong(), any(), anyLong()))
                        .thenReturn(mock(ChatRoomListResponseDTO.class));

                // when
                chatService.getRoomById("creator", 100L);

                // then
                verify(chatEmployeeCreator).rejoinChatRoom(); // 재입장 메서드 호출 확인
                verify(chatEmployeeRepository).save(chatEmployeeCreator);
            }
        }
    }

    @Nested
    @DisplayName("leaveRoom (채팅방 나가기)")
    class LeaveRoom {

        @Test
        @DisplayName("성공: 방 나가기 후 인원이 0명이면 방 삭제")
        void leaveRoom_DeleteRoom_If_Empty() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.of(chatEmployeeCreator));

            given(chatRoom.getIsTeam()).willReturn(true);
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(0L);

            // when
            chatService.leaveRoom("creator", 100L);

            // then
            verify(chatEmployeeCreator).leftChatRoom();
            verify(chatEmployeeRepository).save(chatEmployeeCreator);
            verify(chatRoom).deleteRoom();
            verify(chatRoomRepository).save(chatRoom);
        }
    }
}