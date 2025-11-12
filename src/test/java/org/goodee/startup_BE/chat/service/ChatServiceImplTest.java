package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
import org.goodee.startup_BE.chat.dto.ChatMessageResponseDTO;
import org.goodee.startup_BE.chat.dto.ChatRoomListResponseDTO;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;
// ArgumentMatchers static import
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
// BDDMockito static import
import static org.mockito.BDDMockito.given;
// Mockito static import
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class ChatServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private ChatServiceImpl chatServiceImpl;

    // --- Mock 객체 선언 ---
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatEmployeeRepository chatEmployeeRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CommonCodeRepository commonCodeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    // --- Mock 객체 캡처용 ---
    @Captor
    private ArgumentCaptor<TransactionSynchronization> syncCaptor;
    @Captor
    private ArgumentCaptor<ChatMessage> chatMessageCaptor;
    @Captor
    private ArgumentCaptor<List<ChatEmployee>> chatEmployeeListCaptor;
    @Captor
    private ArgumentCaptor<NotificationRequestDTO> notificationRequestCaptor;

    // --- 테스트 공용 Mock 인스턴스 ---
    private Employee mockCreator;
    private Employee mockInvitee1;
    private Employee mockInvitee2;
    private ChatRoom mockChatRoom;
    private ChatRoom mockTeamChatRoom;
    private ChatMessage mockSystemMessage;
    private ChatMessage mockUserMessage;
    private ChatEmployee mockChatEmployee;
    private CommonCode mockChatInviteCode;

    // 정적(static) Mock을 위한 핸들러
    private static MockedStatic<TransactionSynchronizationManager> mockedTxSyncManager;

    @BeforeAll
    static void beforeAll() {
        // TransactionSynchronizationManager.registerSynchronization 정적 호출을 Mocking
        mockedTxSyncManager = mockStatic(TransactionSynchronizationManager.class);
    }

    @AfterAll
    static void afterAll() {
        // 정적 Mock 닫기
        mockedTxSyncManager.close();
    }


    @BeforeEach // 각 테스트 실행 전 공통 설정
    void setUp() {
        // --- Mock 객체 초기화 ---
        mockCreator = mock(Employee.class);
        mockInvitee1 = mock(Employee.class);
        mockInvitee2 = mock(Employee.class);
        mockChatRoom = mock(ChatRoom.class); // 1:1 채팅방
        mockTeamChatRoom = mock(ChatRoom.class); // 팀 채팅방
        mockSystemMessage = mock(ChatMessage.class);
        mockUserMessage = mock(ChatMessage.class);
        mockChatEmployee = mock(ChatEmployee.class); // mockCreator의 멤버십
        mockChatInviteCode = mock(CommonCode.class);

        // --- DTO 변환(toDTO)을 위한 공통 Mocking (lenient) ---
        // ( ... DTO 변환 Mocking 생략 ... )
        lenient().when(mockCreator.getEmployeeId()).thenReturn(1L);
        lenient().when(mockCreator.getUsername()).thenReturn("creator");
        lenient().when(mockCreator.getName()).thenReturn("CreatorName");
        lenient().when(mockCreator.getProfileImg()).thenReturn("creator.png");

        lenient().when(mockInvitee1.getEmployeeId()).thenReturn(2L);
        lenient().when(mockInvitee1.getUsername()).thenReturn("invitee1");
        lenient().when(mockInvitee1.getName()).thenReturn("InviteeOneName");
        lenient().when(mockInvitee1.getProfileImg()).thenReturn("invitee1.png");

        lenient().when(mockInvitee2.getEmployeeId()).thenReturn(3L);
        lenient().when(mockInvitee2.getUsername()).thenReturn("invitee2");
        lenient().when(mockInvitee2.getName()).thenReturn("InviteeTwoName");
        lenient().when(mockInvitee2.getProfileImg()).thenReturn("invitee2.png");

        lenient().when(mockChatRoom.getChatRoomId()).thenReturn(100L);
        lenient().when(mockChatRoom.getName()).thenReturn("Test Room");
        lenient().when(mockChatRoom.getEmployee()).thenReturn(mockCreator);
        lenient().when(mockChatRoom.getIsTeam()).thenReturn(false);
        lenient().when(mockChatRoom.getCreatedAt()).thenReturn(LocalDateTime.now());

        lenient().when(mockTeamChatRoom.getChatRoomId()).thenReturn(200L);
        lenient().when(mockTeamChatRoom.getName()).thenReturn("Team Room");
        lenient().when(mockTeamChatRoom.getEmployee()).thenReturn(mockCreator);
        lenient().when(mockTeamChatRoom.getIsTeam()).thenReturn(true);

        lenient().when(mockSystemMessage.getChatMessageId()).thenReturn(1001L);
        lenient().when(mockSystemMessage.getChatRoom()).thenReturn(mockChatRoom);
        lenient().when(mockSystemMessage.getEmployee()).thenReturn(null);
        lenient().when(mockSystemMessage.getContent()).thenReturn("System Message");
        lenient().when(mockSystemMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

        lenient().when(mockUserMessage.getChatMessageId()).thenReturn(1002L);
        lenient().when(mockUserMessage.getChatRoom()).thenReturn(mockChatRoom);
        lenient().when(mockUserMessage.getEmployee()).thenReturn(mockCreator);
        lenient().when(mockUserMessage.getContent()).thenReturn("User Message");
        lenient().when(mockUserMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

        lenient().when(mockChatEmployee.getEmployee()).thenReturn(mockCreator);
        lenient().when(mockChatEmployee.getChatRoom()).thenReturn(mockChatRoom);
        lenient().when(mockChatEmployee.getLastReadMessage()).thenReturn(mockSystemMessage);
        lenient().when(mockChatEmployee.getJoinedAt()).thenReturn(LocalDateTime.now().minusHours(1));

        lenient().when(mockChatInviteCode.getCommonCodeId()).thenReturn(50L);

        // 정적 Mockito 호출 설정 (SUT가 호출할 때 아무것도 하지 않도록)
        mockedTxSyncManager.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                .then(invocation -> {
                    return null; // void 메서드처럼 행동
                });

        // [FIXED] 정적 Mock의 호출 횟수를 매 테스트 전에 초기화
        mockedTxSyncManager.clearInvocations();
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        @Test
        @DisplayName("성공 - 팀 채팅방 (2명 초대)")
        void createRoom_Success_TeamChat() {
            // given
            String creatorUsername = "creator";
            String roomName = "New Team Room";
            List<Long> inviteeIds = List.of(2L, 3L);
            List<Employee> invitees = List.of(mockInvitee1, mockInvitee2);

            // 1. Creator 조회
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            // 2. Invitees 조회
            given(employeeRepository.findAllById(new LinkedHashSet<>(inviteeIds))).willReturn(invitees);
            // 3. ChatRoom 저장 (isTeam=true)
            given(chatRoomRepository.save(any(ChatRoom.class))).will(invocation -> {
                return mockTeamChatRoom; // isTeam=true인 Mock 객체
            });
            // 4. 시스템 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            // 5. 알림용 CommonCode 조회
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()
            )).willReturn(List.of(mockChatInviteCode));
            // 6. 팀방 여부 (DTO 반환 값 검증용)
            when(mockTeamChatRoom.getIsTeam()).thenReturn(true);


            // when
            ChatRoomResponseDTO result = chatServiceImpl.createRoom(creatorUsername, roomName, inviteeIds);

            // then
            // [FIXED] 1. verify로 캡처
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            // [FIXED] 2. 캡처 후 실행
            syncCaptor.getValue().afterCommit();

            // 2. DB 저장 로직 검증
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatMessageRepository).save(chatMessageCaptor.capture());
            verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());

            // 3. WebSocket 전송 검증
            verify(simpMessagingTemplate).convertAndSend(
                    eq("/topic/chat/rooms/" + mockTeamChatRoom.getChatRoomId()), // 200L
                    any(ChatMessageResponseDTO.class)
            );

            // 4. 알림 전송 검증 (팀 채팅방이므로 2명에게 전송)
            verify(notificationService, times(2)).create(notificationRequestCaptor.capture());
            List<NotificationRequestDTO> notifications = notificationRequestCaptor.getAllValues();
            assertThat(notifications.get(0).getEmployeeId()).isEqualTo(2L);
            assertThat(notifications.get(1).getEmployeeId()).isEqualTo(3L);
            assertThat(notifications.get(0).getOwnerTypeCommonCodeId()).isEqualTo(50L);

            // 5. 반환 값 검증
            assertThat(result).isNotNull();
            assertThat(result.getChatRoomId()).isEqualTo(mockTeamChatRoom.getChatRoomId());
            assertThat(result.getIsTeam()).isTrue();

            // 6. 저장된 엔티티 값 검증
            assertThat(chatMessageCaptor.getValue().getEmployee()).isNull(); // 시스템 메시지
            assertThat(chatEmployeeListCaptor.getValue()).hasSize(3); // Creator + 2 Invitees
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 (1명 초대)")
        void createRoom_Success_OneToOneChat() {
            // given
            String creatorUsername = "creator";
            String roomName = "1:1 Room";
            List<Long> inviteeIds = List.of(2L); // 1명만 초대
            List<Employee> invitees = List.of(mockInvitee1);

            // SUT의 `isTeamChat = invitees.size() >= 2` 로직에 따라 isTeam=false
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(new LinkedHashSet<>(inviteeIds))).willReturn(invitees);
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(mockChatRoom); // isTeam=false인 Mock 객체
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            // isTeam=false 이므로 DTO 반환을 위해 명시적 Mocking
            when(mockChatRoom.getIsTeam()).thenReturn(false);


            // when
            ChatRoomResponseDTO result = chatServiceImpl.createRoom(creatorUsername, roomName, inviteeIds);

            // then
            // [FIXED] 1. verify로 캡처
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            // [FIXED] 2. 캡처 후 실행
            syncCaptor.getValue().afterCommit(); // afterCommit 실행

            // 1. DB 저장 로직 검증
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());

            // 2. WebSocket 전송 검증
            verify(simpMessagingTemplate).convertAndSend(
                    eq("/topic/chat/rooms/" + mockChatRoom.getChatRoomId()), // 100L
                    any(ChatMessageResponseDTO.class)
            );

            // 3. 알림 전송 검증 (1:1 채팅방이므로 알림 전송 안 함)
            verify(notificationService, never()).create(any(NotificationRequestDTO.class));

            // 4. 반환 값 검증
            assertThat(result).isNotNull();
            assertThat(result.getChatRoomId()).isEqualTo(mockChatRoom.getChatRoomId());
            assertThat(result.getIsTeam()).isFalse(); // 1:1 채팅방

            // 5. 저장된 엔티티 값 검증
            assertThat(chatEmployeeListCaptor.getValue()).hasSize(2); // Creator + 1 Invitee
        }

        @Test
        @DisplayName("실패 - 생성자 없음")
        void createRoom_Fail_CreatorNotFound() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.createRoom("creator", "Room", List.of(2L)))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("존재 하지 않은 사원 입니다");

            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 초대 대상 없음 (Empty List)")
        void createRoom_Fail_EmptyInvitees() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.createRoom("creator", "Room", Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("최소 한 명 이상 초대해야 합니다");
        }

        @Test
        @DisplayName("실패 - 초대 대상에 본인 포함")
        void createRoom_Fail_InviteSelf() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            when(mockCreator.getEmployeeId()).thenReturn(1L);

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.createRoom("creator", "Room", List.of(1L, 2L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("초대할 수 있는 대상이 아닙니다");
        }

        @Test
        @DisplayName("실패 - 초대 대상 중 존재하지 않는 사원")
        void createRoom_Fail_InviteeNotFound() {
            // given
            List<Long> inviteeIds = List.of(2L, 999L); // 999L은 존재하지 않음
            List<Employee> foundInvitees = List.of(mockInvitee1); // 1명만 조회됨

            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(new LinkedHashSet<>(inviteeIds))).willReturn(foundInvitees); // 요청 2명, 반환 1명

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.createRoom("creator", "Room", inviteeIds))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoom {

        // [FIXED] 필드 초기화를 @BeforeEach로 이동
        private Long roomId;
        private List<Long> inviteeIds;
        private List<Employee> invitees;

        @BeforeEach
        void inviteSetup() {
            // [FIXED] NPE 방지. setUp() 이후 mock 객체가 생성된 뒤 필드 초기화
            roomId = 100L;
            inviteeIds = List.of(3L); // mockInvitee2 (3L)
            invitees = List.of(mockInvitee2);
        }


        @Test
        @DisplayName("성공 - 1:1 채팅방에서 초대하여 팀 채팅방으로 전환")
        void inviteToRoom_Success_ConvertToTeamRoom() {
            // given
            // 1. Inviter(creator) 조회
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            // 2. Room 조회 (1:1 채팅방)
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockChatRoom));
            // 3. Inviter가 멤버인지 확인
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(roomId, 1L))
                    .willReturn(true);
            // 4. Invitee(invitee2) 조회
            given(employeeRepository.findAllById(new LinkedHashSet<>(inviteeIds))).willReturn(invitees);
            // 5. 기존 멤버 ID 조회 (creator=1L, invitee1=2L)
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(roomId)).willReturn(Set.of(1L, 2L));
            // 6. 방 상태가 1:1방이라고 명시
            when(mockChatRoom.getIsTeam()).thenReturn(false);
            // 7. 시스템 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            // 8. 알림용 CommonCode 조회
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()
            )).willReturn(List.of(mockChatInviteCode));

            // when
            chatServiceImpl.inviteToRoom("creator", roomId, inviteeIds);

            // then
            // [FIXED] 9. verify로 캡처
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            // [FIXED] 10. 캡처 후 실행
            syncCaptor.getValue().afterCommit();

            // 10. 1:1 -> 팀방으로 전환 메서드 호출 검증
            verify(mockChatRoom).updateToTeamRoom();
            // 11. 시스템 메시지 저장 검증
            verify(chatMessageRepository).save(chatMessageCaptor.capture());
            assertThat(chatMessageCaptor.getValue().getContent()).contains(mockInvitee2.getName());
            // 12. 신규 멤버 저장 검증
            verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());
            assertThat(chatEmployeeListCaptor.getValue()).hasSize(1);
            assertThat(chatEmployeeListCaptor.getValue().get(0).getEmployee()).isEqualTo(mockInvitee2);
            // 13. WebSocket 전송 검증
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/" + roomId), any(ChatMessageResponseDTO.class));
            // 14. 알림 전송 검증
            verify(notificationService).create(notificationRequestCaptor.capture());
            assertThat(notificationRequestCaptor.getValue().getEmployeeId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("성공 - 이미 팀 채팅방에 초대")
        void inviteToRoom_Success_TeamRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            // 2. Room 조회 (팀 채팅방)
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom)); // 팀방
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(roomId, 1L))
                    .willReturn(true);
            given(employeeRepository.findAllById(new LinkedHashSet<>(inviteeIds))).willReturn(invitees);
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(roomId)).willReturn(Set.of(1L, 2L));
            // 6. 방 상태가 팀방이라고 명시
            when(mockTeamChatRoom.getIsTeam()).thenReturn(true);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    anyString(), anyString()
            )).willReturn(List.of(mockChatInviteCode));

            // when
            chatServiceImpl.inviteToRoom("creator", roomId, inviteeIds);

            // then
            // [FIXED]
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            syncCaptor.getValue().afterCommit();

            // 10. 팀방이므로 전환 메서드 호출 안 함
            verify(mockTeamChatRoom, never()).updateToTeamRoom();
            // 11. (검증 동일)
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(chatEmployeeRepository).saveAll(anyList());
            verify(simpMessagingTemplate).convertAndSend(anyString(), any(ChatMessageResponseDTO.class));
            verify(notificationService).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("실패 - 초대자가 멤버가 아님")
        void inviteToRoom_Fail_NotAMember() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockChatRoom));
            // 3. Inviter가 멤버가 아님
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(roomId, 1L))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.inviteToRoom("creator", roomId, inviteeIds))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 구성원이 아니면 초대할 수 없습니다.");
        }

        @Test
        @DisplayName("실패 - 초대 대상이 이미 멤버임")
        void inviteToRoom_Fail_AlreadyMember() {
            // given
            List<Long> alreadyMemberIds = List.of(2L); // 1L(Inviter), 2L(AlreadyMember)
            List<Employee> alreadyMembers = List.of(mockInvitee1);

            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockChatRoom));
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(roomId, 1L))
                    .willReturn(true);
            given(employeeRepository.findAllById(new LinkedHashSet<>(alreadyMemberIds))).willReturn(alreadyMembers);
            // 5. 2L은 이미 멤버임
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(roomId)).willReturn(Set.of(1L, 2L));

            // when
            chatServiceImpl.inviteToRoom("creator", roomId, alreadyMemberIds);

            // then
            // 예외가 발생하지 않고, newInvitees.isEmpty()로 인해 조용히 리턴됨
            verify(chatMessageRepository, never()).save(any());
            verify(chatEmployeeRepository, never()).saveAll(anyList());
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("leaveRoom (채팅방 나가기)")
    class LeaveRoom {

        private Long roomId = 200L; // 팀방
        private Long oneOnOneRoomId = 100L; // 1:1방

        @Test
        @DisplayName("성공 - 팀 채팅방 (시스템 메시지 전송)")
        void leaveRoom_Success_TeamRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom)); // 팀방
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, 1L))
                    .willReturn(Optional.of(mockChatEmployee));
            // 1. 방 상태가 팀방이라고 명시
            when(mockTeamChatRoom.getIsTeam()).thenReturn(true);
            // 2. 시스템 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            // 3. 남은 인원 1명
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(roomId)).willReturn(1L);

            // when
            chatServiceImpl.leaveRoom("creator", roomId);

            // then
            // [FIXED]
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            syncCaptor.getValue().afterCommit();

            // 4. 상태 변경 및 저장 검증
            verify(mockChatEmployee).leftChatRoom();
            verify(chatEmployeeRepository).save(mockChatEmployee);
            // 5. 시스템 메시지 저장 검증
            verify(chatMessageRepository).save(chatMessageCaptor.capture());
            assertThat(chatMessageCaptor.getValue().getContent()).contains("나가셨습니다.");
            // 6. 방 삭제 안 함
            verify(mockTeamChatRoom, never()).deleteRoom();
            // 7. WebSocket 전송 검증
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/" + roomId), any(ChatMessageResponseDTO.class));
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 (시스템 메시지 전송 안 함)")
        void leaveRoom_Success_OneOnOneRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(oneOnOneRoomId)).willReturn(Optional.of(mockChatRoom)); // 1:1방
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(oneOnOneRoomId, 1L))
                    .willReturn(Optional.of(mockChatEmployee));
            // 1. 방 상태가 1:1방이라고 명시
            when(mockChatRoom.getIsTeam()).thenReturn(false);
            // 3. 남은 인원 1명
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(oneOnOneRoomId)).willReturn(1L);

            // when
            chatServiceImpl.leaveRoom("creator", oneOnOneRoomId);

            // then
            // 4. 상태 변경 및 저장 검증
            verify(mockChatEmployee).leftChatRoom();
            verify(chatEmployeeRepository).save(mockChatEmployee);
            // 5. 시스템 메시지 저장 안 함
            verify(chatMessageRepository, never()).save(any());
            // 7. WebSocket 전송 안 함 (afterCommit 캡처 X)
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }

        @Test
        @DisplayName("성공 - 마지막 멤버가 나가서 방 삭제")
        void leaveRoom_Success_LastMemberDeletesRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, 1L))
                    .willReturn(Optional.of(mockChatEmployee));
            when(mockTeamChatRoom.getIsTeam()).thenReturn(true);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockSystemMessage);
            // 3. 남은 인원 0명
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(roomId)).willReturn(0L);

            // when
            chatServiceImpl.leaveRoom("creator", roomId);

            // then
            // [FIXED]
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            syncCaptor.getValue().afterCommit();

            // ... (기본 검증)
            verify(mockChatEmployee).leftChatRoom();
            verify(chatEmployeeRepository).save(mockChatEmployee);
            // 6. 방 삭제 로직 호출 검증
            verify(mockTeamChatRoom).deleteRoom();
            verify(chatRoomRepository).save(mockTeamChatRoom);
        }

        @Test
        @DisplayName("실패 - 멤버가 아님")
        void leaveRoom_Fail_NotAMember() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom));
            // 3. 멤버십 정보 없음
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, 1L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.leaveRoom("creator", roomId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("해당 채팅방의 멤버가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        private Long roomId = 200L; // 팀방
        private String content = "Hello";

        @Test
        @DisplayName("성공 - 팀 채팅방 (미읽음 수 계산)")
        void sendMessage_Success_TeamRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom)); // 팀방
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.of(mockChatEmployee));
            // 1. 메시지 저장
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockUserMessage);
            // 2. DTO용 isTeam
            when(mockTeamChatRoom.getIsTeam()).thenReturn(true);
            // 3. 미읽음 수 계산 (본인 제외 3명)
            given(chatEmployeeRepository.countUnreadForMessage(eq(roomId), eq(1L), any(LocalDateTime.class)))
                    .willReturn(3L);

            // [FIXED] SUT가 'continue' 로직을 통과하도록 발신자 외의 멤버(mockChatEmployee2)를 추가
            ChatEmployee mockChatEmployee2 = mock(ChatEmployee.class);
            Employee mockRecipient = mockInvitee1; // mockInvitee1 (ID=2L, Username=invitee1)
            lenient().when(mockChatEmployee2.getEmployee()).thenReturn(mockRecipient);
            lenient().when(mockRecipient.getEmployeeId()).thenReturn(2L); // SUT의 continue 로직용
            lenient().when(mockRecipient.getUsername()).thenReturn("invitee1"); // convertAndSendToUser 용
            lenient().when(mockChatEmployee2.getLastReadMessage()).thenReturn(mockSystemMessage); // NPE 방지
            lenient().when(mockChatEmployee2.getChatRoom()).thenReturn(mockTeamChatRoom); // NPE 방지
            lenient().when(mockChatEmployee2.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1)); // NPE 방지

            // 4. (Helper) 알림 보낼 멤버 목록 (발신자 + 수신자)
            given(chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(roomId))
                    .willReturn(List.of(mockChatEmployee, mockChatEmployee2));

            // [FIXED] eq() 안에 mock 객체 호출을 방지하기 위해 값을 미리 변수로 추출
            Long lastReadMsgId = mockSystemMessage.getChatMessageId();

            // 5. (Helper) 수신자의 안 읽은 개수 계산
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                    eq(mockTeamChatRoom),
                    eq(lastReadMsgId) // [FIXED]
            )).willReturn(1L); // 1개 안읽음

            // 6. (Helper) 수신자의 총 안 읽은 개수 계산
            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(2L)).willReturn(1L);


            // when
            ChatMessageResponseDTO result = chatServiceImpl.sendMessage("creator", roomId, content);

            // then
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
            syncCaptor.getValue().afterCommit();

            // 6. 본인 메시지 즉시 읽음 처리 검증
            verify(mockChatEmployee).updateLastReadMessage(mockUserMessage);
            verify(chatEmployeeRepository).save(mockChatEmployee);
            // 7. WebSocket 전송 검증
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/" + roomId), eq(result));

            // 8. (Helper) 채팅방 목록 업데이트 알림 전송 검증
            verify(simpMessagingTemplate).convertAndSendToUser(eq("invitee1"), eq("/queue/chat-list-update"), any(ChatRoomListResponseDTO.class));
            // 9. (Helper) 총 미읽음 수 전송 검증
            verify(chatEmployeeRepository).sumTotalUnreadMessagesByEmployeeId(2L); // 수신자 ID
            verify(simpMessagingTemplate).convertAndSendToUser(eq("invitee1"), eq("/queue/unread-count"), any());

            // 10. (Helper) 발신자('creator')에게는 전송되지 않았는지 확인 (SUT의 continue 로직 검증)
            verify(simpMessagingTemplate, never()).convertAndSendToUser(eq("creator"), anyString(), any());

            // 11. 반환 DTO 검증
            assertThat(result).isNotNull();
            assertThat(result.getChatMessageId()).isEqualTo(mockUserMessage.getChatMessageId());
            assertThat(result.getUnreadCount()).isEqualTo(3L); // SUT가 계산한 미읽음 수
        }

        @Test
        @DisplayName("실패 - 빈 메시지")
        void sendMessage_Fail_EmptyContent() {
            // given
            String emptyContent = " ";

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.sendMessage("creator", roomId, emptyContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 내용이 비어 있습니다.");
        }

        @Test
        @DisplayName("실패 - 멤버가 아님")
        void sendMessage_Fail_NotAMember() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(mockTeamChatRoom));
            // 1. 멤버십 없음
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.sendMessage("creator", roomId, content))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {

        private Long roomId = 100L;
        private Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("성공 - 메시지 목록 조회")
        void getMessages_Success() {
            // given
            // 1. 멤버십 조회 (JoinedAt을 얻기 위함)
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.of(mockChatEmployee));
            // 2. 메시지 목록(Page) 조회
            Page<ChatMessage> mockPage = new PageImpl<>(List.of(mockUserMessage, mockSystemMessage), pageable, 2);

            // [FIXED] eq() 안에 mock 객체 호출을 방지하기 위해 값을 미리 변수로 추출
            LocalDateTime joinedAt = mockChatEmployee.getJoinedAt();

            // [CORRECT] 모든 인자를 eq() 매처로 래핑
            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(roomId),
                    eq(joinedAt), // [FIXED]
                    eq(pageable)
            )).willReturn(mockPage);

            // when
            Page<ChatMessageResponseDTO> result = chatServiceImpl.getMessages("creator", roomId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getContent()).isEqualTo(mockUserMessage.getContent());
        }

        @Test
        @DisplayName("실패 - 멤버가 아님")
        void getMessages_Fail_NotAMember() {
            // given
            // 1. 멤버십 없음
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.getMessages("creator", roomId, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (마지막 읽은 메시지 ID 업데이트)")
    class UpdateLastReadMessageId {

        private Long roomId = 100L;
        private Long lastMessageId = 1002L; // mockUserMessage ID

        @Test
        @DisplayName("성공")
        void updateLastReadMessageId_Success() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.of(mockChatEmployee));
            // 1. 메시지 조회
            given(chatMessageRepository.findById(lastMessageId)).willReturn(Optional.of(mockUserMessage));
            // 2. 메시지 방 ID 검증 (일치)
            when(mockUserMessage.getChatRoom()).thenReturn(mockChatRoom); // mockChatRoom ID는 100L
            when(mockChatRoom.getChatRoomId()).thenReturn(roomId);
            // 3. (Helper) 총 미읽음 수 계산
            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(1L)).willReturn(0L);

            // when
            chatServiceImpl.updateLastReadMessageId("creator", roomId, lastMessageId);

            // then

            // [FIXED] 1. verify()를 호출하여 syncCaptor가 인수를 캡처하도록 합니다.
            mockedTxSyncManager.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

            // [FIXED] 2. 캡처가 완료된 후, .getValue()를 호출하여 afterCommit()을 수동 실행합니다.
            syncCaptor.getValue().afterCommit();

            // 4. 멤버십에 마지막 읽은 메시지 업데이트 및 저장 검증
            verify(mockChatEmployee).updateLastReadMessage(mockUserMessage);
            verify(chatEmployeeRepository).save(mockChatEmployee);
            // 5. WebSocket 읽음 브로드캐스트 검증
            verify(simpMessagingTemplate).convertAndSend(
                    eq("/topic/chat/rooms/" + roomId + "/reads"),
                    any(Map.class)
            );
            // 6. (Helper) 총 미읽음 수 전송 검증
            verify(chatEmployeeRepository).sumTotalUnreadMessagesByEmployeeId(1L);
            verify(simpMessagingTemplate).convertAndSendToUser(eq("creator"), eq("/queue/unread-count"), any());
        }

        @Test
        @DisplayName("실패 - 메시지가 다른 채팅방 소속")
        void updateLastReadMessageId_Fail_MessageInDifferentRoom() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(roomId, "creator"))
                    .willReturn(Optional.of(mockChatEmployee));
            given(chatMessageRepository.findById(lastMessageId)).willReturn(Optional.of(mockUserMessage));

            // 2. 메시지 방 ID 검증 (불일치)
            ChatRoom differentRoom = mock(ChatRoom.class);
            when(differentRoom.getChatRoomId()).thenReturn(999L);
            when(mockUserMessage.getChatRoom()).thenReturn(differentRoom);

            // when & then
            assertThatThrownBy(() -> chatServiceImpl.updateLastReadMessageId("creator", roomId, lastMessageId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 채팅방의 메시지가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("findRoomsByUsername (채팅방 목록 조회)")
    class FindRoomsByUsername {

        @Test
        @DisplayName("성공 - 1:1방과 팀방 목록 조회")
        void findRoomsByUsername_Success_MixOfRooms() {
            // given
            // --- User ---
            Employee user = mockCreator;
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(user));

            // --- Room 1 (1:1) ---
            ChatEmployee membership1 = mockChatEmployee; // user의 1:1방 멤버십 (setUp에서 생성됨)
            ChatRoom room1 = mockChatRoom; // 1:1방 (setUp)
            Employee otherUser = mockInvitee1; // 1:1방 상대 (setUp)
            ChatEmployee otherMembership = mock(ChatEmployee.class); // 상대방 멤버십
            ChatMessage lastMsg1 = mockUserMessage; // 마지막 메시지 (setUp)

            given(membership1.getChatRoom()).willReturn(room1);
            when(room1.getIsTeam()).thenReturn(false);
            // 마지막 메시지 조회
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(room1, membership1.getJoinedAt()))
                    .willReturn(Optional.of(lastMsg1));
            // 안 읽은 수 (1:1)
            given(membership1.getLastReadMessage()).willReturn(mockSystemMessage);
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(room1, mockSystemMessage.getChatMessageId()))
                    .willReturn(1L); // 1개 안읽음
            // 1:1 상대방 찾기
            given(chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId()))
                    .willReturn(List.of(membership1, otherMembership));
            when(otherMembership.getEmployee()).thenReturn(otherUser);

            // --- Room 2 (Team) ---
            ChatEmployee membership2 = mock(ChatEmployee.class);
            ChatRoom room2 = mockTeamChatRoom; // 팀방 (setUp)
            ChatMessage lastMsg2 = mock(ChatMessage.class);

            given(membership2.getChatRoom()).willReturn(room2);
            when(room2.getIsTeam()).thenReturn(true);
            when(membership2.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));
            // 마지막 메시지
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(room2, membership2.getJoinedAt()))
                    .willReturn(Optional.of(lastMsg2));
            // 안 읽은 수 (팀)
            given(membership2.getLastReadMessage()).willReturn(lastMsg2); // 다 읽음
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(room2, lastMsg2.getChatMessageId()))
                    .willReturn(0L);

            // --- SUT 진입점 ---
            // 사용자가 속한 모든 멤버십 조회
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user))
                    .willReturn(List.of(membership1, membership2));

            // DTO.toDTO에서 사용할 Getter (lenient로 대부분 커버되지만 확실하게)
            when(lastMsg1.getContent()).thenReturn("1:1 Message");
            when(lastMsg2.getContent()).thenReturn("Team Message");

            // when
            List<ChatRoomListResponseDTO> result = chatServiceImpl.findRoomsByUsername("creator");

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            // (참고: SUT는 DTO 생성 후 마지막 메시지 시각으로 정렬함. 여기서는 메시지 내용으로 구분)
            Optional<ChatRoomListResponseDTO> room1Dto = result.stream().filter(r -> r.getName().equals(otherUser.getName())).findFirst();
            Optional<ChatRoomListResponseDTO> room2Dto = result.stream().filter(r -> r.getName().equals(room2.getName())).findFirst();

            assertThat(room1Dto).isPresent();
            assertThat(room1Dto.get().getChatRoomId()).isEqualTo(room1.getChatRoomId());
            assertThat(room1Dto.get().getName()).isEqualTo(otherUser.getName()); // 상대방 이름
            assertThat(room1Dto.get().getProfile()).isEqualTo(otherUser.getProfileImg()); // 상대방 프로필
            assertThat(room1Dto.get().getUnreadCount()).isEqualTo(1L);
            assertThat(room1Dto.get().getLastMessage()).isEqualTo(lastMsg1.getContent());

            assertThat(room2Dto).isPresent();
            assertThat(room2Dto.get().getChatRoomId()).isEqualTo(room2.getChatRoomId());
            assertThat(room2Dto.get().getName()).isEqualTo(room2.getName()); // 팀방 이름
            assertThat(room2Dto.get().getProfile()).isNull(); // 팀방 프로필
            assertThat(room2Dto.get().getUnreadCount()).isEqualTo(0L);
            assertThat(room2Dto.get().getLastMessage()).isEqualTo(lastMsg2.getContent());
        }
    }
}