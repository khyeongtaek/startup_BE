package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// EmployeeRepositoryTest와 동일한 H2 설정 적용
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// ChatMessage가 의존하는 모든 엔티티(CommonCode, Employee, ChatRoom)를 스캔
@EntityScan(basePackages = "org.goodee.startup_BE")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 의존성 주입: ChatMessage 생성에 필요한 엔티티들의 Repository
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- 테스트용 공통 데이터 ---
    private Employee admin, user1, user2;
    private ChatRoom room1, room2;
    private CommonCode statusActive, roleAdmin, roleUser, deptDev, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    // --- 테스트용 ChatMessage 인스턴스 ---
    private ChatMessage msg_sys_room1; // room1의 최초 시스템 메시지
    private ChatMessage msg1_room1;      // room1의 user1 메시지
    private ChatMessage msg2_room1;      // room1의 user2 메시지
    private ChatMessage msg3_room1_deleted; // room1의 user1 메시지 (삭제됨)
    private ChatMessage msg4_room1;      // room1의 user1 메시지 (최신)

    private ChatMessage msg1_room2;      // room2의 user1 메시지

    private LocalDateTime userJoinedAt; // msg_sys_room1 이후의 시간 (필터링 테스트용)

    @BeforeEach
    void setUp() throws InterruptedException {
        // H2 DB 초기화 (자식 테이블부터 삭제)
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given 1: 공통 코드 데이터 생성 ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);

        commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, posJunior));

        // --- given 2: 직원 데이터 생성 (admin, user1, user2) ---
        admin = createAndSaveEmployee("admin", "admin@test.com", roleAdmin, deptDev);
        user1 = createAndSaveEmployee("user1", "user1@test.com", roleUser, deptDev);
        user2 = createAndSaveEmployee("user2", "user2@test.com", roleUser, deptDev);

        // --- given 3: 채팅방 데이터 생성 (room1, room2) ---
        room1 = chatRoomRepository.save(ChatRoom.createChatRoom(admin, "Room 1", true));
        room2 = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "Room 2", false));

        // --- given 4: 테스트용 채팅 메시지 생성 (시간 순서 보장) ---

        // 4-1. Room 1의 메시지
        msg_sys_room1 = createAndSaveMessage(room1, null, "Room 1 created"); //

        // [FIX 2] createdAt 시간차를 확실히 보장하기 위해 sleep을 먼저 호출
        Thread.sleep(10);
        // userJoinedAt이 msg_sys_room1.createdAt 보다 확실히 나중이 됨
        userJoinedAt = LocalDateTime.now();
        // 다음 메시지와의 시간차도 보장
        Thread.sleep(10);

        msg1_room1 = createAndSaveMessage(room1, user1, "Hello from user1");
        Thread.sleep(10);

        msg2_room1 = createAndSaveMessage(room1, user2, "Hello from user2");
        Thread.sleep(10);

        msg3_room1_deleted = createAndSaveMessage(room1, user1, "This message will be deleted");
        msg3_room1_deleted.deleteChatMessage(); //

        // [FIX 1] save() -> saveAndFlush()로 변경
        // isDeleted=true 변경 사항을 DB에 즉시 반영(flush)
        chatMessageRepository.saveAndFlush(msg3_room1_deleted);
        Thread.sleep(10);

        msg4_room1 = createAndSaveMessage(room1, user1, "Latest message from user1");

        // 4-2. Room 2의 메시지
        msg1_room2 = createAndSaveMessage(room2, user1, "Message in Room 2");
    }

    /**
     * Employee 엔티티를 생성하고 즉시 '저장(save)'한 뒤 반환하는 헬퍼
     */
    private Employee createAndSaveEmployee(String username, String email, CommonCode role, CommonCode dept) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, posJunior,
                null
        );
        employee.updateInitPassword(TEST_PASSWORD, null);
        return employeeRepository.save(employee);
    }

    /**
     * ChatMessage 엔티티를 생성하고 '저장(save)'한 뒤 반환하는 헬퍼
     */
    private ChatMessage createAndSaveMessage(ChatRoom room, Employee emp, String content) {
        ChatMessage msg = ChatMessage.createChatMessage(room, emp, content); //
        return chatMessageRepository.save(msg);
    }

    // --- CRUD Tests ---

    @Test
    @DisplayName("C: 채팅 메시지 생성(save) 테스트 - 사용자 메시지")
    void saveUserMessageTest() {
        // given
        ChatMessage newMessage = ChatMessage.createChatMessage(room1, user1, "A new message");

        // when
        ChatMessage savedMessage = chatMessageRepository.save(newMessage);

        // then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getChatMessageId()).isNotNull();
        assertThat(savedMessage.getContent()).isEqualTo("A new message");
        assertThat(savedMessage.getChatRoom()).isEqualTo(room1);
        assertThat(savedMessage.getEmployee()).isEqualTo(user1);
        assertThat(savedMessage.getIsDeleted()).isFalse(); //
        assertThat(savedMessage.getCreatedAt()).isNotNull(); //
    }

    @Test
    @DisplayName("C: 채팅 메시지 생성(save) 테스트 - 시스템 메시지 (employee=null)")
    void saveSystemMessageTest() {
        // given
        // ChatMessage.java의 employee 필드는 nullable=true
        ChatMessage systemMessage = ChatMessage.createChatMessage(room1, null, "System notification");

        // when
        ChatMessage savedMessage = chatMessageRepository.save(systemMessage);

        // then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getChatMessageId()).isNotNull();
        assertThat(savedMessage.getContent()).isEqualTo("System notification");
        assertThat(savedMessage.getEmployee()).isNull(); //
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        Long targetId = msg1_room1.getChatMessageId();

        // when
        Optional<ChatMessage> foundMsg = chatMessageRepository.findById(targetId);

        // then
        assertThat(foundMsg).isPresent();
        assertThat(foundMsg.get().getContent()).isEqualTo("Hello from user1");
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<ChatMessage> foundMsg = chatMessageRepository.findById(nonExistentId);

        // then
        assertThat(foundMsg).isNotPresent();
    }

    @Test
    @DisplayName("U: 메시지 수정(update) 테스트 - Soft Delete")
    void updateMessageSoftDeleteTest() {
        // given
        Long targetId = msg1_room1.getChatMessageId();

        // when
        ChatMessage msgToUpdate = chatMessageRepository.findById(targetId).get();
        msgToUpdate.deleteChatMessage(); //
        chatMessageRepository.saveAndFlush(msgToUpdate);

        // then
        ChatMessage updatedMsg = chatMessageRepository.findById(targetId).get();
        assertThat(updatedMsg.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("D: 메시지 삭제(deleteById) 테스트 - Hard Delete")
    void deleteMessageTest() {
        // given
        Long targetId = msg1_room2.getChatMessageId();
        assertThat(chatMessageRepository.existsById(targetId)).isTrue();

        // when
        chatMessageRepository.deleteById(targetId);
        chatMessageRepository.flush();

        // then
        assertThat(chatMessageRepository.existsById(targetId)).isFalse();
    }

    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(chatRoom) null 저장 시 예외 발생")
    void saveNullChatRoomTest() {
        // given
        // ChatMessage.java의 chatRoom 필드는 nullable=false
        ChatMessage msg = ChatMessage.createChatMessage(null, user1, "Content");

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.saveAndFlush(msg))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(content) null 저장 시 예외 발생")
    void saveNullContentTest() {
        // given
        // ChatMessage.java의 content 필드는 nullable=false
        ChatMessage msg = ChatMessage.createChatMessage(room1, user1, null);

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.saveAndFlush(msg))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Custom Repository Method Tests ---

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc (페이지네이션)")
    void findByRoomIdAndCreatedAtAfterTest() {
        // given
        // [FIX 1] 수정으로 인해 msg3_room1_deleted(isDeleted=true)가 DB에 반영됨
        // [FIX 2] 수정으로 인해 userJoinedAt이 msg_sys_room1 이후임이 보장됨

        // userJoinedAt 이후, isDeleted=false인 메시지:
        // msg1, msg2, msg4 (총 3개)
        Pageable pageable = PageRequest.of(0, 5);
        Long roomId = room1.getChatRoomId();

        // when
        Page<ChatMessage> page = chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                roomId, userJoinedAt, pageable
        );

        // then
        // 1. 총 개수 확인 (기대값 3L)
        assertThat(page.getTotalElements()).isEqualTo(3);
        // 2. 현재 페이지 내용 확인
        assertThat(page.getContent()).hasSize(3);
        // 3. 정렬 순서 확인 (createdAt 내림차순)
        assertThat(page.getContent()).containsExactly(
                msg4_room1, // 최신
                msg2_room1,
                msg1_room1  // 가장 오래됨 (userJoinedAt 이후)
        );
        // 4. 제외된 메시지 확인
        assertThat(page.getContent()).doesNotContain(
                msg_sys_room1,      // userJoinedAt 이전 메시지
                msg3_room1_deleted, // isDeleted=true
                msg1_room2          // 다른 채팅방
        );
    }

    @Test
    @DisplayName("Custom: findBy... (페이지네이션 2페이지 테스트)")
    void findByRoomIdPaginationTest() {
        // given
        // (total 3개: msg4, msg2, msg1)
        Pageable pageable = PageRequest.of(1, 2); // 2페이지 (0-based), 페이지 크기 2

        // when
        Page<ChatMessage> page = chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                room1.getChatRoomId(), userJoinedAt, pageable
        );

        // then
        // 1. 전체 개수는 동일
        assertThat(page.getTotalElements()).isEqualTo(3);
        // 2. 1페이지(크기 2) = msg4, msg2
        //    2페이지(크기 2) = msg1 (1개만 남음)
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent()).containsExactly(msg1_room1);
    }

    @Test
    @DisplayName("Custom: findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc (최신 메시지 1개)")
    void findTopByChatRoomAndCreatedAtAfterTest() {
        // given
        // room1에서 userJoinedAt 이후 가장 최신 메시지는 msg4_room1
        // [FIX 2]로 인해 msg_sys_room1은 userJoinedAt 이전임이 보장됨

        // when
        Optional<ChatMessage> topMsg = chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(
                room1, userJoinedAt
        );

        // when 2 (결과가 없는 경우)
        Optional<ChatMessage> noMsg = chatMessageRepository.findTopByChatRoomAndCreatedAtAfterOrderByCreatedAtDesc(
                room1, LocalDateTime.now().plusDays(1) // 미래 시간
        );

        // then
        assertThat(topMsg).isPresent();
        assertThat(topMsg.get()).isEqualTo(msg4_room1);
        assertThat(noMsg).isNotPresent();
    }

    @Test
    @DisplayName("Custom: countByChatRoomAndChatMessageIdGreaterThan (ID 기준 카운트)")
    void countByChatRoomAndChatMessageIdGreaterThanTest() {
        // given
        // room1의 메시지 순서: msg_sys -> msg1 -> msg2 -> msg3_del -> msg4
        Long lastReadId = msg1_room1.getChatMessageId();

        // when
        // msg1_room1 이후의 메시지 개수 카운트
        long count = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                room1, lastReadId
        );

        // then
        // msg2, msg3_del, msg4 (3개)
        // 이 쿼리는 isDeleted를 필터링하지 않음
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Custom: countByChatRoomAndChatMessageIdGreaterThan (ID 기준 카0)")
    void countByChatRoomAndChatMessageIdGreaterThanZeroTest() {
        // given
        // 가장 최신 메시지 ID
        Long lastReadId = msg4_room1.getChatMessageId();

        // when
        long count = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThan(
                room1, lastReadId
        );

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Custom: countByChatRoomAndCreatedAtAfter (시간 기준 카운트)")
    void countByChatRoomAndCreatedAtAfterTest() {
        // given
        // [FIX 2]로 인해 userJoinedAt이 msg_sys_room1 이후임이 보장됨
        // userJoinedAt 이후: msg1, msg2, msg3_del, msg4 (총 4개)

        // when
        long count = chatMessageRepository.countByChatRoomAndCreatedAtAfter(
                room1, userJoinedAt
        );

        // then
        // 이 쿼리는 isDeleted를 필터링하지 않음 (기대값 4L)
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Custom: countByChatRoomAndCreatedAtAfter (시간 기준 카운트 0)")
    void countByChatRoomAndCreatedAtAfterZeroTest() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);

        // when
        long count = chatMessageRepository.countByChatRoomAndCreatedAtAfter(
                room1, futureTime
        );

        // then
        assertThat(count).isEqualTo(0);
    }
}