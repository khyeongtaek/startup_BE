package org.goodee.startup_BE.chat.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
class ChatEmployeeRepositoryTest {

    @Autowired
    private ChatEmployeeRepository chatEmployeeRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EntityManager entityManager;

    private Employee user1, user2, user3_left;
    private ChatRoom room1;
    private ChatMessage msg1_system, msg2_user1;
    private ChatEmployee ce1, ce2, ce3;

    @BeforeEach
    void setUp() {
        // DB 초기화
        chatEmployeeRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 ---
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);
        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior));

        // --- given: Employee 생성 ---
        user1 = createPersistableEmployee("user1", "user1@test.com", roleUser, deptDev, posJunior, null);
        user2 = createPersistableEmployee("user2", "user2@test.com", roleUser, deptDev, posJunior, null);
        user3_left = createPersistableEmployee("user3_left", "user3@test.com", roleUser, deptDev, posJunior, null);
        employeeRepository.saveAll(List.of(user1, user2, user3_left));

        // --- given: ChatRoom 생성 ---
        room1 = ChatRoom.createChatRoom(user1, "테스트 채팅방", true);
        chatRoomRepository.save(room1);

        // --- given: 1. 방 생성 (시스템 메시지) ---
        msg1_system = chatMessageRepository.save(ChatMessage.createChatMessage(room1, null, "방 생성"));

        // --- given: 2. 직원들 방에 참여 ---
        // * 중요: 직원들은 msg1_system을 기본 lastReadMessage로 가지고 참여합니다.
        ce1 = ChatEmployee.createChatEmployee(user1, room1, "User1 Room", msg1_system);
        ce2 = ChatEmployee.createChatEmployee(user2, room1, "User2 Room", msg1_system);
        ce3 = ChatEmployee.createChatEmployee(user3_left, room1, "User3 Room", msg1_system);

        // ce3는 나가기 처리
        ce3.leftChatRoom();

        chatEmployeeRepository.saveAll(List.of(ce1, ce2, ce3));

        // * 중요: save가 호출되며 @PrePersist로 ce1, ce2, ce3의 joinedAt이 설정됩니다.
        // 다음 메시지(msg2)가 joinedAt 이후에 생성되도록 flush(DB 반영)를 합니다.
        entityManager.flush();
        // @PrePersist는 flush 시점이 아닌 save 시점에 호출되므로,
        // 혹시 모를 시간차를 위해 sleep을 추가합니다. (DB 트랜잭션 시간차 고려)
        try { Thread.sleep(10); } catch (Exception e) {}

        // --- given: 3. user1이 새 메시지 전송 ---
        msg2_user1 = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "메시지 1"));

        // --- given: 4. 메시지를 보낸 user1은 해당 메시지를 바로 읽음 처리 ---
        ce1.updateLastReadMessage(msg2_user1);
        chatEmployeeRepository.save(ce1);

        // 최종 상태:
        // user1: 활성, msg2_user1 읽음 (joinedAt < msg2_user1.createdAt)
        // user2: 활성, msg1_system 읽음 (joinedAt < msg2_user1.createdAt) -> msg2 안 읽음
        // user3: 나감, msg1_system 읽음
    }

    /** 테스트용 직원 생성 헬퍼 */
    private Employee createPersistableEmployee(String username, String email, CommonCode role, CommonCode dept, CommonCode pos, Employee creator) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), role, role, dept, pos,
                creator
        );
        employee.updateInitPassword("testPassword123!", creator);
        return employee;
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeEmployeeId 테스트")
    void findByRoomAndEmployeeIdTest() {
        // when
        Optional<ChatEmployee> found = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room1.getChatRoomId(), user1.getEmployeeId());
        Optional<ChatEmployee> notFound = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room1.getChatRoomId(), 999L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmployee()).isEqualTo(user1);
        assertThat(notFound).isNotPresent();
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse 테스트")
    void findByRoomAndUsernameAndNotLeftTest() {
        // when
        // user1 (활성) -> 조회 성공
        Optional<ChatEmployee> found = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), user1.getUsername());

        // user3_left (나감) -> 조회 실패
        Optional<ChatEmployee> notFound_left = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), user3_left.getUsername());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmployee()).isEqualTo(user1);
        assertThat(notFound_left).isNotPresent();
    }

    @Test
    @DisplayName("Custom: find...을 사용한 존재 여부 테스트 (isDeleted 속성 문제 수정)")
    void existsByRoomAndEmployeeIdTest() {
        // given
        // 원본 테스트의 existsBy...AndIsDeletedFalse 메서드는
        // ChatEmployee 엔티티에 isDeleted 필드가 없어 PropertyReferenceException을 유발합니다.
        // 따라서 이미 존재하는 findBy... 메서드를 사용하여 동일한 "존재" 여부를 테스트합니다.

        // when
        boolean exists = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room1.getChatRoomId(), user1.getEmployeeId()).isPresent();
        boolean notExists = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room1.getChatRoomId(), 999L).isPresent();

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Custom: findActiveEmployeeIdsByRoomId (@Query) 테스트")
    void findActiveEmployeeIdsByRoomIdTest() {
        // when
        Set<Long> activeIds = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room1.getChatRoomId());

        // then
        // user1, user2만 포함 (user3_left 제외)
        assertThat(activeIds).hasSize(2);
        assertThat(activeIds).containsExactlyInAnyOrder(user1.getEmployeeId(), user2.getEmployeeId());
    }

    @Test
    @DisplayName("Custom: countByChatRoomChatRoomIdAndIsLeftFalse 테스트")
    void countByRoomAndNotLeftTest() {
        // when
        long count = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId());

        // then
        // user1, user2 (user3_left 제외)
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Custom: countUnreadForMessage (@Query) 테스트")
    void countUnreadForMessageTest() {
        // given
        // (setUp에서 재구성됨)
        // user1 (보낸 사람) -> msg2_user1 읽음
        // user2 -> msg1_system 읽음 (msg2_user1 안 읽음)
        // user3_left -> 나감 (카운트 제외)

        // when
        // msg2_user1를 기준으로, 보낸 사람(user1)을 제외하고, 나가지 않은(user2) 사람 중
        // (joinedAt <= msg2.createdAt) 이고 (lastReadMessage.createdAt < msg2.createdAt) 인 사람
        long unreadCount = chatEmployeeRepository.countUnreadForMessage(
                room1.getChatRoomId(),
                user1.getEmployeeId(),
                msg2_user1.getCreatedAt()
        );

        // then
        // user2는 msg1_system을 마지막으로 읽었으므로, msg2_user1은 읽지 않은 상태.
        // user2의 joinedAt은 msg2_user1의 createdAt보다 이전.
        // -> 1명 (user2)
        assertThat(unreadCount).isEqualTo(1);

        // when (msg1_system 기준)
        // msg1_system을 기준으로, 보낸 사람(null, -1L로 임의 처리)을 제외하고,
        // 나가지 않은(user1, user2) 사람 중 안 읽은 사람
        long unreadCountForMsg1 = chatEmployeeRepository.countUnreadForMessage(
                room1.getChatRoomId(),
                -1L, // 시스템 메시지(null) 보낸 사람 ID (임의의 값)
                msg1_system.getCreatedAt()
        );

        // then
        // user1은 msg2를 읽었고, user2는 msg1을 읽었음.
        // 둘 다 (lastReadMessage.createdAt < msg1.createdAt) 조건을 만족하지 못함.
        // -> 0명
        assertThat(unreadCountForMsg1).isEqualTo(0);
    }
}