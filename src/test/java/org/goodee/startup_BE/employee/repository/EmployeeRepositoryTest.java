package org.goodee.startup_BE.employee.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
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
// H2 DB 사용을 위한 모든 설정을 명시적으로 강제 (외부 설정 파일 Override 방지)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop", // 1. 강제 테이블 생성
        "spring.datasource.driver-class-name=org.h2.Driver", // 2. 강제 H2 드라이버
        // 3. 강제 인메모리 DB (testdb는 H2 콘솔에서 식별하기 위한 이름)
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", // 4. H2 기본 계정
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect" // 5. 강제 H2 방언
})
// @DataJpaTest가 엔티티 클래스(Employee, CommonCode 등)를 찾을 수 있도록 스캔 경로를 지정
// 일반적으로 프로젝트의 최상위 기본 패키지를 지정해주는 것이 좋음
@EntityScan(basePackages = "org.goodee.startup_BE")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // 테스트용 공통 데이터
    private Employee creator;
    private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
    private final String TEST_PASSWORD = "testPassword123!";

    @BeforeEach
    void setUp() {
        // H2 DB 초기화
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null, false);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        deptHr = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null, false);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        posSenior = CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null, false);

        commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior));

        // --- given: 생성자(creator) 직원 데이터 생성 ---
        // 요청하신 워크플로우(create -> updateInitPassword -> save)를 적용
        creator = Employee.createEmployee(
                "admin", "관리자", "admin@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleAdmin, deptHr, posSenior,
                null // 최초 생성자는 creator가 null
        );
        // password 및 isInitialPassword 필드 설정
        creator.updateInitPassword(TEST_PASSWORD, null);

        // creator 저장
        employeeRepository.save(creator);
    }

    /**
     * 테스트용 직원을 생성하되, DB에 저장하지 않고
     * '저장 가능한(persistable)' 상태의 엔티티 객체를 반환하는 헬퍼 메서드
     */
    private Employee createPersistableEmployee(String username, String email, CommonCode role, CommonCode dept, CommonCode pos) {
        // 1. 팩토리 메서드로 객체 생성
        Employee employee = Employee.createEmployee(
                username, "테스트유저", email, "010-1234-5678",
                LocalDate.now(), statusActive, role, dept, pos,
                creator // 이 직원의 생성자는 setUp에서 만든 'creator'
        );

        // 2. 초기 비밀번호 설정 (nullable=false 필드 완료)
        employee.updateInitPassword(TEST_PASSWORD, creator);

        return employee;
    }

    @Test
    @DisplayName("C: 직원 생성(save) 테스트")
    void saveEmployeeTest() {
        // given
        // createEmployee + updateInitPassword 호출로 유효한 엔티티 생성
        Employee newEmployee = createPersistableEmployee("testuser", "testuser@test.com", roleUser, deptDev, posJunior);

        // when
        Employee savedEmployee = employeeRepository.save(newEmployee);

        // then
        assertThat(savedEmployee).isNotNull();
        assertThat(savedEmployee.getEmployeeId()).isNotNull();
        assertThat(savedEmployee.getUsername()).isEqualTo("testuser");
        assertThat(savedEmployee.getPassword()).isEqualTo(TEST_PASSWORD); // updateInitPassword가 설정
        assertThat(savedEmployee.getIsInitialPassword()).isTrue(); // updateInitPassword가 설정
        assertThat(savedEmployee.getCreator()).isEqualTo(creator);
        assertThat(savedEmployee.getCreatedAt()).isNotNull();
        assertThat(savedEmployee.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("R: 직원 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        Employee savedEmployee = employeeRepository.save(
                createPersistableEmployee("findMe", "findme@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Optional<Employee> foundEmployee = employeeRepository.findById(savedEmployee.getEmployeeId());

        // then
        assertThat(foundEmployee).isPresent();
        assertThat(foundEmployee.get().getEmployeeId()).isEqualTo(savedEmployee.getEmployeeId());
    }

    @Test
    @DisplayName("R: 직원 ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<Employee> foundEmployee = employeeRepository.findById(nonExistentId);

        // then
        assertThat(foundEmployee).isNotPresent();
    }

    @Test
    @DisplayName("U: 직원 정보 수정(update) 테스트")
    void updateEmployeeTest() throws InterruptedException {
        // given
        Employee savedEmployee = employeeRepository.save(
                createPersistableEmployee("updateMe", "updateme@test.com", roleUser, deptDev, posJunior)
        );
        LocalDateTime createdAt = savedEmployee.getCreatedAt();

        // @PreUpdate 시간을 명확히 구분하기 위해 잠시 대기
        Thread.sleep(10);

        // when
        // findById로 영속성 컨텍스트에서 가져옴
        Employee employeeToUpdate = employeeRepository.findById(savedEmployee.getEmployeeId()).get();

        // 엔티티의 update 메서드 호출 (JPA 변경 감지 - dirty checking)
        employeeToUpdate.updatePhoneNumber("010-9999-9999", creator);
        employeeToUpdate.updateDepartment(deptHr, creator);

        // @DataJpaTest는 기본적으로 트랜잭션 내에서 실행되며,
        // 변경 감지를 테스트하려면 flush()를 명시적으로 호출
        employeeRepository.flush();

        // 검증을 위해 다시 조회
        Employee updatedEmployee = employeeRepository.findById(savedEmployee.getEmployeeId()).get();

        // then
        assertThat(updatedEmployee.getPhoneNumber()).isEqualTo("010-9999-9999");
        assertThat(updatedEmployee.getDepartment()).isEqualTo(deptHr);
        assertThat(updatedEmployee.getUpdater()).isEqualTo(creator);
        assertThat(updatedEmployee.getUpdatedAt()).isAfter(createdAt); // @PreUpdate 동작 확인
    }

    @Test
    @DisplayName("D: 직원 삭제(deleteById) 테스트")
    void deleteEmployeeTest() {
        // given
        Employee savedEmployee = employeeRepository.save(
                createPersistableEmployee("deleteMe", "deleteme@test.com", roleUser, deptDev, posJunior)
        );
        Long employeeId = savedEmployee.getEmployeeId();
        assertThat(employeeRepository.existsById(employeeId)).isTrue();

        // when
        employeeRepository.deleteById(employeeId);
        employeeRepository.flush(); // 삭제 쿼리 즉시 실행

        // then
        assertThat(employeeRepository.existsById(employeeId)).isFalse();
    }

    // --- Custom Repository Method Tests ---

    @Test
    @DisplayName("Custom: findByUsername 테스트 - 성공")
    void findByUsernameSuccessTest() {
        // given
        employeeRepository.save(
                createPersistableEmployee("user123", "user123@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Optional<Employee> found = employeeRepository.findByUsername("user123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("user123");
    }

    @Test
    @DisplayName("Custom: findByUsername 테스트 - 실패")
    void findByUsernameFailureTest() {
        // given (user123은 저장하지 않음)

        // when
        Optional<Employee> found = employeeRepository.findByUsername("nonexistent");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Custom: existsByEmail 테스트 - 성공 (존재함)")
    void existsByEmailSuccessTest() {
        // given
        employeeRepository.save(
                createPersistableEmployee("user456", "user456@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Boolean exists = employeeRepository.existsByEmail("user456@test.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Custom: existsByEmail 테스트 - 실패 (존재하지 않음)")
    void existsByEmailFailureTest() {
        // given

        // when
        Boolean exists = employeeRepository.existsByEmail("nonexistent@test.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Custom: existsByUsername 테스트 - 성공 (존재함)")
    void existsByUsernameSuccessTest() {
        // given
        employeeRepository.save(
                createPersistableEmployee("user789", "user789@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Boolean exists = employeeRepository.existsByUsername("user789");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Custom: existsByUsername 테스트 - 실패 (존재하지 않음)")
    void existsByUsernameFailureTest() {
        // given
        // "nonexistentUser"는 저장하지 않음

        // when
        Boolean exists = employeeRepository.existsByUsername("nonexistentUser");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Custom: findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc 테스트")
    void findByDepartmentAndOrderByPositionTest() {
        // given
        // 직급 정렬 순서: posSenior (sortOrder 2L), posJunior (sortOrder 1L)
        Employee devSenior = createPersistableEmployee("devSenior", "devSenior@test.com", roleUser, deptDev, posSenior);
        Employee devJunior = createPersistableEmployee("devJunior", "devJunior@test.com", roleUser, deptDev, posJunior);
        Employee hrJunior = createPersistableEmployee("hrJunior", "hrJunior@test.com", roleUser, deptHr, posJunior);

        employeeRepository.saveAll(List.of(devSenior, devJunior, hrJunior));

        // when
        // 개발팀(deptDev) 직원들을 직급(position.sortOrder) 내림차순으로 조회
        List<Employee> devTeam = employeeRepository.findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(deptDev.getCommonCodeId());

        // then
        assertThat(devTeam).hasSize(2);
        assertThat(devTeam).doesNotContain(hrJunior); // 다른 부서 직원은 미포함
        // 정렬 순서 확인: Senior (sortOrder 2) -> Junior (sortOrder 1)
        assertThat(devTeam).containsExactly(devSenior, devJunior);
    }

    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 중복 username 저장 시 예외 발생")
    void saveDuplicateUsernameTest() {
        // given
        // Employee.java의 @Column(unique=true) 제약조건 필요
        employeeRepository.save(
                createPersistableEmployee("duplicateUser", "user1@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Employee duplicateEmployee = createPersistableEmployee("duplicateUser", "user2@test.com", roleUser, deptHr, posSenior);

        // then
        // 동일한 username으로 save 시도 시 DataIntegrityViolationException 발생
        // saveAndFlush()로 즉시 DB에 쿼리를 전송하여 제약조건 위반을 확인
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(duplicateEmployee))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 중복 email 저장 시 예외 발생")
    void saveDuplicateEmailTest() {
        // given
        // Employee.java의 @Column(unique=true) 제약조건 필요
        employeeRepository.save(
                createPersistableEmployee("user1", "duplicate@test.com", roleUser, deptDev, posJunior)
        );

        // when
        Employee duplicateEmployee = createPersistableEmployee("user2", "duplicate@test.com", roleUser, deptHr, posSenior);

        // then
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(duplicateEmployee))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(password) null 저장 시 예외 발생")
    void saveNullPasswordTest() {
        // given
        // 헬퍼 메서드를 쓰지 않고, createEmployee만 호출
        Employee incompleteEmployee = Employee.createEmployee(
                "noPassUser", "nopass@test.com", "Nopass", "010-1111-1111",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior,
                creator
        );
        // updateInitPassword()를 고의로 누락

        // when & then
        // password가 null (nullable=false 위반) 상태로 save 시도
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(incompleteEmployee))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(department) null 저장 시 예외 발생")
    void saveNullDepartmentTest() {
        // given
        Employee incompleteEmployee = Employee.createEmployee(
                "noDeptUser", "nodept@test.com", "Nopass", "010-2222-2222",
                LocalDate.now(), statusActive, roleUser,
                null, // department (nullable=false)를 null로 설정
                posJunior,
                creator
        );
        incompleteEmployee.updateInitPassword(TEST_PASSWORD, creator);

        // when & then
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(incompleteEmployee))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}