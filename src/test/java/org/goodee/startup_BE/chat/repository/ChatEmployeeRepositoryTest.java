package org.goodee.startup_BE.chat.repository;

import org.goodee.startup_BE.chat.entity.ChatEmployee;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
// ChatEmployee가 의존하는 모든 엔티티(CommonCode, Employee, ChatRoom, ChatMessage)를 스캔
@EntityScan(basePackages = "org.goodee.startup_BE")
class ChatEmployeeRepositoryTest {

    @Autowired
    private ChatEmployeeRepository chatEmployeeRepository;

    // 의존성 주입: ChatEmployee 생성에 필요한 엔티티들의 Repository
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- 테스트용 공통 데이터 ---
    private Employee admin, user1, user2;
    private ChatRoom room1, room2;
    private ChatMessage msg_room1_sys, msg_room2_sys; // 각 방의 '최초' 메시지 (lastRead용)
    private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    // --- 테스트용 ChatEmployee 인스턴스 ---
    private ChatEmployee ce_user1_room1, ce_user2_room1, ce_user1_room2;


    @BeforeEach
    void setUp() {
        // H2 DB 초기화 (자식 테이블부터 삭제)
        chatEmployeeRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given 1: 공통 코드 데이터 생성 ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        deptHr = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);

        commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior));

        // --- given 2: 직원 데이터 생성 (admin, user1, user2) ---
        admin = createAndSaveEmployee("admin", "admin@test.com", roleAdmin, deptHr);
        user1 = createAndSaveEmployee("user1", "user1@test.com", roleUser, deptDev);
        user2 = createAndSaveEmployee("user2", "user2@test.com", roleUser, deptDev);

        // --- given 3: 채팅방 데이터 생성 (room1, room2) ---
        room1 = chatRoomRepository.save(ChatRoom.createChatRoom(admin, "개발팀 단체방", true));
        room2 = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1, user2 1:1방", false));

        // --- given 4: '최초' 시스템 메시지 생성 (lastReadMessage의 non-null 제약조건 충족용) ---
        // ChatEmployee.java의 lastReadMessage 필드는 nullable=false
        msg_room1_sys = chatMessageRepository.save(
                ChatMessage.createChatMessage(room1, null, "개발팀 단체방이 생성되었습니다.")
        );
        msg_room2_sys = chatMessageRepository.save(
                ChatMessage.createChatMessage(room2, null, "1:1 채팅방이 생성되었습니다.")
        );

        // --- given 5: 테스트용 ChatEmployee 데이터 미리 생성 (커스텀 쿼리 테스트용) ---
        ce_user1_room1 = chatEmployeeRepository.save(
                createPersistableChatEmployee(user1, room1, "개발팀 단체방", msg_room1_sys)
        );
        ce_user2_room1 = chatEmployeeRepository.save(
                createPersistableChatEmployee(user2, room1, "개발팀 단체방", msg_room1_sys)
        );
        ce_user1_room2 = chatEmployeeRepository.save(
                createPersistableChatEmployee(user1, room2, "user2", msg_room2_sys)
        );
    }

    /**
     * EmployeeRepositoryTest의 헬퍼 메서드와 유사하게,
     * Employee 엔티티를 생성하고 즉시 '저장(save)'한 뒤 반환하는 헬퍼
     */
    private Employee createAndSaveEmployee(String username, String email, CommonCode role, CommonCode dept) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, posJunior,
                null // admin 생성시는 creator가 null
        );
        employee.updateInitPassword(TEST_PASSWORD, null);
        return employeeRepository.save(employee);
    }

    /**
     * ChatEmployee 엔티티를 '저장 가능한(persistable)' 상태로 반환하는 헬퍼 메서드
     */
    private ChatEmployee createPersistableChatEmployee(Employee employee, ChatRoom room, String displayName, ChatMessage lastReadMsg) {
        // ChatEmployee.java의 팩토리 메서드 사용
        return ChatEmployee.createChatEmployee(employee, room, displayName, lastReadMsg);
    }

    // --- CRUD Tests ---

    @Test
    @DisplayName("C: 채팅방 참여(save) 테스트")
    void saveChatEmployeeTest() {
        // given
        // user2가 room2에 참여하는 시나리오 (user1은 @BeforeEach에서 이미 참여함)
        ChatEmployee ce_user2_room2 = createPersistableChatEmployee(user2, room2, "user1", msg_room2_sys);

        // when
        ChatEmployee savedCe = chatEmployeeRepository.save(ce_user2_room2);

        // then
        assertThat(savedCe).isNotNull();
        assertThat(savedCe.getChatEmployeeId()).isNotNull();
        assertThat(savedCe.getEmployee()).isEqualTo(user2);
        assertThat(savedCe.getChatRoom()).isEqualTo(room2);
        assertThat(savedCe.getLastReadMessage()).isEqualTo(msg_room2_sys);
        assertThat(savedCe.getIsLeft()).isFalse(); // ChatEmployee.java 기본값
        assertThat(savedCe.getIsNotify()).isTrue(); // ChatEmployee.java 기본값
        assertThat(savedCe.getJoinedAt()).isNotNull(); // @PrePersist 동작 확인
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given (@BeforeEach에서 ce_user1_room1이 저장됨)
        Long targetId = ce_user1_room1.getChatEmployeeId();

        // when
        Optional<ChatEmployee> foundCe = chatEmployeeRepository.findById(targetId);

        // then
        assertThat(foundCe).isPresent();
        assertThat(foundCe.get().getEmployee().getUsername()).isEqualTo("user1");
        assertThat(foundCe.get().getChatRoom().getName()).isEqualTo("개발팀 단체방");
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<ChatEmployee> foundCe = chatEmployeeRepository.findById(nonExistentId);

        // then
        assertThat(foundCe).isNotPresent();
    }

    @Test
    @DisplayName("U: 채팅방 참여 정보 수정(update) 테스트 - 이름 변경, 나가기")
    void updateChatEmployeeTest() {
        // given (@BeforeEach에서 ce_user1_room1이 저장됨)
        Long targetId = ce_user1_room1.getChatEmployeeId();

        // when
        // 영속성 컨텍스트에서 엔티티를 가져옴
        ChatEmployee employeeToUpdate = chatEmployeeRepository.findById(targetId).get();

        // 엔티티의 update 메서드 호출 (JPA 변경 감지)
        employeeToUpdate.changedDisplayName("My Custom Room Name"); //
        employeeToUpdate.leftChatRoom(); //
        employeeToUpdate.disableNotify(); //

        // 변경 감지를 테스트하기 위해 flush() 호출
        chatEmployeeRepository.flush();

        // then
        // 변경된 엔티티를 다시 조회하여 검증
        ChatEmployee updatedEmployee = chatEmployeeRepository.findById(targetId).get();
        assertThat(updatedEmployee.getDisplayName()).isEqualTo("My Custom Room Name");
        assertThat(updatedEmployee.getIsLeft()).isTrue();
        assertThat(updatedEmployee.getIsNotify()).isFalse();
    }

    @Test
    @DisplayName("D: 채팅방 참여 정보 삭제(deleteById) 테스트")
    void deleteChatEmployeeTest() {
        // given (@BeforeEach에서 ce_user1_room1이 저장됨)
        Long targetId = ce_user1_room1.getChatEmployeeId();
        assertThat(chatEmployeeRepository.existsById(targetId)).isTrue();

        // when
        chatEmployeeRepository.deleteById(targetId);
        chatEmployeeRepository.flush(); // 삭제 쿼리 즉시 실행

        // then
        assertThat(chatEmployeeRepository.existsById(targetId)).isFalse();
    }

    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(employee) null 저장 시 예외 발생")
    void saveNullEmployeeTest() {
        // given
        // ChatEmployee.java의 employee 필드는 nullable=false
        ChatEmployee ce = createPersistableChatEmployee(null, room1, "Room 1", msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(chatRoom) null 저장 시 예외 발생")
    void saveNullChatRoomTest() {
        // given
        // ChatEmployee.java의 chatRoom 필드는 nullable=false
        ChatEmployee ce = createPersistableChatEmployee(user1, null, "Room 1", msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(lastReadMessage) null 저장 시 예외 발생")
    void saveNullLastReadMessageTest() {
        // given
        // ChatEmployee.java의 lastReadMessage 필드는 nullable=false
        ChatEmployee ce = createPersistableChatEmployee(user1, room1, "Room 1", null);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(displayName) null 저장 시 예외 발생")
    void saveNullDisplayNameTest() {
        // given
        // ChatEmployee.java의 displayName 필드는 nullable=false
        ChatEmployee ce = createPersistableChatEmployee(user1, room1, null, msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Custom Repository Method Tests ---

    @Test
    @DisplayName("Custom: findAllByEmployeeAndIsLeftFalse 테스트")
    void findAllByEmployeeAndIsLeftFalseTest() {
        // given (@BeforeEach에서 user1은 room1, room2에 참여)
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> roomsForUser1 = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user1); //
        List<ChatEmployee> roomsForUser2 = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user2); //

        // then
        // user1은 room1을 나갔으므로 room2만 조회되어야 함
        assertThat(roomsForUser1).hasSize(1);
        assertThat(roomsForUser1).contains(ce_user1_room2);
        assertThat(roomsForUser1).doesNotContain(ce_user1_room1);

        // user2는 room1에만 있으므로 room1만 조회되어야 함
        assertThat(roomsForUser2).hasSize(1);
        assertThat(roomsForUser2).contains(ce_user2_room1);
    }

    @Test
    @DisplayName("Custom: findAllByChatRoomChatRoomIdAndIsLeftFalse 테스트")
    void findAllByChatRoomChatRoomIdAndIsLeftFalseTest() {
        // given (@BeforeEach에서 user1, user2가 room1에 참여)
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> membersInRoom1 = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId()); //

        // then
        // room1의 활성 멤버는 user2 뿐이어야 함
        assertThat(membersInRoom1).hasSize(1);
        assertThat(membersInRoom1).contains(ce_user2_room1);
        assertThat(membersInRoom1).doesNotContain(ce_user1_room1);
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeEmployeeId 테스트")
    void findByChatRoomChatRoomIdAndEmployeeEmployeeIdTest() {
        // given
        Long roomId = room1.getChatRoomId();
        Long user1Id = user1.getEmployeeId();
        Long user2Id = user2.getEmployeeId();

        // when
        Optional<ChatEmployee> foundUser1 = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, user1Id); //
        Optional<ChatEmployee> notFoundUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room2.getChatRoomId(), user2Id); //

        // then
        assertThat(foundUser1).isPresent();
        assertThat(foundUser1.get()).isEqualTo(ce_user1_room1);
        assertThat(notFoundUser).isNotPresent();
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse 테스트")
    void findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalseTest() {
        // given
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        // user1은 나갔으므로 조회되면 안 됨 (isLeft=false 조건)
        Optional<ChatEmployee> leftUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), "user1"); //

        // user2는 활성 상태이므로 조회되어야 함
        Optional<ChatEmployee> activeUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), "user2"); //

        // then
        assertThat(leftUser).isNotPresent();
        assertThat(activeUser).isPresent();
        assertThat(activeUser.get()).isEqualTo(ce_user2_room1);
    }

    @Test
    @DisplayName("Custom: existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse 테스트")
    void existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalseTest() {
        // given
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        boolean user1Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room1.getChatRoomId(), user1.getEmployeeId()); //
        boolean user2Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room1.getChatRoomId(), user2.getEmployeeId()); //
        boolean nonMemberExists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room2.getChatRoomId(), user2.getEmployeeId()); //

        // then
        assertThat(user1Exists).isFalse(); // isLeft=false 조건 불충족
        assertThat(user2Exists).isTrue();
        assertThat(nonMemberExists).isFalse();
    }

    @Test
    @DisplayName("Custom: findActiveEmployeeIdsByRoomId 테스트")
    void findActiveEmployeeIdsByRoomIdTest() {
        // given
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        // room1의 활성 멤버 ID 조회
        Set<Long> activeIdsInRoom1 = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room1.getChatRoomId()); //

        // room2의 활성 멤버 ID 조회
        Set<Long> activeIdsInRoom2 = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room2.getChatRoomId()); //

        // then
        // room1에는 user2 ID만 있어야 함
        assertThat(activeIdsInRoom1).hasSize(1);
        assertThat(activeIdsInRoom1).containsOnly(user2.getEmployeeId());

        // room2에는 user1 ID만 있어야 함
        assertThat(activeIdsInRoom2).hasSize(1);
        assertThat(activeIdsInRoom2).containsOnly(user1.getEmployeeId());
    }

    @Test
    @DisplayName("Custom: countByChatRoomChatRoomIdAndIsLeftFalse 테스트")
    void countByChatRoomChatRoomIdAndIsLeftFalseTest() {
        // given
        // user1이 room1을 나간(left) 상황을 추가
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        long countRoom1 = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId()); //
        long countRoom2 = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room2.getChatRoomId()); //

        // then
        assertThat(countRoom1).isEqualTo(1); // user2만
        assertThat(countRoom2).isEqualTo(1); // user1만
    }

    @Test
    @DisplayName("Custom: countUnreadForMessage 테스트")
    void countUnreadForMessageTest() throws InterruptedException {
        // given
        // @BeforeEach 상태: user1, user2가 room1에 참여.
        // 둘 다 마지막 읽은 메시지(lrm)는 'msg_room1_sys'
        LocalDateTime lrmCreatedAt = msg_room1_sys.getCreatedAt();

        // 잠시 대기하여 createdAt 시간차 보장
        Thread.sleep(10);

        // user1이 새 메시지(newMsg)를 보냄
        ChatMessage newMsg = chatMessageRepository.save(
                ChatMessage.createChatMessage(room1, user1, "Hello!")
        );
        LocalDateTime newMsgCreatedAt = newMsg.getCreatedAt();

        // when
        // 이 새 메시지(newMsg)에 대해 안 읽은 사람 수를 카운트
        long unreadCount = chatEmployeeRepository.countUnreadForMessage( //
                room1.getChatRoomId(),
                user1.getEmployeeId(), // senderId
                newMsgCreatedAt
        );

        // then
        // 쿼리 조건: senderId(user1)가 아니고, isLeft=false(user2)이고,
        // lrm.createdAt(lrmCreatedAt) < newMsgCreatedAt
        // -> user2가 해당됨
        assertThat(unreadCount).isEqualTo(1);

        // given 2
        // user2가 이 메시지를 읽음 (lastReadMessage 업데이트)
        ce_user2_room1.updateLastReadMessage(newMsg);
        chatEmployeeRepository.save(ce_user2_room1);

        // when 2
        // 다시 카운트
        long unreadCountAfterRead = chatEmployeeRepository.countUnreadForMessage( //
                room1.getChatRoomId(),
                user1.getEmployeeId(),
                newMsgCreatedAt
        );

        // then 2
        // user2의 lrm.createdAt(newMsgCreatedAt)이 newMsgCreatedAt보다 작지 않으므로
        // 카운트에서 제외됨
        assertThat(unreadCountAfterRead).isEqualTo(0);
    }

    @Test
    @DisplayName("Custom: sumTotalUnreadMessagesByEmployeeId 테스트")
    void sumTotalUnreadMessagesByEmployeeIdTest() {
        // given
        // @BeforeEach 상태:
        // user1은 room1, room2에 참여 중
        // (room1) ce_user1_room1.lastReadMessage = msg_room1_sys
        // (room2) ce_user1_room2.lastReadMessage = msg_room2_sys

        // when
        // 아직 새 메시지가 없으므로 0이어야 함
        long initialUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId()); //
        assertThat(initialUnread).isEqualTo(0);

        // given 2: 새 메시지 추가
        // Room 1:
        // 1. user1이 보낸 메시지 (자신이 보낸 것은 카운트 X)
        chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "My own message"));
        // 2. user2가 보낸 메시지 (안 읽음 1)
        chatMessageRepository.save(ChatMessage.createChatMessage(room1, user2, "Message from user2 (R1)"));
        // Room 2:
        // 3. user1이 보낸 메시지 (카운트 X)
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, user1, "My own message 2"));
        // 4. 시스템 메시지 (안 읽음 2)
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, null, "System message (R2)"));

        // when 2
        long totalUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId()); //

        // then 2
        // room1(1개) + room2(1개) = 2개
        assertThat(totalUnread).isEqualTo(2);

        // given 3: user1이 room1을 나감
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when 3
        long totalUnreadAfterLeft = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId()); //

        // then 3
        // isLeft=false 조건에 따라 room1은 집계에서 제외됨
        // room2의 안 읽은 메시지 1개만 카운트됨
        assertThat(totalUnreadAfterLeft).isEqualTo(1);
    }
}