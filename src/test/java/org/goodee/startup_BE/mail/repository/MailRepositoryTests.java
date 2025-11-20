package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.MailReceiver;
import org.goodee.startup_BE.mail.entity.Mailbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
	
	@Autowired
	private MailRepository mailRepository;
	
	@Autowired
	private MailReceiverRepository mailReceiverRepository;
	
	@Autowired
	private MailboxRepository mailboxRepository;
	
	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	// 공통 코드 + 생성자(creator)
	private Employee creator;
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	
	// 수신 타입 코드 (ReceiverType)
	private CommonCode toType, ccType, bccType;
	
	// 메일함 타입 코드 (MailboxType)
	private CommonCode inboxType, sentType, myboxType, trashType;
	
	private final String TEST_PASSWORD = "testPassword123!";
	
	@BeforeEach
	void setUp() {
		// FK 순서 고려: 자식 → 부모
		mailReceiverRepository.deleteAll();
		mailboxRepository.deleteAll();
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// ── 직원용 공통 코드 ──
		statusActive = CommonCode.createCommonCode(
			"STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false
		);
		roleAdmin = CommonCode.createCommonCode(
			"ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null, false
		);
		roleUser = CommonCode.createCommonCode(
			"ROLE_USER", "사용자", "USER", null, null, 2L, null, false
		);
		deptDev = CommonCode.createCommonCode(
			"DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false
		);
		deptHr = CommonCode.createCommonCode(
			"DEPT_HR", "인사팀", "HR", null, null, 2L, null, false
		);
		posJunior = CommonCode.createCommonCode(
			"POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false
		);
		posSenior = CommonCode.createCommonCode(
			"POS_SENIOR", "대리", "SENIOR", null, null, 2L, null, false
		);
		
		// ── 수신 타입 코드 (ReceiverType) ──
		toType = CommonCode.createCommonCode(
			"RT_TO", "수신자", "TO", null, null, 1L, null, false
		);
		ccType = CommonCode.createCommonCode(
			"RT_CC", "참조자", "CC", null, null, 2L, null, false
		);
		bccType = CommonCode.createCommonCode(
			"RT_BCC", "숨은참조자", "BCC", null, null, 3L, null, false
		);
		
		// ── 메일함 타입 코드 (MailboxType) ──
		inboxType = CommonCode.createCommonCode(
			"MT_INBOX", "받은메일함", "INBOX", null, null, 1L, null, false
		);
		sentType = CommonCode.createCommonCode(
			"MT_SENT", "보낸메일함", "SENT", null, null, 2L, null, false
		);
		myboxType = CommonCode.createCommonCode(
			"MT_MYBOX", "개인보관함", "MYBOX", null, null, 3L, null, false
		);
		trashType = CommonCode.createCommonCode(
			"MT_TRASH", "휴지통", "TRASH", null, null, 4L, null, false
		);
		
		commonCodeRepository.saveAll(List.of(
			statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior,
			toType, ccType, bccType,
			inboxType, sentType, myboxType, trashType
		));
		
		// ── creator 직원 ──
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive, roleAdmin, deptHr, posSenior,
			null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		employeeRepository.save(creator);
	}
	
	// ─────────────────────────────────────────────
	// 공통 헬퍼 메서드
	// ─────────────────────────────────────────────
	
	private Employee createPersistableEmployee(String username, String email,
	                                           CommonCode role, CommonCode dept, CommonCode pos) {
		Employee employee = Employee.createEmployee(
			username, "테스트유저", email, "010-1234-5678",
			LocalDate.now(), statusActive, role, dept, pos,
			creator
		);
		employee.updateInitPassword(TEST_PASSWORD, creator);
		return employee;
	}
	
	// 새 발신자까지 같이 만드는 버전 (MailReceiver 테스트 등에서 사용)
	private Mail createAndSaveMailForNewSender(String username, String email, String title) {
		Employee sender = employeeRepository.save(
			createPersistableEmployee(username, email, roleUser, deptDev, posJunior)
		);
		Mail mail = Mail.createBasicMail(sender, title, "내용", LocalDateTime.now());
		return mailRepository.save(mail);
	}
	
	// 이미 생성된 Employee로 메일만 만드는 버전 (Mailbox 테스트에서 사용)
	private Mail createAndSaveMailForExistingSender(Employee sender, String title) {
		Mail mail = Mail.createBasicMail(sender, title, "내용", LocalDateTime.now());
		return mailRepository.save(mail);
	}
	
	private Mailbox createAndSaveMailbox(Employee owner, Mail mail, CommonCode type,
	                                     Boolean isRead, Byte deletedStatus) {
		Mailbox mailbox = Mailbox.createMailbox(owner, mail, type, isRead, deletedStatus);
		return mailboxRepository.save(mailbox);
	}
	
	// ─────────────────────────────────────────────
	// MailRepository 테스트
	// ─────────────────────────────────────────────
	
	@Test
	@DisplayName("Mail C: 메일 생성(save) 테스트")
	void saveMailTest() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("mailSender", "mailSender@test.com", roleUser, deptDev, posJunior)
		);
		LocalDateTime sendAt = LocalDateTime.now();
		
		Mail mail = Mail.createBasicMail(sender, "테스트 제목", "테스트 내용", sendAt);
		
		Mail savedMail = mailRepository.save(mail);
		
		assertThat(savedMail.getMailId()).isNotNull();
		assertThat(savedMail.getTitle()).isEqualTo("테스트 제목");
		assertThat(savedMail.getContent()).isEqualTo("테스트 내용");
		assertThat(savedMail.getEmployee()).isEqualTo(sender);
		assertThat(savedMail.getSendAt()).isEqualTo(sendAt);
		assertThat(savedMail.getCreatedAt()).isNotNull();
		assertThat(savedMail.getUpdatedAt()).isNotNull();
	}
	
	@Test
	@DisplayName("Mail R: 메일 ID로 조회(findById) - 성공")
	void findMailByIdSuccessTest() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("findMail", "findmail@test.com", roleUser, deptDev, posJunior)
		);
		Mail savedMail = mailRepository.save(
			Mail.createBasicMail(sender, "조회 대상 제목", "조회 대상 내용", LocalDateTime.now())
		);
		
		Optional<Mail> foundMail = mailRepository.findById(savedMail.getMailId());
		
		assertThat(foundMail).isPresent();
		assertThat(foundMail.get().getMailId()).isEqualTo(savedMail.getMailId());
		assertThat(foundMail.get().getTitle()).isEqualTo("조회 대상 제목");
	}
	
	@Test
	@DisplayName("Mail R: 메일 ID로 조회(findById) - 실패 (존재하지 않는 ID)")
	void findMailByIdFailureTest() {
		Long nonExistentId = 9999L;
		
		Optional<Mail> foundMail = mailRepository.findById(nonExistentId);
		
		assertThat(foundMail).isNotPresent();
	}
	
	@Test
	@DisplayName("Mail U: 메일 제목/내용 수정(update) 테스트")
	void updateMailTest() throws InterruptedException {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("updateMail", "updateMail@test.com", roleUser, deptDev, posJunior)
		);
		Mail savedMail = mailRepository.save(
			Mail.createBasicMail(sender, "원본 제목", "원본 내용", LocalDateTime.now())
		);
		LocalDateTime createdAt = savedMail.getCreatedAt();
		
		Thread.sleep(10);
		
		Mail mailToUpdate = mailRepository.findById(savedMail.getMailId()).get();
		mailToUpdate.updateTitle("수정된 제목");
		mailToUpdate.updateContent("수정된 내용");
		
		mailRepository.flush();
		
		Mail updatedMail = mailRepository.findById(savedMail.getMailId()).get();
		
		assertThat(updatedMail.getTitle()).isEqualTo("수정된 제목");
		assertThat(updatedMail.getContent()).isEqualTo("수정된 내용");
		assertThat(updatedMail.getUpdatedAt()).isAfter(createdAt);
	}
	
	@Test
	@DisplayName("Mail D: 메일 삭제(deleteById) 테스트")
	void deleteMailTest() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("deleteMail", "deleteMail@test.com", roleUser, deptDev, posJunior)
		);
		Mail savedMail = mailRepository.save(
			Mail.createBasicMail(sender, "삭제 대상 제목", "삭제 대상 내용", LocalDateTime.now())
		);
		Long mailId = savedMail.getMailId();
		assertThat(mailRepository.existsById(mailId)).isTrue();
		
		mailRepository.deleteById(mailId);
		mailRepository.flush();
		
		assertThat(mailRepository.existsById(mailId)).isFalse();
	}
	
	@Test
	@DisplayName("Mail Exception: 제목(null) 메일 저장 시 예외 발생")
	void saveNullTitleMailTest() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("nullTitleMail", "nullTitleMail@test.com", roleUser, deptDev, posJunior)
		);
		Mail invalidMail = Mail.createBasicMail(sender, null, "내용만 있는 메일", LocalDateTime.now());
		
		assertThatThrownBy(() -> mailRepository.saveAndFlush(invalidMail))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	// ─────────────────────────────────────────────
	// MailReceiverRepository 테스트
	// ─────────────────────────────────────────────
	
	@Test
	@DisplayName("MailReceiver C: 메일 수신자 생성(save) 테스트")
	void saveMailReceiverTest() {
		Mail mail = createAndSaveMailForNewSender("receiverUser", "receiverUser@test.com", "수신자 생성 테스트");
		MailReceiver receiver = MailReceiver.createMailReceiver(mail, "user1@test.com", toType);
		
		MailReceiver saved = mailReceiverRepository.save(receiver);
		
		assertThat(saved.getReceiverId()).isNotNull();
		assertThat(saved.getMail()).isEqualTo(mail);
		assertThat(saved.getEmail()).isEqualTo("user1@test.com");
		assertThat(saved.getType()).isEqualTo(toType);
	}
	
	@Test
	@DisplayName("MailReceiver R: 메일 + 타입 기준 수신자 목록 조회(findAllByMailAndType)")
	void findAllByMailAndTypeTest() {
		Mail mail = createAndSaveMailForNewSender("receiverUser2", "receiverUser2@test.com", "수신자 조회 테스트");
		
		MailReceiver to1 = MailReceiver.createMailReceiver(mail, "to1@test.com", toType);
		MailReceiver to2 = MailReceiver.createMailReceiver(mail, "to2@test.com", toType);
		MailReceiver cc1 = MailReceiver.createMailReceiver(mail, "cc1@test.com", ccType);
		
		mailReceiverRepository.saveAll(List.of(to1, to2, cc1));
		
		List<MailReceiver> toReceivers = mailReceiverRepository.findAllByMailAndType(mail, toType);
		
		assertThat(toReceivers).hasSize(2);
		assertThat(toReceivers)
			.extracting(MailReceiver::getEmail)
			.containsExactlyInAnyOrder("to1@test.com", "to2@test.com");
		assertThat(toReceivers)
			.allMatch(r -> r.getType().equals(toType));
	}
	
	@Test
	@DisplayName("MailReceiver Exception: 동일 (mail, email, type) 중복 저장 시 예외 발생")
	void saveDuplicateMailReceiverTest() {
		Mail mail = createAndSaveMailForNewSender("dupUser", "dupUser@test.com", "중복 수신자 테스트");
		
		MailReceiver first = MailReceiver.createMailReceiver(mail, "dup@test.com", toType);
		mailReceiverRepository.saveAndFlush(first);
		
		MailReceiver duplicate = MailReceiver.createMailReceiver(mail, "dup@test.com", toType);
		
		assertThatThrownBy(() -> mailReceiverRepository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("MailReceiver Exception: 이메일(null) 수신자 저장 시 예외 발생")
	void saveNullEmailReceiverTest() {
		Mail mail = createAndSaveMailForNewSender("nullEmailUser", "nullEmailUser@test.com", "이메일 null 테스트");
		MailReceiver invalid = MailReceiver.createMailReceiver(mail, null, toType);
		
		assertThatThrownBy(() -> mailReceiverRepository.saveAndFlush(invalid))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	// ─────────────────────────────────────────────
	// MailboxRepository 테스트
	// ─────────────────────────────────────────────
	
	@Test
	@DisplayName("Mailbox C: 메일함 항목 생성 + @PrePersist 기본값 테스트")
	void saveMailboxTest() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("boxOwner", "boxOwner@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = createAndSaveMailForExistingSender(owner, "메일함 생성 테스트");
		
		Mailbox mailbox = Mailbox.createMailbox(owner, mail, inboxType, null, null);
		
		Mailbox saved = mailboxRepository.save(mailbox);
		
		assertThat(saved.getBoxId()).isNotNull();
		assertThat(saved.getEmployee()).isEqualTo(owner);
		assertThat(saved.getMail()).isEqualTo(mail);
		assertThat(saved.getTypeId()).isEqualTo(inboxType);
		assertThat(saved.getIsRead()).isFalse();
		assertThat(saved.getDeletedStatus()).isEqualTo((byte) 0);
	}
	
	@Test
	@DisplayName("Mailbox R: employeeId + mailId 기준 단건 조회(findFirstByEmployeeEmployeeIdAndMailMailId)")
	void findFirstByEmployeeAndMailTest() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("owner1", "owner1@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = createAndSaveMailForExistingSender(owner, "조회 대상 메일");
		Mailbox mailbox = createAndSaveMailbox(owner, mail, inboxType, false, (byte) 0);
		
		Optional<Mailbox> found =
			mailboxRepository.findFirstByEmployeeEmployeeIdAndMailMailId(owner.getEmployeeId(), mail.getMailId());
		
		assertThat(found).isPresent();
		assertThat(found.get().getBoxId()).isEqualTo(mailbox.getBoxId());
		assertThat(found.get().getMail().getMailId()).isEqualTo(mail.getMailId());
	}
	
	@Test
	@DisplayName("Mailbox R: boxId 목록 + username 기준 조회(findAllByBoxIdInAndEmployeeUsername)")
	void findAllByBoxIdInAndEmployeeUsernameTest() {
		Employee owner1 = employeeRepository.save(
			createPersistableEmployee("user1", "user1@test.com", roleUser, deptDev, posJunior)
		);
		Employee owner2 = employeeRepository.save(
			createPersistableEmployee("user2", "user2@test.com", roleUser, deptDev, posJunior)
		);
		
		Mail mail1 = createAndSaveMailForExistingSender(owner1, "메일1");
		Mail mail2 = createAndSaveMailForExistingSender(owner1, "메일2");
		Mail mail3 = createAndSaveMailForExistingSender(owner2, "메일3");
		
		Mailbox box1 = createAndSaveMailbox(owner1, mail1, inboxType, false, (byte) 0);
		Mailbox box2 = createAndSaveMailbox(owner1, mail2, inboxType, false, (byte) 0);
		Mailbox box3 = createAndSaveMailbox(owner2, mail3, inboxType, false, (byte) 0);
		
		List<Long> boxIds = List.of(box1.getBoxId(), box2.getBoxId(), box3.getBoxId());
		
		List<Mailbox> result =
			mailboxRepository.findAllByBoxIdInAndEmployeeUsername(boxIds, owner1.getUsername());
		
		assertThat(result).hasSize(2);
		assertThat(result)
			.extracting(Mailbox::getBoxId)
			.containsExactlyInAnyOrder(box1.getBoxId(), box2.getBoxId());
		assertThat(result)
			.allMatch(mb -> mb.getEmployee().equals(owner1));
	}
	
	@Test
	@DisplayName("Mailbox R: username + 타입 + 삭제상태 기준 페이징 조회(findByEmployeeUsernameAndTypeIdValue1AndDeletedStatus)")
	void findByUsernameAndTypeAndDeletedStatusTest() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("pageUser", "pageUser@test.com", roleUser, deptDev, posJunior)
		);
		
		Mail mail1 = createAndSaveMailForExistingSender(owner, "INBOX 메일1");
		Mail mail2 = createAndSaveMailForExistingSender(owner, "INBOX 메일2");
		Mail mail3 = createAndSaveMailForExistingSender(owner, "TRASH 메일");
		
		createAndSaveMailbox(owner, mail1, inboxType, false, (byte) 0);
		createAndSaveMailbox(owner, mail2, inboxType, false, (byte) 0);
		createAndSaveMailbox(owner, mail3, trashType, false, (byte) 1);
		
		Pageable pageable = PageRequest.of(0, 10);
		
		Page<Mailbox> inboxPage =
			mailboxRepository.findByEmployeeUsernameAndTypeIdValue1AndDeletedStatus(
				owner.getUsername(), "INBOX", (byte) 0, pageable
			);
		
		assertThat(inboxPage.getTotalElements()).isEqualTo(2);
		assertThat(inboxPage.getContent())
			.allMatch(mb -> "INBOX".equals(mb.getTypeId().getValue1())
				                && mb.getDeletedStatus() == 0);
	}
	
	@Test
	@DisplayName("Mailbox U: 메일함 상태 변경(markAsRead / moveMail / deleteFromTrash) 테스트")
	void updateMailboxStateTest() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("stateUser", "stateUser@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = createAndSaveMailForExistingSender(owner, "상태 변경 메일");
		
		Mailbox mailbox = createAndSaveMailbox(owner, mail, inboxType, false, (byte) 0);
		
		mailbox.markAsRead();
		mailboxRepository.flush();
		Mailbox afterRead = mailboxRepository.findById(mailbox.getBoxId()).get();
		
		assertThat(afterRead.getIsRead()).isTrue();
		assertThat(afterRead.getDeletedStatus()).isEqualTo((byte) 0);
		
		afterRead.moveMail(trashType);
		mailboxRepository.flush();
		Mailbox afterMove = mailboxRepository.findById(mailbox.getBoxId()).get();
		
		assertThat(afterMove.getTypeId()).isEqualTo(trashType);
		assertThat(afterMove.getDeletedStatus()).isEqualTo((byte) 1);
		
		afterMove.deleteFromTrash();
		mailboxRepository.flush();
		Mailbox afterDelete = mailboxRepository.findById(mailbox.getBoxId()).get();
		
		assertThat(afterDelete.getDeletedStatus()).isEqualTo((byte) 2);
	}
	
	@Test
	@DisplayName("Mailbox Exception: 동일 (employee, mail, type)으로 메일함 중복 생성 시 예외 발생")
	void saveDuplicateMailboxTest() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("dupBoxUser", "dupBoxUser@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = createAndSaveMailForExistingSender(owner, "중복 메일함 테스트");
		
		Mailbox first = createAndSaveMailbox(owner, mail, inboxType, false, (byte) 0);
		Mailbox duplicate = Mailbox.createMailbox(owner, mail, inboxType, false, (byte) 0);
		
		assertThat(first.getBoxId()).isNotNull();
		assertThatThrownBy(() -> mailboxRepository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
