package org.goodee.startup_BE.schedule.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.schedule.entity.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
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
@EntityScan(basePackages = "org.goodee.startup_BE")
@EnableJpaRepositories(basePackages = "org.goodee.startup_BE")
class ScheduleRepositoryTest {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private Employee employee;
    private CommonCode categoryWork;
    private CommonCode colorBlue;

    @BeforeEach
    void setUp() {
        scheduleRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- 공통 코드 (status, role, dept, pos) 생성 ---
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 1L, null, false);
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);

        // --- 일정용 공통 코드 (카테고리/색상) 생성 ---
        categoryWork = CommonCode.createCommonCode("SC_WORK", "업무", "WORK", null, null, 1L, null, false);
        colorBlue = CommonCode.createCommonCode("CL_BLUE", "파란색", "BLUE", null, null, 1L, null, false);

        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior, categoryWork, colorBlue));

        // --- 직원 생성 ---
        employee = Employee.createEmployee(
                "tester", "테스트", "test@test.com", "010-0000-0000",
                java.time.LocalDate.now(), statusActive,
                roleUser, deptDev, posJunior, null
        );
        employee.updateInitPassword("1234", null);
        employeeRepository.save(employee);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("C: 일정 등록(save) 테스트")
    void saveScheduleTest() {
        // given
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        Schedule schedule = Schedule.createSchedule(employee, "테스트 일정", "테스트 내용", categoryWork,  start, end);

        // when
        Schedule saved = scheduleRepository.save(schedule);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getScheduleId()).isNotNull();
        assertThat(saved.getEmployee()).isEqualTo(employee);
        assertThat(saved.getCategory()).isEqualTo(categoryWork);
        assertThat(saved.getIsDeleted()).isFalse();
    }

    // ==================== READ ====================

    @Test
    @DisplayName("R: 삭제되지 않은 전체 일정 조회(findByIsDeletedFalse) 테스트")
    void findByIsDeletedFalseTest() {
        // given
        Schedule s1 = Schedule.createSchedule(employee, "회의", "회의 내용", categoryWork, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        Schedule s2 = Schedule.createSchedule(employee, "보고서", "보고서 작성", categoryWork, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        s2.delete(); // soft delete
        scheduleRepository.saveAll(List.of(s1, s2));

        // when
        List<Schedule> result = scheduleRepository.findByIsDeletedFalse();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("회의");
        assertThat(result.get(0).getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("R: 기간별 일정 조회(findByStartTimeBetweenAndIsDeletedFalse) 테스트")
    void findByStartTimeBetweenAndIsDeletedFalseTest() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Schedule s1 = Schedule.createSchedule(employee, "업무1", "내용1", categoryWork,  now.minusDays(2), now.minusDays(2).plusHours(1));
        Schedule s2 = Schedule.createSchedule(employee, "업무2", "내용2", categoryWork,  now.minusDays(1), now.minusDays(1).plusHours(1));
        Schedule s3 = Schedule.createSchedule(employee, "업무3", "내용3", categoryWork, now.plusDays(1), now.plusDays(1).plusHours(1));
        scheduleRepository.saveAll(List.of(s1, s2, s3));

        // when
        List<Schedule> result = scheduleRepository.findByStartTimeBetweenAndIsDeletedFalse(
                now.minusDays(2).minusHours(1),
                now
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Schedule::getTitle).containsExactly("업무1", "업무2");
    }

    @Test
    @DisplayName("R: 단일 일정 조회(findById) 테스트")
    void findByIdTest() {
        // given
        Schedule schedule = Schedule.createSchedule(employee, "단일 일정", "상세 내용", categoryWork, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        Schedule saved = scheduleRepository.save(schedule);

        // when
        Optional<Schedule> found = scheduleRepository.findById(saved.getScheduleId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("단일 일정");
    }
}