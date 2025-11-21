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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
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

    // --- Mock Objects ---
    private Employee creator, invitee1, invitee2;
    private CommonCode posManager, posStaff;
    private ChatRoom chatRoom, teamChatRoom;
    private ChatMessage chatMessage, systemMessage;
    private ChatEmployee chatEmployeeCreator, chatEmployeeInvitee1;
    private CommonCode commonCodeTeamNoti, commonCodeChat;

    // --- Captors ---
    @Captor private ArgumentCaptor<TransactionSynchronization> syncCaptor;
    @Captor private ArgumentCaptor<List<ChatEmployee>> chatEmployeesCaptor;
    @Captor private ArgumentCaptor<NotificationRequestDTO> notificationCaptor;
    @Captor private ArgumentCaptor<ChatMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        // 1. CommonCode (직급, OwnerType 등)
        posManager = mock(CommonCode.class);
        posStaff = mock(CommonCode.class);
        lenient().when(posManager.getValue1()).thenReturn("Manager");
        lenient().when(posStaff.getValue1()).thenReturn("Staff");

        commonCodeTeamNoti = mock(CommonCode.class);
        lenient().when(commonCodeTeamNoti.getCommonCodeId()).thenReturn(500L);

        commonCodeChat = mock(CommonCode.class);
        lenient().when(commonCodeChat.getCommonCodeId()).thenReturn(600L);

        // 2. Employee
        creator = mock(Employee.class);
        lenient().when(creator.getEmployeeId()).thenReturn(1L);
        lenient().when(creator.getUsername()).thenReturn("creator");
        lenient().when(creator.getName()).thenReturn("Creator Name");
        lenient().when(creator.getPosition()).thenReturn(posManager);
        lenient().when(creator.getProfileImg()).thenReturn("creator.png");

        invitee1 = mock(Employee.class);
        lenient().when(invitee1.getEmployeeId()).thenReturn(2L);
        lenient().when(invitee1.getUsername()).thenReturn("invitee1");
        lenient().when(invitee1.getName()).thenReturn("Invitee1 Name");
        lenient().when(invitee1.getPosition()).thenReturn(posStaff);
        lenient().when(invitee1.getProfileImg()).thenReturn("invitee1.png");

        invitee2 = mock(Employee.class);
        lenient().when(invitee2.getEmployeeId()).thenReturn(3L);
        lenient().when(invitee2.getUsername()).thenReturn("invitee2");
        lenient().when(invitee2.getName()).thenReturn("Invitee2 Name");

        // 3. ChatRoom (1:1)
        chatRoom = mock(ChatRoom.class);
        lenient().when(chatRoom.getChatRoomId()).thenReturn(100L);
        lenient().when(chatRoom.getName()).thenReturn("OneOnOne Room");
        lenient().when(chatRoom.getIsTeam()).thenReturn(false);
        lenient().when(chatRoom.getEmployee()).thenReturn(creator);
        lenient().when(chatRoom.getCreatedAt()).thenReturn(LocalDateTime.now());

        // 4. ChatMessage
        chatMessage = mock(ChatMessage.class);
        lenient().when(chatMessage.getChatMessageId()).thenReturn(1000L);
        lenient().when(chatMessage.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatMessage.getEmployee()).thenReturn(creator);
        lenient().when(chatMessage.getContent()).thenReturn("Test Content");
        lenient().when(chatMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(chatMessage.getMessageType()).thenReturn(OwnerType.CHAT_USER);

        // 5. ChatEmployee
        chatEmployeeCreator = mock(ChatEmployee.class);
        lenient().when(chatEmployeeCreator.getEmployee()).thenReturn(creator);
        lenient().when(chatEmployeeCreator.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatEmployeeCreator.getIsLeft()).thenReturn(false);
        lenient().when(chatEmployeeCreator.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));

        chatEmployeeInvitee1 = mock(ChatEmployee.class);
        lenient().when(chatEmployeeInvitee1.getEmployee()).thenReturn(invitee1);
        lenient().when(chatEmployeeInvitee1.getChatRoom()).thenReturn(chatRoom);
        lenient().when(chatEmployeeInvitee1.getIsLeft()).thenReturn(false);
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        @Test
        @DisplayName("성공 - 1:1 채팅방 신규 생성 (기존 방 없음)")
        void createRoom_Success_New_1on1() {
            // given
            List<Long> inviteeIds = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(Collections.emptyList());

            // save 시 Mock 객체가 아닌 실제 로직 흐름을 타서 반환해야 하므로 Answer 사용
            // (Service에서 ChatRoom.createChatRoom 호출 -> Repository.save)
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
                ChatRoom room = invocation.getArgument(0);
                ReflectionTestUtils.setField(room, "chatRoomId", 100L);
                ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
                return room; // ID가 세팅된 객체 반환
            });

            given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
                ChatMessage msg = invocation.getArgument(0);
                ReflectionTestUtils.setField(msg, "chatMessageId", 500L);
                return msg;
            });

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "New Room", inviteeIds);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getIsTeam()).isFalse();
                assertThat(result.getDisplayName()).isEqualTo("Invitee1 Name"); // 상대방 이름 표시 확인

                verify(chatRoomRepository).save(any(ChatRoom.class));
                verify(chatEmployeeRepository).saveAll(chatEmployeesCaptor.capture());

                List<ChatEmployee> savedMembers = chatEmployeesCaptor.getValue();
                assertThat(savedMembers).hasSize(2); // 생성자 + 초대자

                // Transaction Sync - After Commit
                syncCaptor.getValue().afterCommit();
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 기존 방 재활성화 (상대방이 나갔던 경우)")
        void createRoom_Success_Rejoin_1on1() {
            // given
            List<Long> inviteeIds = List.of(2L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1));
            // 기존 방 발견
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(List.of(chatRoom));

            // 기존 멤버 조회 (Invitee1은 나간 상태)
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true);
            given(chatEmployeeCreator.getLastReadMessage()).willReturn(chatMessage);

            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "Rejoin", inviteeIds);

                // then
                verify(chatRoomRepository, never()).save(any(ChatRoom.class)); // 방 생성 안함
                verify(chatEmployeeInvitee1, atLeastOnce()).rejoinChatRoom(); // 재입장 로직 수행 확인
                verify(chatEmployeeRepository, atLeastOnce()).save(chatEmployeeInvitee1);

                assertThat(result.getChatRoomId()).isEqualTo(100L);
            }
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 생성")
        void createRoom_Success_Team() {
            // given
            List<Long> inviteeIds = List.of(2L, 3L); // 2명 이상 -> 팀방
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee1, invitee2));

            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
                ChatRoom room = invocation.getArgument(0);
                ReflectionTestUtils.setField(room, "chatRoomId", 200L);
                ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
                return room;
            });
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage); // System Msg

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(commonCodeTeamNoti));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                ChatRoomResponseDTO result = chatService.createRoom("creator", "Team Room", inviteeIds);

                // then
                assertThat(result.getIsTeam()).isTrue();
                verify(notificationService, times(2)).create(any(NotificationRequestDTO.class));
            }
        }

        @Test
        @DisplayName("실패 - 본인을 초대한 경우")
        void createRoom_Fail_SelfInvite() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            // 본인 ID 포함
            List<Long> inviteeIds = List.of(1L);

            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creator", "Title", inviteeIds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("초대할 수 있는 대상이 아닙니다");
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoom {

        @Test
        @DisplayName("성공 - 1:1 방에서 초대 시 팀 방으로 승격")
        void inviteToRoom_Success_UpgradeToTeam() {
            // given
            List<Long> newInvitees = List.of(3L);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));

            // 권한 체크
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L))
                    .willReturn(true);

            given(employeeRepository.findAllById(anyCollection())).willReturn(List.of(invitee2));
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // 방금 전까지 1:1 방이었음
            // chatRoom Mock은 기본적으로 isTeam=false
            // updateToTeamRoom 호출 여부 확인을 위해 Spy 사용 가능하나 여기선 verify로 충분

            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(systemMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(commonCodeTeamNoti));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.inviteToRoom("creator", 100L, newInvitees);

                // then
                verify(chatRoom).updateToTeamRoom(); // 승격 확인
                verify(chatEmployeeRepository, atLeastOnce()).saveAll(anyList()); // 새 멤버 저장
                verify(notificationService).create(any(NotificationRequestDTO.class));
            }
        }

        @Test
        @DisplayName("실패 - 권한 없음 (방 멤버가 아님)")
        void inviteToRoom_Fail_AccessDenied() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L))
                    .willReturn(false); // 멤버 아님

            // when & then
            assertThatThrownBy(() -> chatService.inviteToRoom("creator", 100L, List.of(3L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 구성원이 아니면 초대할 수 없습니다.");
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

            // 나간 후 남은 인원 0명 가정
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(0L);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.leaveRoom("creator", 100L);

                // then
                verify(chatEmployeeCreator).leftChatRoom();
                verify(chatRoom).deleteRoom(); // 방 삭제 마킹
                verify(chatRoomRepository).save(chatRoom);
            }
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        @Test
        @DisplayName("성공 - 1:1 방에서 상대방이 나갔으면 자동 재입장")
        void sendMessage_Success_Rejoin_Opponent() {
            // given
            MessageSendPayloadDTO payload = new MessageSendPayloadDTO("Hello", null);
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom)); // isTeam=false

            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 상대방(Invitee1)은 나간 상태(isLeft=true)
            given(chatEmployeeInvitee1.getIsLeft()).willReturn(true);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(chatMessage);

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.sendMessage("creator", 100L, payload);

                // then
                verify(chatEmployeeInvitee1).rejoinChatRoom(); // 재입장
                verify(chatEmployeeRepository).save(chatEmployeeInvitee1);
                verify(chatMessageRepository).save(any(ChatMessage.class));
            }
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {

        @Test
        @DisplayName("성공 - 페이징 조회 및 DTO 변환")
        void getMessages_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // DTO 변환 과정에서 ChatMessage -> Employee -> Name 참조 발생하므로
            // setUp()에서 설정한 Mock 객체들이 정상 동작해야 함.
            Page<ChatMessage> page = new PageImpl<>(List.of(chatMessage));

            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(100L), any(), eq(pageable)
            )).willReturn(page);

            // 글로벌 최신 메시지 (읽음 처리 로직용 - 비어있다고 가정하여 업데이트 스킵)
            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(100L), any(), eq(Pageable.ofSize(1))
            )).willReturn(Page.empty());

            // 첨부파일 조회용 Code
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name()))
                    .willReturn(List.of(commonCodeChat));
            given(attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(any(), anyList()))
                    .willReturn(Collections.emptyList());

            // when
            Page<ChatMessageResponseDTO> result = chatService.getMessages("creator", 100L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("Test Content");
            assertThat(result.getContent().get(0).getSenderName()).isEqualTo("Creator Name");
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (읽음 처리)")
    class UpdateLastReadMessageId {

        @Test
        @DisplayName("성공 - 새로운 메시지 읽음 처리 및 STOMP 전송")
        void updateLastReadMessageId_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 타겟 메시지
            given(chatMessageRepository.findById(1000L)).willReturn(Optional.of(chatMessage));
            // 읽음 카운트
            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(1L)).willReturn(5L);

            // 읽은 구간 메시지들 (상대방 화면 업데이트용)
            given(chatMessageRepository.findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    eq(100L), any(), any()
            )).willReturn(List.of(chatMessage));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // when
                chatService.updateLastReadMessageId("creator", 100L, 1000L);

                // then
                verify(chatEmployeeCreator).updateLastReadMessage(chatMessage);

                // After Commit 검증
                syncCaptor.getValue().afterCommit();

                // 1. 전체 안읽음 개수 전송 확인
                verify(simpMessagingTemplate).convertAndSendToUser(eq("creator"), eq("/queue/unread-count"), any(TotalUnreadCountResponseDTO.class));

                // 2. 채팅방 내 읽음 숫자 업데이트 전송 확인
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100/unread-updates"), anyMap());
            }
        }
    }

    @Nested
    @DisplayName("findRoomsByUsername (채팅방 목록)")
    class FindRoomsByUsername {
        @Test
        @DisplayName("성공 - 목록 조회 및 1:1 상대방 정보 매핑")
        void findRoomsByUsername_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(creator))
                    .willReturn(List.of(chatEmployeeCreator));

            // 1:1 방이므로 모든 멤버 조회 필요
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(chatEmployeeCreator, chatEmployeeInvitee1));

            // when
            List<ChatRoomListResponseDTO> result = chatService.findRoomsByUsername("creator");

            // then
            assertThat(result).hasSize(1);
            // DTO 매핑 확인 (상대방 이름이 나와야 함)
            assertThat(result.get(0).getName()).isEqualTo("Invitee1 Name");
            assertThat(result.get(0).getProfile()).isEqualTo("invitee1.png");
        }
    }

    @Nested
    @DisplayName("sendMessageWithFiles (파일 전송)")
    class SendMessageWithFiles {
        @Test
        @DisplayName("성공 - 파일 업로드 서비스 호출 및 메시지 저장")
        void sendMessageWithFiles_Success() {
            // given
            List<MultipartFile> files = List.of(mock(MultipartFile.class));
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(creator));
            given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creator"))
                    .willReturn(Optional.of(chatEmployeeCreator));

            // 메시지 저장 (ID 세팅)
            given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(inv -> {
                ChatMessage msg = inv.getArgument(0);
                ReflectionTestUtils.setField(msg, "chatMessageId", 1000L);
                ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.now());
                return msg;
            });

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.CHAT.name()))
                    .willReturn(List.of(commonCodeChat));

            // 파일 서비스 Mock
            given(attachmentFileService.uploadFiles(anyList(), eq(600L), eq(1000L)))
                    .willReturn(List.of(new AttachmentFileResponseDTO()));

            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any())).then(invocation -> null);

                // when
                chatService.sendMessageWithFiles("creator", 100L, "File Msg", files);

                // then
                verify(attachmentFileService).uploadFiles(anyList(), eq(600L), eq(1000L));
                verify(chatMessageRepository).save(any(ChatMessage.class));
            }
        }
    }
}