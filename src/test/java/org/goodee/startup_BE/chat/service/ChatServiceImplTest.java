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

    // [신규] updateLastReadMessageId 테스트용 캡터
    @Captor
    private ArgumentCaptor<Map<String, Object>> unreadUpdateCaptor;


    @BeforeEach
    void setUp() {
        mockCreator = mock(Employee.class);
        mockInvitee1 = mock(Employee.class);
        mockInvitee2 = mock(Employee.class);
        mockRoom = mock(ChatRoom.class);
        mockMessage = mock(ChatMessage.class);
        mockChatEmployee = mock(ChatEmployee.class);
        mockCommonCode = mock(CommonCode.class);
        mockChatEmployeeInvitee1 = mock(ChatEmployee.class);

        // 'when' 충돌을 막기 위해 Mockito.lenient() 명시적 호출
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

        Mockito.lenient().when(mockChatEmployeeInvitee1.getEmployee()).thenReturn(mockInvitee1);
        Mockito.lenient().when(mockChatEmployeeInvitee1.getLastReadMessage()).thenReturn(mockMessage);
        // [FIX 3] NPE 해결: mockChatEmployeeInvitee1의 getJoinedAt() stubbing 추가
        Mockito.lenient().when(mockChatEmployeeInvitee1.getJoinedAt()).thenReturn(LocalDateTime.now().minusDays(1));


        Mockito.lenient().when(mockCommonCode.getCommonCodeId()).thenReturn(99L);
    }

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoom {

        // [수정] 1:1 채팅방 생성 시, 기존 방 재사용 로직 테스트 (findExistingOneOnOneRooms)
        @Test
        @DisplayName("성공 - 1:1 채팅방 (기존 방 재사용)")
        void createRoom_Success_1to1_Rejoin() throws Exception {
            // given
            String creatorUsername = "creatorUser";
            String roomName = "1:1 채팅방";
            List<Long> inviteeIds = List.of(2L);
            List<Employee> invitees = List.of(mockInvitee1);

            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(employeeRepository.findAllById(anySet())).willReturn(invitees);

            // [FIX 1] Optional -> List<ChatRoom> 반환 (findExistingOneOnOneRooms)
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(List.of(mockRoom));

            // '나간' 멤버(mockChatEmployeeInvitee1)와 '방장'(mockChatEmployee)을 반환
            given(mockChatEmployee.getIsLeft()).willReturn(false);
            given(mockChatEmployeeInvitee1.getIsLeft()).willReturn(true); // [!] 나간 상태
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(mockChatEmployee, mockChatEmployeeInvitee1));

            ChatRoomResponseDTO expectedDto = ChatRoomResponseDTO.builder().chatRoomId(100L).build();

            // when
            try (MockedStatic<ChatRoomResponseDTO> roomDtoMock = mockStatic(ChatRoomResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                roomDtoMock.when(ChatRoomResponseDTO::builder).thenCallRealMethod();
                roomDtoMock.when(() -> ChatRoomResponseDTO.toDTO(mockRoom)).thenReturn(expectedDto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                        .then(invocation -> null);

                // --- service call ---
                ChatRoomResponseDTO result = chatService.createRoom(creatorUsername, roomName, inviteeIds);
                // --- service call ---

                // then
                assertThat(result).isSameAs(expectedDto);
                verify(chatRoomRepository, never()).save(any(ChatRoom.class)); // 새 방 생성 안 함
                verify(chatMessageRepository, never()).save(any(ChatMessage.class)); // 시스템 메시지 생성 안 함

                // [!] '나간' 유저(mockChatEmployeeInvitee1)만 rejoinChatRoom()이 호출되어야 함
                verify(mockChatEmployeeInvitee1).rejoinChatRoom();
                verify(chatEmployeeRepository).save(mockChatEmployeeInvitee1);

                // '방장'(mockChatEmployee)은 호출 안 됨
                verify(mockChatEmployee, never()).rejoinChatRoom();
            }
        }

        // ... (기존 createRoom 테스트들 - 수정 불필요)
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
            given(chatRoomRepository.findExistingOneOnOneRooms(1L, 2L)).willReturn(Collections.emptyList()); // [FIX]
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

                roomDtoMock.when(ChatRoomResponseDTO::builder).thenCallRealMethod();
                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();
                roomMock.when(() -> ChatRoom.createChatRoom(mockCreator, roomName, false)).thenReturn(mockRoom);
                msgMock.when(() -> ChatMessage.createChatMessage(any(), any(), anyString())).thenCallRealMethod();
                empMock.when(() -> ChatEmployee.createChatEmployee(any(Employee.class), eq(mockRoom), eq(roomName), eq(mockMessage)))
                        .thenReturn(mockChatEmployee);
                roomDtoMock.when(() -> ChatRoomResponseDTO.toDTO(mockRoom)).thenReturn(expectedDto);
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(messageDto);
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

        // ... (이하 createRoom의 다른 테스트는 수정 불필요)
    }

    // ... (Nested: inviteToRoom) - 수정 불필요

    // ... (Nested: leaveRoom) - 수정 불필요

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessage {

        @BeforeEach
        void sendSetup() {
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatRoomRepository.findById(100L)).thenReturn(Optional.of(mockRoom));
            Mockito.lenient().when(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .thenReturn(Optional.of(mockChatEmployee));
            Mockito.lenient().when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        }

        // ... (sendMessage_Success_ReactivatesLeftUser 테스트 - 수정 불필요)

        @Test
        @DisplayName("성공 - 팀 채팅방 메시지 전송 (미읽음 카운트 2)")
        void sendMessage_Success_TeamChat() throws Exception {
            // given
            String content = "안녕하세요";
            given(mockRoom.getIsTeam()).willReturn(true);

            // [!] countUnreadForMessage 호출 (미읽음 2명)
            given(chatEmployeeRepository.countUnreadForMessage(100L, 1L, mockMessage.getCreatedAt())).willReturn(2L);

            ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder().unreadCount(0).build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();
                msgMock.when(() -> ChatMessage.createChatMessage(any(ChatRoom.class), any(Employee.class), anyString())).thenCallRealMethod();

                // [!] ChatMessageResponseDTO.toDTO는 unreadCount가 0인 DTO를 반환함
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

                // [!] 서비스가 unreadCount를 2로 설정했는지 검증
                assertThat(result.getUnreadCount()).isEqualTo(2L);

                // --- afterCommit 강제 실행 (notifyParticipantsOfNewMessage 호출) ---
                given(chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(100L))
                        .willReturn(List.of(mockChatEmployee, mockChatEmployeeInvitee1));

                // [FIX 1] 컴파일 에러 수정: ...AndEmployeeIsNotNull 추가
                given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(any(ChatRoom.class), anyLong())).willReturn(1L);

                ChatRoomListResponseDTO mockListDtoTeam = ChatRoomListResponseDTO.builder().build();
                ChatRoomListResponseDTO mockListDto1to1 = ChatRoomListResponseDTO.builder().build();

                try (MockedStatic<ChatRoomListResponseDTO> listDtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                    listDtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();
                    listDtoMock.when(() -> ChatRoomListResponseDTO.toDTO(any(ChatRoom.class), anyLong(), any(Optional.class)))
                            .thenReturn(mockListDtoTeam);
                    listDtoMock.when(() -> ChatRoomListResponseDTO.toDTO(any(ChatRoom.class), any(Employee.class), anyLong(), any(Optional.class)))
                            .thenReturn(mockListDto1to1);

                    syncCaptor.getValue().afterCommit();
                }

                // ... (after commit 'then' 절은 동일)
            }
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 메시지 전송 (미읽음 카운트 1)")
        void sendMessage_Success_1to1() throws Exception {
            // given
            String content = "안녕하세요";
            given(mockRoom.getIsTeam()).willReturn(false); // [!] 1:1방

            // [신규] 1:1방 미읽음 카운트 1명 Mock
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(100L, 1L))
                    .willReturn(1L);

            ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder().unreadCount(0).build();

            // when
            try (MockedStatic<ChatMessage> msgMock = mockStatic(ChatMessage.class);
                 MockedStatic<ChatMessageResponseDTO> msgDtoMock = mockStatic(ChatMessageResponseDTO.class);
                 MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {

                msgDtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();
                msgMock.when(() -> ChatMessage.createChatMessage(any(ChatRoom.class), any(Employee.class), anyString())).thenCallRealMethod();
                msgDtoMock.when(() -> ChatMessageResponseDTO.toDTO(mockMessage)).thenReturn(dto);
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                ChatMessageResponseDTO result = chatService.sendMessage("creatorUser", 100L, content);
                // --- service call ---

                // then
                // [!] 1:1방이므로 countUnreadForMessage는 호출 안 됨
                verify(chatEmployeeRepository, never()).countUnreadForMessage(anyLong(), anyLong(), any(LocalDateTime.class));
                // [!] 1:1방 카운트(1L)가 설정되었는지 검증
                assertThat(result.getUnreadCount()).isEqualTo(1L);
                verify(chatMessageRepository).save(chatMessageCaptor.capture());
                assertThat(chatMessageCaptor.getValue().getContent()).isEqualTo(content);
            }
        }

        // ... (sendMessage 실패 케이스 - 수정 불필요)
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessages {

        @Test
        @DisplayName("성공 - 메시지 페이지 조회 (신규: 미읽음 카운트 포함)")
        void getMessages_Success_WithUnreadCount() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime joinedAt = LocalDateTime.now().minusDays(1);
            LocalDateTime msgCreatedAt = LocalDateTime.now().minusHours(1);

            given(mockChatEmployee.getJoinedAt()).willReturn(joinedAt);
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .willReturn(Optional.of(mockChatEmployee));

            // [신규] 2개의 메시지 Mock (하나는 시스템 메시지)
            ChatMessage userMessage = mock(ChatMessage.class);
            ChatMessage systemMessage = mock(ChatMessage.class);
            given(userMessage.getEmployee()).willReturn(mockCreator); // 유저 메시지
            given(userMessage.getCreatedAt()).willReturn(msgCreatedAt);
            given(systemMessage.getEmployee()).willReturn(null); // 시스템 메시지

            List<ChatMessage> messages = List.of(userMessage, systemMessage);
            Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 2);

            given(chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                    100L, joinedAt, pageable
            )).willReturn(messagePage);

            // [신규] 유저 메시지에 대한 미읽음 카운트 Mock (3명)
            given(chatEmployeeRepository.countUnreadByAllParticipants(100L, msgCreatedAt)).willReturn(3L);

            // DTO Mock (기본 변환)
            ChatMessageResponseDTO userDto = ChatMessageResponseDTO.builder().chatMessageId(1001L).build();
            ChatMessageResponseDTO sysDto = ChatMessageResponseDTO.builder().chatMessageId(1002L).build();

            // when
            try (MockedStatic<ChatMessageResponseDTO> dtoMock = mockStatic(ChatMessageResponseDTO.class)) {
                dtoMock.when(ChatMessageResponseDTO::builder).thenCallRealMethod();
                // [!] .toDTO는 unreadCount가 0인 DTO를 반환
                dtoMock.when(() -> ChatMessageResponseDTO.toDTO(userMessage)).thenReturn(userDto);
                dtoMock.when(() -> ChatMessageResponseDTO.toDTO(systemMessage)).thenReturn(sysDto);

                // --- service call ---
                Page<ChatMessageResponseDTO> result = chatService.getMessages("creatorUser", 100L, pageable);
                // --- service call ---

                // then
                assertThat(result).isNotNull();
                assertThat(result.getTotalElements()).isEqualTo(2);

                // [신규] 1. 유저 메시지(userDto)는 unreadCount가 3으로 설정되어야 함
                assertThat(result.getContent().get(0).getChatMessageId()).isEqualTo(1001L);
                assertThat(result.getContent().get(0).getUnreadCount()).isEqualTo(3L);

                // [신규] 2. 시스템 메시지(sysDto)는 unreadCount가 0 (기본값)이어야 함
                assertThat(result.getContent().get(1).getChatMessageId()).isEqualTo(1002L);
                assertThat(result.getContent().get(1).getUnreadCount()).isEqualTo(0L);

                // [신규] 3. countUnreadByAllParticipants는 유저 메시지에 대해서만 1번 호출됨
                verify(chatEmployeeRepository, times(1)).countUnreadByAllParticipants(100L, msgCreatedAt);
            }
        }

        // ... (getMessages 실패 케이스 - 수정 불필요)
    }

    @Nested
    @DisplayName("updateLastReadMessageId (마지막 읽은 메시지 업데이트)")
    class UpdateLastReadMessageId {

        @BeforeEach
        void updateReadSetup() {
            Mockito.lenient().when(employeeRepository.findByUsername("creatorUser")).thenReturn(Optional.of(mockCreator));
            Mockito.lenient().when(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(100L, "creatorUser"))
                    .thenReturn(Optional.of(mockChatEmployee));
        }

        @Test
        @DisplayName("성공 - 읽음 처리 및 unread-updates 브로드캐스트 (신규 로직)")
        void updateLastReadMessageId_Success_NewLogic() throws Exception {
            // given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oldReadTime = now.minusMinutes(10);

            ChatMessage oldMessage = mock(ChatMessage.class);
            ChatMessage newMessage = mock(ChatMessage.class); // 1000L

            given(newMessage.getChatMessageId()).willReturn(1000L);
            given(newMessage.getChatRoom()).willReturn(mockRoom);
            given(newMessage.getCreatedAt()).willReturn(now);
            given(newMessage.getEmployee()).willReturn(mockInvitee1); // 다른 사람이 보낸 메시지

            given(mockChatEmployee.getLastReadMessage()).willReturn(oldMessage);
            given(oldMessage.getCreatedAt()).willReturn(oldReadTime);
            given(mockChatEmployee.getJoinedAt()).willReturn(oldReadTime.minusDays(1));

            given(chatMessageRepository.findById(1000L)).willReturn(Optional.of(newMessage));

            // [신규] 1. '방금 읽은' 메시지 목록 Mock
            given(chatMessageRepository.findAllByChatRoomChatRoomIdAndCreatedAtAfterAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                    100L, oldReadTime, now
            )).willReturn(List.of(newMessage));

            // [신규] 2. '새로운 안 읽음 카운트' Mock (0으로 줄어듦)
            given(chatEmployeeRepository.countUnreadByAllParticipants(100L, now)).willReturn(0L);

            // [신규] 3. '총 안 읽음 개수' Mock
            given(chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(1L)).willReturn(0L);

            // when
            try (MockedStatic<TransactionSynchronizationManager> syncMock = mockStatic(TransactionSynchronizationManager.class)) {
                syncMock.when(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .then(invocation -> null);

                // --- service call ---
                chatService.updateLastReadMessageId("creatorUser", 100L, 1000L);
                // --- service call ---

                syncCaptor.getValue().afterCommit();

                // then
                // 1. DB 업데이트 검증
                verify(mockChatEmployee).updateLastReadMessage(newMessage);
                verify(chatEmployeeRepository).save(mockChatEmployee);

                // 2. [신규] /unread-updates 토픽으로 갱신된 카운트 전송 검증
                verify(simpMessagingTemplate).convertAndSend(
                        eq("/topic/chat/rooms/100/unread-updates"),
                        unreadUpdateCaptor.capture()
                );
                // 캡처된 Map 검증
                Map<String, Object> payload = unreadUpdateCaptor.getValue();
                assertThat(payload).containsKey("unreadUpdates");
                List<Map<String, Object>> updates = (List<Map<String, Object>>) payload.get("unreadUpdates");
                assertThat(updates).hasSize(1);
                assertThat(updates.get(0)).containsEntry("chatMessageId", 1000L);
                assertThat(updates.get(0)).containsEntry("newUnreadCount", 0L);

                // 3. [신규] [FIX]
                // 모호성(ambiguity) 에러 해결을 위해 any() -> any(Object.class)로 변경
                verify(simpMessagingTemplate, never()).convertAndSend(
                        eq("/topic/chat/rooms/100/reads"),
                        any(Object.class)
                );

                // 4. 총 안 읽은 개수 전송 검증
                verify(chatEmployeeRepository).sumTotalUnreadMessagesByEmployeeId(1L);
                verify(simpMessagingTemplate).convertAndSendToUser(
                        eq("creatorUser"),
                        eq("/queue/unread-count"),
                        any(TotalUnreadCountResponseDTO.class)
                );
            }
        }

        // ... (updateLastReadMessageId 실패 케이스 - 수정 불필요)
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

            // [FIX 2] NPE 해결: lastReadMessage와 createdAt, joinedAt을 stubbing
            LocalDateTime time = LocalDateTime.now();
            given(membership1.getJoinedAt()).willReturn(time.minusDays(1));
            given(membership2.getJoinedAt()).willReturn(time.minusDays(1));

            ChatMessage lastReadMsg1 = mock(ChatMessage.class);
            given(lastReadMsg1.getCreatedAt()).willReturn(time.minusHours(1));
            given(membership1.getLastReadMessage()).willReturn(lastReadMsg1);

            ChatMessage lastReadMsg2 = mock(ChatMessage.class);
            given(lastReadMsg2.getCreatedAt()).willReturn(time.minusHours(1));
            given(membership2.getLastReadMessage()).willReturn(lastReadMsg2);

            // [FIX] 1:1방의 상대방을 찾기 위한 stubbing
            lenient().when(membership1.getEmployee()).thenReturn(mockCreator);

            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(mockCreator))
                    .willReturn(List.of(membership1, membership2));

            ChatEmployee otherMembership = mock(ChatEmployee.class);
            given(otherMembership.getEmployee()).willReturn(mockInvitee1);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(101L))
                    .willReturn(List.of(membership1, otherMembership));

            ChatMessage msg1 = mock(ChatMessage.class);
            given(msg1.getContent()).willReturn("Apple");
            ChatMessage msg2 = mock(ChatMessage.class);
            given(msg2.getContent()).willReturn("Zebra");
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(room1), any()))
                    .willReturn(Optional.of(msg1));
            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(room2), any()))
                    .willReturn(Optional.of(msg2));

            // [FIX 1] 컴파일 에러 수정: ...AndEmployeeIsNotNull 추가
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(eq(room1), anyLong())).willReturn(1L);
            // [FIX 2] 컴파일 에러 수정: ...AndEmployeeIsNotNull 추가
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(eq(room2), anyLong())).willReturn(2L);

            ChatRoomListResponseDTO dto1 = ChatRoomListResponseDTO.builder().chatRoomId(101L).lastMessage("Apple").build();
            ChatRoomListResponseDTO dto2 = ChatRoomListResponseDTO.builder().chatRoomId(102L).lastMessage("Zebra").build();

            // when
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                dtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();

                // [!] ChatRoomListResponseDTO.java의 실제 정적 팩토리 메소드 시그니처와 일치시킴
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
        @DisplayName("성공 - 1:1방 나간 유저(A)가 알림 클릭으로 재입장 (버그 수정 검증)")
        void getRoomById_Success_RejoinLeftUser() throws Exception {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.of(mockChatEmployee));
            given(mockChatEmployee.getChatRoom()).willReturn(mockRoom);
            given(mockChatEmployee.getIsLeft()).willReturn(true); // [!] 나간 상태
            given(mockRoom.getIsTeam()).willReturn(false); // [!] 1:1방

            ChatEmployee otherMember = mock(ChatEmployee.class);
            given(otherMember.getEmployee()).willReturn(mockInvitee1);
            given(chatEmployeeRepository.findAllByChatRoomChatRoomId(100L))
                    .willReturn(List.of(mockChatEmployee, otherMember));

            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(any(), any()))
                    .willReturn(Optional.empty());

            // [FIX 1] UnnecessaryStubbingException 해결:
            // @BeforeEach의 mockChatEmployee.getLastReadMessage() stubbing 때문에
            // if (line 697)가 true가 되므로, else 블록의 stub(CreatedAt)을 지우고
            // if 블록의 stub(ChatMessageIdGreaterThan)을 추가합니다.
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(any(), anyLong())).willReturn(0L);

            ChatRoomListResponseDTO expectedDto = ChatRoomListResponseDTO.builder().chatRoomId(100L).build();

            // when
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                dtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();
                // [!] ChatRoomListResponseDTO.java의 실제 정적 팩토리 메소드 시그니처와 일치시킴
                dtoMock.when(() -> ChatRoomListResponseDTO.toDTO(mockRoom, mockInvitee1, 0L, Optional.empty()))
                        .thenReturn(expectedDto);

                // --- service call ---
                ChatRoomListResponseDTO result = chatService.getRoomById("creatorUser", 100L);
                // --- service call ---

                // then
                assertThat(result).isSameAs(expectedDto);
                verify(mockChatEmployee).rejoinChatRoom();
                verify(chatEmployeeRepository).save(mockChatEmployee);
            }
        }

        @Test
        @DisplayName("성공 - ID로 팀 채팅방 정보 조회")
        void getRoomById_Success_TeamRoom() throws Exception {
            // given
            given(employeeRepository.findByUsername("creatorUser")).willReturn(Optional.of(mockCreator));
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(100L, 1L))
                    .willReturn(Optional.of(mockChatEmployee));
            given(mockChatEmployee.getChatRoom()).willReturn(mockRoom);
            given(mockRoom.getIsTeam()).willReturn(true);

            given(chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(eq(mockRoom), any()))
                    .willReturn(Optional.of(mockMessage));

            // [FIX 4] 컴파일 에러 수정: ...AndEmployeeIsNotNull 추가
            // [!] @BeforeEach의 stubbing으로 if(line 697)가 true가 되므로 이 stub은 "필요함"
            given(chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(eq(mockRoom), anyLong())).willReturn(3L);

            ChatRoomListResponseDTO expectedDto = ChatRoomListResponseDTO.builder()
                    .chatRoomId(100L)
                    .name("테스트 채팅방")
                    .unreadCount(3L)
                    .build();

            // when
            try (MockedStatic<ChatRoomListResponseDTO> dtoMock = mockStatic(ChatRoomListResponseDTO.class)) {
                dtoMock.when(ChatRoomListResponseDTO::builder).thenCallRealMethod();
                // [!] ChatRoomListResponseDTO.java의 실제 정적 팩토리 메소드 시그니처와 일치시킴
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

        // ... (getRoomById 실패 케이스 - 수정 불필요)
    }
}