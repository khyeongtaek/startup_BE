package org.goodee.startup_BE.attendance.respository;

import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.entity.AttendanceWorkHistory;
import org.goodee.startup_BE.attendance.repository.AttendanceRepository;
import org.goodee.startup_BE.attendance.repository.AttendanceWorkHistoryRepository;
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
@EnableJpaRepositories(basePackages = "org.goodee.startup_BE")
class AttendanceWorkHistoryRepositoryTest {

    @Autowired
    private AttendanceWorkHistoryRepository historyRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private Employee employee;
    private CommonCode wsNormal;
    private CommonCode wsLate;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- 공통 코드 (부서/직급/상태) 생성 ---
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 1L, null, false);
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        commonCodeRepository.saveAll(List.of(deptDev, posJunior, roleUser, statusActive));

        // --- 테스트용 직원 생성 ---
        employee = Employee.createEmployee(
                "tester", "테스트", "test@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior, null
        );
        employee.updateInitPassword("1234", null);
        employeeRepository.save(employee);

        // --- 근무 상태 코드 생성 ---
        wsNormal = CommonCode.createCommonCode("WS_NORMAL", "정상근무", "NORMAL", null, null, 1L, employee, false);
        wsLate = CommonCode.createCommonCode("WS_LATE", "지각", "LATE", null, null, 2L, employee, false);
        commonCodeRepository.saveAll(List.of(wsNormal, wsLate));

        // --- 출근 기록 생성 ---
        attendance = Attendance.createAttendance(employee, LocalDate.now(), wsNormal);
        attendanceRepository.save(attendance);
    }

    @Test
    @DisplayName("C: 근무 이력 저장(save) 테스트")
    void saveHistoryTest() {
        // given
        AttendanceWorkHistory history = AttendanceWorkHistory.builder()
                .attendance(attendance)
                .employee(employee)
                .actionCode(wsNormal)
                .actionTime(LocalDateTime.now())
                .build();

        // when
        AttendanceWorkHistory saved = historyRepository.save(history);

        // then
        assertThat(saved.getHistoryId()).isNotNull();
        assertThat(saved.getEmployee().getEmployeeId()).isEqualTo(employee.getEmployeeId());
        assertThat(saved.getActionCode().getValue1()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("R: 사원별 이력 조회(findByEmployeeEmployeeIdOrderByActionTimeDesc) 테스트")
    void findByEmployeeEmployeeIdOrderByActionTimeDescTest() {
        // given
        historyRepository.save(AttendanceWorkHistory.builder()
                .attendance(attendance).employee(employee).actionCode(wsNormal)
                .actionTime(LocalDateTime.now().minusMinutes(2)).build());
        historyRepository.save(AttendanceWorkHistory.builder()
                .attendance(attendance).employee(employee).actionCode(wsLate)
                .actionTime(LocalDateTime.now()).build());

        // when
        List<AttendanceWorkHistory> result = historyRepository.findByEmployeeEmployeeIdOrderByActionTimeDesc(employee.getEmployeeId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getActionCode().getValue1()).isEqualTo("LATE");
    }

    @Test
    @DisplayName("R: 최근 이력 1건 조회(findTopByEmployeeEmployeeIdOrderByActionTimeDesc) 테스트")
    void findTopByEmployeeEmployeeIdOrderByActionTimeDescTest() {
        // given
        historyRepository.save(AttendanceWorkHistory.builder()
                .attendance(attendance).employee(employee).actionCode(wsNormal)
                .actionTime(LocalDateTime.now().minusMinutes(1)).build());
        historyRepository.save(AttendanceWorkHistory.builder()
                .attendance(attendance).employee(employee).actionCode(wsLate)
                .actionTime(LocalDateTime.now()).build());

        // when
        AttendanceWorkHistory last = historyRepository.findTopByEmployeeEmployeeIdOrderByActionTimeDesc(employee.getEmployeeId());

        // then
        assertThat(last).isNotNull();
        assertThat(last.getActionCode().getValue1()).isEqualTo("LATE");
    }
}