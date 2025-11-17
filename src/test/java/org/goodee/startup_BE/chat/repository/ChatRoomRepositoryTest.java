package org.goodee.startup_BE.chat.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.chat.entity.ChatEmployee;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
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
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- [신규] 쿼리 테스트를 위해 리포지토리 주입 ---
    @Autowired
    private ChatEmployeeRepository chatEmployeeRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    // --- [신규] ---

    @Autowired
    private EntityManager entityManager;

    private Employee creator;
    private CommonCode statusActive, roleUser, deptDev, posJunior;

    @BeforeEach
    void setUp() {
        // H2 DB 초기화 (자식 테이블부터)
        chatEmployeeRepository.deleteAll(); // [신규]
        chatMessageRepository.deleteAll(); // [신규]
        chatRoomRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior));

        // --- given: 생성자(creator) 직원 데이터 생성 ---
        creator = createPersistableEmployee("creator", "creator@test.com", roleUser, deptDev, posJunior, null);
        employeeRepository.save(creator);
    }

    /** 테스트용 직원 생성 헬퍼 (EmployeeRepositoryTest에서 가져옴) */
    private Employee createPersistableEmployee(String username, String email, CommonCode role, CommonCode dept, CommonCode pos, Employee creator) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, pos,
                creator
        );
        employee.updateInitPassword("testPassword123!", creator);
        return employee;
    }

    // --- (기존 CRUD 및 Exception 테스트는 동일) ---

    @Test
    @DisplayName("C: 채팅방 생성(save) 테스트")
    void saveChatRoomTest() {
        // given
        ChatRoom newRoom = ChatRoom.createChatRoom(creator, "테스트 채팅방", true);

        // when
        ChatRoom savedRoom = chatRoomRepository.save(newRoom);

        // then
        assertThat(savedRoom).isNotNull();
        assertThat(savedRoom.getChatRoomId()).isNotNull();
        assertThat(savedRoom.getName()).isEqualTo("테스트 채팅방");
        assertThat(savedRoom.getEmployee()).isEqualTo(creator);
        assertThat(savedRoom.getIsTeam()).isTrue();
        assertThat(savedRoom.getCreatedAt()).isNotNull(); // @PrePersist 동작 확인
    }

    @Test
    @DisplayName("R: 채팅방 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        ChatRoom savedRoom = chatRoomRepository.save(
                ChatRoom.createChatRoom(creator, "조회용 채팅방", false)
        );

        // when
        Optional<ChatRoom> foundRoom = chatRoomRepository.findById(savedRoom.getChatRoomId());

        // then
        assertThat(foundRoom).isPresent();
        assertThat(foundRoom.get().getChatRoomId()).isEqualTo(savedRoom.getChatRoomId());
    }

    @Test
    @DisplayName("U: 채팅방 정보 수정(update) 테스트")
    void updateChatRoomTest() {
        // given
        ChatRoom savedRoom = chatRoomRepository.save(
                ChatRoom.createChatRoom(creator, "1:1 채팅방", false)
        );

        // 영속성 컨텍스트에서 분리 후 다시 로드 (업데이트 확인용)
        entityManager.flush();
        entityManager.clear();

        ChatRoom roomToUpdate = chatRoomRepository.findById(savedRoom.getChatRoomId()).get();

        // when
        // ChatRoom 엔티티의 updateToTeamRoom 메서드 호출
        roomToUpdate.updateToTeamRoom();
        chatRoomRepository.save(roomToUpdate); // 변경 감지(dirty checking) 또는 save
        entityManager.flush();

        // 검증을 위해 다시 조회
        ChatRoom updatedRoom = chatRoomRepository.findById(savedRoom.getChatRoomId()).get();

        // then
        assertThat(updatedRoom.getIsTeam()).isTrue();
    }

    @Test
    @DisplayName("Exception: 필수 필드(creator) null 저장 시 예외 발생")
    void saveNullCreatorTest() {
        // given
        // employee (생성자)는 nullable=false
        ChatRoom incompleteRoom = ChatRoom.createChatRoom(null, "잘못된 방", true);

        // when & then
        // creator가 null (nullable=false 위반) 상태로 save 시도
        assertThatThrownBy(() -> chatRoomRepository.saveAndFlush(incompleteRoom))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- [신규] 커스텀 쿼리 테스트 ---

    @Test
    @DisplayName("Custom: [신규] findExistingOneOnOneRooms 테스트 (1:1방, 팀방, 순서)")
    void findExistingOneOnOneRoomsTest() throws InterruptedException {
        // === given ===
        // 1. 추가 사용자 생성 (user1, user2, user3)
        Employee user1 = employeeRepository.save(createPersistableEmployee("user1", "user1@test.com", roleUser, deptDev, posJunior, creator));
        Employee user2 = employeeRepository.save(createPersistableEmployee("user2", "user2@test.com", roleUser, deptDev, posJunior, creator));
        Employee user3 = employeeRepository.save(createPersistableEmployee("user3", "user3@test.com", roleUser, deptDev, posJunior, creator));

        // 2. 테스트용 채팅방 4개 생성 (시간차를 두어 createdAt 순서 보장)
        // (1) user1-user2 1:1 방 (가장 오래됨)
        ChatRoom room1_1on1_older = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1-user2 (Old)", false));
        Thread.sleep(10);

        // (2) user1-user2 팀 방 (테스트에서 제외되어야 함)
        ChatRoom room2_team = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1-user2 (Team)", true));
        Thread.sleep(10);

        // (3) user1-user3 1:1 방 (테스트에서 제외되어야 함)
        ChatRoom room3_1on1_other = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1-user3 (1:1)", false));
        Thread.sleep(10);

        // (4) user1-user2 1:1 방 (가장 최신)
        ChatRoom room4_1on1_newest = chatRoomRepository.save(ChatRoom.createChatRoom(user1, "user1-user2 (New)", false));

        // 3. 각 방의 '최초' 메시지 생성 (lastReadMessage FK용)
        ChatMessage msg1 = chatMessageRepository.save(ChatMessage.createChatMessage(room1_1on1_older, null, "..."));
        ChatMessage msg2 = chatMessageRepository.save(ChatMessage.createChatMessage(room2_team, null, "..."));
        ChatMessage msg3 = chatMessageRepository.save(ChatMessage.createChatMessage(room3_1on1_other, null, "..."));
        ChatMessage msg4 = chatMessageRepository.save(ChatMessage.createChatMessage(room4_1on1_newest, null, "..."));

        // 4. ChatEmployee로 사용자들을 방에 연결
        // (1) room1_1on1_older (user1, user2)
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room1_1on1_older, "user2", msg1));
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user2, room1_1on1_older, "user1", msg1));
        // (2) room2_team (user1, user2)
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room2_team, "Team", msg2));
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user2, room2_team, "Team", msg2));
        // (3) room3_1on1_other (user1, user3)
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room3_1on1_other, "user3", msg3));
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user3, room3_1on1_other, "user1", msg3));
        // (4) room4_1on1_newest (user1, user2)
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user1, room4_1on1_newest, "user2", msg4));
        chatEmployeeRepository.save(ChatEmployee.createChatEmployee(user2, room4_1on1_newest, "user1", msg4));

        // === when ===
        // 1. user1과 user2 간의 1:1 방 조회
        List<ChatRoom> foundRooms = chatRoomRepository.findExistingOneOnOneRooms(user1.getEmployeeId(), user2.getEmployeeId());

        // 2. user1과 user3 간의 1:1 방 조회
        List<ChatRoom> foundRooms_other = chatRoomRepository.findExistingOneOnOneRooms(user1.getEmployeeId(), user3.getEmployeeId());

        // 3. 관계없는 사용자(creator)와의 1:1 방 조회
        List<ChatRoom> emptyRooms = chatRoomRepository.findExistingOneOnOneRooms(creator.getEmployeeId(), user1.getEmployeeId());

        // === then ===
        // 1. room1(오래된 1:1), room4(최신 1:1)만 조회되어야 함. room2(팀방), room3(다른사용자)는 제외
        assertThat(foundRooms).hasSize(2);
        // 2. 쿼리의 "ORDER BY createdAt DESC"가 정확히 동작하는지 확인
        assertThat(foundRooms).containsExactly(room4_1on1_newest, room1_1on1_older);

        // 3. user1, user3 간에는 room3(1:1) 하나만 조회되어야 함
        assertThat(foundRooms_other).hasSize(1);
        assertThat(foundRooms_other).contains(room3_1on1_other);

        // 4. creator와 user1 간에는 1:1 방이 없으므로 비어있어야 함
        assertThat(emptyRooms).isEmpty();
    }
}