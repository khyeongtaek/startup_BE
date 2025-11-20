package org.goodee.startup_BE.mail.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.mail.dto.*;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.MailReceiver;
import org.goodee.startup_BE.mail.entity.Mailbox;
import org.goodee.startup_BE.mail.enums.MailboxType;
import org.goodee.startup_BE.mail.enums.ReceiverType;
import org.goodee.startup_BE.mail.repository.MailReceiverRepository;
import org.goodee.startup_BE.mail.repository.MailRepository;
import org.goodee.startup_BE.mail.repository.MailboxRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
@Import(MailServiceTests.TestConfig.class)
@TestPropertySource(properties = {
	"file.storage.root=/tmp" // 사용 안 하지만 placeholder
})
@Transactional
class MailServiceTests {
	
	@Autowired
	private MailService mailService;
	
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
	
	@Autowired
	private FakeNotificationService fakeNotificationService;
	
	// 공통 코드
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private CommonCode ownerTypeMail;
	private CommonCode toType, ccType, bccType;
	private CommonCode inboxType, sentType, myboxType, trashType;
	
	// creator
	private Employee creator;
	
	private final String TEST_PASSWORD = "testPassword123!";
	
	@BeforeEach
	void setUp() {
		// FK 순서: 자식 → 부모
		mailReceiverRepository.deleteAll();
		mailboxRepository.deleteAll();
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		fakeNotificationService.clear();
		
		// 직원용 공통 코드
		statusActive = CommonCode.createCommonCode(
			"ST_EMP_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false
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
		
		// OwnerType MAIL
		ownerTypeMail = CommonCode.createCommonCode(
			"OT_MAIL", "메일 모듈", OwnerType.MAIL.name(), null, null, 1L, null, false
		);
		
		// 수신 타입 코드
		toType = CommonCode.createCommonCode(
			"RT_TO", "수신자", ReceiverType.TO.name(), null, null, 1L, null, false
		);
		ccType = CommonCode.createCommonCode(
			"RT_CC", "참조자", ReceiverType.CC.name(), null, null, 2L, null, false
		);
		bccType = CommonCode.createCommonCode(
			"RT_BCC", "숨은참조자", ReceiverType.BCC.name(), null, null, 3L, null, false
		);
		
		// 메일함 타입 코드
		inboxType = CommonCode.createCommonCode(
			"MT_INBOX", "받은메일함", MailboxType.INBOX.name(), null, null, 1L, null, false
		);
		sentType = CommonCode.createCommonCode(
			"MT_SENT", "보낸메일함", MailboxType.SENT.name(), null, null, 2L, null, false
		);
		myboxType = CommonCode.createCommonCode(
			"MT_MYBOX", "개인보관함", MailboxType.MYBOX.name(), null, null, 3L, null, false
		);
		trashType = CommonCode.createCommonCode(
			"MT_TRASH", "휴지통", MailboxType.TRASH.name(), null, null, 4L, null, false
		);
		
		commonCodeRepository.saveAll(List.of(
			statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior,
			ownerTypeMail,
			toType, ccType, bccType,
			inboxType, sentType, myboxType, trashType
		));
		
		// creator
		creator = Employee.createEmployee(
			"admin", "관리자", "admin@test.com", "010-0000-0000",
			LocalDate.now(), statusActive, roleAdmin, deptHr, posSenior,
			null
		);
		creator.updateInitPassword(TEST_PASSWORD, null);
		employeeRepository.save(creator);
	}
	
	// ───────────────────── 헬퍼 ─────────────────────
	
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
	
	// ───────────────────── sendMail 테스트 ─────────────────────
	
	@Test
	@DisplayName("sendMail: 정상 메일 발송 (TO 1명, 첨부 없음)")
	void sendMail_success_basic() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("sender", "sender@test.com", roleUser, deptDev, posJunior)
		);
		Employee receiver = employeeRepository.save(
			createPersistableEmployee("recv1", "recv1@test.com", roleUser, deptDev, posJunior)
		);
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("테스트 제목")
			                             .content("테스트 내용")
			                             .to(List.of(receiver.getEmail()))
			                             .cc(null)
			                             .bcc(null)
			                             .build();
		
		MailSendResponseDTO response =
			mailService.sendMail(request, sender.getUsername(), null);
		
		assertThat(response.getMailId()).isNotNull();
		assertThat(response.getTitle()).isEqualTo("테스트 제목");
		assertThat(response.getSendAt()).isNotNull();
		assertThat(response.getToCount()).isEqualTo(1);
		assertThat(response.getCcCount()).isEqualTo(0);
		assertThat(response.getBccCount()).isEqualTo(0);
		assertThat(response.getAttachmentCount()).isEqualTo(0);
		assertThat(response.getEmlPath()).isEqualTo("test-eml/" + response.getMailId() + ".eml");
		
		Mail mail = mailRepository.findById(response.getMailId()).orElseThrow();
		assertThat(mail.getTitle()).isEqualTo("테스트 제목");
		assertThat(mail.getEmployee().getUsername()).isEqualTo("sender");
		
		List<MailReceiver> receivers = mailReceiverRepository.findAll();
		assertThat(receivers).hasSize(1);
		assertThat(receivers.get(0).getEmail()).isEqualTo("recv1@test.com");
		assertThat(receivers.get(0).getType().getValue1()).isEqualTo(ReceiverType.TO.name());
		
		List<Mailbox> boxes = mailboxRepository.findAll();
		assertThat(boxes).hasSize(2); // sender SENT + receiver INBOX
		
		Mailbox senderBox = boxes.stream()
			                    .filter(b -> b.getEmployee().getUsername().equals("sender"))
			                    .findFirst().orElseThrow();
		Mailbox recvBox = boxes.stream()
			                  .filter(b -> b.getEmployee().getUsername().equals("recv1"))
			                  .findFirst().orElseThrow();
		
		assertThat(senderBox.getTypeId().getValue1()).isEqualTo(MailboxType.SENT.name());
		assertThat(senderBox.getIsRead()).isFalse();
		assertThat(senderBox.getDeletedStatus()).isEqualTo((byte) 0);
		
		assertThat(recvBox.getTypeId().getValue1()).isEqualTo(MailboxType.INBOX.name());
		assertThat(recvBox.getIsRead()).isFalse();
		assertThat(recvBox.getDeletedStatus()).isEqualTo((byte) 0);
		
		assertThat(fakeNotificationService.getCreated().size()).isEqualTo(1);
		NotificationRequestDTO noti = fakeNotificationService.getCreated().get(0);
		assertThat(noti.getEmployeeId()).isEqualTo(receiver.getEmployeeId());
		assertThat(noti.getOwnerTypeCommonCodeId()).isEqualTo(ownerTypeMail.getCommonCodeId());
		assertThat(noti.getUrl()).isEqualTo("/mail/detail/" + mail.getMailId());
	}
	
	@Test
	@DisplayName("sendMail: 수신자(to/cc/bcc 모두 비어있으면 IllegalArgumentException")
	void sendMail_noReceivers_throwsIllegalArgumentException() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("sender2", "sender2@test.com", roleUser, deptDev, posJunior)
		);
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(Collections.emptyList())
			                             .cc(null)
			                             .bcc(null)
			                             .build();
		
		assertThatThrownBy(() -> mailService.sendMail(request, sender.getUsername(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("수신자");
	}
	
	@Test
	@DisplayName("sendMail: 사내 미등록 이메일 포함 시 ResourceNotFoundException")
	void sendMail_unknownEmail_throwsResourceNotFoundException() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("sender3", "sender3@test.com", roleUser, deptDev, posJunior)
		);
		Employee receiver = employeeRepository.save(
			createPersistableEmployee("recvKnown", "known@test.com", roleUser, deptDev, posJunior)
		);
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(List.of(receiver.getEmail(), "unknown@test.com"))
			                             .cc(null)
			                             .bcc(null)
			                             .build();
		
		assertThatThrownBy(() -> mailService.sendMail(request, sender.getUsername(), null))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("사내 미등록 이멜");
	}
	
	// ───────────────────── getMailDetail 테스트 ─────────────────────
	
	@Test
	@DisplayName("getMailDetail: 수신자 조회 시 읽음 처리 및 BCC 노출 안됨")
	void getMailDetail_asReceiver_marksRead_andNoBcc() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("sender4", "sender4@test.com", roleUser, deptDev, posJunior)
		);
		Employee to = employeeRepository.save(
			createPersistableEmployee("toUser", "touser@test.com", roleUser, deptDev, posJunior)
		);
		Employee bcc = employeeRepository.save(
			createPersistableEmployee("bccUser", "bccuser@test.com", roleUser, deptDev, posJunior)
		);
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(List.of(to.getEmail()))
			                             .cc(null)
			                             .bcc(List.of(bcc.getEmail()))
			                             .build();
		
		MailSendResponseDTO sendResp =
			mailService.sendMail(request, sender.getUsername(), null);
		
		Long mailId = sendResp.getMailId();
		
		MailDetailResponseDTO detail =
			mailService.getMailDetail(mailId, to.getUsername(), true);
		
		assertThat(detail.getMailId()).isEqualTo(mailId);
		assertThat(detail.getMailboxType()).isEqualTo(MailboxType.INBOX.name());
		assertThat(detail.getIsRead()).isTrue();
		assertThat(detail.getBcc()).isEmpty();   // 수신자는 BCC 안 보임
		
		Mailbox inboxBox = mailboxRepository
			                   .findFirstByEmployeeEmployeeIdAndMailMailId(to.getEmployeeId(), mailId)
			                   .orElseThrow();
		assertThat(inboxBox.getIsRead()).isTrue();
	}
	
	
	@Test
	@DisplayName("getMailDetail: 발신자 조회 시 BCC 포함, 삭제된 메일은 조회 불가")
	void getMailDetail_asSender_seesBcc_andDeletedMailThrows() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("sender5", "sender5@test.com", roleUser, deptDev, posJunior)
		);
		Employee to = employeeRepository.save(
			createPersistableEmployee("toUser5", "touser5@test.com", roleUser, deptDev, posJunior)   // ✅ 소문자
		);
		Employee bcc = employeeRepository.save(
			createPersistableEmployee("bccUser5", "bccuser5@test.com", roleUser, deptDev, posJunior) // ✅ 소문자
		);
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목-BCC")
			                             .content("내용-BCC")
			                             .to(List.of(to.getEmail()))
			                             .cc(null)
			                             .bcc(List.of(bcc.getEmail()))
			                             .build();
		
		MailSendResponseDTO sendResp =
			mailService.sendMail(request, sender.getUsername(), null);
		Long mailId = sendResp.getMailId();
		
		// 발신자 조회 -> BCC 보임
		MailDetailResponseDTO senderDetail =
			mailService.getMailDetail(mailId, sender.getUsername(), false);
		
		assertThat(senderDetail.getBcc()).isNotNull();
		assertThat(senderDetail.getBcc()).hasSize(1);
		assertThat(senderDetail.getBcc().get(0).getEmail())
			.isEqualTo("bccuser5@test.com");
		
		// 수신자 메일박스 하나 골라서 삭제 상태 2로 설정
		Mailbox recvBox = mailboxRepository
			                  .findFirstByEmployeeEmployeeIdAndMailMailId(to.getEmployeeId(), mailId)
			                  .orElseThrow();
		recvBox.deleteFromTrash();
		mailboxRepository.flush();
		
		assertThatThrownBy(() ->
			                   mailService.getMailDetail(mailId, to.getUsername(), false)
		).isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("삭제된 메일");
	}
	
	// ───────────────────── moveMails 테스트 ─────────────────────
	
	@Test
	@DisplayName("moveMails: MYBOX로 이동 성공")
	void moveMails_toMybox_success() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("ownerMove", "ownerMove@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = mailRepository.save(
			Mail.createBasicMail(owner, "제목", "내용", LocalDateTime.now())
		);
		
		Mailbox box = mailboxRepository.save(
			Mailbox.createMailbox(owner, mail, inboxType, false, (byte) 0)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box.getBoxId()))
			                             .targetType("MYBOX")
			                             .build();
		
		mailService.moveMails(request, owner.getUsername());
		
		Mailbox updated = mailboxRepository.findById(box.getBoxId()).orElseThrow();
		assertThat(updated.getTypeId().getValue1()).isEqualTo(MailboxType.MYBOX.name());
		assertThat(updated.getDeletedStatus()).isEqualTo((byte) 0);
	}
	
	@Test
	@DisplayName("moveMails: TRASH로 이동 시 deletedStatus=1")
	void moveMails_toTrash_setsDeletedStatus() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("ownerTrash", "ownerTrash@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = mailRepository.save(
			Mail.createBasicMail(owner, "제목", "내용", LocalDateTime.now())
		);
		
		Mailbox box = mailboxRepository.save(
			Mailbox.createMailbox(owner, mail, inboxType, false, (byte) 0)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box.getBoxId()))
			                             .targetType("TRASH")
			                             .build();
		
		mailService.moveMails(request, owner.getUsername());
		
		Mailbox updated = mailboxRepository.findById(box.getBoxId()).orElseThrow();
		assertThat(updated.getTypeId().getValue1()).isEqualTo(MailboxType.TRASH.name());
		assertThat(updated.getDeletedStatus()).isEqualTo((byte) 1);
	}
	
	@Test
	@DisplayName("moveMails: 다른 사용자의 메일함 포함 시 AccessDeniedException")
	void moveMails_unauthorized_throwsAccessDenied() {
		Employee owner1 = employeeRepository.save(
			createPersistableEmployee("ownerA", "ownerA@test.com", roleUser, deptDev, posJunior)
		);
		Employee owner2 = employeeRepository.save(
			createPersistableEmployee("ownerB", "ownerB@test.com", roleUser, deptDev, posJunior)
		);
		
		Mail mail1 = mailRepository.save(
			Mail.createBasicMail(owner1, "제목1", "내용1", LocalDateTime.now())
		);
		Mail mail2 = mailRepository.save(
			Mail.createBasicMail(owner2, "제목2", "내용2", LocalDateTime.now())
		);
		
		Mailbox box1 = mailboxRepository.save(
			Mailbox.createMailbox(owner1, mail1, inboxType, false, (byte) 0)
		);
		Mailbox box2 = mailboxRepository.save(
			Mailbox.createMailbox(owner2, mail2, inboxType, false, (byte) 0)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box1.getBoxId(), box2.getBoxId()))
			                             .targetType("MYBOX")
			                             .build();
		
		assertThatThrownBy(() ->
			                   mailService.moveMails(request, owner1.getUsername())
		).isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("권한이 없거나 존재하지 않는 항목");
	}
	
	@Test
	@DisplayName("moveMails: 지원하지 않는 타입으로 이동 시 IllegalArgumentException")
	void moveMails_invalidTargetType_throwsIllegalArgumentException() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("ownerInvalid", "ownerInvalid@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = mailRepository.save(
			Mail.createBasicMail(owner, "제목", "내용", LocalDateTime.now())
		);
		Mailbox box = mailboxRepository.save(
			Mailbox.createMailbox(owner, mail, inboxType, false, (byte) 0)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box.getBoxId()))
			                             .targetType("UNKNOWN")
			                             .build();
		
		assertThatThrownBy(() ->
			                   mailService.moveMails(request, owner.getUsername())
		).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 타입은 존재하지 않습니다");
	}
	
	// ───────────────────── deleteMails 테스트 ─────────────────────
	
	@Test
	@DisplayName("deleteMails: TRASH에 있는 메일 완전 삭제 (deletedStatus=2)")
	void deleteMails_success() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("delOwner", "delOwner@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = mailRepository.save(
			Mail.createBasicMail(owner, "삭제 제목", "삭제 내용", LocalDateTime.now())
		);
		Mailbox box = mailboxRepository.save(
			Mailbox.createMailbox(owner, mail, trashType, false, (byte) 1)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box.getBoxId()))
			                             .targetType("TRASH") // 실제로는 targetType 안 쓰지만 DTO 재사용
			                             .build();
		
		mailService.deleteMails(request, owner.getUsername());
		
		Mailbox updated = mailboxRepository.findById(box.getBoxId()).orElseThrow();
		assertThat(updated.getDeletedStatus()).isEqualTo((byte) 2);
	}
	
	@Test
	@DisplayName("deleteMails: TRASH가 아닌 메일 삭제 시 IllegalStateException")
	void deleteMails_notInTrash_throwsIllegalStateException() {
		Employee owner = employeeRepository.save(
			createPersistableEmployee("delOwner2", "delOwner2@test.com", roleUser, deptDev, posJunior)
		);
		Mail mail = mailRepository.save(
			Mail.createBasicMail(owner, "제목", "내용", LocalDateTime.now())
		);
		Mailbox box = mailboxRepository.save(
			Mailbox.createMailbox(owner, mail, inboxType, false, (byte) 0)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box.getBoxId()))
			                             .targetType("TRASH")
			                             .build();
		
		assertThatThrownBy(() ->
			                   mailService.deleteMails(request, owner.getUsername())
		).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("메일이 휴지통에 존재하지 않습니다");
	}
	
	@Test
	@DisplayName("deleteMails: 다른 사용자의 메일함 포함 시 AccessDeniedException")
	void deleteMails_unauthorized_throwsAccessDenied() {
		Employee owner1 = employeeRepository.save(
			createPersistableEmployee("delA", "delA@test.com", roleUser, deptDev, posJunior)
		);
		Employee owner2 = employeeRepository.save(
			createPersistableEmployee("delB", "delB@test.com", roleUser, deptDev, posJunior)
		);
		
		Mail mail1 = mailRepository.save(
			Mail.createBasicMail(owner1, "제목1", "내용1", LocalDateTime.now())
		);
		Mail mail2 = mailRepository.save(
			Mail.createBasicMail(owner2, "제목2", "내용2", LocalDateTime.now())
		);
		
		Mailbox box1 = mailboxRepository.save(
			Mailbox.createMailbox(owner1, mail1, trashType, false, (byte) 1)
		);
		Mailbox box2 = mailboxRepository.save(
			Mailbox.createMailbox(owner2, mail2, trashType, false, (byte) 1)
		);
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(box1.getBoxId(), box2.getBoxId()))
			                             .targetType("TRASH")
			                             .build();
		
		assertThatThrownBy(() ->
			                   mailService.deleteMails(request, owner1.getUsername())
		).isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("권한이 없거나 존재하지 않는 항목");
	}
	
	// ───────────────────── getMailboxList 테스트 ─────────────────────
	
	@Test
	@DisplayName("getMailboxList: INBOX 메일함 리스트 조회")
	void getMailboxList_inbox_success() {
		Employee sender = employeeRepository.save(
			createPersistableEmployee("listSender", "listsender@test.com", roleUser, deptDev, posJunior)
		);
		Employee receiver = employeeRepository.save(
			createPersistableEmployee("listRecv", "listrecv@test.com", roleUser, deptDev, posJunior)
		);
		
		// 메일 2개 발송
		MailSendRequestDTO req1 = MailSendRequestDTO.builder()
			                          .title("첫 번째 메일")
			                          .content("내용1")
			                          .to(List.of(receiver.getEmail()))
			                          .build();
		
		MailSendRequestDTO req2 = MailSendRequestDTO.builder()
			                          .title("두 번째 메일")
			                          .content("내용2")
			                          .to(List.of(receiver.getEmail()))
			                          .build();
		
		mailService.sendMail(req1, sender.getUsername(), null);
		mailService.sendMail(req2, sender.getUsername(), null);
		
		Page<MailboxListDTO> inboxPage =
			mailService.getMailboxList(receiver.getUsername(), "INBOX", 0, 10);
		
		assertThat(inboxPage.getTotalElements()).isEqualTo(2);
		assertThat(inboxPage.getContent())
			.extracting(MailboxListDTO::getTitle)
			.containsExactlyInAnyOrder("첫 번째 메일", "두 번째 메일");
		
		// 수신자 DTO 리스트 확인 (MailReceiverDTO)
		inboxPage.getContent().forEach(dto -> {
			assertThat(dto.getReceivers()).hasSize(1);
			MailReceiverDTO recvDto = dto.getReceivers().get(0);
			assertThat(recvDto.getName()).isEqualTo("테스트유저");
			assertThat(recvDto.getEmail()).isEqualTo("listrecv@test.com");
			assertThat(dto.getSenderName()).isEqualTo("테스트유저");
		});
	}
	
	
	// ───────────────────── TestConfig & Stubs ─────────────────────
	
	@TestConfiguration
	static class TestConfig {
		
		@Bean
		MailService mailService(MailRepository mailRepository,
		                        MailReceiverRepository mailReceiverRepository,
		                        MailboxRepository mailboxRepository,
		                        EmployeeRepository employeeRepository,
		                        CommonCodeRepository commonCodeRepository,
		                        AttachmentFileService attachmentFileService,
		                        EmlService emlService,
		                        NotificationService notificationService) {
			return new MailServiceImpl(
				mailRepository,
				mailReceiverRepository,
				mailboxRepository,
				employeeRepository,
				commonCodeRepository,
				attachmentFileService,
				emlService,
				notificationService
			);
		}
		
		@Bean
		EmlService emlService() {
			return new FakeEmlService();
		}
		
		@Bean
		AttachmentFileService attachmentFileService() {
			return new FakeAttachmentFileService();
		}
		
		@Bean
		FakeNotificationService notificationService() {
			return new FakeNotificationService();
		}
	}
	
	// 첨부파일은 서비스 테스트에서 실제 파일 저장/조회 안 함
	static class FakeAttachmentFileService implements AttachmentFileService {
		@Override
		public List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFile, Long ownerTypeId, Long ownerId) {
			return List.of();
		}
		
		@Override
		public List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId) {
			return List.of();
		}
		
		@Override
		public ResponseEntity<Resource> downloadFile(Long fileId) {
			throw new UnsupportedOperationException("downloadFile not supported in test");
		}
		
		@Override
		public void deleteFile(Long fileId) {
			// no-op
		}
	}
	
	// EML은 경로만 고정 패턴으로 리턴
	static class FakeEmlService implements EmlService {
		@Override
		public String generate(Mail mail,
		                       List<String> to, List<String> cc, List<String> bcc,
		                       List<AttachmentFileResponseDTO> attachmentFiles,
		                       Employee sender) {
			return "test-eml/" + mail.getMailId() + ".eml";
		}
	}
	
	// NotificationService는 DB 사용 안 하고 호출만 기록
	static class FakeNotificationService implements NotificationService {
		
		private final List<NotificationRequestDTO> created = new ArrayList<>();
		
		List<NotificationRequestDTO> getCreated() {
			return created;
		}
		
		void clear() {
			created.clear();
		}
		
		@Override
		public org.goodee.startup_BE.notification.dto.NotificationResponseDTO create(NotificationRequestDTO requestDTO) {
			created.add(requestDTO);
			return null;
		}
		
		@Override
		public Page<org.goodee.startup_BE.notification.dto.NotificationResponseDTO> list(String username, org.springframework.data.domain.Pageable pageable) {
			throw new UnsupportedOperationException("list not used in MailService test");
		}
		
		@Override
		public String getUrl(Long notificationId, String username) {
			throw new UnsupportedOperationException("getUrl not used in MailService test");
		}
		
		@Override
		public void softDelete(Long notificationId, String username) {
			// no-op
		}
		
		@Override
		public long getUnreadNotiCount(String username) {
			return 0;
		}
		
		@Override
		public void readAll(String username) {
			// no-op
		}
		
		@Override
		public void softDeleteAll(String username) {
			// no-op
		}
		
		@Override
		public void sendNotificationCounts(String username) {
			// no-op
		}
	}
}
