package org.goodee.startup_BE.employee.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.entity.EmployeeHistory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// JPA 관련 컴포넌트만 테스트
// H2 DB 사용을 위한 모든 설정을 명시적으로 강제 (EmployeeRepositoryTest와 동일)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// 엔티티 클래스 스캔 경로 지정 (EmployeeRepositoryTest와 동일)
@EntityScan(basePackages = "org.goodee.startup_BE")
class EmployeeHistoryRepositoryTest {

    @Autowired
    private EmployeeHistoryRepository employeeHistoryRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // 테스트용 공통 데이터
    private Employee adminUpdater; // 변경 수행자
    private Employee targetEmployee; // 변경 대상자
    private CommonCode statusActive, roleUser, deptDev, posJunior, posSenior;
    private final String TEST_PASSWORD = "testPassword123!";

    @BeforeEach
    void setUp() {
        // H2 DB 초기화
        employeeHistoryRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 (EmployeeRepositoryTest와 동일) ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        CommonCode roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        CommonCode deptHr = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);
        posSenior = CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null);

        commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior));

        // --- given: 변경 수행자(adminUpdater) 직원 데이터 생성 ---
        adminUpdater = Employee.createEmployee(
                "admin", "관리자", "admin@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleAdmin, deptHr, posSenior,
                null // 최초 생성자는 creator가 null
        );
        adminUpdater.updateInitPassword(TEST_PASSWORD, null);
        employeeRepository.save(adminUpdater);

        // --- given: 변경 대상자(targetEmployee) 직원 데이터 생성 ---
        targetEmployee = createPersistableEmployee("targetUser", "target@test.com", roleUser, deptDev, posJunior);
        employeeRepository.save(targetEmployee);
    }

    /**
     * EmployeeRepositoryTest의 헬퍼 메서드와 동일
     */
    private Employee createPersistableEmployee(String username, String email, CommonCode role, CommonCode dept, CommonCode pos) {
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, pos,
                adminUpdater // 이 직원의 생성자는 setUp에서 만든 'adminUpdater'
        );
        employee.updateInitPassword(TEST_PASSWORD, adminUpdater);
        return employee;
    }

    /**
     * 테스트용 이력(History) 엔티티를 생성하되, DB에 저장하지 않고
     * '저장 가능한(persistable)' 상태의 엔티티 객체를 반환하는 헬퍼 메서드
     */
    private EmployeeHistory createPersistableHistory(
            Employee target, Employee updater, String fieldName, String oldValue, String newValue
    ) {
        return EmployeeHistory.createHistory(
                target, updater, fieldName, oldValue, newValue
        );
    }

    @Test
    @DisplayName("C: 이력 생성(save) 테스트")
    void saveHistoryTest() {
        // given
        EmployeeHistory newHistory = createPersistableHistory(
                targetEmployee, adminUpdater, "position", "사원", "대리"
        );

        // when
        EmployeeHistory savedHistory = employeeHistoryRepository.save(newHistory);

        // then
        assertThat(savedHistory).isNotNull();
        assertThat(savedHistory.getHistoryId()).isNotNull();
        assertThat(savedHistory.getEmployee()).isEqualTo(targetEmployee);
        assertThat(savedHistory.getUpdater()).isEqualTo(adminUpdater);
        assertThat(savedHistory.getFieldName()).isEqualTo("position");
        assertThat(savedHistory.getOldValue()).isEqualTo("사원");
        assertThat(savedHistory.getNewValue()).isEqualTo("대리");
        assertThat(savedHistory.getChangedAt()).isNotNull(); // @PrePersist 동작 확인
    }

    @Test
    @DisplayName("R: 이력 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        EmployeeHistory savedHistory = employeeHistoryRepository.save(
                createPersistableHistory(targetEmployee, adminUpdater, "status", "재직", "휴직")
        );

        // when
        Optional<EmployeeHistory> foundHistory = employeeHistoryRepository.findById(savedHistory.getHistoryId());

        // then
        assertThat(foundHistory).isPresent();
        assertThat(foundHistory.get().getHistoryId()).isEqualTo(savedHistory.getHistoryId());
        assertThat(foundHistory.get().getFieldName()).isEqualTo("status");
    }

    @Test
    @DisplayName("R: 이력 ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<EmployeeHistory> foundHistory = employeeHistoryRepository.findById(nonExistentId);

        // then
        assertThat(foundHistory).isNotPresent();
    }

    @Test
    @DisplayName("D: 이력 삭제(deleteById) 테스트")
    void deleteHistoryTest() {
        // given
        EmployeeHistory savedHistory = employeeHistoryRepository.save(
                createPersistableHistory(targetEmployee, adminUpdater, "department", "개발팀", "인사팀")
        );
        Long historyId = savedHistory.getHistoryId();
        assertThat(employeeHistoryRepository.existsById(historyId)).isTrue();

        // when
        employeeHistoryRepository.deleteById(historyId);
        employeeHistoryRepository.flush(); // 삭제 쿼리 즉시 실행

        // then
        assertThat(employeeHistoryRepository.existsById(historyId)).isFalse();
    }

    // --- Custom Repository Method Tests ---

    @Test
    @DisplayName("Custom: findByEmployeeEmployeeIdOrderByChangedAtDesc 테스트")
    void findByEmployeeIdAndOrderByChangedAtDescTest() throws InterruptedException {
        // given
        // 1. 다른 직원(otherEmployee) 생성
        Employee otherEmployee = createPersistableEmployee("otherUser", "other@test.com", roleUser, deptDev, posJunior);
        employeeRepository.save(otherEmployee);

        // 2. targetEmployee에 대한 이력 2건 생성 (시간차 발생)
        EmployeeHistory history1_target = createPersistableHistory(targetEmployee, adminUpdater, "position", "사원", "대리");
        employeeHistoryRepository.save(history1_target);

        Thread.sleep(10); // @PrePersist 시간차를 두기 위함

        EmployeeHistory history2_target = createPersistableHistory(targetEmployee, adminUpdater, "department", "개발팀", "인사팀");
        employeeHistoryRepository.save(history2_target);

        Thread.sleep(10);

        // 3. otherEmployee에 대한 이력 1건 생성
        EmployeeHistory history3_other = createPersistableHistory(otherEmployee, adminUpdater, "status", "재직", "휴직");
        employeeHistoryRepository.save(history3_other);

        // when
        // targetEmployee의 ID로 이력 조회 (변경 시간 내림차순)
        List<EmployeeHistory> histories = employeeHistoryRepository.findByEmployeeEmployeeIdOrderByChangedAtDesc(targetEmployee.getEmployeeId());

        // then
        assertThat(histories).hasSize(2);
        assertThat(histories).doesNotContain(history3_other); // 다른 직원 이력 미포함 확인
        // 정렬 순서 확인: history2 (최신) -> history1 (과거)
        assertThat(histories).containsExactly(history2_target, history1_target);
    }

    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(employee) null 저장 시 예외 발생")
    void saveNullEmployeeTest() {
        // given
        // EmployeeHistory.java의 @JoinColumn(name = "employee_id", nullable = false) 제약조건
        EmployeeHistory historyWithNullEmployee = createPersistableHistory(
                null, adminUpdater, "field", "old", "new"
        );

        // when & then
        // nullable=false 위반
        assertThatThrownBy(() -> employeeHistoryRepository.saveAndFlush(historyWithNullEmployee))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(updater) null 저장 시 예외 발생")
    void saveNullUpdaterTest() {
        // given
        // EmployeeHistory.java의 @JoinColumn(name = "updater_id", nullable = false) 제약조건
        EmployeeHistory historyWithNullUpdater = createPersistableHistory(
                targetEmployee, null, "field", "old", "new"
        );

        // when & then
        // nullable=false 위반
        assertThatThrownBy(() -> employeeHistoryRepository.saveAndFlush(historyWithNullUpdater))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(fieldName) null 저장 시 예외 발생")
    void saveNullFieldNameTest() {
        // given
        // EmployeeHistory.java의 @Column(nullable = false) 제약조건
        EmployeeHistory historyWithNullField = createPersistableHistory(
                targetEmployee, adminUpdater, null, "old", "new"
        );

        // when & then
        // nullable=false 위반
        assertThatThrownBy(() -> employeeHistoryRepository.saveAndFlush(historyWithNullField))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}