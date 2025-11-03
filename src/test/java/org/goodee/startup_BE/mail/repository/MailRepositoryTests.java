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
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.beans.factory.annotation.Autowired;

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
class MailRepositoryTests {
	
	@Autowired MailRepository mailRepository;
	@Autowired EmployeeRepository employeeRepository;
	@Autowired CommonCodeRepository commonCodeRepository;
	@Autowired EntityManager em;
	
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private Employee admin, dev1;
	
	private CommonCode cc(String code, String desc, String v1, String v2, String v3, long sort) {
		return org.goodee.startup_BE.common.entity.CommonCode.createCommonCode(code, desc, v1, v2, v3, sort, null);
	}
	
	private void seedCommonCodes() {
		statusActive = cc("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L);
		roleAdmin    = cc("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L);
		roleUser     = cc("ROLE_USER", "사용자", "USER", null, null, 2L);
		deptDev      = cc("DEPT_DEV", "개발팀", "DEV", null, null, 1L);
		deptHr       = cc("DEPT_HR", "인사팀", "HR", null, null, 2L);
		posJunior    = cc("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L);
		posSenior    = cc("POS_SENIOR", "대리", "SENIOR", null, null, 2L);
		commonCodeRepository.saveAll(List.of(statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior));
	}
	
	private Employee newEmp(String username, String name, String email, CommonCode role, CommonCode dept, CommonCode pos, Employee creator) {
		Employee e = Employee.createEmployee(
			username, name, email, "010-0000-0000",
			LocalDate.now(), statusActive, "default.png", role, dept, pos, creator
		);
		e.updateInitPassword("Pw1234!", creator);
		return e;
	}
	
	@BeforeEach
	void setUp() {
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		seedCommonCodes();
		
		admin = newEmp("admin", "관리자", "admin@test.com", roleAdmin, deptHr, posSenior, null);
		dev1  = newEmp("dev1", "개발자1", "dev1@test.com", roleUser, deptDev, posJunior, admin);
		employeeRepository.saveAll(List.of(admin, dev1));
	}
	
	@Test
	@DisplayName("C/R: Mail 저장 및 조회")
	void save_and_find() {
		Mail mail = Mail.createBasicMail(admin, "제목", "<p>내용</p>", LocalDateTime.now());
		Mail saved = mailRepository.save(mail);
		
		assertThat(saved.getMailId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		
		Mail found = mailRepository.findById(saved.getMailId()).orElseThrow();
		assertThat(found.getTitle()).isEqualTo("제목");
		assertThat(found.getEmployee().getUsername()).isEqualTo("admin");
	}
	
	@Test
	@DisplayName("U: Mail 제목/본문 업데이트")
	void update_mail() throws Exception {
		Mail mail = mailRepository.saveAndFlush(
			Mail.createBasicMail(admin, "Old", "OldC", LocalDateTime.now())
		);
		LocalDateTime updated0 = mail.getUpdatedAt();
		
		// 정밀도 이슈 회피
		Thread.sleep(1000);
		
		mail.updateTitle("New");
		mail.updateContent("NewC");
		
		mailRepository.flush(); // @PreUpdate 트리거
		em.clear();             // 2차 캐시/영속성 컨텍스트 비우기
		
		Mail updated = mailRepository.findById(mail.getMailId()).orElseThrow();
		assertThat(updated.getTitle()).isEqualTo("New");
		assertThat(updated.getContent()).isEqualTo("NewC");
		assertThat(updated.getUpdatedAt()).isAfter(updated0);
	}
	
	@Test
	@DisplayName("D: Mail 삭제")
	void delete_mail() {
		Mail mail = mailRepository.save(Mail.createBasicMail(admin, "제목", null, LocalDateTime.now()));
		Long id = mail.getMailId();
		assertThat(mailRepository.existsById(id)).isTrue();
		
		mailRepository.deleteById(id);
		assertThat(mailRepository.existsById(id)).isFalse();
	}
	
	@Test
	@DisplayName("회신 메일(부모/스레드) 저장")
	void save_reply_mail() {
		Mail parent = mailRepository.saveAndFlush(
			Mail.createBasicMail(admin, "원본", "C", LocalDateTime.now())
		);
		
		Mail reply = Mail.createReplyMail(
			dev1, "RE: 원본", "RC", LocalDateTime.now(), parent, 100L
		);
		mailRepository.saveAndFlush(reply);
		
		em.clear(); // DB에서 다시 로드되게
		
		Mail parentFound = mailRepository.findById(parent.getMailId()).orElseThrow();
		assertThat(parentFound.getReplies())
			.extracting(Mail::getTitle)
			.contains("RE: 원본");
		
		Mail savedReply = mailRepository.findById(reply.getMailId()).orElseThrow();
		assertThat(savedReply.getParentMail().getMailId()).isEqualTo(parent.getMailId());
		assertThat(savedReply.getThreadId()).isEqualTo(100L);
	}
}
