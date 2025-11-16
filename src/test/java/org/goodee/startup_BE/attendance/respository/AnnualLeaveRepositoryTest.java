package org.goodee.startup_BE.attendance.respository;

import org.goodee.startup_BE.attendance.entity.AnnualLeave;
import org.goodee.startup_BE.attendance.repository.AnnualLeaveRepository;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackageClasses = {
        AnnualLeave.class, Employee.class, CommonCode.class
})
@EnableJpaRepositories(basePackageClasses = {
        AnnualLeaveRepository.class, EmployeeRepository.class, CommonCodeRepository.class
})
class AnnualLeaveRepositoryTest {

    @Autowired
    private AnnualLeaveRepository annualLeaveRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private Employee employee;

    @BeforeEach
    void setUp() {
        annualLeaveRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- 공통 코드 생성 ---
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 3L, null, false);
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 4L, null, false);
        commonCodeRepository.saveAll(List.of(deptDev, roleUser, posJunior, statusActive));

        // --- 직원 생성 ---
        employee = Employee.createEmployee(
                "tester", "테스트", "test@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior, null
        );
        employee.updateInitPassword("1234", null);
        employeeRepository.save(employee);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("C: 연차 자동 생성(createInitialLeave) 테스트")
    void saveAnnualLeaveTest() {
        // given
        AnnualLeave newLeave = AnnualLeave.createInitialLeave(employee);

        // when
        AnnualLeave saved = annualLeaveRepository.save(newLeave);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getLeaveId()).isNotNull();
        assertThat(saved.getEmployee().getEmployeeId()).isEqualTo(employee.getEmployeeId());
        assertThat(saved.getTotalDays()).isEqualTo(15L);
        assertThat(saved.getUsedDays()).isEqualTo(0L);
        assertThat(saved.getYear()).isEqualTo((long) LocalDate.now().getYear());
        assertThat(saved.getIsDeleted()).isFalse();
    }

    // ==================== READ ====================

    @Test
    @DisplayName("R: findByEmployeeEmployeeIdAndYear() - 직원 + 연도 기준 조회 테스트")
    void findByEmployeeEmployeeIdAndYearTest() {
        // given
        AnnualLeave leave = AnnualLeave.createInitialLeave(employee);
        annualLeaveRepository.save(leave);

        // when
        Long employeeId = employee.getEmployeeId();
        Long year = leave.getYear();
        Optional<AnnualLeave> found = annualLeaveRepository.findByEmployeeEmployeeIdAndYear(employeeId, year);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmployee().getEmployeeId()).isEqualTo(employeeId);
        assertThat(found.get().getTotalDays()).isEqualTo(15L);
        assertThat(found.get().getUsedDays()).isEqualTo(0L);
        assertThat(found.get().getIsDeleted()).isFalse();
    }
}