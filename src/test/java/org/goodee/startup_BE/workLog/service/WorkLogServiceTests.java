package org.goodee.startup_BE.workLog.service;

import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.CommonCodeService;
import org.goodee.startup_BE.common.service.CommonCodeServiceImpl;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.work_log.dto.WorkLogCodeListDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.repository.WorkLogReadRepository;
import org.goodee.startup_BE.work_log.repository.WorkLogRepository;
import org.goodee.startup_BE.work_log.service.WorkLogService;
import org.goodee.startup_BE.work_log.service.WorkLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
@Import({WorkLogServiceImpl.class, CommonCodeServiceImpl.class})
class WorkLogServiceTests {
	
	@Autowired
	private WorkLogService workLogService;
	
	@Autowired
	private WorkLogRepository workLogRepository;
	
	@Autowired
	private WorkLogReadRepository workLogReadRepository;
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	@Autowired
	private CommonCodeService commonCodeService;
	
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
		
		// 공통 코드 기본 데이터 (직원 상태/권한/부서/직급)
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
		
		// 생성자(creator) 직원
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive,
			roleAdmin, deptHr, posSenior,
			null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		creator = employeeRepository.save(creator);
		creator.updateInitPassword(TEST_PASSWORD, null);
		creator = employeeRepository.save(creator);
		
		// 업무일지용 공통 코드 (WT / WO prefix)
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
		
		// 테스트용 직원들
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
	
	private WorkLogRequestDTO createWorkLogRequest(Long workTypeId, Long workOptionId, String title, String content, LocalDateTime workDate) {
		return WorkLogRequestDTO.builder()
			       .workTypeId(workTypeId)
			       .workOptionId(workOptionId)
			       .workDate(workDate)
			       .title(title)
			       .content(content)
			       .build();
	}
	
	private WorkLogResponseDTO createSampleWorkLogForEmployee(Employee employee, String title) {
		WorkLogRequestDTO dto = createWorkLogRequest(
			workTypeDev.getCommonCodeId(),
			workOptionCoding.getCommonCodeId(),
			title,
			"내용 - " + title,
			LocalDateTime.now().minusDays(1)
		);
		return workLogService.saveWorkLog(dto, employee.getUsername());
	}
	
	// ---------------------------
	// saveWorkLog (C)
	// ---------------------------
	
	@Test
	@DisplayName("C: 업무일지 등록 - 성공")
	void saveWorkLog_success() {
		WorkLogRequestDTO request = createWorkLogRequest(
			workTypeDev.getCommonCodeId(),
			workOptionCoding.getCommonCodeId(),
			"업무일지 등록 테스트",
			"테스트 내용",
			LocalDateTime.now().minusMinutes(1)
		);
		
		WorkLogResponseDTO response = workLogService.saveWorkLog(request, writer.getUsername());
		
		assertThat(response).isNotNull();
		assertThat(response.getWorkLogId()).isNotNull();
		assertThat(response.getTitle()).isEqualTo("업무일지 등록 테스트");
		assertThat(response.getEmployeeName()).isEqualTo(writer.getName());
		
		List<WorkLog> all = workLogRepository.findAll();
		assertThat(all).hasSize(1);
		assertThat(all.get(0).getTitle()).isEqualTo("업무일지 등록 테스트");
	}
	
	@Test
	@DisplayName("C: 업무일지 등록 - 실패 (존재하지 않는 직원)")
	void saveWorkLog_employeeNotFound() {
		WorkLogRequestDTO request = createWorkLogRequest(
			workTypeDev.getCommonCodeId(),
			workOptionCoding.getCommonCodeId(),
			"업무일지 등록 실패 케이스",
			"내용",
			LocalDateTime.now().minusMinutes(1)
		);
		
		assertThatThrownBy(() -> workLogService.saveWorkLog(request, "unknownUser"))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("직원이 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("C: 업무일지 등록 - 실패 (존재하지 않는 업무구분 코드)")
	void saveWorkLog_workTypeNotFound() {
		WorkLogRequestDTO request = createWorkLogRequest(
			9999L,
			workOptionCoding.getCommonCodeId(),
			"업무일지 등록 실패 - 업무구분",
			"내용",
			LocalDateTime.now().minusMinutes(1)
		);
		
		assertThatThrownBy(() -> workLogService.saveWorkLog(request, writer.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무분류 코드가 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("C: 업무일지 등록 - 실패 (존재하지 않는 업무옵션 코드)")
	void saveWorkLog_workOptionNotFound() {
		WorkLogRequestDTO request = createWorkLogRequest(
			workTypeDev.getCommonCodeId(),
			9999L,
			"업무일지 등록 실패 - 업무옵션",
			"내용",
			LocalDateTime.now().minusMinutes(1)
		);
		
		assertThatThrownBy(() -> workLogService.saveWorkLog(request, writer.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무옵션 코드가 존재하지 않습니다.");
	}
	
	// ---------------------------
	// updateWorkLog (U)
	// ---------------------------
	
	@Test
	@DisplayName("U: 업무일지 수정 - 성공")
	void updateWorkLog_success() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "원본 제목");
		
		WorkLogRequestDTO updateRequest = WorkLogRequestDTO.builder()
			                                  .workLogId(created.getWorkLogId())
			                                  .workTypeId(workTypeOps.getCommonCodeId())
			                                  .workOptionId(workOptionMeeting.getCommonCodeId())
			                                  .workDate(LocalDateTime.now().minusDays(2))
			                                  .title("수정된 제목")
			                                  .content("수정된 내용")
			                                  .build();
		
		WorkLogResponseDTO updated = workLogService.updateWorkLog(updateRequest, writer.getUsername());
		
		assertThat(updated.getWorkLogId()).isEqualTo(created.getWorkLogId());
		assertThat(updated.getTitle()).isEqualTo("수정된 제목");
		assertThat(updated.getContent()).isEqualTo("수정된 내용");
		assertThat(updated.getWorkTypeId()).isEqualTo(workTypeOps.getCommonCodeId());
		assertThat(updated.getWorkOptionId()).isEqualTo(workOptionMeeting.getCommonCodeId());
		
		WorkLog entity = workLogRepository.findById(created.getWorkLogId()).orElseThrow();
		assertThat(entity.getTitle()).isEqualTo("수정된 제목");
		assertThat(entity.getContent()).isEqualTo("수정된 내용");
	}
	
	@Test
	@DisplayName("U: 업무일지 수정 - 실패 (존재하지 않는 업무일지)")
	void updateWorkLog_notFound() {
		WorkLogRequestDTO updateRequest = WorkLogRequestDTO.builder()
			                                  .workLogId(9999L)
			                                  .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(updateRequest, writer.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무일지가 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("U: 업무일지 수정 - 실패 (작성자 아님)")
	void updateWorkLog_accessDenied() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "수정 권한 테스트");
		
		WorkLogRequestDTO updateRequest = WorkLogRequestDTO.builder()
			                                  .workLogId(created.getWorkLogId())
			                                  .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(updateRequest, "notOwner"))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("수정 권한이 없습니다.");
	}
	
	@Test
	@DisplayName("U: 업무일지 수정 - 실패 (업무구분 코드 없음)")
	void updateWorkLog_workTypeNotFound() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "업무구분 수정 실패");
		
		WorkLogRequestDTO updateRequest = WorkLogRequestDTO.builder()
			                                  .workLogId(created.getWorkLogId())
			                                  .workTypeId(9999L)
			                                  .workOptionId(workOptionCoding.getCommonCodeId())
			                                  .workDate(LocalDateTime.now())
			                                  .title("제목")
			                                  .content("내용")
			                                  .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(updateRequest, writer.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무구분 코드가 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("U: 업무일지 수정 - 실패 (업무옵션 코드 없음)")
	void updateWorkLog_workOptionNotFound() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "업무옵션 수정 실패");
		
		WorkLogRequestDTO updateRequest = WorkLogRequestDTO.builder()
			                                  .workLogId(created.getWorkLogId())
			                                  .workTypeId(workTypeDev.getCommonCodeId())
			                                  .workOptionId(9999L)
			                                  .workDate(LocalDateTime.now())
			                                  .title("제목")
			                                  .content("내용")
			                                  .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(updateRequest, writer.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무옵션 코드가 존재하지 않습니다.");
	}
	
	// ---------------------------
	// deleteWorkLog (D - soft delete)
	// ---------------------------
	
	@Test
	@DisplayName("D: 업무일지 삭제(소프트 삭제) - 성공")
	void deleteWorkLog_success() {
		WorkLogResponseDTO log1 = createSampleWorkLogForEmployee(writer, "삭제 대상 1");
		WorkLogResponseDTO log2 = createSampleWorkLogForEmployee(writer, "삭제 대상 2");
		
		workLogService.deleteWorkLog(List.of(log1.getWorkLogId(), log2.getWorkLogId()), writer.getUsername());
		
		WorkLog deleted1 = workLogRepository.findById(log1.getWorkLogId()).orElseThrow();
		WorkLog deleted2 = workLogRepository.findById(log2.getWorkLogId()).orElseThrow();
		
		assertThat(deleted1.getIsDeleted()).isTrue();
		assertThat(deleted2.getIsDeleted()).isTrue();
	}
	
	@Test
	@DisplayName("D: 업무일지 삭제 - 실패 (존재하지 않는 ID 포함)")
	void deleteWorkLog_notFound() {
		WorkLogResponseDTO log1 = createSampleWorkLogForEmployee(writer, "삭제 대상 1");
		
		assertThatThrownBy(() ->
			                   workLogService.deleteWorkLog(List.of(log1.getWorkLogId(), 9999L), writer.getUsername())
		).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("존재하지 않는 업무일지가 있습니다.");
	}
	
	@Test
	@DisplayName("D: 업무일지 삭제 - 실패 (삭제 권한 없는 업무일지 포함)")
	void deleteWorkLog_accessDenied() {
		WorkLogResponseDTO log1 = createSampleWorkLogForEmployee(writer, "삭제 권한 테스트");
		
		assertThatThrownBy(() ->
			                   workLogService.deleteWorkLog(List.of(log1.getWorkLogId()), "notOwner")
		).isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("삭제 권한이 없는 업무일지가 포함되어 있습니다.");
	}
	
	// ---------------------------
	// getWorkLogDetail (R - 단건 조회 + 읽음 처리)
	// ---------------------------
	
	@Test
	@DisplayName("R: 업무일지 상세 조회 - 성공 및 읽음 처리")
	void getWorkLogDetail_successAndMarkRead() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "상세 조회 테스트");
		
		WorkLogResponseDTO detail = workLogService.getWorkLogDetail(created.getWorkLogId(), writer.getUsername());
		
		assertThat(detail.getWorkLogId()).isEqualTo(created.getWorkLogId());
		assertThat(detail.getIsRead()).isTrue();
		
		WorkLog workLog = workLogRepository.findById(created.getWorkLogId()).orElseThrow();
		Employee persistedWriter = employeeRepository.findByUsername(writer.getUsername()).orElseThrow();
		
		boolean existsRead = workLogReadRepository.existsByWorkLogAndEmployee(workLog, persistedWriter);
		assertThat(existsRead).isTrue();
	}
	
	@Test
	@DisplayName("R: 업무일지 상세 조회 - 두 번 조회해도 읽음 기록 중복 생성 안됨")
	void getWorkLogDetail_secondReadDoesNotDuplicate() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "중복 읽음 테스트");
		
		WorkLogResponseDTO first = workLogService.getWorkLogDetail(created.getWorkLogId(), writer.getUsername());
		long countAfterFirst = workLogReadRepository.count();
		
		WorkLogResponseDTO second = workLogService.getWorkLogDetail(created.getWorkLogId(), writer.getUsername());
		long countAfterSecond = workLogReadRepository.count();
		
		assertThat(first.getIsRead()).isTrue();
		assertThat(second.getIsRead()).isTrue();
		assertThat(countAfterSecond).isEqualTo(countAfterFirst);
	}
	
	@Test
	@DisplayName("R: 업무일지 상세 조회 - 실패 (직원 없음)")
	void getWorkLogDetail_employeeNotFound() {
		WorkLogResponseDTO created = createSampleWorkLogForEmployee(writer, "직원 없음 테스트");
		
		assertThatThrownBy(() ->
			                   workLogService.getWorkLogDetail(created.getWorkLogId(), "unknownUser")
		).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("직원이 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("R: 업무일지 상세 조회 - 실패 (업무일지 없음)")
	void getWorkLogDetail_workLogNotFound() {
		assertThatThrownBy(() ->
			                   workLogService.getWorkLogDetail(9999L, writer.getUsername())
		).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("업무일지가 존재하지 않습니다.");
	}
	
	// ---------------------------
	// getWorkLogList (R - 목록 조회)
	// ---------------------------
	
	@Test
	@DisplayName("R: 업무일지 목록 조회 - type=all (전체)")
	void getWorkLogList_all() {
		createSampleWorkLogForEmployee(writer, "writer-log-1");
		createSampleWorkLogForEmployee(writer, "writer-log-2");
		createSampleWorkLogForEmployee(deptMate, "deptMate-log-1");
		createSampleWorkLogForEmployee(otherDeptEmployee, "otherDept-log-1");
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(writer.getUsername(), "all", 0, 10);
		
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2", "deptMate-log-1", "otherDept-log-1");
	}
	
	@Test
	@DisplayName("R: 업무일지 목록 조회 - type=dept (부서)")
	void getWorkLogList_dept() {
		createSampleWorkLogForEmployee(writer, "writer-log-1");
		createSampleWorkLogForEmployee(writer, "writer-log-2");
		createSampleWorkLogForEmployee(deptMate, "deptMate-log-1");
		createSampleWorkLogForEmployee(otherDeptEmployee, "otherDept-log-1");
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(writer.getUsername(), "dept", 0, 10);
		
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2", "deptMate-log-1");
	}
	
	@Test
	@DisplayName("R: 업무일지 목록 조회 - type=my (나의)")
	void getWorkLogList_my() {
		createSampleWorkLogForEmployee(writer, "writer-log-1");
		createSampleWorkLogForEmployee(writer, "writer-log-2");
		createSampleWorkLogForEmployee(deptMate, "deptMate-log-1");
		createSampleWorkLogForEmployee(otherDeptEmployee, "otherDept-log-1");
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(writer.getUsername(), "my", 0, 10);
		
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent())
			.extracting(WorkLogResponseDTO::getTitle)
			.containsExactlyInAnyOrder("writer-log-1", "writer-log-2");
	}
	
	@Test
	@DisplayName("R: 업무일지 목록 조회 - 실패 (직원 없음)")
	void getWorkLogList_employeeNotFound() {
		assertThatThrownBy(() ->
			                   workLogService.getWorkLogList("unknownUser", "all", 0, 10)
		).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("직원이 존재하지 않습니다.");
	}
	
	// ---------------------------
	// getWorkLogCodes (업무일지 코드 조회)
	// ---------------------------
	
	@Test
	@DisplayName("R: 업무일지 코드 목록 조회 - WT/WO prefix, ROOT 코드 제외")
	void getWorkLogCodes_returnFilteredCodes() {
		WorkLogCodeListDTO dto = workLogService.getWorkLogCodes();
		
		assertThat(dto).isNotNull();
		assertThat(dto.getWorkTypes())
			.extracting(CommonCodeResponseDTO::getCode)
			.contains("WT1", "WT2")
			.doesNotContain("WT0");
		assertThat(dto.getWorkOptions())
			.extracting(CommonCodeResponseDTO::getCode)
			.contains("WO1", "WO2")
			.doesNotContain("WO0");
	}
}
