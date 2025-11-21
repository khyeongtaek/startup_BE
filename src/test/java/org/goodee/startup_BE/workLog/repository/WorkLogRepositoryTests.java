package org.goodee.startup_BE.workLog.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.entity.WorkLogRead;
import org.goodee.startup_BE.work_log.repository.WorkLogReadRepository;
import org.goodee.startup_BE.work_log.repository.WorkLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
class WorkLogRepositoryTest {
	
	@Autowired
	private WorkLogRepository workLogRepository;
	
	@Autowired
	private WorkLogReadRepository workLogReadRepository;
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	@Autowired
	private EntityManager entityManager;
	
	private final String TEST_PASSWORD = "testPassword123!";
	
	private Employee creator;
	private Employee writer;
	private Employee deptMate;
	private Employee otherDeptEmployee;
	
	private CommonCode statusActive;
	private CommonCode roleAdmin;
	private CommonCode roleUser;
	private CommonCode deptDev;
	private CommonCode deptHr;
	private CommonCode posJunior;
	private CommonCode posSenior;
	
	private CommonCode workTypeRoot;
	private CommonCode workTypeDev;
	private CommonCode workTypeOps;
	private CommonCode workOptionRoot;
	private CommonCode workOptionCoding;
	private CommonCode workOptionMeeting;
	
	@BeforeEach
	void setUp() {
		workLogReadRepository.deleteAll();
		workLogRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// 공통 코드 (직원 상태/권한/부서/직급)
		statusActive = commonCodeRepository.save(
			CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false)
		);
		roleAdmin = commonCodeRepository.save(
			CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null, false)
		);
		roleUser = commonCodeRepository.save(
			CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false)
		);
		deptDev = commonCodeRepository.save(
			CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false)
		);
		deptHr = commonCodeRepository.save(
			CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null, false)
		);
		posJunior = commonCodeRepository.save(
			CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false)
		);
		posSenior = commonCodeRepository.save(
			CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null, false)
		);
		
		// creator
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive,
			roleAdmin, deptHr, posSenior,
			null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		creator = employeeRepository.save(creator);
		
		// 업무일지용 공통 코드 (WT / WO)
		workTypeRoot = commonCodeRepository.save(
			CommonCode.createCommonCode("WT0", "업무구분 ROOT", null, null, null, 0L, creator, false)
		);
		workTypeDev = commonCodeRepository.save(
			CommonCode.createCommonCode("WT1", "개발", "DEV", null, null, 1L, creator, false)
		);
		workTypeOps = commonCodeRepository.save(
			CommonCode.createCommonCode("WT2", "운영", "OPS", null, null, 2L, creator, false)
		);
		workOptionRoot = commonCodeRepository.save(
			CommonCode.createCommonCode("WO0", "업무옵션 ROOT", null, null, null, 0L, creator, false)
		);
		workOptionCoding = commonCodeRepository.save(
			CommonCode.createCommonCode("WO1", "코딩", "CODING", null, null, 1L, creator, false)
		);
		workOptionMeeting = commonCodeRepository.save(
			CommonCode.createCommonCode("WO2", "회의", "MEETING", null, null, 2L, creator, false)
		);
		
		// 직원들
		writer = employeeRepository.save(
			createPersistableEmployee("writer", "writer@test.com", roleUser, deptDev, posJunior)
		);
		deptMate = employeeRepository.save(
			createPersistableEmployee("deptMate", "deptMate@test.com", roleUser, deptDev, posJunior)
		);
		otherDeptEmployee = employeeRepository.save(
			createPersistableEmployee("otherDept", "otherDept@test.com", roleUser, deptHr, posJunior)
		);
	}
	
	private Employee createPersistableEmployee(String username, String email,
	                                           CommonCode role, CommonCode dept, CommonCode pos) {
		Employee employee = Employee.createEmployee(
			username, "테스트유저", email, "010-1234-5678",
			LocalDate.now(), statusActive,
			role, dept, pos,
			creator
		);
		employee.updateInitPassword(TEST_PASSWORD, creator);
		return employee;
	}
	
	private WorkLog createWorkLogEntity(Employee employee, String title, String content,
	                                    CommonCode workType, CommonCode workOption,
	                                    LocalDateTime workDate) {
		return WorkLog.createWorkLog(
			employee, workType, workOption,
			workDate, title, content
		);
	}
	
	// C + R 기본 CRUD
	
	@Test
	@DisplayName("C/R: 업무일지 저장 후 ID로 조회")
	void saveAndFindById() {
		WorkLog workLog = createWorkLogEntity(
			writer,
			"저장 테스트",
			"내용",
			workTypeDev,
			workOptionCoding,
			LocalDateTime.now().minusDays(1)
		);
		
		WorkLog saved = workLogRepository.save(workLog);
		
		Optional<WorkLog> found = workLogRepository.findById(saved.getWorkLogId());
		
		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("저장 테스트");
		assertThat(found.get().getEmployee().getUsername()).isEqualTo("writer");
		assertThat(found.get().getWorkType().getCode()).isEqualTo("WT1");
		assertThat(found.get().getWorkOption().getCode()).isEqualTo("WO1");
	}
	
	@Test
	@DisplayName("R: 존재하지 않는 업무일지 ID 조회 시 비어있는 Optional 반환")
	void findById_notFound() {
		Optional<WorkLog> found = workLogRepository.findById(9999L);
		assertThat(found).isNotPresent();
	}
	
	// U
	
	@Test
	@DisplayName("U: 업무일지 업데이트 - 엔티티 메서드 + flush")
	void updateWorkLog() {
		WorkLog workLog = createWorkLogEntity(
			writer,
			"원본 제목",
			"원본 내용",
			workTypeDev,
			workOptionCoding,
			LocalDateTime.now().minusDays(1)
		);
		WorkLog saved = workLogRepository.save(workLog);
		
		LocalDateTime newDate = LocalDateTime.now().minusDays(2);
		saved.updateWorkLog(
			workTypeOps,
			workOptionMeeting,
			newDate,
			"수정된 제목",
			"수정된 내용"
		);
		workLogRepository.flush();
		
		WorkLog updated = workLogRepository.findById(saved.getWorkLogId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo("수정된 제목");
		assertThat(updated.getContent()).isEqualTo("수정된 내용");
		assertThat(updated.getWorkType().getCode()).isEqualTo("WT2");
		assertThat(updated.getWorkOption().getCode()).isEqualTo("WO2");
		assertThat(updated.getWorkDate()).isEqualTo(newDate);
	}
	
	// D (soft delete + @Where)
	
	@Test
	@DisplayName("D: 업무일지 소프트 삭제 - isDeleted 플래그 및 @Where 동작 확인")
	void softDeleteWorkLog_andWhereFilter() {
		WorkLog workLog = createWorkLogEntity(
			writer,
			"삭제 테스트",
			"내용",
			workTypeDev,
			workOptionCoding,
			LocalDateTime.now().minusDays(1)
		);
		WorkLog saved = workLogRepository.saveAndFlush(workLog);
		
		// 소프트 삭제
		saved.deleteWorkLog();
		workLogRepository.saveAndFlush(saved);
		
		// 1차 캐시 비우고 다시 조회해야 @Where가 적용된 쿼리 수행됨
		entityManager.clear();
		
		Optional<WorkLog> found = workLogRepository.findById(saved.getWorkLogId());
		assertThat(found).isNotPresent();
	}
	
	// WorkLogReadRepository existsByWorkLogAndEmployee
	
	@Test
	@DisplayName("WorkLogRead: existsByWorkLogAndEmployee - 읽음/미읽음 여부 판별")
	void workLogRead_existsByWorkLogAndEmployee() {
		WorkLog workLog = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"읽음 테스트",
				"내용",
				workTypeDev,
				workOptionCoding,
				LocalDateTime.now().minusDays(1)
			)
		);
		
		WorkLogRead read = WorkLogRead.createWorkLogRead(workLog, writer);
		workLogReadRepository.save(read);
		
		boolean existsForWriter = workLogReadRepository.existsByWorkLogAndEmployee(workLog, writer);
		boolean existsForDeptMate = workLogReadRepository.existsByWorkLogAndEmployee(workLog, deptMate);
		
		assertThat(existsForWriter).isTrue();
		assertThat(existsForDeptMate).isFalse();
	}
	
	// findWithRead 커스텀 쿼리
	
	@Test
	@DisplayName("findWithRead: 전체 조회(type=all 역할) - 읽음 여부 포함")
	void findWithRead_all() {
		WorkLog log1 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-1",
				"내용1",
				workTypeDev,
				workOptionCoding,
				LocalDateTime.now().minusDays(1)
			)
		);
		WorkLog log2 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-2",
				"내용2",
				workTypeDev,
				workOptionMeeting,
				LocalDateTime.now().minusDays(2)
			)
		);
		WorkLog log3 = workLogRepository.save(
			createWorkLogEntity(
				deptMate,
				"deptMate-log-1",
				"내용3",
				workTypeOps,
				workOptionCoding,
				LocalDateTime.now().minusDays(3)
			)
		);
		WorkLog log4 = workLogRepository.save(
			createWorkLogEntity(
				otherDeptEmployee,
				"otherDept-log-1",
				"내용4",
				workTypeOps,
				workOptionMeeting,
				LocalDateTime.now().minusDays(4)
			)
		);
		
		// writer가 log1만 읽음 상태
		workLogReadRepository.save(WorkLogRead.createWorkLogRead(log1, writer));
		
		Pageable pageable = PageRequest.of(0, 10);
		Page<WorkLogResponseDTO> page = workLogRepository.findWithRead(
			writer.getEmployeeId(),
			null,
			false,
			pageable
		);
		
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2", "deptMate-log-1", "otherDept-log-1");
		
		List<String> readTitles = page.getContent()
			                          .stream()
			                          .filter(WorkLogResponseDTO::getIsRead)
			                          .map(WorkLogResponseDTO::getTitle)
			                          .toList();
		
		assertThat(readTitles).containsExactlyInAnyOrder("writer-log-1");
	}
	
	@Test
	@DisplayName("findWithRead: 부서별 조회(dept) - deptId 조건만 적용")
	void findWithRead_deptFilter() {
		WorkLog log1 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-1",
				"내용1",
				workTypeDev,
				workOptionCoding,
				LocalDateTime.now().minusDays(1)
			)
		);
		WorkLog log2 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-2",
				"내용2",
				workTypeDev,
				workOptionMeeting,
				LocalDateTime.now().minusDays(2)
			)
		);
		WorkLog log3 = workLogRepository.save(
			createWorkLogEntity(
				deptMate,
				"deptMate-log-1",
				"내용3",
				workTypeOps,
				workOptionCoding,
				LocalDateTime.now().minusDays(3)
			)
		);
		WorkLog log4 = workLogRepository.save(
			createWorkLogEntity(
				otherDeptEmployee,
				"otherDept-log-1",
				"내용4",
				workTypeOps,
				workOptionMeeting,
				LocalDateTime.now().minusDays(4)
			)
		);
		
		Pageable pageable = PageRequest.of(0, 10);
		Page<WorkLogResponseDTO> page = workLogRepository.findWithRead(
			writer.getEmployeeId(),
			deptDev.getCommonCodeId(),   // 개발팀만
			false,
			pageable
		);
		
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2", "deptMate-log-1");
	}
	
	@Test
	@DisplayName("findWithRead: onlyMine=true - 내 것만 조회")
	void findWithRead_onlyMine() {
		WorkLog log1 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-1",
				"내용1",
				workTypeDev,
				workOptionCoding,
				LocalDateTime.now().minusDays(1)
			)
		);
		WorkLog log2 = workLogRepository.save(
			createWorkLogEntity(
				writer,
				"writer-log-2",
				"내용2",
				workTypeDev,
				workOptionMeeting,
				LocalDateTime.now().minusDays(2)
			)
		);
		WorkLog log3 = workLogRepository.save(
			createWorkLogEntity(
				deptMate,
				"deptMate-log-1",
				"내용3",
				workTypeOps,
				workOptionCoding,
				LocalDateTime.now().minusDays(3)
			)
		);
		WorkLog log4 = workLogRepository.save(
			createWorkLogEntity(
				otherDeptEmployee,
				"otherDept-log-1",
				"내용4",
				workTypeOps,
				workOptionMeeting,
				LocalDateTime.now().minusDays(4)
			)
		);
		
		Pageable pageable = PageRequest.of(0, 10);
		Page<WorkLogResponseDTO> page = workLogRepository.findWithRead(
			writer.getEmployeeId(),
			null,
			true,     // onlyMine
			pageable
		);
		
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2");
	}
	
	@Test
	@DisplayName("findWithRead: 직원 FK가 NULL인 업무일지는 조회되지 않는다")
	void findWithRead_employeeNull() {
		// 1. 직원이 있는 상태로 업무일지 생성/저장
		WorkLog workLog = createWorkLogEntity(
			writer,
			"작성자 없음 시뮬레이션",
			"내용",
			workTypeDev,
			workOptionCoding,
			LocalDateTime.now().minusDays(1)
		);
		WorkLog saved = workLogRepository.saveAndFlush(workLog);
		
		// 2. 직원 FK를 NULL로 변경 (삭제된 직원 시뮬레이션)
		entityManager.createNativeQuery(
				"UPDATE tbl_work_log SET employee_id = NULL WHERE work_log_id = :id"
			)
			.setParameter("id", saved.getWorkLogId())
			.executeUpdate();
		
		entityManager.clear();
		
		Pageable pageable = PageRequest.of(0, 10);
		Page<WorkLogResponseDTO> page = workLogRepository.findWithRead(
			writer.getEmployeeId(),
			null,
			false,
			pageable
		);
		
		// employee_id 가 NULL인 업무일지는 이 쿼리 결과에 포함되지 않는다
		assertThat(page.getTotalElements()).isZero();
	}
}
