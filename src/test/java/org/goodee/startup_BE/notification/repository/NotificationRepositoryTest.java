package org.goodee.startup_BE.notification.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.entity.Notification;
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
@EntityScan(basePackages = "org.goodee.startup_BE") // Entity 스캔 범위 설정
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EntityManager entityManager; // 영속성 컨텍스트 관리를 위해

    // 테스트용 공통 데이터
    private Employee user1, user2, creator;
    private CommonCode statusActive, roleUser, deptDev, posJunior, ownerMail, ownerApproval;

    @BeforeEach
    void setUp() {
        // DB 초기화
        notificationRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: CommonCode 생성 ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        // 알림용 CommonCode
        ownerMail = CommonCode.createCommonCode("OT_MAIL", "메일", "MAIL", null, null, 1L, null, false);
        ownerApproval = CommonCode.createCommonCode("OT_APPROVAL", "결재", "APPROVAL", null, null, 2L, null, false);

        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior, ownerMail, ownerApproval));

        // --- given: Employee 생성 (EmployeeRepositoryTest 헬퍼 메서드 활용) ---
        creator = createPersistableEmployee("creator", "creator@test.com", roleUser, deptDev, posJunior, null);
        creator = employeeRepository.save(creator); // creator는 ID가 있어야 함

        user1 = createPersistableEmployee("user1", "user1@test.com", roleUser, deptDev, posJunior, creator);
        user2 = createPersistableEmployee("user2", "user2@test.com", roleUser, deptDev, posJunior, creator);

        employeeRepository.saveAll(List.of(user1, user2));
    }

    /**
     * 테스트용 직원 생성 헬퍼 메서드 (Employee 엔티티의 createEmployee 활용)
     */
    private Employee createPersistableEmployee(String username, String email, CommonCode role, CommonCode dept, CommonCode pos, Employee creator) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, pos,
                creator
        );
        employee.updateInitPassword("testPassword123!", creator);
        return employee;
    }

    /**
     * 테스트용 알림 생성 헬퍼 메서드 (Notification 엔티티의 createNotification 활용)
     */
    private Notification createPersistableNotification(Employee recipient, CommonCode ownerType, String title) {
        return Notification.createNotification(
                recipient, ownerType, "/" + ownerType.getValue1() + "/1", title, "내용"
        );
    }

    @Test
    @DisplayName("C: 알림 생성(save) 테스트")
    void saveNotificationTest() {
        // given
        Notification newNotification = createPersistableNotification(user1, ownerMail, "새 메일");

        // when
        Notification savedNotification = notificationRepository.save(newNotification);

        // then
        assertThat(savedNotification).isNotNull();
        assertThat(savedNotification.getNotificationId()).isNotNull();
        assertThat(savedNotification.getEmployee()).isEqualTo(user1);
        assertThat(savedNotification.getOwnerType()).isEqualTo(ownerMail);
        assertThat(savedNotification.getTitle()).isEqualTo("새 메일");
        assertThat(savedNotification.getCreatedAt()).isNotNull(); // @PrePersist 동작 확인
        assertThat(savedNotification.getReadAt()).isNull();
        assertThat(savedNotification.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("R: findByEmployeeEmployeeId... (목록 조회 - Paging, 삭제 제외, 최신순)")
    void findByEmployeeEmployeeIdTest() throws InterruptedException {
        // given: user1에게 알림 3개, user2에게 알림 1개 생성
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-메일1"));
        Thread.sleep(10); // createdAt 순서 보장
        Notification noti2 = notificationRepository.save(createPersistableNotification(user1, ownerApproval, "user1-결재1"));
        Thread.sleep(10);
        Notification noti3 = notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-메일2"));

        notificationRepository.save(createPersistableNotification(user2, ownerMail, "user2-메일1"));

        // user1의 알림 중 1개 삭제
        noti2.deleteNotification();
        notificationRepository.save(noti2);

        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

        // when: user1의 삭제되지 않은 알림 목록을 createdAt 내림차순으로 조회
        Page<Notification> resultPage = notificationRepository.findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(user1.getEmployeeId(), pageable);

        // then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2); // 삭제된 noti2 제외
        assertThat(resultPage.getContent()).hasSize(2);
        // 순서 검증 (noti3이 가장 최신)
        assertThat(resultPage.getContent().get(0).getTitle()).isEqualTo("user1-메일2");
        assertThat(resultPage.getContent().get(1).getTitle()).isEqualTo("user1-메일1");
    }

    @Test
    @DisplayName("R: countByEmployeeUsernameAndReadAtIsNull... (읽지 않은 개수)")
    void countUnreadTest() {
        // given: user1에게 읽지 않은 알림 2개, 읽은 알림 1개, user2에게 읽지 않은 알림 1개
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-unread1"));
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-unread2"));

        Notification readNoti = createPersistableNotification(user1, ownerMail, "user1-read");
        readNoti.readNotification(); // 읽음 처리
        notificationRepository.save(readNoti);

        notificationRepository.save(createPersistableNotification(user2, ownerMail, "user2-unread"));

        // when: user1의 읽지 않은 알림 개수 조회
        long count = notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(user1.getUsername());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("R: findByEmployeeUsernameAndReadAtIsNull... (읽지 않은 알림 목록)")
    void findUnreadListTest() {
        // given: (위와 동일)
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-unread1"));
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-unread2"));

        Notification readNoti = createPersistableNotification(user1, ownerMail, "user1-read");
        readNoti.readNotification();
        notificationRepository.save(readNoti);

        // 읽지 않았지만 삭제된 알림
        Notification deletedNoti = createPersistableNotification(user1, ownerMail, "user1-deleted");
        deletedNoti.deleteNotification();
        notificationRepository.save(deletedNoti);

        // when: user1의 읽지 않았고 삭제되지 않은 알림 목록 조회
        List<Notification> resultList = notificationRepository.findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(user1.getUsername());

        // then
        assertThat(resultList).hasSize(2);
        assertThat(resultList).extracting(Notification::getTitle).containsExactlyInAnyOrder("user1-unread1", "user1-unread2");
    }

    @Test
    @DisplayName("R: findByEmployeeUsernameAndIsDeletedFalse (삭제되지 않은 모든 알림 목록)")
    void findNotDeletedTest() {
        // given
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-noti1"));
        notificationRepository.save(createPersistableNotification(user1, ownerMail, "user1-noti2"));

        Notification deletedNoti = createPersistableNotification(user1, ownerMail, "user1-deleted");
        deletedNoti.deleteNotification();
        notificationRepository.save(deletedNoti);

        notificationRepository.save(createPersistableNotification(user2, ownerMail, "user2-noti1"));

        // when: user1의 삭제되지 않은 모든 알림 조회
        List<Notification> resultList = notificationRepository.findByEmployeeUsernameAndIsDeletedFalse(user1.getUsername());

        // then
        assertThat(resultList).hasSize(2);
        assertThat(resultList).extracting(Notification::getTitle).containsExactlyInAnyOrder("user1-noti1", "user1-noti2");
    }
}