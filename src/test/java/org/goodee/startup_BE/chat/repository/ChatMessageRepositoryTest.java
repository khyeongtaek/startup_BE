package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
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

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- 테스트 데이터 ---
    private Employee admin, user1, user2;
    private ChatRoom room1;
    private CommonCode statusActive, roleUser, roleAdmin, deptDev, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    // --- 테스트용 메시지 인스턴스 (시간 순서대로 저장됨) ---
    private ChatMessage msg1_early;
    private ChatMessage msg2_mid;
    private ChatMessage msg3_sys; // 시스템 메시지 (Employee Null)
    private ChatMessage msg4_late;
    private ChatMessage msg5_deleted; // 삭제된 메시지

    @BeforeEach
    void setUp() throws InterruptedException {
        // 1. 초기화 (FK 역순 삭제)
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // 2. CommonCode 생성 (Employee 의존성)
        statusActive = createAndSaveCode("STATUS_ACTIVE", "재직", "ACTIVE", 1L);
        roleAdmin = createAndSaveCode("ROLE_ADMIN", "관리자", "ADMIN", 1L);
        roleUser = createAndSaveCode("ROLE_USER", "사용자", "USER", 2L);
        deptDev = createAndSaveCode("DEPT_DEV", "개발팀", "DEV", 1L);
        posJunior = createAndSaveCode("POS_JUNIOR", "사원", "JUNIOR", 1L);

        // 3. Employee 생성
        admin = createAndSaveEmployee("admin", "admin@test.com", roleAdmin);
        user1 = createAndSaveEmployee("user1", "user1@test.com", roleUser);
        user2 = createAndSaveEmployee("user2", "user2@test.com", roleUser);

        // 4. ChatRoom 생성
        room1 = chatRoomRepository.save(ChatRoom.createChatRoom(admin, "개발팀 챗방", true));

        // 5. ChatMessage 생성 (시간차를 두어 순서 보장)
        // T1
        msg1_early = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "첫번째 메시지"));
        Thread.sleep(20);

        // T2
        msg2_mid = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user2, "두번째 메시지"));
        Thread.sleep(20);

        // T3 (System Message)
        msg3_sys = chatMessageRepository.save(ChatMessage.createSystemMessage(room1, "시스템 알림: 공지"));
        Thread.sleep(20);

        // T4
        msg4_late = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "네번째 메시지(최신)"));
        Thread.sleep(20);

        // T5 (Deleted)
        ChatMessage tempDeleted = ChatMessage.createChatMessage(room1, user2, "삭제될 메시지");
        tempDeleted.deleteChatMessage(); // isDeleted = true 설정
        msg5_deleted = chatMessageRepository.save(tempDeleted);
    }

    // --- Helper Methods ---
    private CommonCode createAndSaveCode(String code, String name, String val1, Long seq) {
        CommonCode c = CommonCode.createCommonCode(code, name, val1, null, null, seq, null, false);
        return commonCodeRepository.save(c);
    }

    private Employee createAndSaveEmployee(String username, String email, CommonCode role) {
        Employee e = Employee.createEmployee(username, "이름", email, "010-0000-0000", LocalDate.now(),
                statusActive, role, deptDev, posJunior, null);
        e.updateInitPassword(TEST_PASSWORD, null);
        return employeeRepository.save(e);
    }

    // --- CRUD & Basic Logic Tests ---

    @Test
    @DisplayName("C: 사용자 메시지 저장 테스트")
    void saveUserMessageTest() {
        // given
        ChatMessage newMessage = ChatMessage.createChatMessage(room1, user1, "새 메시지");

        // when
        ChatMessage saved = chatMessageRepository.save(newMessage);

        // then
        assertThat(saved.getChatMessageId()).isNotNull();
        assertThat(saved.getContent()).isEqualTo("새 메시지");
        assertThat(saved.getEmployee()).isEqualTo(user1);
        assertThat(saved.getMessageType().name()).isEqualTo(OwnerType.CHAT_USER.name());
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("C: 시스템 메시지 저장 테스트 (Employee Null 확인)")
    void saveSystemMessageTest() {
        // given
        ChatMessage sysMsg = ChatMessage.createSystemMessage(room1, "방 생성");

        // when
        ChatMessage saved = chatMessageRepository.save(sysMsg);

        // then
        assertThat(saved.getEmployee()).isNull(); // 시스템 메시지는 직원이 없음
        assertThat(saved.getMessageType().name()).isEqualTo(OwnerType.CHAT_SYSTEM.name());
        assertThat(saved.getContent()).isEqualTo("방 생성");
    }

    @Test
    @DisplayName("R: ID로 조회 테스트")
    void findByIdTest() {
        // given
        Long id = msg1_early.getChatMessageId();

        // when
        Optional<ChatMessage> found = chatMessageRepository.findById(id);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo(msg1_early.getContent());
    }

    @Test
    @DisplayName("U: 소프트 삭제(Soft Delete) 테스트")
    void softDeleteTest() {
        // given
        ChatMessage target = chatMessageRepository.findById(msg1_early.getChatMessageId()).orElseThrow();

        // when
        target.deleteChatMessage(); // 엔티티 메서드 호출
        chatMessageRepository.saveAndFlush(target);

        // then
        ChatMessage updated = chatMessageRepository.findById(msg1_early.getChatMessageId()).orElseThrow();
        assertThat(updated.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("D: 하드 삭제(Hard Delete) 테스트")
    void hardDeleteTest() {
        // given
        Long id = msg4_late.getChatMessageId();

        // when
        chatMessageRepository.deleteById(id);
        chatMessageRepository.flush();

        // then
        assertThat(chatMessageRepository.existsById(id)).isFalse();
    }

    // --- Exception Tests ---

    @Test
    @DisplayName("Exception: 내용(Content)이 없으면 예외 발생")
    void saveNullContentTest() {
        // ChatMessage.java: @Column(nullable = false) content
        // given
        ChatMessage invalidMsg = ChatMessage.createChatMessage(room1, user1, null);

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.save(invalidMsg))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 채팅방(ChatRoom)이 없으면 예외 발생")
    void saveNullRoomTest() {
        // ChatMessage.java: @JoinColumn(nullable = false) chatRoom
        // given
        ChatMessage invalidMsg = ChatMessage.createChatMessage(null, user1, "내용");

        // when & then
        assertThatThrownBy(() -> chatMessageRepository.save(invalidMsg))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 사용자 메시지에 Employee가 없으면 IllegalArgumentException (팩토리 메서드 검증)")
    void saveUserMessageWithoutEmployeeTest() {
        // ChatMessage.createChatMessage 내부 검증 로직
        assertThatThrownBy(() -> ChatMessage.createChatMessage(room1, null, "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자 정보가 필수");
    }

    // --- Custom Query Tests ---

    @Test
    @DisplayName("Query: findBy...CreatedAtAfterAndIsDeletedFalse... (페이지네이션+필터링)")
    void findActiveMessagesAfterJoinTest() {
        // given
        // msg1_early의 시간보다 아주 조금 뒤를 "참여 시간"으로 설정
        // 예상 결과: msg1(제외-시간이전), msg5(제외-삭제됨), 남은 것: msg4, msg3, msg2
        LocalDateTime joinedAt = msg1_early.getCreatedAt().plusNanos(1000);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<ChatMessage> result = chatMessageRepository.findByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndIsDeletedFalseOrderByCreatedAtDesc(
                room1.getChatRoomId(), joinedAt, pageable
        );

        // then
        List<ChatMessage> content = result.getContent();
        assertThat(content).hasSize(3); // msg4, msg3, msg2
        assertThat(content.get(0)).isEqualTo(msg4_late); // 최신순 정렬 확인
        assertThat(content.get(1)).isEqualTo(msg3_sys);
        assertThat(content.get(2)).isEqualTo(msg2_mid);
        assertThat(content).doesNotContain(msg1_early, msg5_deleted);
    }

    @Test
    @DisplayName("Query: findTop...OrderByCreatedAtDesc (최신 메시지 조회)")
    void findTopMessageTest() {
        // given
        // joinedAt을 아주 과거로 설정하여 모든 메시지 대상
        LocalDateTime joinedAt = LocalDateTime.now().minusYears(1);

        // when
        Optional<ChatMessage> topMsg = chatMessageRepository.findTopByChatRoomAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                room1, joinedAt
        );

        // then
        // msg5는 삭제되었지만 이 쿼리는 isDeleted 조건을 메서드 이름에 명시하지 않았으므로 가장 최신인 msg5가 나옴
        assertThat(topMsg).isPresent();
        assertThat(topMsg.get()).isEqualTo(msg5_deleted);
    }

    @Test
    @DisplayName("Query: count...GreaterThanAndEmployeeIsNotNull (ID 기준 안 읽은 메시지 수, 시스템 메시지 제외)")
    void countUnreadByIdExcludeSystemTest() {
        // given
        // 마지막 읽은 메시지가 msg1이라고 가정
        Long lastReadId = msg1_early.getChatMessageId();

        // 남은 메시지 ID 순서: msg2(User), msg3(Sys), msg4(User), msg5(User-Del)
        // 쿼리 조건: ID > lastReadId AND Employee Is Not Null
        // msg3(Sys)는 Employee가 Null이므로 제외되어야 함.
        // msg5(Del)는 Employee가 있으므로 포함됨 (삭제 여부 조건 없음)

        // when
        long count = chatMessageRepository.countByChatRoomAndChatMessageIdGreaterThanAndEmployeeIsNotNull(
                room1, lastReadId
        );

        // then
        // msg2, msg4, msg5 -> 총 3개 예상
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Query: count...CreatedAtAfterAndEmployeeIsNotNull (시간 기준 안 읽은 메시지 수, 시스템 메시지 제외)")
    void countUnreadByTimeExcludeSystemTest() {
        // given
        // msg1 생성 직후 입장
        LocalDateTime joinedAt = msg1_early.getCreatedAt().plusNanos(1000);

        // 대상: msg2(User), msg3(Sys), msg4(User), msg5(User-Del)
        // EmployeeIsNotNull 조건으로 msg3 제외

        // when
        long count = chatMessageRepository.countByChatRoomAndCreatedAtGreaterThanEqualAndEmployeeIsNotNull(
                room1, joinedAt
        );

        // then
        // msg2, msg4, msg5 -> 3개
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Query: findAllBy...CreatedAtLessThanEqual... (기간 조회)")
    void findMessagesBetweenDatesTest() {
        // given
        // 범위: msg2 ~ msg4 (msg2 포함, msg4 포함)

        // [수정] H2 DB 정밀도 문제 방지:
        // msg2보다 10ms 전부터, msg4보다 10ms 후까지로 범위 설정 (메시지 간격은 20ms이므로 안전함)
        LocalDateTime afterTime = msg2_mid.getCreatedAt().minusNanos(10_000_000); // -10ms
        LocalDateTime untilTime = msg4_late.getCreatedAt().plusNanos(10_000_000); // +10ms

        // when
        List<ChatMessage> msgs = chatMessageRepository.findAllByChatRoomChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                room1.getChatRoomId(), afterTime, untilTime
        );

        // then
        // 예상: msg2, msg3, msg4
        // (msg1은 범위 밖, msg5는 untilTime보다 뒤라서 제외)
        assertThat(msgs).hasSize(3);
        assertThat(msgs).containsExactly(msg2_mid, msg3_sys, msg4_late); // 오름차순 정렬
    }
}