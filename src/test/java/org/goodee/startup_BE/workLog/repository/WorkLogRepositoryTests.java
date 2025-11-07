package org.goodee.startup_BE.workLog.repository;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;

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
class WorkLogRepositoryTests {
	
	@Autowired private WorkLogRepository workLogRepository;
	@Autowired private WorkLogReadRepository workLogReadRepository;
	@Autowired private EmployeeRepository employeeRepository;
	@Autowired private CommonCodeRepository commonCodeRepository;
	@Autowired private EntityManager em;
	
	private Employee creator;
	private Employee devUser1, devUser2, hrUser;
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private CommonCode wtProject, wtStudy, woMeeting, woCoding;
	
	private static final String TEST_PASSWORD = "Init#1234";
	
	@BeforeEach
	void setUp() {
		// 삭제 순서: Read -> WorkLog -> Employee -> CommonCode
		workLogReadRepository.deleteAll();
		workLogRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// CommonCode 기본 (직원 관련)
		statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
		roleAdmin    = CommonCode.createCommonCode("ROLE_ADMIN"   , "관리자", "ADMIN" , null, null, 1L, null);
		roleUser     = CommonCode.createCommonCode("ROLE_USER"    , "사용자", "USER"  , null, null, 2L, null);
		deptDev      = CommonCode.createCommonCode("DEPT_DEV"     , "개발팀", "DEV"   , null, null, 1L, null);
		deptHr       = CommonCode.createCommonCode("DEPT_HR"      , "인사팀", "HR"    , null, null, 2L, null);
		posJunior    = CommonCode.createCommonCode("POS_JUNIOR"   , "사원" , "JUNIOR", null, null, 1L, null);
		posSenior    = CommonCode.createCommonCode("POS_SENIOR"   , "대리" , "SENIOR", null, null, 2L, null);
		
		// CommonCode 업무유형/옵션 (WorkLog용, value2 사용)
		wtProject = CommonCode.createCommonCode("WT_PROJECT", "업무구분:프로젝트", "PROJECT", "프로젝트", null, 1L, null);
		wtStudy   = CommonCode.createCommonCode("WT_STUDY"  , "업무구분:학습"   , "STUDY"  , "학습"   , null, 2L, null);
		woMeeting = CommonCode.createCommonCode("WO_MEETING", "옵션:미팅"       , "MEETING", "고객 미팅", null, 1L, null);
		woCoding  = CommonCode.createCommonCode("WO_CODING" , "옵션:개발"       , "CODING" , "개발"     , null, 2L, null);
		
		commonCodeRepository.saveAll(List.of(
			statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior,
			wtProject, wtStudy, woMeeting, woCoding
		));
		
		// 직원 생성자(creator)
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive, 
			roleAdmin, deptHr, posSenior, null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		creator = employeeRepository.save(creator);
		
		// 직원들
		devUser1 = Employee.createEmployee(
			"dev1", "개발1", "dev1@test.com", "010-1111-1111",
			LocalDate.now(), statusActive, 
			roleUser, deptDev, posJunior, creator
		);
		devUser1.updateInitPassword(TEST_PASSWORD, creator);
		devUser1 = employeeRepository.save(devUser1);
		
		devUser2 = Employee.createEmployee(
			"dev2", "개발2", "dev2@test.com", "010-2222-2222",
			LocalDate.now(), statusActive, 
			roleUser, deptDev, posSenior, creator
		);
		devUser2.updateInitPassword(TEST_PASSWORD, creator);
		devUser2 = employeeRepository.save(devUser2);
		
		hrUser = Employee.createEmployee(
			"hr1", "인사1", "hr1@test.com", "010-3333-3333",
			LocalDate.now(), statusActive, 
			roleUser, deptHr, posJunior, creator
		);
		hrUser.updateInitPassword(TEST_PASSWORD, creator);
		hrUser = employeeRepository.save(hrUser);
	}
	
	private WorkLog buildWorkLog(Employee emp, CommonCode type, CommonCode opt, String title, String content, LocalDateTime dateTime) {
		return WorkLog.createWorkLog(emp, type, opt, dateTime, title, content);
	}
	
	// =========================
	// CRUD
	// =========================
	@Test
	@DisplayName("C: WorkLog 저장")
	void saveWorkLog() {
		WorkLog wl = buildWorkLog(devUser1, wtProject, woMeeting, "제목A", "내용A", LocalDateTime.now());
		WorkLog saved = workLogRepository.save(wl);
		
		assertThat(saved.getWorkLogId()).isNotNull();
		assertThat(saved.getEmployee()).isEqualTo(devUser1);
		assertThat(saved.getWorkType()).isEqualTo(wtProject);
		assertThat(saved.getWorkOption()).isEqualTo(woMeeting);
		assertThat(saved.getTitle()).isEqualTo("제목A");
		assertThat(saved.getIsDeleted()).isFalse();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}
	
	@Test
	@DisplayName("R: WorkLog findById - 성공")
	void findById_success() {
		WorkLog saved = workLogRepository.save(
			buildWorkLog(devUser1, wtProject, woMeeting, "제목", "내용", LocalDateTime.now())
		);
		assertThat(workLogRepository.findById(saved.getWorkLogId())).isPresent();
	}
	
	@Test
	@DisplayName("R: WorkLog findById - 실패(없는 PK)")
	void findById_failure() {
		assertThat(workLogRepository.findById(9999L)).isNotPresent();
	}
	
	@Test
	@DisplayName("U: WorkLog 업데이트(dirty checking + flush)")
	void updateWorkLog() throws Exception {
		WorkLog saved = workLogRepository.save(
			buildWorkLog(devUser1, wtProject, woMeeting, "제목A", "내용A", LocalDateTime.now())
		);
		LocalDateTime createdAt = saved.getCreatedAt();
		Thread.sleep(10);
		
		WorkLog toUpdate = workLogRepository.findById(saved.getWorkLogId()).orElseThrow();
		LocalDateTime newDate = LocalDateTime.now().minusDays(1);
		toUpdate.updateWorkLog(wtStudy, woCoding, newDate, "제목B", "내용B");
		workLogRepository.flush();
		
		WorkLog updated = workLogRepository.findById(saved.getWorkLogId()).orElseThrow();
		assertThat(updated.getWorkType()).isEqualTo(wtStudy);
		assertThat(updated.getWorkOption()).isEqualTo(woCoding);
		assertThat(updated.getWorkDate()).isEqualTo(newDate);
		assertThat(updated.getTitle()).isEqualTo("제목B");
		assertThat(updated.getContent()).isEqualTo("내용B");
		assertThat(updated.getUpdatedAt()).isAfter(createdAt);
	}
	
	@Test
	@DisplayName("D: WorkLog 삭제 - soft delete(@Where) 동작")
	void softDeleteWorkLog() {
		WorkLog saved = workLogRepository.save(
			buildWorkLog(devUser1, wtProject, woMeeting, "제목", "내용", LocalDateTime.now())
		);
		
		// soft delete
		saved.deleteWorkLog();
		workLogRepository.flush();
		
		// ★ 중요: 1차 캐시 비우기 → DB 재조회 강제
		em.clear();
		
		// @Where로 인해 조회되지 않음
		assertThat(workLogRepository.findById(saved.getWorkLogId())).isEmpty();
		
		// 네이티브로 실제 행 존재 + is_deleted=true 확인
		Object flag = em.createNativeQuery(
				"SELECT is_deleted FROM tbl_work_log WHERE work_log_id = ?")
			              .setParameter(1, saved.getWorkLogId())
			              .getSingleResult();
		
		boolean isDeleted =
			(flag instanceof Boolean) ? (Boolean) flag :
				(flag instanceof Number)  ? ((Number) flag).intValue() == 1 :
					"true".equalsIgnoreCase(flag.toString()) || "1".equals(flag.toString());
		
		assertThat(isDeleted).isTrue();
	}
	
	// =========================
	// Custom Query: findWithRead
	// =========================
	@Test
	@DisplayName("findWithRead: 부서 필터 + 읽음 플래그 + 카운트 일치")
	void findWithRead_deptFilter_and_readFlag() {
		// dev 부서 2건, hr 부서 1건
		WorkLog dev1Log = workLogRepository.save(
			buildWorkLog(devUser1, wtProject, woMeeting, "dev1-로그", "A", LocalDateTime.now())
		);
		WorkLog dev2Log = workLogRepository.save(
			buildWorkLog(devUser2, wtStudy, woCoding, "dev2-로그", "B", LocalDateTime.now())
		);
		WorkLog hrLog = workLogRepository.save(
			buildWorkLog(hrUser, wtProject, woCoding, "hr-로그", "C", LocalDateTime.now())
		);
		
		// devUser1이 dev2Log를 읽음 처리
		WorkLogRead read = WorkLogRead.createWorkLogRead(dev2Log, devUser1);
		workLogReadRepository.save(read);
		
		Page<WorkLogResponseDTO> page = workLogRepository.findWithRead(
			devUser1.getEmployeeId(),
			deptDev.getCommonCodeId(),
			false,
			PageRequest.of(0, 10)
		);
		
		// dev 부서만, 총 2건(dev1Log, dev2Log)
		assertThat(page.getTotalElements()).isEqualTo(2);
		List<WorkLogResponseDTO> list = page.getContent();
		assertThat(list).extracting(WorkLogResponseDTO::getTitle).containsExactlyInAnyOrder("dev1-로그", "dev2-로그");
		
		// 읽음 플래그 검증: dev2Log만 읽음(true), dev1Log는 false
		WorkLogResponseDTO dev2Row = list.stream().filter(r -> "dev2-로그".equals(r.getTitle())).findFirst().orElseThrow();
		WorkLogResponseDTO dev1Row = list.stream().filter(r -> "dev1-로그".equals(r.getTitle())).findFirst().orElseThrow();
		assertThat(dev2Row.getIsRead()).isTrue();
		assertThat(dev1Row.getIsRead()).isFalse();
		
		// hrLog는 포함되지 않음
		assertThat(list).extracting(WorkLogResponseDTO::getTitle).doesNotContain("hr-로그");
	}
	
	@Test
	@DisplayName("findWithRead: onlyMine=true → 본인 작성글만")
	void findWithRead_onlyMine() {
		WorkLog dev1LogA = workLogRepository.save(
			buildWorkLog(devUser1, wtProject, woMeeting, "dev1-A", "A", LocalDateTime.now())
		);
		WorkLog dev2LogA = workLogRepository.save(
			buildWorkLog(devUser2, wtProject, woMeeting, "dev2-A", "B", LocalDateTime.now())
		);
		
		Page<WorkLogResponseDTO> mine = workLogRepository.findWithRead(
			devUser1.getEmployeeId(),
			null,
			true,
			PageRequest.of(0, 10)
		);
		
		assertThat(mine.getTotalElements()).isEqualTo(1);
		assertThat(mine.getContent()).extracting(WorkLogResponseDTO::getTitle).containsExactly("dev1-A");
		assertThat(mine.getContent()).extracting(WorkLogResponseDTO::getEmployeeName).containsExactly("개발1");
		
		// soft delete가 projection에도 반영되는지 확인
		dev1LogA.deleteWorkLog();
		workLogRepository.flush();
		
		Page<WorkLogResponseDTO> mineAfterDelete = workLogRepository.findWithRead(
			devUser1.getEmployeeId(),
			null,
			true,
			PageRequest.of(0, 10)
		);
		assertThat(mineAfterDelete.getTotalElements()).isEqualTo(0);
	}
	
	// =========================
	// WorkLogRead Repository & 제약
	// =========================
	@Test
	@DisplayName("WorkLogRead.existsByWorkLogAndEmployee")
	void workLogRead_existsBy() {
		WorkLog wl = workLogRepository.save(
			buildWorkLog(devUser2, wtProject, woMeeting, "dev2-log", "x", LocalDateTime.now())
		);
		
		assertThat(workLogReadRepository.existsByWorkLogAndEmployee(wl, devUser1)).isFalse();
		
		workLogReadRepository.save(WorkLogRead.createWorkLogRead(wl, devUser1));
		assertThat(workLogReadRepository.existsByWorkLogAndEmployee(wl, devUser1)).isTrue();
	}
	
	@Test
	@DisplayName("WorkLogRead 유니크 제약: (work_log_id, employee_id) 중복 저장 시 예외")
	void workLogRead_uniqueConstraint() {
		WorkLog wl = workLogRepository.save(
			buildWorkLog(devUser2, wtProject, woMeeting, "dup-log", "x", LocalDateTime.now())
		);
		workLogReadRepository.saveAndFlush(WorkLogRead.createWorkLogRead(wl, devUser1));
		
		assertThatThrownBy(() ->
			                   workLogReadRepository.saveAndFlush(WorkLogRead.createWorkLogRead(wl, devUser1))
		).isInstanceOf(DataIntegrityViolationException.class);
	}
	
	// =========================
	// 제약/예외 (NOT NULL / FK)
	// =========================
	@Test
	@DisplayName("예외: title=null 저장 시 제약 위반")
	void constraint_title_null() {
		WorkLog wl = WorkLog.createWorkLog(devUser1, wtProject, woMeeting, LocalDateTime.now(), null, "내용");
		assertThatThrownBy(() -> workLogRepository.saveAndFlush(wl))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("예외: employee=null 저장 시 FK 제약 위반")
	void constraint_employee_null() {
		WorkLog wl = WorkLog.createWorkLog(null, wtProject, woMeeting, LocalDateTime.now(), "제목", "내용");
		assertThatThrownBy(() -> workLogRepository.saveAndFlush(wl))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("예외: workType=null 저장 시 FK 제약 위반")
	void constraint_workType_null() {
		WorkLog wl = WorkLog.createWorkLog(devUser1, null, woMeeting, LocalDateTime.now(), "제목", "내용");
		assertThatThrownBy(() -> workLogRepository.saveAndFlush(wl))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("예외: workOption=null 저장 시 FK 제약 위반")
	void constraint_workOption_null() {
		WorkLog wl = WorkLog.createWorkLog(devUser1, wtProject, null, LocalDateTime.now(), "제목", "내용");
		assertThatThrownBy(() -> workLogRepository.saveAndFlush(wl))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("예외: workDate=null 저장 시 제약 위반")
	void constraint_workDate_null() {
		WorkLog wl = WorkLog.createWorkLog(devUser1, wtProject, woMeeting, null, "제목", "내용");
		assertThatThrownBy(() -> workLogRepository.saveAndFlush(wl))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
