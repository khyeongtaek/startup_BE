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

    // [추가] DB와 Java 시간 동기화를 위해 EntityManager 주입
    @Autowired
    private EntityManager entityManager;

    // --- 테스트용 공통 데이터 ---
    private Employee admin, user1, user2, user3;
    private ChatRoom room1, room2;
    private ChatMessage msg_room1_sys, msg_room2_sys;
    private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    // --- 테스트용 ChatEmployee 인스턴스 (setUp에서 저장됨) ---
    private ChatEmployee ce_user1_room1, ce_user2_room1, ce_user1_room2;

    @BeforeEach
    void setUp() {
        // 1. 데이터 초기화 (참조 무결성 역순 삭제)
        chatEmployeeRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // 2. CommonCode 생성
        statusActive = createAndSaveCode("STATUS_ACTIVE", "재직", "ACTIVE", 1L);
        roleAdmin = createAndSaveCode("ROLE_ADMIN", "관리자", "ADMIN", 1L);
        roleUser = createAndSaveCode("ROLE_USER", "사용자", "USER", 2L);
        deptDev = createAndSaveCode("DEPT_DEV", "개발팀", "DEV", 1L);
        deptHr = createAndSaveCode("DEPT_HR", "인사팀", "HR", 2L);
        posJunior = createAndSaveCode("POS_JUNIOR", "사원", "JUNIOR", 1L);

        // 3. Employee 생성
        admin = createAndSaveEmployee("admin", "admin@test.com", roleAdmin, deptHr);
        user1 = createAndSaveEmployee("user1", "user1@test.com", roleUser, deptDev);
        user2 = createAndSaveEmployee("user2", "user2@test.com", roleUser, deptDev);
        user3 = createAndSaveEmployee("user3", "user3@test.com", roleUser, deptDev);

        // 4. ChatRoom 생성
        room1 = chatRoomRepository.save(ChatRoom.createChatRoom(admin, "개발팀 단체방", true));
        room2 = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1-user2 1:1", false));

        // 5. 각 방의 최초 시스템 메시지 생성 (ChatEmployee의 lastReadMessage 필수 조건 충족용)
        msg_room1_sys = chatMessageRepository.save(ChatMessage.createSystemMessage(room1, "개발팀 방 생성"));
        msg_room2_sys = chatMessageRepository.save(ChatMessage.createSystemMessage(room2, "1:1 방 생성"));

        // 6. ChatEmployee 기본 데이터 생성 및 저장
        // Room1: user1, user2 참여
        ce_user1_room1 = chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room1, "개발팀 단체방", msg_room1_sys));
        ce_user2_room1 = chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user2, room1, "개발팀 단체방", msg_room1_sys));

        // Room2: user1 참여 (user2는 테스트 메서드에서 참여시킬 예정)
        ce_user1_room2 = chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room2, "user2", msg_room2_sys));
    }

    // --- Helper Methods ---

    private CommonCode createAndSaveCode(String code, String name, String val1, Long seq) {
        CommonCode c = CommonCode.createCommonCode(code, name, val1, null, null, seq, null, false);
        return commonCodeRepository.save(c);
    }

    private Employee createAndSaveEmployee(String username, String email, CommonCode role, CommonCode dept) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, posJunior, null
        );
        employee.updateInitPassword(TEST_PASSWORD, null);
        return employeeRepository.save(employee);
    }

    // --- CRUD Tests ---

    @Test
    @DisplayName("C: 채팅방 참여(save) 테스트")
    void saveChatEmployeeTest() {
        // given
        ChatEmployee newMember = ChatEmployee.createChatEmployee(user2, room2, "user1", msg_room2_sys);

        // when
        ChatEmployee savedCe = chatEmployeeRepository.save(newMember);

        // then
        assertThat(savedCe.getChatEmployeeId()).isNotNull();
        assertThat(savedCe.getEmployee()).isEqualTo(user2);
        assertThat(savedCe.getChatRoom()).isEqualTo(room2);
        assertThat(savedCe.getIsLeft()).isFalse();
        assertThat(savedCe.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트")
    void findByIdTest() {
        // given
        Long id = ce_user1_room1.getChatEmployeeId();

        // when
        Optional<ChatEmployee> found = chatEmployeeRepository.findById(id);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("개발팀 단체방");
    }

    @Test
    @DisplayName("U: 정보 수정(update) 테스트 - Dirty Checking")
    void updateChatEmployeeTest() {
        // given
        ChatEmployee target = chatEmployeeRepository.findById(ce_user1_room1.getChatEmployeeId()).get();

        // when
        target.changedDisplayName("변경된 방이름");
        target.disableNotify();
        target.leftChatRoom();
        chatEmployeeRepository.flush(); // DB 반영

        // then
        ChatEmployee updated = chatEmployeeRepository.findById(ce_user1_room1.getChatEmployeeId()).get();
        assertThat(updated.getDisplayName()).isEqualTo("변경된 방이름");
        assertThat(updated.getIsNotify()).isFalse();
        assertThat(updated.getIsLeft()).isTrue();
    }

    @Test
    @DisplayName("D: 삭제(delete) 테스트")
    void deleteChatEmployeeTest() {
        // given
        Long id = ce_user1_room1.getChatEmployeeId();

        // when
        chatEmployeeRepository.deleteById(id);
        chatEmployeeRepository.flush();

        // then
        assertThat(chatEmployeeRepository.existsById(id)).isFalse();
    }

    // --- Exception Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(lastReadMessage) 누락 시 예외")
    void saveNullLastReadMessageTest() {
        // given
        ChatEmployee invalidCe = ChatEmployee.createChatEmployee(user3, room1, "Room", null);

        // when & then
        assertThatThrownBy(() -> chatEmployeeRepository.saveAndFlush(invalidCe))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Custom Repository Queries Tests ---

    @Test
    @DisplayName("Custom: findAllByEmployeeAndIsLeftFalse - 나간 방 제외 조회")
    void findAllByEmployeeAndIsLeftFalseTest() {
        // given
        // user1은 room1, room2에 모두 참여 중이었으나, room1에서 나감
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> result = chatEmployeeRepository.findAllByEmployeeAndIsLeftFalse(user1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChatRoom()).isEqualTo(room2); // room2만 조회되어야 함
    }

    @Test
    @DisplayName("Custom: findAllByChatRoomChatRoomIdAndIsLeftFalse - 방의 활성 멤버 조회")
    void findAllByChatRoomChatRoomIdAndIsLeftFalseTest() {
        // given
        ce_user1_room1.leftChatRoom(); // user1 나감
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> members = chatEmployeeRepository.findAllByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId());

        // then
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getEmployee()).isEqualTo(user2); // user2만 남음
    }

    @Test
    @DisplayName("Custom: findAllByChatRoomChatRoomId - 나간 멤버 포함 조회")
    void findAllByChatRoomChatRoomIdTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        List<ChatEmployee> allMembers = chatEmployeeRepository.findAllByChatRoomChatRoomId(room1.getChatRoomId());

        // then
        assertThat(allMembers).hasSize(2); // user1(나감), user2(참여중) 모두 조회
    }

    @Test
    @DisplayName("Custom: findByChatRoomChatRoomIdAndEmployeeEmployeeId - 특정 멤버 조회")
    void findByChatRoomAndEmployeeIdTest() {
        // when
        Optional<ChatEmployee> result = chatEmployeeRepository.findByChatRoomChatRoomIdAndEmployeeEmployeeId(
                room1.getChatRoomId(), user1.getEmployeeId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(ce_user1_room1);
    }

    @Test
    @DisplayName("Custom: existsByChatRoom...AndIsLeftFalse - 활성 멤버 존재 여부")
    void existsActiveMemberTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        boolean user1Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(
                room1.getChatRoomId(), user1.getEmployeeId());
        boolean user2Exists = chatEmployeeRepository.existsByChatRoomChatRoomIdAndEmployeeEmployeeIdAndIsLeftFalse(
                room1.getChatRoomId(), user2.getEmployeeId());

        // then
        assertThat(user1Exists).isFalse();
        assertThat(user2Exists).isTrue();
    }

    @Test
    @DisplayName("Custom: findActiveEmployeeIdsByRoomId - 활성 멤버 ID Set 조회")
    void findActiveEmployeeIdsTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        Set<Long> ids = chatEmployeeRepository.findActiveEmployeeIdsByRoomId(room1.getChatRoomId());

        // then
        assertThat(ids).hasSize(1);
        assertThat(ids).containsOnly(user2.getEmployeeId());
    }

    @Test
    @DisplayName("Custom: countByChatRoomChatRoomIdAndIsLeftFalse - 활성 멤버 수 카운트")
    void countActiveMembersTest() {
        // given
        ce_user1_room1.leftChatRoom();
        chatEmployeeRepository.save(ce_user1_room1);

        // when
        long count = chatEmployeeRepository.countByChatRoomChatRoomIdAndIsLeftFalse(room1.getChatRoomId());

        // then
        assertThat(count).isEqualTo(1); // user2 한 명
    }

    @Test
    @DisplayName("Custom: countByChatRoom...AndEmployee...Not... - 1:1 채팅방 상대방 존재 확인")
    void countOtherMemberInOneOnOneTest() {
        // given
        // Room2에는 현재 user1만 있음 (@BeforeEach 기준)
        // user2를 room2에 추가
        ChatEmployee ce_user2_room2 = chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user2, room2, "user1", msg_room2_sys));

        // when
        // user1 입장에서 나(user1)를 제외한 활성 멤버 수
        long count = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room2.getChatRoomId(), user1.getEmployeeId());

        // then
        assertThat(count).isEqualTo(1); // user2가 있으므로 1

        // user2가 나가면?
        ce_user2_room2.leftChatRoom();
        chatEmployeeRepository.save(ce_user2_room2);
        long countAfterLeft = chatEmployeeRepository.countByChatRoomChatRoomIdAndEmployeeEmployeeIdNotAndIsLeftFalse(
                room2.getChatRoomId(), user1.getEmployeeId());
        assertThat(countAfterLeft).isEqualTo(0);
    }

    // --- Complex JPQL Tests (Statistics) ---

    @Test
    @DisplayName("Custom: countUnreadForMessage - 특정 메시지를 안 읽은 사람 수 (보낸사람 제외)")
    void countUnreadForMessageTest() throws InterruptedException {
        // given
        // user3을 Room1에 추가 (User1, User2, User3 참여 중)
        ChatEmployee ce_user3_room1 = chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user3, room1, "Room1", msg_room1_sys));

        // [수정] 시간차 확보 및 정밀도 문제 해결을 위해 flush 후 refresh
        ChatMessage newMsg = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "Hello Team!"));
        chatMessageRepository.flush();
        entityManager.refresh(newMsg); // DB의 시간(Precision)을 Java 객체에 반영

        LocalDateTime msgTime = newMsg.getCreatedAt();

        // 상태:
        // User1: 보낸 사람 (제외됨)
        // User2: lrm = msg_room1_sys (오래됨) -> 카운트 대상 O
        // User3: lrm = msg_room1_sys (오래됨) -> 카운트 대상 O

        // when
        long unreadCount = chatEmployeeRepository.countUnreadForMessage(room1.getChatRoomId(), user1.getEmployeeId(), msgTime);

        // then
        assertThat(unreadCount).isEqualTo(2);

        // case 2: User2가 메시지를 읽음
        ce_user2_room1.updateLastReadMessage(newMsg);
        chatEmployeeRepository.save(ce_user2_room1);

        long unreadCount2 = chatEmployeeRepository.countUnreadForMessage(room1.getChatRoomId(), user1.getEmployeeId(), msgTime);
        assertThat(unreadCount2).isEqualTo(1); // User3만 남음
    }

    @Test
    @DisplayName("Custom: countUnreadByAllParticipants - 특정 메시지를 안 읽은 사람 수 (전체)")
    void countUnreadByAllParticipantsTest() throws InterruptedException {
        // given
        // 시스템 메시지 발생
        ChatMessage sysMsg = chatMessageRepository.save(ChatMessage.createSystemMessage(room1, "공지사항"));

        // [수정] 핵심 수정: DB에 저장된 시간(H2는 Microsecond)과 Java 시간(Nanosecond)의 정밀도 차이를 맞춤
        chatMessageRepository.flush();
        entityManager.refresh(sysMsg);

        LocalDateTime msgTime = sysMsg.getCreatedAt();

        // User1, User2 모두 아직 lastReadMessage가 초기 msg_room1_sys 상태임.

        // when
        long unreadCount = chatEmployeeRepository.countUnreadByAllParticipants(room1.getChatRoomId(), msgTime);

        // then
        assertThat(unreadCount).isEqualTo(2); // User1, User2 모두 안 읽음

        // case: User1이 읽음 처리
        ce_user1_room1.updateLastReadMessage(sysMsg);
        chatEmployeeRepository.save(ce_user1_room1);

        // JPA update 반영
        chatEmployeeRepository.flush();

        assertThat(chatEmployeeRepository.countUnreadByAllParticipants(room1.getChatRoomId(), msgTime)).isEqualTo(1);
    }

    @Test
    @DisplayName("Custom: sumTotalUnreadMessagesByEmployeeId - 통합 안 읽은 메시지 수")
    void sumTotalUnreadMessagesByEmployeeIdTest() throws InterruptedException {
        // given
        // 상황: User1은 Room1, Room2에 참여 중.

        // Room1에 메시지 2개 추가 (User2가 보냄)
        ChatMessage r1_m1 = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user2, "Hi 1"));
        ChatMessage r1_m2 = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user2, "Hi 2"));

        // Room2에 메시지 3개 추가
        // User1이 보낸 것 (카운트 X)
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, user1, "My Msg"));
        // 시스템 메시지 (카운트 O)
        ChatMessage r2_m1 = chatMessageRepository.save(ChatMessage.createSystemMessage(room2, "Sys Msg"));
        // User1이 다시 보낸 것 (카운트 X)
        chatMessageRepository.save(ChatMessage.createChatMessage(room2, user1, "My Msg 2"));

        // [수정] 모든 저장 후 DB 동기화
        chatMessageRepository.flush();
        entityManager.clear(); // 캐시를 비워 쿼리 실행 시 최신 데이터 참조 보장

        // 주의: entityManager.clear()를 하면 기존 엔티티들(user1 등)이 Detached 상태가 됨
        // 하지만 아래 테스트는 ID값만 사용하므로 문제 없음.

        // when
        long totalUnread = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId());

        // then
        // Room1: 2개 (r1_m1, r1_m2)
        // Room2: 0개 (시스템 메시지는 createChatMessage와 달리 employee가 null이라 쿼리 로직 상 제외될 수 있음)
        //         -> 질문자의 쿼리 로직에 m.employee IS NOT NULL이 포함되어 있다면 0개.
        //         -> 만약 포함되지 않았다면 1개(r2_m1).
        //         -> 현재 코드 로직상 2개로 검증 (User2가 보낸 2개만 확실함)
        assertThat(totalUnread).isEqualTo(2);
    }

    @Test
    @DisplayName("Custom: sumTotalUnreadMessagesByEmployeeId - 내가 보낸 메시지는 카운트 제외 확인")
    void sumTotalUnreadExcludeMyMessageTest() throws InterruptedException {
        // given
        // User1이 Room1에 메시지 5개 보냄
        for(int i=0; i<5; i++) {
            chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "Me " + i));
        }
        chatMessageRepository.flush();

        // when
        long count = chatEmployeeRepository.sumTotalUnreadMessagesByEmployeeId(user1.getEmployeeId());

        // then
        assertThat(count).isEqualTo(0);
    }
}