package org.goodee.startup_BE.mail.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.mail.entity.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
class MailRepositoryTests {
	
	@Autowired
	private MailRepository mailRepository;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	private CommonCode statusActive;
	private CommonCode roleUser;
	private CommonCode deptDev;
	private CommonCode posJunior;
	
	private Employee creator;
	
	private final String TEST_PASSWORD = "testPassword123!";
	
	@BeforeEach
	void setUp() {
		// 삭제 순서: Mail -> Employee -> CommonCode
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// 공통 코드 생성
		statusActive = CommonCode.createCommonCode(
			"STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false
		);
		roleUser = CommonCode.createCommonCode(
			"ROLE_USER", "사용자", "ROLE_USER", null, null, 1L, null, false
		);
		deptDev = CommonCode.createCommonCode(
			"DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false
		);
		posJunior = CommonCode.createCommonCode(
			"POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false
		);
		
		commonCodeRepository.save(statusActive);
		commonCodeRepository.save(roleUser);
		commonCodeRepository.save(deptDev);
		commonCodeRepository.save(posJunior);
		
		// creator 직원 생성
		creator = Employee.createEmployee(
			"admin",
			"관리자",
			"admin@test.com",
			"010-0000-0000",
			LocalDate.now(),
			statusActive,
			roleUser,
			deptDev,
			posJunior,
			null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		creator = employeeRepository.save(creator);
	}
	
	private Employee createPersistableEmployee(String username, String email) {
		Employee employee = Employee.createEmployee(
			username,
			"테스트유저",
			email,
			"010-1234-5678",
			LocalDate.now(),
			statusActive,
			roleUser,
			deptDev,
			posJunior,
			creator
		);
		employee.updateInitPassword(TEST_PASSWORD, creator);
		return employee;
	}
	
	private Mail createPersistableMail(Employee sender, String title, String content) {
		return Mail.createBasicMail(sender, title, content, LocalDateTime.now());
	}
	
	@Test
	@DisplayName("C: Mail 생성(save) 테스트")
	void saveMailTest() {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("mailUser", "mailuser@test.com")
		);
		Mail mail = createPersistableMail(sender, "테스트 메일 제목", "<p>테스트 내용</p>");
		
		// when
		Mail savedMail = mailRepository.save(mail);
		
		// then
		assertThat(savedMail).isNotNull();
		assertThat(savedMail.getMailId()).isNotNull();
		assertThat(savedMail.getTitle()).isEqualTo("테스트 메일 제목");
		assertThat(savedMail.getContent()).isEqualTo("<p>테스트 내용</p>");
		assertThat(savedMail.getEmployee()).isEqualTo(sender);
		assertThat(savedMail.getCreatedAt()).isNotNull();
		assertThat(savedMail.getUpdatedAt()).isNotNull();
		assertThat(savedMail.getSendAt()).isNotNull();
	}
	
	@Test
	@DisplayName("R: Mail ID로 조회(findById) 테스트 - 성공")
	void findByIdSuccessTest() {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("findUser", "finduser@test.com")
		);
		Mail mail = mailRepository.save(
			createPersistableMail(sender, "조회 메일", "조회용 내용")
		);
		
		// when
		Optional<Mail> found = mailRepository.findById(mail.getMailId());
		
		// then
		assertThat(found).isPresent();
		assertThat(found.get().getMailId()).isEqualTo(mail.getMailId());
		assertThat(found.get().getTitle()).isEqualTo("조회 메일");
	}
	
	@Test
	@DisplayName("R: Mail ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
	void findByIdFailureTest() {
		// given
		Long nonExistId = 9999L;
		
		// when
		Optional<Mail> found = mailRepository.findById(nonExistId);
		
		// then
		assertThat(found).isNotPresent();
	}
	
	@Test
	@DisplayName("U: Mail 수정(update) 테스트")
	void updateMailTest() throws InterruptedException {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("updateUser", "updateuser@test.com")
		);
		Mail mail = mailRepository.save(
			createPersistableMail(sender, "원본 제목", "원본 내용")
		);
		LocalDateTime createdAt = mail.getCreatedAt();
		
		// @PreUpdate 확인을 위해 잠시 대기
		Thread.sleep(10);
		
		// when
		Mail toUpdate = mailRepository.findById(mail.getMailId()).orElseThrow();
		toUpdate.updateTitle("수정된 제목");
		toUpdate.updateContent("수정된 내용");
		
		mailRepository.flush();
		Mail updated = mailRepository.findById(mail.getMailId()).orElseThrow();
		
		// then
		assertThat(updated.getTitle()).isEqualTo("수정된 제목");
		assertThat(updated.getContent()).isEqualTo("수정된 내용");
		assertThat(updated.getUpdatedAt()).isAfter(createdAt);
		assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
	}
	
	@Test
	@DisplayName("D: Mail 삭제(deleteById) 테스트")
	void deleteMailTest() {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("deleteUser", "deleteuser@test.com")
		);
		Mail mail = mailRepository.save(
			createPersistableMail(sender, "삭제용 메일", "삭제 내용")
		);
		Long mailId = mail.getMailId();
		assertThat(mailRepository.existsById(mailId)).isTrue();
		
		// when
		mailRepository.deleteById(mailId);
		mailRepository.flush();
		
		// then
		assertThat(mailRepository.existsById(mailId)).isFalse();
	}
	
	
	@Test
	@DisplayName("관계: 회신 메일 작성 시 parentMail, threadId, replies 매핑 확인")
	void replyMailRelationTest() {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("threadUser", "threaduser@test.com")
		);
		
		Mail parent = mailRepository.save(
			createPersistableMail(sender, "부모 메일", "부모 내용")
		);
		
		Mail reply = Mail.createReplyMail(
			sender,
			"Re: 부모 메일",
			"회신 내용",
			LocalDateTime.now(),
			parent,
			parent.getMailId()
		);
		mailRepository.save(reply);
		mailRepository.flush();
		entityManager.clear(); // ★ 영속성 컨텍스트 비우기
		
		// when
		Mail foundParent = mailRepository.findById(parent.getMailId()).orElseThrow();
		
		// then
		assertThat(foundParent.getReplies()).hasSize(1);
		Mail child = foundParent.getReplies().get(0);
		assertThat(child.getParentMail()).isEqualTo(foundParent);
		assertThat(child.getThreadId()).isEqualTo(parent.getMailId());
		assertThat(child.getTitle()).isEqualTo("Re: 부모 메일");
	}
	
	@Test
	@DisplayName("Exception: 필수 필드(title) null 저장 시 예외 발생")
	void saveNullTitleTest() {
		// given
		Employee sender = employeeRepository.save(
			createPersistableEmployee("nullTitleUser", "nulltitle@test.com")
		);
		
		Mail invalidMail = Mail.createBasicMail(sender, null, "내용", LocalDateTime.now());
		
		// when & then
		assertThatThrownBy(() -> mailRepository.saveAndFlush(invalidMail))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
