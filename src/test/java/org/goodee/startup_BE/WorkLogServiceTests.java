package org.goodee.startup_BE;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.work_log.dto.WorkLogRequestDTO;
import org.goodee.startup_BE.work_log.dto.WorkLogResponseDTO;
import org.goodee.startup_BE.work_log.entity.WorkLog;
import org.goodee.startup_BE.work_log.repository.WorkLogReadRepository;
import org.goodee.startup_BE.work_log.repository.WorkLogRepository;
import org.goodee.startup_BE.work_log.service.WorkLogService;
import org.goodee.startup_BE.work_log.service.WorkLogServiceImpl;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

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
class WorkLogServiceTests {
	
	@Autowired private WorkLogRepository workLogRepository;
	@Autowired private WorkLogReadRepository workLogReadRepository;
	@Autowired private EmployeeRepository employeeRepository;
	@Autowired private CommonCodeRepository commonCodeRepository;
	@Autowired private EntityManager em;
	
	private WorkLogService workLogService;
	
	// 공통 코드
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private CommonCode wtProject, wtStudy, woMeeting, woCoding;
	
	// 직원
	private Employee creator;
	private final String TEST_PASSWORD = "testPassword123!";
	
	@BeforeEach
	void setUp() {
		workLogReadRepository.deleteAll();
		workLogRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// 공통 코드 준비
		statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
		roleAdmin   = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null);
		roleUser    = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
		deptDev     = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
		deptHr      = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null);
		posJunior   = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);
		posSenior   = CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null);
		
		// 업무구분/옵션(value2를 한글명으로 사용)
		wtProject   = CommonCode.createCommonCode("WT_PROJECT", "업무구분:프로젝트", "PROJECT", "프로젝트", null, 1L, null);
		wtStudy     = CommonCode.createCommonCode("WT_STUDY", "업무구분:학습", "STUDY", "학습", null, 2L, null);
		woMeeting   = CommonCode.createCommonCode("WO_MEETING", "옵션:미팅", "MEETING", "미팅", null, 1L, null);
		woCoding    = CommonCode.createCommonCode("WO_CODING", "옵션:코딩", "CODING", "코딩", null, 2L, null);
		
		commonCodeRepository.saveAll(
			List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior,
				wtProject, wtStudy, woMeeting, woCoding)
		);
		
		// 생성자(관리자)
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive, "default.png", roleAdmin, deptHr, posSenior, null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		employeeRepository.save(creator);
		
		// Service 인스턴스 (첨부파일 서비스는 현재 미사용이므로 Mock)
		var attachmentFileService = Mockito.mock(org.goodee.startup_BE.common.service.AttachmentFileServiceImpl.class);
		workLogService = new WorkLogServiceImpl(
			workLogRepository, workLogReadRepository, employeeRepository, commonCodeRepository, attachmentFileService
		);
	}
	
	// ====== 헬퍼 ======
	private Employee createPersistableEmployee(String username, String email, CommonCode dept, CommonCode pos) {
		Employee e = Employee.createEmployee(
			username, "테스터-"+username, email, "010-1111-2222",
			LocalDate.now(), statusActive, "default.png", roleUser, dept, pos, creator
		);
		e.updateInitPassword(TEST_PASSWORD, creator);
		return employeeRepository.save(e);
	}
	
	private WorkLog createAndSaveWorkLog(Employee writer, CommonCode wt, CommonCode wo, String title) {
		WorkLog w = WorkLog.createWorkLog(writer, wt, wo, LocalDateTime.now(), title, "내용:"+title);
		return workLogRepository.save(w);
	}
	
	private WorkLogRequestDTO buildRequest(Long workTypeId, Long workOptionId, LocalDateTime date, String title, String content) {
		return WorkLogRequestDTO.builder()
			       .workTypeId(workTypeId)
			       .workOptionId(workOptionId)
			       .workDate(date)
			       .title(title)
			       .content(content)
			       .build();
	}
	
	// ====== C: 생성 ======
	@Test
	@DisplayName("C: saveWorkLog - 정상 생성")
	void saveWorkLog_success() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		
		WorkLogRequestDTO dto = buildRequest(
			wtProject.getCommonCodeId(), woMeeting.getCommonCodeId(),
			LocalDateTime.now(), "프로젝트 미팅", "요약"
		);
		
		WorkLogResponseDTO res = workLogService.saveWorkLog(dto, dev1.getUsername());
		
		assertThat(res).isNotNull();
		assertThat(res.getTitle()).isEqualTo("프로젝트 미팅");
		assertThat(res.getWorkTypeName()).isEqualTo("프로젝트");
		assertThat(res.getWorkOptionName()).isEqualTo("미팅");
		
		// 영속 확인
		assertThat(workLogRepository.findById(res.getWorkLogId())).isPresent();
	}
	
	@Test
	@DisplayName("C: saveWorkLog - 잘못된 업무구분/옵션 ID -> ResourceNotFoundException")
	void saveWorkLog_invalidCommonCode_throws() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		
		// 잘못된 workTypeId
		WorkLogRequestDTO wrongType = buildRequest(
			999999L, woMeeting.getCommonCodeId(), LocalDateTime.now(), "제목", "내용"
		);
		assertThatThrownBy(() -> workLogService.saveWorkLog(wrongType, dev1.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
		
		// 잘못된 workOptionId
		WorkLogRequestDTO wrongOption = buildRequest(
			wtProject.getCommonCodeId(), 999999L, LocalDateTime.now(), "제목", "내용"
		);
		assertThatThrownBy(() -> workLogService.saveWorkLog(wrongOption, dev1.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ====== R: 상세 ======
	@Test
	@DisplayName("R: getWorkLogDetail - 읽음 처리(upsert) 1회만 생성")
	void getWorkLogDetail_marksReadOnce() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(dev1, wtProject, woMeeting, "상세조회 테스트");
		
		// 첫 조회: 읽음 생성
		WorkLogResponseDTO d1 = workLogService.getWorkLogDetail(wl.getWorkLogId(), dev1.getUsername());
		assertThat(d1.getIsRead()).isTrue();
		// 두 번째 조회: 중복 생성 방지
		WorkLogResponseDTO d2 = workLogService.getWorkLogDetail(wl.getWorkLogId(), dev1.getUsername());
		assertThat(d2.getIsRead()).isTrue();
		
		long rows = workLogReadRepository.findAll().stream()
			            .filter(r -> r.getWorkLog().getWorkLogId().equals(wl.getWorkLogId())
				                         && r.getEmployee().getEmployeeId().equals(dev1.getEmployeeId()))
			            .count();
		assertThat(rows).isEqualTo(1L);
	}
	
	@Test
	@DisplayName("R: getWorkLogDetail - 존재하지 않는 사용자/업무일지 -> ResourceNotFoundException")
	void getWorkLogDetail_notFound() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		// 존재하지 않는 WorkLog
		assertThatThrownBy(() -> workLogService.getWorkLogDetail(123456L, dev1.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
		// 존재하지 않는 사용자
		WorkLog wl = createAndSaveWorkLog(dev1, wtProject, woMeeting, "임시");
		assertThatThrownBy(() -> workLogService.getWorkLogDetail(wl.getWorkLogId(), "no-user"))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ====== U: 수정 ======
	@Test
	@DisplayName("U: updateWorkLog - 본인 작성건 정상 수정 + @PreUpdate 갱신")
	void updateWorkLog_success() throws Exception {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(dev1, wtProject, woMeeting, "수정전 제목");
		
		LocalDateTime createdAt = wl.getCreatedAt();
		Thread.sleep(10);
		
		WorkLogRequestDTO upd = WorkLogRequestDTO.builder()
			                        .workLogId(wl.getWorkLogId())
			                        .workTypeId(wtStudy.getCommonCodeId())
			                        .workOptionId(woCoding.getCommonCodeId())
			                        .workDate(LocalDateTime.now())
			                        .title("수정후 제목")
			                        .content("수정후 내용")
			                        .build();
		
		WorkLogResponseDTO res = workLogService.updateWorkLog(upd, dev1.getUsername());
		workLogRepository.flush();
		
		assertThat(res.getTitle()).isEqualTo("수정후 제목");
		// 엔티티 재조회로 시간 검증
		WorkLog updated = workLogRepository.findById(wl.getWorkLogId()).orElseThrow();
		assertThat(updated.getUpdatedAt()).isAfter(createdAt);
		assertThat(updated.getWorkType().getCommonCodeId()).isEqualTo(wtStudy.getCommonCodeId());
		assertThat(updated.getWorkOption().getCommonCodeId()).isEqualTo(woCoding.getCommonCodeId());
	}
	
	@Test
	@DisplayName("U: updateWorkLog - 타인 수정 -> AccessDeniedException")
	void updateWorkLog_accessDenied() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		Employee other = createPersistableEmployee("dev2", "dev2@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(owner, wtProject, woMeeting, "본인 글");
		
		WorkLogRequestDTO upd = WorkLogRequestDTO.builder()
			                        .workLogId(wl.getWorkLogId())
			                        .workTypeId(wtProject.getCommonCodeId())
			                        .workOptionId(woMeeting.getCommonCodeId())
			                        .workDate(LocalDateTime.now())
			                        .title("타인 수정")
			                        .content("타인 수정")
			                        .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(upd, other.getUsername()))
			.isInstanceOf(AccessDeniedException.class);
	}
	
	@Test
	@DisplayName("U: updateWorkLog - 잘못된 업무구분/옵션 ID -> ResourceNotFoundException")
	void updateWorkLog_invalidCodes() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(owner, wtProject, woMeeting, "본인 글");
		
		WorkLogRequestDTO wrongType = WorkLogRequestDTO.builder()
			                              .workLogId(wl.getWorkLogId())
			                              .workTypeId(999999L)
			                              .workOptionId(woMeeting.getCommonCodeId())
			                              .workDate(LocalDateTime.now())
			                              .title("x")
			                              .content("x")
			                              .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(wrongType, owner.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
		
		WorkLogRequestDTO wrongOption = WorkLogRequestDTO.builder()
			                                .workLogId(wl.getWorkLogId())
			                                .workTypeId(wtProject.getCommonCodeId())
			                                .workOptionId(999999L)
			                                .workDate(LocalDateTime.now())
			                                .title("x")
			                                .content("x")
			                                .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(wrongOption, owner.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	@Test
	@DisplayName("U: updateWorkLog - 존재하지 않는 WorkLog -> ResourceNotFoundException")
	void updateWorkLog_notFound() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		
		WorkLogRequestDTO upd = WorkLogRequestDTO.builder()
			                        .workLogId(123456L)
			                        .workTypeId(wtProject.getCommonCodeId())
			                        .workOptionId(woMeeting.getCommonCodeId())
			                        .workDate(LocalDateTime.now())
			                        .title("x")
			                        .content("x")
			                        .build();
		
		assertThatThrownBy(() -> workLogService.updateWorkLog(upd, owner.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ====== D: 삭제(소프트) ======
	@Test
	@DisplayName("D: deleteWorkLog - 본인 글 소프트 삭제(@Where로 조회 제외) + 네이티브로 is_deleted 확인")
	void deleteWorkLog_softDelete() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(owner, wtProject, woMeeting, "삭제 대상");
		
		workLogService.deleteWorkLog(wl.getWorkLogId(), owner.getUsername());
		workLogRepository.flush();
		
		// ⭐ 1차 캐시 비우기 (중요)
		em.clear();
		
		// @Where로 인해 조회되지 않음
		assertThat(workLogRepository.findById(wl.getWorkLogId())).isEmpty();
		
		// 네이티브로 is_deleted 확인
		Object flag = em.createNativeQuery("SELECT is_deleted FROM tbl_work_log WHERE work_log_id = ?")
			              .setParameter(1, wl.getWorkLogId())
			              .getSingleResult();
		
		boolean isDeleted = (flag instanceof Boolean) ? (Boolean) flag : Integer.valueOf(flag.toString()) == 1;
		assertThat(isDeleted).isTrue();
	}
	
	@Test
	@DisplayName("D: deleteWorkLog - 타인 글 삭제 -> AccessDeniedException")
	void deleteWorkLog_accessDenied() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		Employee other = createPersistableEmployee("dev2", "dev2@test.com", deptDev, posJunior);
		WorkLog wl = createAndSaveWorkLog(owner, wtProject, woMeeting, "남의 글");
		
		assertThatThrownBy(() -> workLogService.deleteWorkLog(wl.getWorkLogId(), other.getUsername()))
			.isInstanceOf(AccessDeniedException.class);
	}
	
	@Test
	@DisplayName("D: deleteWorkLog - 존재하지 않는 WorkLog -> ResourceNotFoundException")
	void deleteWorkLog_notFound() {
		Employee owner = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		assertThatThrownBy(() -> workLogService.deleteWorkLog(123456L, owner.getUsername()))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ====== 목록 ======
	@Test
	@DisplayName("L: getWorkLogList - all: 전체(소프트삭제 제외), 읽음 여부 포함")
	void getWorkLogList_all() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		Employee dev2 = createPersistableEmployee("dev2", "dev2@test.com", deptDev, posSenior);
		Employee hr1  = createPersistableEmployee("hr1",  "hr1@test.com",  deptHr,  posJunior);
		
		WorkLog w1 = createAndSaveWorkLog(dev1, wtProject, woMeeting, "dev1-1");
		WorkLog w2 = createAndSaveWorkLog(dev2, wtProject, woCoding,  "dev2-1");
		WorkLog w3 = createAndSaveWorkLog(hr1,  wtStudy,   woMeeting, "hr1-1");
		
		// 하나는 dev1이 상세조회해서 '읽음'으로 표시
		workLogService.getWorkLogDetail(w2.getWorkLogId(), dev1.getUsername());
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(dev1.getUsername(), "all", 0, 10);
		assertThat(page.getTotalElements()).isEqualTo(3);
		// dev2-1은 읽음 true
		boolean hasReadTrue = page.getContent().stream()
			                      .anyMatch(dto -> dto.getWorkLogId().equals(w2.getWorkLogId()) && Boolean.TRUE.equals(dto.getIsRead()));
		assertThat(hasReadTrue).isTrue();
	}
	
	@Test
	@DisplayName("L: getWorkLogList - dept: 동일 부서만")
	void getWorkLogList_dept() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		Employee dev2 = createPersistableEmployee("dev2", "dev2@test.com", deptDev, posSenior);
		Employee hr1  = createPersistableEmployee("hr1",  "hr1@test.com",  deptHr,  posJunior);
		
		WorkLog w1 = createAndSaveWorkLog(dev1, wtProject, woMeeting, "dev1-1");
		WorkLog w2 = createAndSaveWorkLog(dev2, wtProject, woCoding,  "dev2-1");
		WorkLog w3 = createAndSaveWorkLog(hr1,  wtStudy,   woMeeting, "hr1-1");
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(dev1.getUsername(), "dept", 0, 10);
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getContent().stream().map(WorkLogResponseDTO::getTitle))
			.contains("dev1-1", "dev2-1")
			.doesNotContain("hr1-1");
	}
	
	@Test
	@DisplayName("L: getWorkLogList - 기본 분기(기타 타입): 내 것만")
	void getWorkLogList_onlyMine() {
		Employee dev1 = createPersistableEmployee("dev1", "dev1@test.com", deptDev, posJunior);
		Employee dev2 = createPersistableEmployee("dev2", "dev2@test.com", deptDev, posSenior);
		
		WorkLog w1 = createAndSaveWorkLog(dev1, wtProject, woMeeting, "dev1-1");
		WorkLog w2 = createAndSaveWorkLog(dev2, wtProject, woCoding,  "dev2-1");
		
		Page<WorkLogResponseDTO> page = workLogService.getWorkLogList(dev1.getUsername(), "mine", 0, 10);
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).getTitle()).isEqualTo("dev1-1");
	}
	
	@Test
	@DisplayName("L: getWorkLogList - 존재하지 않는 사용자 -> ResourceNotFoundException")
	void getWorkLogList_userNotFound() {
		assertThatThrownBy(() -> workLogService.getWorkLogList("no-user", "all", 0, 10))
			.isInstanceOf(ResourceNotFoundException.class);
	}
}
