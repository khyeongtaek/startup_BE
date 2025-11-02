package org.goodee.startup_BE.chat.service;

import jakarta.persistence.EntityNotFoundException;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
// BDDMockito static import
import static org.mockito.BDDMockito.*;
// Mockito static import
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
// lenient(): Mocking이 모든 테스트에서 사용되지 않아도 경고/오류 미발생 (setUp에서 공통 Mocking 시 유용)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private ChatServiceImpl chatService;

    // Mock 객체로 생성
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

    // 테스트용 공통 Mock 객체
    private Employee mockCreator;
    private Employee mockMember1;
    private Employee mockMember2;
    private ChatRoom mockRoom;
    private ChatEmployee mockChatEmployeeCreator;
    private ChatMessage mockMessage;
    private CommonCode mockInviteCode;

    /**
     * 각 테스트 실행 전 공통 Mock 객체 설정
     */
    @BeforeEach
    void setUp() {
        // Mock 객체 초기화 (lenient 설정으로 인해 @BeforeEach에서 초기화)
        mockCreator = mock(Employee.class);
        mockMember1 = mock(Employee.class);
        mockMember2 = mock(Employee.class);
        mockRoom = mock(ChatRoom.class);
        mockChatEmployeeCreator = mock(ChatEmployee.class);
        mockMessage = mock(ChatMessage.class);
        mockInviteCode = mock(CommonCode.class);

        // 공통 더미 값 설정
        lenient().when(mockCreator.getEmployeeId()).thenReturn(1L);
        lenient().when(mockCreator.getUsername()).thenReturn("creator");
        lenient().when(mockCreator.getName()).thenReturn("CreatorName");

        lenient().when(mockMember1.getEmployeeId()).thenReturn(2L);
        lenient().when(mockMember1.getUsername()).thenReturn("m1");
        lenient().when(mockMember1.getName()).thenReturn("Member1Name");

        lenient().when(mockMember2.getEmployeeId()).thenReturn(3L);
        lenient().when(mockMember2.getUsername()).thenReturn("m2");
        lenient().when(mockMember2.getName()).thenReturn("Member2Name");

        lenient().when(mockRoom.getChatRoomId()).thenReturn(10L);
        lenient().when(mockRoom.getName()).thenReturn("Test Room");
        lenient().when(mockRoom.getIsTeam()).thenReturn(true); // 기본: 팀방

        // DTO 변환 시 NullPointerException 방지를 위한 Mocking
        lenient().when(mockRoom.getEmployee()).thenReturn(mockCreator);
        lenient().when(mockRoom.getCreatedAt()).thenReturn(LocalDateTime.now());

        lenient().when(mockChatEmployeeCreator.getEmployee()).thenReturn(mockCreator);

        lenient().when(mockMessage.getChatMessageId()).thenReturn(100L);
        lenient().when(mockMessage.getChatRoom()).thenReturn(mockRoom);
        lenient().when(mockMessage.getContent()).thenReturn("hello");
        lenient().when(mockMessage.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(mockMessage.getEmployee()).thenReturn(mockCreator);

        lenient().when(mockInviteCode.getCommonCodeId()).thenReturn(999L); // 알림용 가짜 ID

        // findAllById(Iterable) 어떤 인자든 매칭되도록 설정
        // 서비스 로직이 List<Long>을 사용하므로, 이를 처리
        lenient().when(employeeRepository.findAllById(any(Iterable.class)))
                .thenAnswer(invocation -> {
                    Iterable<Long> ids = invocation.getArgument(0);
                    List<Employee> result = new ArrayList<>();
                    for (Long id : iterableToList(ids)) {
                        if (Objects.equals(id, 1L)) result.add(mockCreator);
                        if (Objects.equals(id, 2L)) result.add(mockMember1);
                        if (Objects.equals(id, 3L)) result.add(mockMember2);
                    }
                    return result;
                });
    }

    /**
     * Iterable을 List로 변환하는 헬퍼 메서드
     */
    private static <T> List<T> iterableToList(Iterable<T> it) {
        if (it instanceof Collection) return new ArrayList<>((Collection<T>) it);
        List<T> list = new ArrayList<>();
        it.forEach(list::add);
        return list;
    }

    /**
     * TransactionSynchronizationManager 래퍼
     */
    private void withTxSync(Runnable r) {
        // 테스트 환경에서 TransactionSynchronizationManager 활성화
        TransactionSynchronizationManager.initSynchronization();
        try {
            r.run();
            // afterCommit 훅 실행
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        } finally {
            // 테스트 완료 후 정리
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    // -------------------- Tests --------------------

    @Nested
    @DisplayName("createRoom (채팅방 생성)")
    class CreateRoomTests {

        @Test
        @DisplayName("성공 - 팀 채팅방 생성 (초대 2명)")
        void createRoom_Success_Team() {
            // given
            String creatorUsername = "creator";
            String roomName = "Team Room";
            // [수정] 서비스 시그니처에 맞춰 List<Long> 사용
            List<Long> inviteeIds = List.of(2L, 3L);

            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(mockRoom); // 저장된 방 반환
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage); // 저장된 시스템 메시지 반환

            // isTeamChat = (invitees.size() >= 2) -> true
            given(mockRoom.getIsTeam()).willReturn(true);

            // 알림 전송 로직 Mocking
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    eq(OwnerType.PREFIX), eq(OwnerType.TEAMCHATNOTI.name())))
                    .willReturn(List.of(mockInviteCode)); // CommonCode 찾기 성공

            // when
            ChatRoomResponseDTO result = chatService.createRoom(creatorUsername, roomName, inviteeIds);

            // then
            // 1. 핵심 객체 저장 확인
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            verify(chatEmployeeRepository, times(1)).saveAll(anyList()); // 3명(creator + 2 invitees) 저장

            // 2. WebSocket 브로드캐스트 확인 (afterCommit은 withTxSync로 대체 불가, 직접 호출)
            verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/rooms/" + 10L), any(ChatMessageResponseDTO.class));

            // 3. 알림 전송 확인 (초대 2명)
            verify(notificationService, times(2)).create(any(NotificationRequestDTO.class));

            // 4. 반환값 검증
            assertThat(result).isNotNull();
            assertThat(result.getChatRoomId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 생성 (초대 1명, 알림 없음)")
        void createRoom_Success_Single() {
            // given
            String creatorUsername = "creator";
            String roomName = "1:1 Room";
            List<Long> inviteeIds = List.of(2L); // 1명만 초대

            // isTeamChat = (invitees.size() >= 2) -> false
            given(mockRoom.getIsTeam()).willReturn(false);

            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(mockRoom);
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);

            // when
            ChatRoomResponseDTO result = chatService.createRoom(creatorUsername, roomName, inviteeIds);

            // then
            // 1. 핵심 객체 저장 확인
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            verify(chatEmployeeRepository, times(1)).saveAll(anyList()); // 2명(creator + 1 invitee) 저장

            // 2. WebSocket 브로드캐스트 확인
            verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/rooms/" + 10L), any(ChatMessageResponseDTO.class));

            // 3. 알림 전송이 "호출되지 않음" 확인
            verify(notificationService, never()).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("예외 - 초대 리스트 비어있음")
        void createRoom_Fail_EmptyInvitee() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));

            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creator", "room", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("최소 한 명 이상 초대해야 합니다");
        }

        @Test
        @DisplayName("예외 - 생성자 자신을 초대")
        void createRoom_Fail_InviteSelf() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));

            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creator", "room", List.of(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("초대할 수 있는 대상이 아닙니다");
        }

        @Test
        @DisplayName("예외 - 초대 대상 중 존재하지 않는 사원")
        void createRoom_Fail_InviteeNotFound() {
            // given
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            List<Long> inviteeIds = List.of(2L, 999L); // 999L은 존재하지 않음 (findAllById Mocking 기준)

            // when & then
            assertThatThrownBy(() -> chatService.createRoom("creator", "room", inviteeIds))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("최대 대상 중 존재하지 않은 사원이 있습니다.");
        }
    }

    @Nested
    @DisplayName("inviteToRoom (채팅방 초대)")
    class InviteToRoomTests {

        @BeforeEach
        void inviteSetup() {
            // 공통 given: 초대자(creator), 방(room)은 존재함
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(mockRoom));
            // 공통 given: 초대자는 방 멤버임
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(10L, 1L))
                    .willReturn(true);
        }

        @Test
        @DisplayName("성공 - 1:1 방에서 초대 (팀방으로 변경)")
        void invite_Success_FromSingleRoom() {
            // given
            given(mockRoom.getIsTeam()).willReturn(false); // 1:1 방
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(10L)).willReturn(Set.of(1L, 2L)); // 기존 멤버

            List<Long> inviteeIds = List.of(3L); // 신규 멤버 3L
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(any(), any())).willReturn(List.of(mockInviteCode));

            // when
            // afterCommit 훅을 테스트하기 위해 withTxSync 래퍼 사용
            withTxSync(() -> chatService.inviteToRoom("creator", 10L, inviteeIds));

            // then
            // 1. 1:1 -> 팀방 변경 메서드 호출 확인
            verify(mockRoom, times(1)).updateToTeamRoom();

            // 2. 시스템 메시지 저장 확인
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));

            // 3. 신규 멤버(1명) 저장 확인
            verify(chatEmployeeRepository, times(1)).saveAll(argThat(list -> ((Collection<?>) list).size() == 1));

            // 4. (afterCommit) WebSocket 브로드캐스트 확인
            verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/rooms/10"), any(ChatMessageResponseDTO.class));

            // 5. (afterCommit) 알림 전송 확인
            verify(notificationService, times(1)).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("성공 - 이미 멤버인 인원 제외")
        void invite_Success_FilterExisting() {
            // given
            given(mockRoom.getIsTeam()).willReturn(true); // 팀방
            given(chatEmployeeRepository.findActiveEmployeeIdsByRoomId(10L)).willReturn(Set.of(1L, 2L)); // 1, 2는 이미 멤버

            List<Long> inviteeIds = List.of(2L, 3L); // 2L(기존), 3L(신규)

            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(any(), any())).willReturn(List.of(mockInviteCode));

            // when
            withTxSync(() -> chatService.inviteToRoom("creator", 10L, inviteeIds));

            // then
            // 1. 신규 멤버(1명: 3L)만 저장되는지 확인
            verify(chatEmployeeRepository, times(1)).saveAll(argThat(list -> ((Collection<?>) list).size() == 1));

            // 2. (afterCommit) 알림도 1명에게만 전송되는지 확인
            verify(notificationService, times(1)).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("예외 - 초대자가 멤버가 아님")
        void invite_Fail_NotMember() {
            // given
            // 초대자 권한 체크 실패
            given(chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(10L, 1L))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatService.inviteToRoom("creator", 10L, List.of(2L)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 구성원이 아니면 초대할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("leaveRoom (채팅방 나가기)")
    class LeaveRoomTests {

        @BeforeEach
        void leaveSetup() {
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(mockRoom));
        }

        @Test
        @DisplayName("성공 - 팀 채팅방 나가기 (남은 인원 1명 이상)")
        void leave_Success_Team_MembersRemain() {
            // given
            given(mockRoom.getIsTeam()).willReturn(true); // 팀방
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(10L, 1L))
                    .willReturn(Optional.of(mockChatEmployeeCreator));

            // 남은 인원 1명 (0 아님)
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(10L)).willReturn(1L);

            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);

            // when
            withTxSync(() -> chatService.leaveRoom("creator", 10L));

            // then
            // 1. 나가기 처리 확인
            verify(mockChatEmployeeCreator, times(1)).leftChatRoom();
            verify(chatEmployeeRepository, times(1)).save(mockChatEmployeeCreator);

            // 2. 시스템 메시지 저장 확인
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));

            // 3. (afterCommit) 브로드캐스트 확인
            verify(simpMessagingTemplate, times(1)).convertAndSend(anyString(), any(ChatMessageResponseDTO.class));

            // 4. 방 삭제 "호출되지 않음" 확인
            verify(mockRoom, never()).deleteRoom(); // (ChatRoom.java에 deleteRoom() 추가 필요)
        }

        @Test
        @DisplayName("성공 - 1:1 채팅방 나가기 (메시지 없음)")
        void leave_Success_Single() {
            // given
            given(mockRoom.getIsTeam()).willReturn(false); // 1:1 방
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(10L, 1L))
                    .willReturn(Optional.of(mockChatEmployeeCreator));
            given(chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(10L)).willReturn(1L);

            // when
            withTxSync(() -> chatService.leaveRoom("creator", 10L));

            // then
            // 1. 나가기 처리 확인
            verify(mockChatEmployeeCreator, times(1)).leftChatRoom();

            // 2. 시스템 메시지 "저장되지 않음" 확인
            verify(chatMessageRepository, never()).save(any(ChatMessage.class));

            // 3. (afterCommit) 브로드캐스트 "호출되지 않음" 확인
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageResponseDTO.class));

            // 4. 방 삭제 "호출되지 않음" 확인
            verify(mockRoom, never()).deleteRoom();
        }

        @Test
        @DisplayName("예외 - 멤버가 아님")
        void leave_Fail_NotMember() {
            // given
            // 멤버 조회 실패
            given(chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(10L, 1L))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.leaveRoom("creator", 10L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("해당 채팅방의 멤버가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("sendMessage (메시지 전송)")
    class SendMessageTests {

        @BeforeEach
        void sendSetup() {
            // 공통 given: sender, room, membership 조회 성공
            given(employeeRepository.findByUsername("creator")).willReturn(Optional.of(mockCreator));
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(mockRoom));
            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.of(mockChatEmployeeCreator));
        }

        @Test
        @DisplayName("성공 - 팀방: 미읽음 카운트 포함")
        void send_Success_Team_UnreadCount() {
            // given
            long expectedUnreadCount = 2L;
            given(mockRoom.getIsTeam()).willReturn(true); // 팀방
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);

            // DTO 변환용 Mocking
            given(mockMessage.getEmployee()).willReturn(mockCreator);
            given(mockCreator.getName()).willReturn("CreatorName");

            // 미읽음 카운트 Mocking
            given(chatEmployeeRepository.countUnreadForMessage(eq(10L), eq(1L), any(LocalDateTime.class)))
                    .willReturn(expectedUnreadCount);

            // when
            ChatMessageResponseDTO resultDTO = null;
            withTxSync(() -> {
                // withTxSync 내부에서 결과를 받아야 함
                ChatMessageResponseDTO dto = chatService.sendMessage("creator", 10L, " hello ");
                // DTO에 unreadCount가 설정되었는지 확인
                // (ChatMessageResponseDTO에 @Setter와 unreadCount 필드 추가 필요)
                assertThat(dto.getUnreadCount()).isEqualTo(expectedUnreadCount);
            });

            // then
            // 1. 메시지 저장 확인
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            // 2. 본인 읽음 처리 확인
            verify(mockChatEmployeeCreator, times(1)).updateLastReadMessage(mockMessage);
            verify(chatEmployeeRepository, times(1)).save(mockChatEmployeeCreator);
            // 3. (afterCommit) 브로드캐스트 확인
            verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/rooms/10"), any(ChatMessageResponseDTO.class));
        }

        @Test
        @DisplayName("성공 - 1:1 방: 미읽음 카운트 0")
        void send_Success_Single_UnreadZero() {
            // given
            given(mockRoom.getIsTeam()).willReturn(false); // 1:1 방
            given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(mockMessage);
            given(mockMessage.getEmployee()).willReturn(mockCreator);
            given(mockCreator.getName()).willReturn("CreatorName");

            // when
            withTxSync(() -> {
                ChatMessageResponseDTO dto = chatService.sendMessage("creator", 10L, " hello ");
                // DTO에 unreadCount가 0으로 설정되었는지 확인
                assertThat(dto.getUnreadCount()).isZero();
            });

            // then
            // 1. 미읽음 카운트 쿼리 "호출되지 않음" 확인
            verify(chatEmployeeRepository, never()).countUnreadForMessage(anyLong(), anyLong(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("예외 - 빈 메시지 전송")
        void send_Fail_EmptyContent() {
            // when & then
            assertThatThrownBy(() -> chatService.sendMessage("creator", 10L, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 내용이 비어 있습니다.");
        }

        @Test
        @DisplayName("예외 - 멤버가 아님")
        void send_Fail_NotMember() {
            // given
            // 멤버십 조회 실패
            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.sendMessage("creator", 10L, " hello "))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("채팅방 멤버가 아니거나 이미 나간 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("getMessages (메시지 목록 조회)")
    class GetMessagesTests {

        @Test
        @DisplayName("성공 - joinedAt 이후 메시지 페이징 조회")
        void getMessages_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime joinedAt = LocalDateTime.now().minusMinutes(5);

            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.of(mockChatEmployeeCreator));
            given(mockChatEmployeeCreator.getJoinedAt()).willReturn(joinedAt);

            // DTO 변환용 Mocking
            given(mockMessage.getEmployee()).willReturn(mockCreator);
            given(mockCreator.getName()).willReturn("CreatorName");

            Page<ChatMessage> mockPage = new PageImpl<>(List.of(mockMessage));
            given(chatMessageRepository
                    .findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                            eq(10L), eq(joinedAt), eq(pageable)))
                    .willReturn(mockPage);

            // when
            Page<ChatMessageResponseDTO> resultPage = chatService.getMessages("creator", 10L, pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).getContent()).isEqualTo("hello");
            // getMessages는 미읽음 카운트를 설정하지 않으므로 기본값 0
            assertThat(resultPage.getContent().get(0).getUnreadCount()).isZero();
        }

        @Test
        @DisplayName("예외 - 멤버가 아님")
        void getMessages_Fail_NotMember() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatService.getMessages("creator", 10L, pageable))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("updateLastReadMessageId (읽음 처리)")
    class UpdateLastReadMessageIdTests {

        @Test
        @DisplayName("성공")
        void updateLastRead_Success() {
            // given
            Long messageId = 100L;
            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.of(mockChatEmployeeCreator));
            given(chatMessageRepository.findById(messageId)).willReturn(Optional.of(mockMessage));
            given(mockMessage.getChatRoom()).willReturn(mockRoom);
            given(mockRoom.getChatRoomId()).willReturn(10L); // 방 ID 일치
            given(mockChatEmployeeCreator.getEmployee()).willReturn(mockCreator); // Map 생성용

            // when
            withTxSync(() -> chatService.updateLastReadMessageId("creator", 10L, messageId));

            // then
            // 1. ChatEmployee의 updateLastReadMessage 메서드 호출 검증
            verify(mockChatEmployeeCreator, times(1)).updateLastReadMessage(mockMessage);
            // 2. ChatEmployee 저장 검증
            verify(chatEmployeeRepository, times(1)).save(mockChatEmployeeCreator);
            // 3. (afterCommit) WebSocket 브로드캐스트 검증
            verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/rooms/10/reads"), any(Map.class));
        }

        @Test
        @DisplayName("예외 - 메시지가 다른 방 소속")
        void updateLastRead_Fail_MismatchedRoom() {
            // given
            Long messageId = 100L;
            ChatRoom otherRoom = mock(ChatRoom.class);
            when(otherRoom.getChatRoomId()).thenReturn(99L); // 다른 방 ID

            given(chatEmployeeRepository
                    .findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(10L, "creator"))
                    .willReturn(Optional.of(mockChatEmployeeCreator));
            given(chatMessageRepository.findById(messageId)).willReturn(Optional.of(mockMessage));
            given(mockMessage.getChatRoom()).willReturn(otherRoom); // 메시지가 다른 방 소속

            // when & then
            assertThatThrownBy(() -> chatService.updateLastReadMessageId("creator", 10L, messageId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 채팅방의 메시지가 아닙니다.");
        }
    }
}