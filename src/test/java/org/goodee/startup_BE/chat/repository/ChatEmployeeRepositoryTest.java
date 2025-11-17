package org.goodee.startup_BE.chat.repository;

import jakarta.persistence.EntityManager;
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
    private EmployeeRepository employeeRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EntityManager entityManager;

    private Employee admin, user1, user2;
    private ChatRoom room1, room2;
    private ChatMessage msg_room1_sys, msg_room2_sys;
    private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    private ChatEmployee ce_user1_room1, ce_user2_room1, ce_user1_room2;


    @BeforeEach
    void setUp() {
        chatEmployeeRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null, false);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        deptHr = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null, false);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior));

        admin = createAndSaveEmployee("admin", "admin@test.com", roleAdmin, deptHr);
        user1 = createAndSaveEmployee("user1", "user1@test.com", roleUser, deptDev);
        user2 = createAndSaveEmployee("user2", "user2@test.com", roleUser, deptDev);

        room1 = chatRoomRepository.save(ChatRoom.createChatRoom(admin, "개발팀 단체방", true));
        room2 = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1, user2 1:1방", false));

        msg_room1_sys = chatMessageRepository.save(
                ChatMessage.createChatMessage(room1, null, "개발팀 단체방이 생성되었습니다.")
        );
        msg_room2_sys = chatMessageRepository.save(
                ChatMessage.createChatMessage(room2, null, "1:1 채팅방이 생성되었습니다.")
        );

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

    private Employee createAndSaveEmployee(String username, String email, CommonCode role, CommonCode dept) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, posJunior,
                null
        );
        employee.updateInitPassword(TEST_PASSWORD, null);
        return employeeRepository.save(employee);
    }

    private ChatEmployee createPersistableChatEmployee(Employee employee, ChatRoom room, String displayName, ChatMessage lastReadMsg) {
        return ChatEmployee.createChatEmployee(employee, room, displayName, lastReadMsg);
    }

    // --- (CRUD, Exception,
    //      findAllBy..., findBy..., existsBy..., findActive..., countBy...
    //      테스트는 모두 동일) ---
    @Test
    @DisplayName("C: 채팅방 참여(save) 테스트")
    void saveChatEmployeeTest() {
        // given
        ChatEmployee ce_user2_room2 = createPersistableChatEmployee(user2, room2, "user1", msg_room2_sys);

        // when
        ChatEmployee savedCe = chatEmployeeRepository.save(ce_user2_room2);

        // then
        assertThat(savedCe).isNotNull();
        assertThat(savedCe.getChatEmployeeId()).isNotNull();
        assertThat(savedCe.getEmployee()).isEqualTo(user2);
        assertThat(savedCe.getChatRoom()).isEqualTo(room2);
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        Long targetId = ce_user1_room1.getChatEmployeeId();

        // when
        Optional<ChatEmployee> foundCe = chatEmployeeRepository.findById(targetId);

        // then
        assertThat(foundCe).isPresent();
        assertThat(foundCe.get().getEmployee().getUsername()).isEqualTo("user1");
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
        // given
        Long targetId = ce_user1_room1.getChatEmployeeId();

        // when
        ChatEmployee employeeToUpdate = chatEmployeeRepository.findById(targetId).get();
        employeeToUpdate.changedDisplayName("My Custom Room Name");
        employeeToUpdate.leftChatRoom();
        employeeToUpdate.disableNotify();
        chatEmployeeRepository.flush();

        // then
        ChatEmployee updatedEmployee = chatEmployeeRepository.findById(targetId).get();
        assertThat(updatedEmployee.getDisplayName()).isEqualTo("My Custom Room Name");
        assertThat(updatedEmployee.getIsLeft()).isTrue();
        assertThat(updatedEmployee.getIsNotify()).isFalse();
    }

    @Test
    @DisplayName("D: 채팅방 참여 정보 삭제(deleteById) 테스트")
    void deleteChatEmployeeTest() {
        // given
        Long targetId = ce_user1_room1.getChatEmployeeId();
        assertThat(chatEmployeeRepository.existsById(targetId)).isTrue();

        // when
        chatEmployeeRepository.deleteById(targetId);
        chatEmployeeRepository.flush();

        // then
        assertThat(chatEmployeeRepository.existsById(targetId)).isFalse();
    }

    @Test
    @DisplayName("Exception: 필수 FK(employee) null 저장 시 예외 발생")
    void saveNullEmployeeTest() {
        // given
        ChatEmployee ce = createPersistableChatEmployee(null, room1, "Room 1", msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(chatRoom) null 저장 시 예외 발생")
    void saveNullChatRoomTest() {
        // given
        ChatEmployee ce = createPersistableChatEmployee(user1, null, "Room 1", msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(lastReadMessage) null 저장 시 예외 발생")
    void saveNullLastReadMessageTest() {
        // given
        ChatEmployee ce = createPersistableChatEmployee(user1, room1, "Room 1", null);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(displayName) null 저장 시 예외 발생")
    void saveNullDisplayNameTest() {
        // given
        ChatEmployee ce = createPersistableChatEmployee(user1, room1, null, msg_room1_sys);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(ce))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Custom: findAllByEmployeeAndIsLeftFalse 테스트")
    void findAllByEmployeeAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> roomsForUser1 = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user1);
        List<ChatEmployee> roomsForUser2 = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user2);

        // then
        assertThat(roomsForUser1).hasSize(1);
        assertThat(roomsForUser1).contains(ce_user1_room2);
        assertThat(roomsForUser2).hasSize(1);
        assertThat(roomsForUser2).contains(ce_user2_room1);
    }

    @Test
    @DisplayName("Custom: findAllByChatRoomChatRoomIdAndIsLeftFalse 테스트")
    void findAllByChatRoomChatRoomIdAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> membersInRoom1 = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId());

        // then
        assertThat(membersInRoom1).hasSize(1);
        assertThat(membersInRoom1).contains(ce_user2_room1);
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeEmployeeId 테스트")
    void findByChatRoomChatRoomIdAndEmployeeEmployeeIdTest() {
        // given
        Long roomId = room1.getChatRoomId();
        Long user1Id = user1.getEmployeeId();
        Long user2Id = user2.getEmployeeId();

        // when
        Optional<ChatEmployee> foundUser1 = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(roomId, user1Id);
        Optional<ChatEmployee> notFoundUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(room2.getChatRoomId(), user2Id);

        // then
        assertThat(foundUser1).isPresent();
        assertThat(notFoundUser).isNotPresent();
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse 테스트")
    void findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        Optional<ChatEmployee> leftUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), "user1");
        Optional<ChatEmployee> activeUser = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeUsernameAndIsLeftFalse(room1.getChatRoomId(), "user2");

        // then
        assertThat(leftUser).isNotPresent();
        assertThat(activeUser).isPresent();
    }

    @Test
    @DisplayName("Custom: existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse 테스트")
    void existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        boolean user1Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room1.getChatRoomId(), user1.getEmployeeId());
        boolean user2Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room1.getChatRoomId(), user2.getEmployeeId());
        boolean nonMemberExists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(room2.getChatRoomId(), user2.getEmployeeId());

        // then
        assertThat(user1Exists).isFalse();
        assertThat(user2Exists).isTrue();
        assertThat(nonMemberExists).isFalse();
    }

    @Test
    @DisplayName("Custom: findActiveEmployeeIdsByRoomId 테스트")
    void findActiveEmployeeIdsByRoomIdTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        Set<Long> activeIdsInRoom1 = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room1.getChatRoomId());
        Set<Long> activeIdsInRoom2 = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room2.getChatRoomId());

        // then
        assertThat(activeIdsInRoom1).hasSize(1);
        assertThat(activeIdsInRoom1).containsOnly(user2.getEmployeeId());
        assertThat(activeIdsInRoom2).hasSize(1);
        assertThat(activeIdsInRoom2).containsOnly(user1.getEmployeeId());
    }

    @Test
    @DisplayName("Custom: countByChatRoomChatRoomIdAndIsLeftFalse 테스트")
    void countByChatRoomChatRoomIdAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        long countRoom1 = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId());
        long countRoom2 = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room2.getChatRoomId());

        // then
        assertThat(countRoom1).isEqualTo(1);
        assertThat(countRoom2).isEqualTo(1);
    }

    @Test
    @DisplayName("Custom: countUnreadForMessage 테스트 (컨텍스트 제어)")
    void countUnreadForMessageTest() {
        // given
        LocalDateTime lrmCreatedAt = msg_room1_sys.getCreatedAt();
        ChatMessage newMsg = chatMessageRepository.saveAndFlush(
                ChatMessage.createChatMessage(room1, user1, "Hello!")
        );
        LocalDateTime queryTime = lrmCreatedAt.plusSeconds(1);

        // when
        long unreadCount = chatEmployeeRepository.countUnreadForMessage(
                room1.getChatRoomId(),
                user1.getEmployeeId(),
                queryTime
        );

        // then
        assertThat(unreadCount).isEqualTo(1);

        // given 2
        // [FIX] 1. 영속성 컨텍스트에서 엔티티를 분리(detach)합니다.
        // 이렇게 하면 ce_user2_room1은 더 이상 1차 캐시의 관리를 받지 않습니다.
        entityManager.detach(ce_user2_room1);

        // [FIX] 2. DB에서 엔티티를 *다시* 조회합니다.
        ChatEmployee found_ce_user2_room1 = chatEmployeeRepository.findById(ce_user2_room1.getChatEmployeeId()).get();

        // [FIX] 3. 새로 조회한 엔티티를 수정합니다.
        found_ce_user2_room1.updateLastReadMessage(newMsg);

        // [FIX] 4. saveAndFlush로 DB에 즉시 반영합니다.
        chatEmployeeRepository.saveAndFlush(found_ce_user2_room1);

        // when 2
        LocalDateTime newMsgCreatedAt = newMsg.getCreatedAt();
        long unreadCountAfterRead = chatEmployeeRepository.countUnreadForMessage(
                room1.getChatRoomId(),
                user1.getEmployeeId(),
                newMsgCreatedAt
        );

        // then 2
        assertThat(unreadCountAfterRead).isEqualTo(0L);
    }

    @Test
    @DisplayName("Custom: sumTotalUnreadMessagesByEmployeeId 테스트")
    void sumTotalUnreadMessagesByEmployeeIdTest() {
        // when
        long initialUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId());
        assertThat(initialUnread).isEqualTo(0);

        // given 2: 새 메시지 추가
        chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "My own message"));
        chatMessageRepository.save(ChatMessage.createChatMessage(room1, user2, "Message from user2 (R1)"));
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, user1, "My own message 2"));
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, null, "System message (R2)"));

        // when 2
        long totalUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId());

        // then 2
        assertThat(totalUnread).isEqualTo(1); // room1(1개) + room2(0개) = 1개

        // given 3
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when 3
        long totalUnreadAfterLeft = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId());

        // then 3
        assertThat(totalUnreadAfterLeft).isEqualTo(0);
    }

    @Test
    @DisplayName("Custom: findAllByChatRoomChatRoomId 테스트 (isLeft 무관)")
    void findAllByChatRoomChatRoomIdTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> allMembersInRoom1 = chatEmployeeRepository.findAllByChatRoomChatRoomId(room1.getChatRoomId());

        // then
        assertThat(allMembersInRoom1).hasSize(2);
        assertThat(allMembersInRoom1).containsExactlyInAnyOrder(ce_user1_room1, ce_user2_room1);
    }

    @Test
    @DisplayName("Custom: [신규] countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse 테스트")
    void countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalseTest() {
        // when
        long count1 = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room1.getChatRoomId(),
                user1.getEmployeeId()
        );
        long count2 = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room1.getChatRoomId(),
                user2.getEmployeeId()
        );
        long count3 = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room2.getChatRoomId(),
                user1.getEmployeeId()
        );

        // then
        assertThat(count1).isEqualTo(1);
        assertThat(count2).isEqualTo(1);
        assertThat(count3).isEqualTo(0);

        // given 2
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when 2
        long count4 = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room1.getChatRoomId(),
                user2.getEmployeeId()
        );

        // then 2
        assertThat(count4).isEqualTo(0);
    }

    @Test
    @DisplayName("Custom: [신규] countUnreadByAllParticipants 테스트 (컨텍스트 제어)")
    void countUnreadByAllParticipantsTest() {
        // given
        LocalDateTime lrmCreatedAt = msg_room1_sys.getCreatedAt();
        ChatMessage newMsg = chatMessageRepository.saveAndFlush(
                ChatMessage.createChatMessage(room1, user1, "Test Message")
        );
        LocalDateTime queryTime = lrmCreatedAt.plusSeconds(1);

        // when
        long unreadCount = chatEmployeeRepository.countUnreadByAllParticipants(
                room1.getChatRoomId(),
                queryTime
        );

        // then
        assertThat(unreadCount).isEqualTo(2);

        // given 2
        // [FIX] 1. 영속성 컨텍스트에서 엔티티를 분리(detach)합니다.
        entityManager.detach(ce_user1_room1);
        // [FIX] 2. DB에서 엔티티를 *다시* 조회합니다.
        ChatEmployee found_ce_user1_room1 = chatEmployeeRepository.findById(ce_user1_room1.getChatEmployeeId()).get();
        // [FIX] 3. 새로 조회한 엔티티를 수정합니다.
        found_ce_user1_room1.updateLastReadMessage(newMsg);
        // [FIX] 4. saveAndFlush로 DB에 즉시 반영합니다.
        chatEmployeeRepository.saveAndFlush(found_ce_user1_room1);


        // when 2
        LocalDateTime newMsgCreatedAt = newMsg.getCreatedAt();
        long unreadCountAfterRead = chatEmployeeRepository.countUnreadByAllParticipants(
                room1.getChatRoomId(),
                newMsgCreatedAt
        );

        // then 2
        assertThat(unreadCountAfterRead).isEqualTo(1); // user2만

        // given 3
        // [FIX] 1. 영속성 컨텍스트에서 엔티티를 분리(detach)합니다.
        entityManager.detach(ce_user2_room1);
        // [FIX] 2. DB에서 user2 엔티티를 *다시* 가져와서
        ChatEmployee found_ce_user2_room1 = chatEmployeeRepository.findById(ce_user2_room1.getChatEmployeeId()).get();
        // [FIX] 3. 수정한 뒤
        found_ce_user2_room1.updateLastReadMessage(newMsg);
        // [FIX] 4. saveAndFlush
        chatEmployeeRepository.saveAndFlush(found_ce_user2_room1);

        // when 3
        long unreadCountAllRead = chatEmployeeRepository.countUnreadByAllParticipants(
                room1.getChatRoomId(),
                newMsgCreatedAt
        );

        // then 3
        assertThat(unreadCountAllRead).isEqualTo(0);
    }
}