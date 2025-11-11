package org.goodee.startup_BE.chat.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.chat.entity.ChatRoom;
import org.goodee.startup_BE.chat.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EntityManager entityManager;

    private Employee user1;
    private ChatRoom room1;
    private LocalDateTime user1JoinedAt;

    @BeforeEach
    void setUp() {
        // H2 DB 초기화
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

        // --- given: Employee 및 ChatRoom 생성 ---
        user1 = createPersistableEmployee("user1", "user1@test.com", roleUser, deptDev, posJunior, null);
        employeeRepository.save(user1);

        room1 = ChatRoom.createChatRoom(user1, "테스트 채팅방", true);
        chatRoomRepository.save(room1);

        // --- given: 메시지 데이터 생성 ---
        // 1. 참여 전 메시지 (보이면 안 됨)
        chatMessageRepository.save(ChatMessage.createChatMessage(room1, null, "초대 전 메시지"));

        // 2. 참여 시각
        user1JoinedAt = LocalDateTime.now();

        // 3. 참여 후 메시지 (보여야 함)
        try { Thread.sleep(10); } catch (Exception e) {} // 시간차 보장
        ChatMessage msg1 = chatMessageRepository.save(ChatMessage.createChatMessage(room1, user1, "안녕하세요"));
        try { Thread.sleep(10); } catch (Exception e) {}
        ChatMessage msg2 = chatMessageRepository.save(ChatMessage.createChatMessage(room1, null, "참여 후 시스템 메시지"));

        // 4. 참여 후 삭제된 메시지 (보이면 안 됨)
        ChatMessage deletedMsg = ChatMessage.createChatMessage(room1, user1, "삭제될 메시지");
        deletedMsg.deleteChatMessage();
        chatMessageRepository.save(deletedMsg);
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
    @DisplayName("Custom: findByChatRoomChatRoomIdAndCreatedAtAfter... (메시지 목록 조회 - joinedAt 필터링)")
    void findMessagesAfterJoinedAtTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        // user1이 참여한 시각(user1JoinedAt) 이후의 삭제되지 않은(isDeleted=false) 메시지를 최신순으로 조회
        Page<ChatMessage> resultPage = chatMessageRepository
                .findByChatRoomChatRoomIdAndCreatedAtAfterAndIsDeletedFalseOrderByCreatedAtDesc(
                        room1.getChatRoomId(),
                        user1JoinedAt,
                        pageable
                );

        // then
        assertThat(resultPage).isNotNull();
        // 참여 후 메시지 2개만 조회되어야 함 (msg1, msg2)
        // (초대 전 메시지, 삭제된 메시지 제외)
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getContent()).hasSize(2);

        // 정렬 순서 검증 (최신순: msg2 -> msg1)
        assertThat(resultPage.getContent().get(0).getContent()).isEqualTo("참여 후 시스템 메시지");
        assertThat(resultPage.getContent().get(1).getContent()).isEqualTo("안녕하세요");
    }
}