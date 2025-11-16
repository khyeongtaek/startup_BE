package org.goodee.startup_BE.attendance.respository;

import org.goodee.startup_BE.attendance.entity.Attendance;
import org.goodee.startup_BE.attendance.repository.AttendanceRepository;
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
        "spring.jpa.hibernate.ddl-auto=create-drop",  // 테스트 시 테이블 생성 후 삭제
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackageClasses = {Attendance.class, Employee.class, CommonCode.class})
@EnableJpaRepositories(basePackageClasses = {
        AttendanceRepository.class,
        EmployeeRepository.class,
        CommonCodeRepository.class
})
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private Employee employee;
    private CommonCode wsNormal;
    private CommonCode wsClockOut;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- 공통 코드 (부서, 역할, 직급, 상태) 생성 ---
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 1L, null, false);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        commonCodeRepository.saveAll(List.of(deptDev, roleUser, posJunior, statusActive));

        // --- 테스트용 직원 생성 ---
        employee = Employee.createEmployee(
                "tester", "테스트", "test@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior, null
        );
        employee.updateInitPassword("1234", null);
        employeeRepository.save(employee);

        // --- 근무 상태 코드 생성 ---
        wsNormal = CommonCode.createCommonCode("WS_NORMAL", "정상근무", "NORMAL", null, null, 1L, employee, false);
        wsClockOut = CommonCode.createCommonCode("WS_CLOCK_OUT", "퇴근", "CLOCK_OUT", null, null, 2L, employee, false);
        commonCodeRepository.saveAll(List.of(wsNormal, wsClockOut));
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("C: 출근 기록 생성(save) 테스트")
    void saveAttendanceTest() {
        // given
        Attendance attendance = Attendance.createAttendance(employee, LocalDate.now(), wsNormal);

        // when
        Attendance saved = attendanceRepository.save(attendance);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getAttendanceId()).isNotNull();
        assertThat(saved.getEmployee()).isEqualTo(employee);
        assertThat(saved.getWorkStatus()).isEqualTo(wsNormal);
        assertThat(saved.getAttendanceDate()).isEqualTo(LocalDate.now());
    }

    // ==================== READ ====================

    @Test
    @DisplayName("R: 오늘 출근 기록 조회(findByEmployeeEmployeeIdAndAttendanceDate) 테스트")
    void findByEmployeeEmployeeIdAndAttendanceDateTest() {
        // given
        Attendance attendance = Attendance.createAttendance(employee, LocalDate.now(), wsNormal);
        attendanceRepository.save(attendance);

        // when
        Optional<Attendance> found = attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDate(
                employee.getEmployeeId(), LocalDate.now()
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmployee().getEmployeeId()).isEqualTo(employee.getEmployeeId());
    }

    @Test
    @DisplayName("R: 근무 중인 출근 기록 조회(findCurrentWorkingRecord) 테스트")
    void findCurrentWorkingRecordTest() {
        // given
        attendanceRepository.save(Attendance.createAttendance(employee, LocalDate.now(), wsNormal));

        // when
        Optional<Attendance> found = attendanceRepository.findCurrentWorkingRecord(employee.getEmployeeId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getWorkStatus().getValue1()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("R: 특정 기간 내 근태 목록 조회(findByEmployeeEmployeeIdAndAttendanceDateBetween) 테스트")
    void findByEmployeeEmployeeIdAndAttendanceDateBetweenTest() {
        // given
        LocalDate today = LocalDate.now();
        attendanceRepository.save(Attendance.createAttendance(employee, today.minusDays(2), wsNormal));
        attendanceRepository.save(Attendance.createAttendance(employee, today.minusDays(1), wsNormal));
        attendanceRepository.save(Attendance.createAttendance(employee, today, wsNormal));

        // when
        List<Attendance> list = attendanceRepository.findByEmployeeEmployeeIdAndAttendanceDateBetween(
                employee.getEmployeeId(), today.minusDays(3), today
        );

        // then
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getEmployee()).isEqualTo(employee);
    }

    @Test
    @DisplayName("R: 삭제되지 않은 전체 근태 목록 조회(findByIsDeletedIsFalse) 테스트")
    void findByIsDeletedIsFalseTest() {
        // given
        Attendance attendance1 = Attendance.createAttendance(employee, LocalDate.now(), wsNormal);
        Attendance attendance2 = Attendance.createAttendance(employee, LocalDate.now().minusDays(1), wsNormal);
        attendance2.delete(); // soft delete
        attendanceRepository.saveAll(List.of(attendance1, attendance2));

        // when
        List<Attendance> result = attendanceRepository.findByIsDeletedIsFalse();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsDeleted()).isFalse();
    }
}