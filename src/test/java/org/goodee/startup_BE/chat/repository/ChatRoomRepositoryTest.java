package org.goodee.startup_BE.chat.repository;

import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    private Employee creator;
    private CommonCode statusActive, roleUser, deptDev, posJunior;

    @BeforeEach
    void setUp() {
        // H2 DB 초기화
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
}