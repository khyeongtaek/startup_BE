package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;

// Mockito static import
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
// BDDMockito static import
import static org.mockito.BDDMockito.given;
// Mockito static import
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl 테스트")
class ChatServiceImplTest {

    @InjectMocks
    private ChatServiceImpl chatService;

    // --- Repositories ---
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

    // --- Services & Utils ---
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    // --- Mock Objects ---
    private Employee mockCreator;
    private Employee mockInvitee1;
    private Employee mockInvitee2;
    private ChatRoom mockRoom;
    private ChatMessage mockMessage;
    private ChatEmployee mockChatEmployee;
    private CommonCode mockCommonCode;

    // [FIX] 'WantedButNotInvoked' 오류 수정을 위해 mockChatEmployeeInvitee1 추가
    private ChatEmployee mockChatEmployeeInvitee1;

    // --- Argument Captors ---
    @Captor
    private ArgumentCaptor<TransactionSynchronization> syncCaptor;
    @Captor
    private ArgumentCaptor<ChatMessage> chatMessageCaptor;
    @Captor
    private ArgumentCaptor<NotificationRequestDTO> notificationCaptor;
    @Captor
    private ArgumentCaptor<List<ChatEmployee>> chatEmployeeListCaptor;

    @BeforeEach
    void setUp() {
        mockCreator = mock(Employee.class);
        mockInvitee1 = mock(Employee.class);
        mockInvitee2 = mock(Employee.class);
        mockRoom = mock(ChatRoom.class);
        mockMessage = mock(ChatMessage.class);
        mockChatEmployee = mock(ChatEmployee.class);
        mockCommonCode = mock(CommonCode.class);
        mockChatEmployeeInvitee1 = mock(ChatEmployee.class); // [FIX] mock 객체 초기화

        // 'when' 충돌을 막기 위해 Mockito.when() 명시적 호출
        Mockito.lenient().when(mockCreator.getEmployeeId()).thenReturn(1L);
        Mockito.lenient().when(mockCreator.getUsername()).thenReturn("creatorUser");
        Mockito.lenient().when(mockCreator.getName()).thenReturn("김생성");

        Mockito.lenient().when(mockInvitee1.getEmployeeId()).thenReturn(2L);
        Mockito.lenient().when(mockInvitee1.getUsername()).thenReturn("inviteeUser1");
        Mockito.lenient().when(mockInvitee1.getName()).thenReturn("이초대");

        Mockito.lenient().when(mockInvitee2.getEmployeeId()).thenReturn(3L);
        Mockito.lenient().when(mockInvitee2.getUsername()).thenReturn("inviteeUser2");
        Mockito.lenient().when(mockInvitee2.getName()).thenReturn("박초대");

        Mockito.lenient().when(mockRoom.getChatRoomId()).thenReturn(100L);
        Mockito.lenient().when(mockRoom.getName()).thenReturn("테스트 채팅방");

        Mockito.lenient().when(mockMessage.getChatMessageId()).thenReturn(1000L);
        Mockito.lenient().when(mockMessage.getChatRoom()).thenReturn(mockRoom);
        Mockito.lenient().when(mockMessage.getEmployee()).thenReturn(mockCreator);
        Mockito.lenient().when(mockMessage.getContent()).thenReturn("테스트 메시지");
        Mockito.lenient().when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());

        Mockito.lenient().when(mockChatEmployee.getEmployee()).thenReturn(mockCreator);
        Mockito.lenient().when(mockChatEmployee.getChatRoom()).thenReturn(mockRoom);
        Mockito.lenient().when(mockChatEmployee.getLastReadMessage()).thenReturn(mockMessage);
        Mockito.lenient().when(mockChatEmployee.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));

        // [FIX] mockChatEmployeeInvitee1의 기본 stubbing
        Mockito.lenient().when(mockChatEmployeeInvitee1.getEmployee()).thenReturn(mockInvitee1);
        Mockito.lenient().when(mockChatEmployeeInvitee1.getLastReadMessage()).thenReturn(mockMessage);


        Mockito.lenient().when(mockCommonCode.getCommonCodeId()).thenReturn(99L);
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        @Test
        @DisplayName("성공 - 1:1 채팅방 (초대 1명)")
        void createRoom_Success_1to1() throws Exception {
            // given (@Test 내부: given/willReturn)
            String creatorUsername = "creatorUser";
            String roomName = "1:1 채팅방";
            List<Long> inviteeIds = List.of(2L);
            List<Employee> invitees = List.of(mockInvitee1);

            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(anySet())).willReturn(invitees);
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(mockRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);

            ChatRoomResponseDTO expectedDto = ChatRoomResponseDTO.builder().chatRoomId(100L).build();
            ChatMessageResponseDTO messageDto = ChatMessageResponseDTO.builder().build();

            // when
            try (MockedStatic<ChatRoom> roomMock = mockStatic(ChatRoom.class);
                 MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatEmployee> empMock = mockStatic(ChatEmployee.class);
                 MockedStatic<ChatRoomResponseDTO> roomDtoMock = mockStatic(ChatRoomResponseDTO.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                roomDtoMock.when(ChatRoomResponseDTO::builder).thenCallRealMethod();
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Factory Mocking (실제 메서드 호출)
                roomMock.when(() -> ChatRoom.createChatRoom(mockCreator, roomName, false)).thenReturn(mockRoom);
                msgMock.when(() -> ChatMessage.createChatMessage(any(), any(), anyString())).thenCallRealMethod();
                empMock.when(() -> ChatEmployee.createChatEmployee(any(Employee.class), eq(mockRoom), eq(roomName), eq(mockMessage)))
                        .thenReturn(mockChatEmployee);

                // DTO Mapper Mocking
                roomDtoMock.when(() -> ChatRoomResponseDTO.toDTO(mockRoom)).thenReturn(expectedDto);
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(messageDto);

                // TransactionManager Mocking
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                ChatRoomResponseDTO result = chatService.createRoom(creatorUsername, roomName, inviteeIds);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                assertThat(result).isSameAs(expectedDto);
                verify(chatRoomRepository).save(mockRoom);
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo("김생성님이 채팅방을 생성 했습니다.");
                verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());
                assertThat(chatEmployeeListCaptor.getValue()).hasSize(2);
                verify(simpMessagingTemplate).convertAndSend("/topic/chat/rooms/100", messageDto);
                verify(notificationService, never()).create(any(NotificationRequestDTO.class));
            }
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 (초대 2명) 및 알림 전송")
        void createRoom_Success_TeamChat() throws Exception {
            // given
            String creatorUsername = "creatorUser";
            String roomName = "팀 채팅방";
            List<Long> inviteeIds = List.of(2L, 3L);
            List<Employee> invitees = List.of(mockInvitee1, mockInvitee2);

            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(anySet())).willReturn(invitees);
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(mockRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.TEAMCHATNOTI.name()))
                    .willReturn(List.of(mockCommonCode));

            ChatRoomResponseDTO expectedDto = ChatRoomResponseDTO.builder().build();
            ChatMessageResponseDTO messageDto = ChatMessageResponseDTO.builder().build();

            // when
            try (MockedStatic<ChatRoom> roomMock = mockStatic(ChatRoom.class);
                 MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatEmployee> empMock = mockStatic(ChatEmployee.class);
                 MockedStatic<ChatRoomResponseDTO> roomDtoMock = mockStatic(ChatRoomResponseDTO.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                roomDtoMock.when(ChatRoomResponseDTO::builder).thenCallRealMethod();
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Mocks (실제 메서드 호출)
                roomMock.when(() -> ChatRoom.createChatRoom(mockCreator, roomName, true)).thenReturn(mockRoom);
                msgMock.when(() -> ChatMessage.createChatMessage(any(), any(), anyString())).thenCallRealMethod();
                empMock.when(() -> ChatEmployee.createChatEmployee(any(), any(), any(), any())).thenReturn(mockChatEmployee);
                roomDtoMock.when(() -> ChatRoomResponseDTO.toDTO(mockRoom)).thenReturn(expectedDto);
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(messageDto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                chatService.createRoom(creatorUsername, roomName, inviteeIds);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());
                assertThat(chatEmployeeListCaptor.getValue()).hasSize(3);
                verify(notificationService, times(2)).create(notificationCaptor.capture());
                List<NotificationRequestDTO> notifications = notificationCaptor.getAllValues();
                assertThat(notifications.get(0).getEmployeeId()).isEqualTo(2L);
                assertThat(notifications.get(1).getEmployeeId()).isEqualTo(3L);
            }
        }

        // ... (실패 케이스는 동일)
        @Test
        @DisplayName("실패 - 초대 대상 없음 (IllegalArgumentException)")
        void createRoom_Fail_NoInvitees() {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creatorUser", "방이름", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("최소 한 명 이상 초대해야 합니다");
        }

        @Test
        @DisplayName("실패 - 자기 자신 초대 (IllegalArgumentException)")
        void createRoom_Fail_InvitingSelf() {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creatorUser", "방이름", List.of(1L, 2L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("초대할 수 있는 대상이 아닙니다");
        }

        @Test
        @DisplayName("실패 - 생성자 없음 (UsernameNotFoundException)")
        void createRoom_Fail_CreatorNotFound() {
            // given
            given(employeeRepository.findByUsername("unknownUser")).willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.createRoom("unknownUser", "방이름", List.of(2L)))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("존재 하지 않은 사원 입니다");
        }

        @Test
        @DisplayName("실패 - 초대 대상 중 존재하지 않는 사원 (EntityNotFoundException)")
        void createRoom_Fail_InviteeNotFound() {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(anySet())).willReturn(List.of(mockInvitee1)); // 1명만 반환
            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creatorUser", "방이름", List.of(2L, 4L))) // 2명 요청
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoom {

        @BeforeEach
        void inviteSetup() {
            // @BeforeEach: Mockito.lenient().when().thenReturn()
            Mockito.lenient().when(mockRoom.getIsTeam()).thenReturn(false);
            Mockito.lenient().when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(mockRoom));
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L))
                    .thenReturn(true);
        }

        @Test
        @DisplayName("성공 - 1:1방에서 초대하여 팀 채팅방으로 전환")
        void inviteToRoom_Success_ConvertToTeam() throws Exception {
            // given
            List<Long> inviteeIds = List.of(2L);
            List<Employee> candidates = List.of(mockInvitee1);

            // 'argThat' 람다 수정 (Iterable -> Set 변환)
            given(employeeRepository.findAllById(argThat(s -> {
                Set<Long> set = new HashSet<>();
                s.forEach(set::add);
                return set.size() == 1 && set.contains(2L);
            }))).willReturn(candidates);

            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(100L)).willReturn(Set.of(1L));
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(anyString(), anyString()))
                    .willReturn(List.of(mockCommonCode));

            ChatMessageResponseDTO messageDto = ChatMessageResponseDTO.builder().build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatEmployee> empMock = mockStatic(ChatEmployee.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Mocks (실제 메서드 호출)
                msgMock.when(() -> ChatMessage.createChatMessage(any(), any(), anyString())).thenCallRealMethod();
                empMock.when(() -> ChatEmployee.createChatEmployee(mockInvitee1, mockRoom, "테스트 채팅방", mockMessage)).thenReturn(mockChatEmployee);
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(messageDto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                chatService.inviteToRoom("creatorUser", 100L, inviteeIds);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                verify(mockRoom).updateToTeamRoom();
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo("김생성님이 이초대님을 초대했습니다.");
                verify(chatEmployeeRepository).saveAll(chatEmployeeListCaptor.capture());
                assertThat(chatEmployeeListCaptor.getValue()).hasSize(1);
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
                verify(notificationService).create(notificationCaptor.capture());
                assertThat(notificationCaptor.getValue().getEmployeeId()).isEqualTo(2L);
            }
        }

        // ... (실패 케이스는 동일)
        @Test
        @DisplayName("실패 - 초대자가 멤버가 아님 (AccessDeniedException)")
        void inviteToRoom_Fail_NotAMember() {
            // given
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(100L, 1L))
                    .willReturn(false);
            // when & then
            assertThatThrownBy(() -> chatService.inviteToRoom("creatorUser", 100L, List.of(2L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 구성원이 아니면 초대할 수 없습니다.");
        }

        @Test
        @DisplayName("실패 - 이미 멤버인 인원만 초대 (아무 일도 없음)")
        void inviteToRoom_Fail_AlreadyMember() {
            // given
            List<Long> inviteeIds = List.of(2L);
            List<Employee> candidates = List.of(mockInvitee1);

            given(employeeRepository.findAllById(anySet())).willReturn(candidates);
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(100L)).willReturn(Set.of(1L, 2L)); // Set으로 반환
            // when
            chatService.inviteToRoom("creatorUser", 100L, inviteeIds);
            // then
            verify(chatEmployeeRepository, never()).saveAll(anyList());
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
            verify(notificationService, never()).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 채팅방 (EntityNotFoundException)")
        void inviteToRoom_Fail_RoomNotFound() {
            // given
            given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.inviteToRoom("creatorUser", 999L, List.of(2L)))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("존재하지 않는 채팅방입니다.");
        }
    }

    @Nested
    @DisplayName("leaveRoom (채팅방 나가기)")
    class LeaveRoom {

        @BeforeEach
        void leaveSetup() {
            // @BeforeEach: Mockito.lenient().when().thenReturn()
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(mockRoom));
            Mockito.lenient().when(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .thenReturn(Optional.of(mockChatEmployee));
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 나가기 (시스템 메시지 전송)")
        void leaveRoom_Success_TeamChat() throws Exception {
            // given
            given(mockRoom.getIsTeam()).willReturn(true);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(2L);

            ChatMessageResponseDTO messageDto = ChatMessageResponseDTO.builder().build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Mocks (실제 메서드 호출)
                msgMock.when(() -> ChatMessage.createChatMessage(any(), isNull(), anyString())).thenCallRealMethod();
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(messageDto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                chatService.leaveRoom("creatorUser", 100L);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                verify(mockChatEmployee).leftChatRoom();
                verify(chatEmployeeRepository).save(mockChatEmployee);
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo("김생성님이 채팅방에서 나가셨습니다.");
                verify(mockRoom, never()).deleteRoom();
                verify(simpMessagingTemplate).convertAndSend(eq("/topic/chat/rooms/100"), any(ChatMessageResponseDTO.class));
            }
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 나가기 (시스템 메시지 전송 안 함)")
        void leaveRoom_Success_1to1() throws Exception {
            // given
            given(mockRoom.getIsTeam()).willReturn(false);
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(1L);
            // when
            chatService.leaveRoom("creatorUser", 100L);
            // then
            verify(mockChatEmployee).leftChatRoom();
            verify(chatEmployeeRepository).save(mockChatEmployee);
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

            // [FIX] 멍청한 실수 (incompatible types) 수정: static verify 문법 수정
            // (Verification, VerificationMode) 순서
            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)), never());
            }
        }

        // ... (실패 케이스는 동일)
        @Test
        @DisplayName("성공 - 마지막 인원 나가기 (채팅방 삭제)")
        void leaveRoom_Success_LastMemberDeletesRoom() {
            // given
            given(mockRoom.getIsTeam()).willReturn(true);
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(100L)).willReturn(0L);
            // when
            chatService.leaveRoom("creatorUser", 100L);
            // then
            verify(mockChatEmployee).leftChatRoom();
            verify(mockRoom).deleteRoom();
            verify(chatRoomRepository).save(mockRoom);
        }

        @Test
        @DisplayName("실패 - 채팅방 멤버 아님 (EntityNotFoundException)")
        void leaveRoom_Fail_NotAMember() {
            // given
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.leaveRoom("creatorUser", 100L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("해당 채팅방의 멤버가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        @BeforeEach
        void sendSetup() {
            // @BeforeEach: Mockito.lenient().when().thenReturn()
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(mockRoom));
            Mockito.lenient().when(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .thenReturn(Optional.of(mockChatEmployee));
            Mockito.lenient().when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 메시지 전송 (미읽음 카운트 계산)")
        void sendMessage_Success_TeamChat() throws Exception {
            // given
            String content = "안녕하세요";
            given(mockRoom.getIsTeam()).willReturn(true);
            given(chatEmployeeRepository.countUnreadForMessage(100L, 1L, mockMessage.getCreatedAt())).willReturn(2L);

            ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder().unreadCount(0).build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Mocks (실제 메서드 호출)
                msgMock.when(() -> ChatMessage.createChatMessage(any(ChatRoom.class), any(Employee.class), anyString())).thenCallRealMethod();
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(dto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                ChatMessageResponseDTO result = chatService.sendMessage("creatorUser", 100L, content);
                // --- service call ---

                // then (before commit)
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo(content);
                verify(mockChatEmployee).updateLastReadMessage(mockMessage);
                verify(chatEmployeeRepository).save(mockChatEmployee);
                assertThat(result.getUnreadCount()).isEqualTo(2L);

                // --- afterCommit 강제 실행 (notifyParticipantsOfNewMessage 호출) ---

                // [FIX] 멍청한 실수 (WantedButNotInvoked) 수정:
                // 참여자 목록에 '보낸 사람'과 '다른 사람'을 모두 포함
                given(chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(100L))
                        .willReturn(List.of(mockChatEmployee, mockChatEmployeeInvitee1)); // 2명
                given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(any(ChatRoom.class), anyLong())).willReturn(1L);

                // DTO를 밖에서 미리 생성
                ChatRoomListResponseDTO mockListDtoTeam = ChatRoomListResponseDTO.builder().build();
                ChatRoomListResponseDTO mockListDto1to1 = ChatRoomListResponseDTO.builder().build();

                try (MockedStatic<ChatRoomListResponseDTO> listDtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                    listDtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();
                    // 오버로딩된 2개 메서드 모두 stubbing
                    listDtoMock.when(() -> ChatRoomListResponseDTO.toDTO(any(ChatRoom.class), anyLong(), any(Optional.class)))
                            .thenReturn(mockListDtoTeam);
                    listDtoMock.when(() -> ChatRoomListResponseDTO.toDTO(any(ChatRoom.class), any(Employee.class), anyLong(), any(Optional.class)))
                            .thenReturn(mockListDto1to1);

                    syncCaptor.getValue().afterCommit();
                }

                // then (after commit)
                // [FIX] 멍청한 실수 (WantedButNotInvoked) 수정:
                // 1. /topic/chat/rooms/100 (공통) 에는 1번 전송됨
                verify(simpMessagingTemplate, times(1)).convertAndSend("/topic/chat/rooms/100", result);

                // 2. "다른 사람"("inviteeUser1")에게는 알림(unread-count)이 전송됨
                verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                        eq("inviteeUser1"),
                        eq("/queue/unread-count"),
                        any(TotalUnreadCountResponseDTO.class)
                );

                // 3. "보낸 사람"("creatorUser")에게는 알림이 전송되지 *않음* (서비스 로직)
                verify(simpMessagingTemplate, never()).convertAndSendToUser(
                        eq("creatorUser"),
                        eq("/queue/unread-count"),
                        any(TotalUnreadCountResponseDTO.class)
                );
            }
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 메시지 전송 (미읽음 카운트 0)")
        void sendMessage_Success_1to1() throws Exception {
            // given
            String content = "안녕하세요";
            given(mockRoom.getIsTeam()).willReturn(false);

            ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder().unreadCount(0).build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                // Static Mocking (NPE 방지)
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();

                // Static Mocks (실제 메서드 호출)
                msgMock.when(() -> ChatMessage.createChatMessage(any(ChatRoom.class), any(Employee.class), anyString())).thenCallRealMethod();
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(dto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                ChatMessageResponseDTO result = chatService.sendMessage("creatorUser", 100L, content);
                // --- service call ---

                // then
                verify(chatEmployeeRepository, never()).countUnreadForMessage(anyLong(), anyLong(), any(LocalDateTime.class));
                assertThat(result.getUnreadCount()).isEqualTo(0L);
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo(content);
            }
        }

        // ... (실패 케이스는 동일)
        @Test
        @DisplayName("실패 - 빈 메시지 (IllegalArgumentException)")
        void sendMessage_Fail_EmptyContent() {
            // when & then
            assertThatThrownBy(() -> chatService.sendMessage("creatorUser", 100L, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 내용이 비어 있습니다.");
        }

        @Test
        @DisplayName("실패 - 멤버가 아님 (AccessDeniedException)")
        void sendMessage_Fail_NotAMember() {
            // given
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.sendMessage("creatorUser", 100L, "내용"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {

        @Test
        @DisplayName("성공 - 메시지 페이지 조회")
        void getMessages_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);

            given(mockChatEmployee.getJoinedAt()).willReturn(joinedAt);
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.of(mockChatEmployee));

            List<ChatMessage> messages = List.of(mockMessage);
            Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 1);

            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                    100L, joinedAt, pageable
            )).willReturn(messagePage);

            ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder().chatMessageId(1000L).build();

            // when
            try (MockedStatic<ChatMessageResponseDTO> dtoMock = mockStatic(ChatMessageResponseDTO.class)) {
                // Static Mocking (NPE 방지)
                dtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();
                dtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(dto);

                // --- service call ---
                Page<ChatMessageResponseDTO> result = chatService.getMessages("creatorUser", 100L, pageable);
                // --- service call ---

                // then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent().get(0).getChatMessageId()).isEqualTo(1000L);
            }
        }

        @Test
        @DisplayName("실패 - 멤버가 아님 (AccessDeniedException)")
        void getMessages_Fail_NotAMember() {
            // given
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.getMessages("creatorUser", 100L, PageRequest.of(0, 10)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (마지막 읽은 메시지 업데이트)")
    class UpdateLastReadMessageId {

        @BeforeEach
        void updateReadSetup() {
            // @BeforeEach: Mockito.lenient().when().thenReturn()
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .thenReturn(Optional.of(mockChatEmployee));
            Mockito.lenient().when(chatMessageRepository.findById(1000L)).thenReturn(Optional.of(mockMessage));
            Mockito.lenient().when(mockMessage.getChatRoom()).thenReturn(mockRoom);
            Mockito.lenient().when(mockRoom.getChatRoomId()).thenReturn(100L);
        }

        @Test
        @DisplayName("성공 - 읽음 처리 및 브로드캐스트")
        void updateLastReadMessageId_Success() throws Exception {
            // given
            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(1L)).willReturn(5L);

            // when
            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                // Static Mock
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                chatService.updateLastReadMessageId("creatorUser", 100L, 1000L);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                verify(mockChatEmployee).updateLastReadMessage(mockMessage);
                verify(chatEmployeeRepository).save(mockChatEmployee);
                verify(simpMessagingTemplate).convertAndSend(
                        eq("/topic/chat/rooms/100/reads"),
                        eq(Map.of("lastMessageId", 1L, "readUpToMessageId", 1000L))
                );
                verify(chatEmployeeRepository).sumTotalUnreadMessagesByEmployeeId(1L);
                verify(simpMessagingTemplate).convertAndSendToUser(
                        eq("creatorUser"),
                        eq("/queue/unread-count"),
                        any(TotalUnreadCountResponseDTO.class)
                );
            }
        }

        // ... (실패 케이스는 동일)
        @Test
        @DisplayName("실패 - 메시지 ID가 null (IllegalArgumentException)")
        void updateLastReadMessageId_Fail_NullId() {
            // when & then
            assertThatThrownBy(() -> chatService.updateLastReadMessageId("creatorUser", 100L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lastMessageId는 필수입니다.");
        }

        @Test
        @DisplayName("실패 - 다른 채팅방의 메시지 (AccessDeniedException)")
        void updateLastReadMessageId_Fail_WrongRoom() {
            // given
            ChatRoom otherRoom = mock(ChatRoom.class);
            given(otherRoom.getChatRoomId()).willReturn(999L);
            given(mockMessage.getChatRoom()).willReturn(otherRoom); // setup 덮어쓰기
            // when & then
            assertThatThrownBy(() -> chatService.updateLastReadMessageId("creatorUser", 100L, 1000L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 채팅방의 메시지가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("findRoomsByUsername (채팅방 목록 조회)")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class FindRoomsByUsername {

        @Test
        @DisplayName("성공 - 1:1방, 팀방 목록 및 정렬(메시지 내용 역순) 조회")
        void findRoomsByUsername_Success() throws Exception {
            // given
            ChatRoom room1 = mock(ChatRoom.class);
            ChatRoom room2 = mock(ChatRoom.class);
            given(room1.getChatRoomId()).willReturn(101L);
            given(room1.getIsTeam()).willReturn(false); // 1:1
            given(room2.getChatRoomId()).willReturn(102L);
            given(room2.getIsTeam()).willReturn(true); // 팀

            ChatEmployee membership1 = mock(ChatEmployee.class);
            ChatEmployee membership2 = mock(ChatEmployee.class);
            given(membership1.getChatRoom()).willReturn(room1);
            given(membership2.getChatRoom()).willReturn(room2);
            given(membership1.getLastReadMessage()).willReturn(mock(ChatMessage.class));
            given(membership2.getLastReadMessage()).willReturn(mock(ChatMessage.class));

            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(mockCreator))
                    .willReturn(List.of(membership1, membership2));

            ChatEmployee otherMembership = mock(ChatEmployee.class);
            given(otherMembership.getEmployee()).willReturn(mockInvitee1);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(101L))
                    .willReturn(List.of(membership1, otherMembership));

            // [FIX] 1: 'lenient().given()' -> 'lenient().when()'으로 수정
            // 이 stub은 1:1 채팅방의 'otherUser'를 찾는 스트림 필터에서만 사용될 수 있음
            lenient().when(membership1.getEmployee()).thenReturn(mockCreator);

            ChatMessage msg1 = mock(ChatMessage.class);
            given(msg1.getContent()).willReturn("Apple");
            ChatMessage msg2 = mock(ChatMessage.class);
            given(msg2.getContent()).willReturn("Zebra");
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(room1), any()))
                    .willReturn(Optional.of(msg1));
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(room2), any()))
                    .willReturn(Optional.of(msg2));

            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(eq(room1), anyLong())).willReturn(1L);
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(eq(room2), anyLong())).willReturn(2L);

            ChatRoomListResponseDTO dto1 = ChatRoomListResponseDTO.builder().chatRoomId(101L).lastMessage("Apple").build();
            ChatRoomListResponseDTO dto2 = ChatRoomListResponseDTO.builder().chatRoomId(102L).lastMessage("Zebra").build();

            // when
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                // Static Mocking (NPE 방지)
                dtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();

                // [FIX] 2 & 3: 'dtoMock.lenient().when()' -> 'dtoMock.when()'으로 수정
                // MockedStatic 객체는 .lenient() 메서드를 지원하지 않습니다.
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(room1, mockInvitee1, 1L, Optional.of(msg1)))
                        .thenReturn(dto1);
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(room2, 2L, Optional.of(msg2)))
                        .thenReturn(dto2);

                // --- service call ---
                List<ChatRoomListResponseDTO> result = chatService.findRoomsByUsername("creatorUser");
                // --- service call ---

                // then
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getChatRoomId()).isEqualTo(102L); // Zebra
                assertThat(result.get(1).getChatRoomId()).isEqualTo(101L); // Apple
            }
        }
    }

    @Nested
    @DisplayName("getRoomById (ID로 채팅방 조회)")
    class GetRoomById {

        @Test
        @DisplayName("성공 - ID로 팀 채팅방 정보 조회")
        void getRoomById_Success_TeamRoom() throws Exception {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.of(mockChatEmployee));
            given(mockChatEmployee.getChatRoom()).willReturn(mockRoom);
            given(mockRoom.getIsTeam()).willReturn(true);

            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(mockRoom), any()))
                    .willReturn(Optional.of(mockMessage));
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(eq(mockRoom), anyLong())).willReturn(3L);

            ChatRoomListResponseDTO expectedDto = ChatRoomListResponseDTO.builder()
                    .chatRoomId(100L)
                    .name("테스트 채팅방")
                    .unreadCount(3L)
                    .build();

            // when
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                // Static Mocking (NPE 방지)
                dtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(mockRoom, 3L, Optional.of(mockMessage)))
                        .thenReturn(expectedDto);

                // --- service call ---
                ChatRoomListResponseDTO result = chatService.getRoomById("creatorUser", 100L);
                // --- service call ---

                // then
                assertThat(result).isSameAs(expectedDto);
                assertThat(result.getUnreadCount()).isEqualTo(3L);
                assertThat(result.getName()).isEqualTo("테스트 채팅방");
            }
        }

        @Test
        @DisplayName("실패 - 멤버가 아님 (AccessDeniedException)")
        void getRoomById_Fail_NotAMember() {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.empty());
            // when & then
            assertThatThrownBy(() -> chatService.getRoomById("creatorUser", 100L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }
}