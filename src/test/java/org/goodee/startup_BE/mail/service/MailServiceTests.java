package org.goodee.startup_BE.mail.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.mail.dto.*;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.MailReceiver;
import org.goodee.startup_BE.mail.entity.Mailbox;
import org.goodee.startup_BE.mail.repository.MailReceiverRepository;
import org.goodee.startup_BE.mail.repository.MailRepository;
import org.goodee.startup_BE.mail.repository.MailboxRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
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
class MailServiceTests {
	
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
	
	private MailService mailService;
	
	private StubNotificationService stubNotificationService;
	
	// 공통 코드
	private CommonCode statusActive;
	private CommonCode roleUser;
	private CommonCode deptDev;
	private CommonCode posJunior;
	
	private CommonCode ownerTypeMail;
	private CommonCode receiverToCode;
	private CommonCode receiverCcCode;
	private CommonCode receiverBccCode;
	private CommonCode mailboxInboxCode;
	private CommonCode mailboxSentCode;
	private CommonCode mailboxMyboxCode;
	private CommonCode mailboxTrashCode;
	
	private Employee creator;
	
	private static final String TEST_PASSWORD = "testPassword123!";
	
	@BeforeEach
	void setUp() {
		mailReceiverRepository.deleteAll();
		mailboxRepository.deleteAll();
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// 공통 코드 세팅 (isDisabled = false)
		statusActive = CommonCode.createCommonCode(
			"STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
		roleUser = CommonCode.createCommonCode(
			"ROLE_USER", "일반 사용자", "ROLE_USER", null, null, 1L, null, false);
		deptDev = CommonCode.createCommonCode(
			"DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
		posJunior = CommonCode.createCommonCode(
			"POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
		
		// OwnerType / ReceiverType / MailboxType
		ownerTypeMail = CommonCode.createCommonCode(
			"OT_MAIL", "메일 첨부", "MAIL", null, null, 1L, null, false);
		
		receiverToCode = CommonCode.createCommonCode(
			"RT_TO", "수신자", "TO", null, null, 1L, null, false);
		receiverCcCode = CommonCode.createCommonCode(
			"RT_CC", "참조", "CC", null, null, 2L, null, false);
		receiverBccCode = CommonCode.createCommonCode(
			"RT_BCC", "숨은참조", "BCC", null, null, 3L, null, false);
		
		mailboxInboxCode = CommonCode.createCommonCode(
			"MT_INBOX", "받은메일함", "INBOX", null, null, 1L, null, false);
		mailboxSentCode = CommonCode.createCommonCode(
			"MT_SENT", "보낸메일함", "SENT", null, null, 2L, null, false);
		mailboxMyboxCode = CommonCode.createCommonCode(
			"MT_MYBOX", "개인보관함", "MYBOX", null, null, 3L, null, false);
		mailboxTrashCode = CommonCode.createCommonCode(
			"MT_TRASH", "휴지통", "TRASH", null, null, 4L, null, false);
		
		commonCodeRepository.saveAll(List.of(
			statusActive, roleUser, deptDev, posJunior,
			ownerTypeMail,
			receiverToCode, receiverCcCode, receiverBccCode,
			mailboxInboxCode, mailboxSentCode, mailboxMyboxCode, mailboxTrashCode
		));
		
		// creator 직원
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
		
		stubNotificationService = new StubNotificationService();
		
		mailService = new MailServiceImpl(
			mailRepository,
			mailReceiverRepository,
			mailboxRepository,
			employeeRepository,
			commonCodeRepository,
			new StubAttachmentFileService(),
			new StubEmlService(),
			stubNotificationService
		);
	}
	
	private Employee createAndSaveEmployee(String username, String email) {
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
		return employeeRepository.save(employee);
	}
	
	private MailSendResponseDTO sendSimpleMail(Employee sender, Employee receiver) {
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("테스트 메일")
			                             .content("<p>내용</p>")
			                             .to(List.of(receiver.getEmail()))
			                             .cc(Collections.emptyList())
			                             .bcc(Collections.emptyList())
			                             .build();
		return mailService.sendMail(request, sender.getUsername(), null);
	}
	
	private Mailbox findSingleMailbox(String username, String typeValue1) {
		byte deleted = (byte) ("TRASH".equals(typeValue1) ? 1 : 0);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Mailbox> page = mailboxRepository
			                     .findByEmployeeUsernameAndTypeIdValue1AndDeletedStatus(username, typeValue1, deleted, pageable);
		assertThat(page.getTotalElements())
			.as("username=" + username + ", type=" + typeValue1 + " 메일함이 최소 1개 있어야 합니다.")
			.isGreaterThan(0);
		return page.getContent().get(0);
	}
	
	// C ----------------------------------------------------------------------
	
	@Test
	@DisplayName("C: sendMail - 정상 발송 시 메일/수신자/메일함/알림이 생성된다.")
	void sendMail_success_createsMailAndRelations() {
		// given
		Employee sender = createAndSaveEmployee("sender1", "sender1@test.com");
		Employee receiver1 = createAndSaveEmployee("receiver1", "receiver1@test.com");
		Employee receiver2 = createAndSaveEmployee("receiver2", "receiver2@test.com");
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("테스트 메일")
			                             .content("<p>내용</p>")
			                             .to(List.of(receiver1.getEmail()))
			                             .cc(List.of(receiver2.getEmail()))
			                             .bcc(Collections.emptyList())
			                             .build();
		
		// when
		MailSendResponseDTO response = mailService.sendMail(request, sender.getUsername(), null);
		
		// then
		assertThat(response).isNotNull();
		assertThat(response.getMailId()).isNotNull();
		assertThat(response.getTitle()).isEqualTo("테스트 메일");
		assertThat(response.getToCount()).isEqualTo(1);
		assertThat(response.getCcCount()).isEqualTo(1);
		assertThat(response.getBccCount()).isZero();
		assertThat(response.getAttachmentCount()).isZero();
		assertThat(response.getEmlPath()).isNotNull();
		
		Mail savedMail = mailRepository.findById(response.getMailId()).orElseThrow();
		assertThat(savedMail.getEmployee().getUsername()).isEqualTo(sender.getUsername());
		assertThat(savedMail.getTitle()).isEqualTo(request.getTitle());
		assertThat(savedMail.getContent()).isEqualTo(request.getContent());
		
		List<MailReceiver> receivers = mailReceiverRepository.findAll();
		assertThat(receivers).hasSize(2);
		assertThat(receivers)
			.extracting(MailReceiver::getEmail)
			.containsExactlyInAnyOrder(receiver1.getEmail(), receiver2.getEmail());
		
		List<Mailbox> mailboxes = mailboxRepository.findAll();
		assertThat(mailboxes).hasSize(3);
		
		long sentCount = mailboxes.stream()
			                 .filter(mb -> "SENT".equals(mb.getTypeId().getValue1()))
			                 .count();
		long inboxCount = mailboxes.stream()
			                  .filter(mb -> "INBOX".equals(mb.getTypeId().getValue1()))
			                  .count();
		
		assertThat(sentCount).isEqualTo(1);
		assertThat(inboxCount).isEqualTo(2);
		
		// 알림 생성 횟수 (수신자 2명)
		assertThat(stubNotificationService.getCreatedCount()).isEqualTo(2);
	}
	
	@Test
	@DisplayName("C: sendMail - 존재하지 않는 발신자 username이면 ResourceNotFoundException")
	void sendMail_fail_senderNotFound() {
		// given
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(List.of("receiver@test.com"))
			                             .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.sendMail(request, "unknownUser", null))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("직원이 존재하지 않습니다");
	}
	
	@Test
	@DisplayName("C: sendMail - 수신자(to/cc/bcc)가 모두 없으면 IllegalArgumentException")
	void sendMail_fail_noReceivers() {
		// given
		Employee sender = createAndSaveEmployee("sender2", "sender2@test.com");
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(Collections.emptyList())
			                             .cc(Collections.emptyList())
			                             .bcc(Collections.emptyList())
			                             .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.sendMail(request, sender.getUsername(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("수신자(to/cc/bcc) 중 최소 1명은 필요합니다.");
	}
	
	@Test
	@DisplayName("C: sendMail - 사내 미등록 이메일이 포함되면 ResourceNotFoundException")
	void sendMail_fail_unknownReceiverEmail() {
		// given
		Employee sender = createAndSaveEmployee("sender3", "sender3@test.com");
		
		String unknownEmail = "unknown@test.com";
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(List.of(unknownEmail))
			                             .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.sendMail(request, sender.getUsername(), null))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("사내 미등록 이멜");
	}
	
	// R ----------------------------------------------------------------------
	
	@Test
	@DisplayName("R: getMailDetail - 수신자가 상세 조회 시 읽음 처리 및 DTO 매핑")
	void getMailDetail_asReceiver_marksReadAndReturnsDto() {
		// given
		Employee sender = createAndSaveEmployee("sender4", "sender4@test.com");
		Employee receiver = createAndSaveEmployee("receiver4", "receiver4@test.com");
		
		MailSendResponseDTO sendResponse = sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		assertThat(inboxBox.getIsRead()).isFalse();
		
		// when
		MailDetailResponseDTO detail = mailService.getMailDetail(
			sendResponse.getMailId(),
			inboxBox.getBoxId(),
			receiver.getUsername(),
			true
		);
		
		// then
		assertThat(detail.getMailId()).isEqualTo(sendResponse.getMailId());
		assertThat(detail.getMailboxType()).isEqualTo("INBOX");
		assertThat(detail.getBoxId()).isEqualTo(inboxBox.getBoxId());
		assertThat(detail.getIsRead()).isTrue();
		assertThat(detail.getSenderEmail()).isEqualTo(sender.getEmail());
		assertThat(detail.getTo())
			.extracting(MailReceiverDTO::getEmail)
			.containsExactly(receiver.getEmail());
		
		Mailbox updated = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		assertThat(updated.getIsRead()).isTrue();
	}
	
	@Test
	@DisplayName("R: getMailDetail - 발신자는 BCC 목록을 조회 가능, 수신자는 불가")
	void getMailDetail_bccVisibilityDependsOnSender() {
		// given
		Employee sender = createAndSaveEmployee("sender5", "sender5@test.com");
		Employee to = createAndSaveEmployee("to5", "to5@test.com");
		Employee bcc = createAndSaveEmployee("bcc5", "bcc5@test.com");
		
		MailSendRequestDTO request = MailSendRequestDTO.builder()
			                             .title("제목")
			                             .content("내용")
			                             .to(List.of(to.getEmail()))
			                             .cc(Collections.emptyList())
			                             .bcc(List.of(bcc.getEmail()))
			                             .build();
		
		MailSendResponseDTO response = mailService.sendMail(request, sender.getUsername(), null);
		
		Mailbox sentBox = findSingleMailbox(sender.getUsername(), "SENT");
		Mailbox inboxOfTo = findSingleMailbox(to.getUsername(), "INBOX");
		
		// when - 발신자
		MailDetailResponseDTO senderView = mailService.getMailDetail(
			response.getMailId(),
			sentBox.getBoxId(),
			sender.getUsername(),
			false
		);
		
		// then
		assertThat(senderView.getBcc()).isNotNull();
		assertThat(senderView.getBcc())
			.extracting(MailReceiverDTO::getEmail)
			.containsExactly(bcc.getEmail());
		
		// when - 수신자
		MailDetailResponseDTO receiverView = mailService.getMailDetail(
			response.getMailId(),
			inboxOfTo.getBoxId(),
			to.getUsername(),
			false
		);
		
		// then
		// DTO에서 null 대신 빈 리스트로 반환하니까 이렇게 체크
		assertThat(receiverView.getBcc()).isEmpty();
	}
	
	
	@Test
	@DisplayName("R: getMailDetail - 삭제된 메일은 조회 시 ResourceNotFoundException")
	void getMailDetail_fail_whenDeletedStatus2() {
		// given
		Employee sender = createAndSaveEmployee("sender6", "sender6@test.com");
		Employee receiver = createAndSaveEmployee("receiver6", "receiver6@test.com");
		
		MailSendResponseDTO response = sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		// 휴지통으로 이동
		MailMoveRequestDTO moveToTrash = MailMoveRequestDTO.builder()
			                                 .mailIds(List.of(inboxBox.getBoxId()))
			                                 .targetType("TRASH")
			                                 .build();
		mailService.moveMails(moveToTrash, receiver.getUsername());
		
		Mailbox trashBox = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		// TRASH 이동 후: typeId는 기존 메일함(INBOX) 유지, deletedStatus 로 휴지통 여부 판단
		assertThat(trashBox.getTypeId().getValue1()).isEqualTo("INBOX");
		assertThat(trashBox.getDeletedStatus()).isEqualTo((byte) 1);
		
		// 휴지통에서 삭제
		MailMoveRequestDTO deleteReq = MailMoveRequestDTO.builder()
			                               .mailIds(List.of(inboxBox.getBoxId()))
			                               .targetType("TRASH")
			                               .build();
		mailService.deleteMails(deleteReq, receiver.getUsername());
		
		Mailbox deletedBox = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		assertThat(deletedBox.getDeletedStatus()).isEqualTo((byte) 2);
		
		// when & then
		assertThatThrownBy(() ->
			                   mailService.getMailDetail(response.getMailId(), inboxBox.getBoxId(), receiver.getUsername(), false)
		)
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("삭제된 메일입니다.");
	}
	
	@Test
	@DisplayName("R: getMailboxList - INBOX/TRASH에 따라 삭제 상태에 맞는 목록만 조회")
	void getMailboxList_filtersByTypeAndDeletedStatus() {
		// given
		Employee sender = createAndSaveEmployee("sender12", "sender12@test.com");
		Employee receiver = createAndSaveEmployee("receiver12", "receiver12@test.com");
		
		MailSendRequestDTO r1 = MailSendRequestDTO.builder()
			                        .title("메일1")
			                        .content("내용1")
			                        .to(List.of(receiver.getEmail()))
			                        .build();
		MailSendRequestDTO r2 = MailSendRequestDTO.builder()
			                        .title("메일2")
			                        .content("내용2")
			                        .to(List.of(receiver.getEmail()))
			                        .build();
		
		mailService.sendMail(r1, sender.getUsername(), null);
		mailService.sendMail(r2, sender.getUsername(), null);
		
		Page<MailboxListDTO> inboxPage = mailService.getMailboxList(receiver.getUsername(), "INBOX", 0, 10);
		assertThat(inboxPage.getTotalElements()).isEqualTo(2);
		
		// 하나를 휴지통으로 이동
		Long boxIdToTrash = inboxPage.getContent().get(0).getBoxId();
		MailMoveRequestDTO moveToTrash = MailMoveRequestDTO.builder()
			                                 .mailIds(List.of(boxIdToTrash))
			                                 .targetType("TRASH")
			                                 .build();
		mailService.moveMails(moveToTrash, receiver.getUsername());
		
		Page<MailboxListDTO> inboxAfter = mailService.getMailboxList(receiver.getUsername(), "INBOX", 0, 10);
		assertThat(inboxAfter.getTotalElements()).isEqualTo(1);
		
		Page<MailboxListDTO> trashList = mailService.getMailboxList(receiver.getUsername(), "TRASH", 0, 10);
		assertThat(trashList.getTotalElements()).isEqualTo(1);
	}
	
	// U ----------------------------------------------------------------------
	
	@Test
	@DisplayName("U: moveMails - MYBOX로 이동 시 타입과 deletedStatus 변경")
	void moveMails_moveToMybox_success() {
		// given
		Employee sender = createAndSaveEmployee("sender7", "sender7@test.com");
		Employee receiver = createAndSaveEmployee("receiver7", "receiver7@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(inboxBox.getBoxId()))
			                             .targetType("MYBOX")
			                             .build();
		
		// when
		mailService.moveMails(request, receiver.getUsername());
		
		// then
		Mailbox updated = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		assertThat(updated.getTypeId().getValue1()).isEqualTo("MYBOX");
		assertThat(updated.getDeletedStatus()).isEqualTo((byte) 0);
		assertThat(updated.getIsRead()).isFalse();
	}
	
	@Test
	@DisplayName("U: moveMails - TRASH로 이동 시 deletedStatus=1")
	void moveMails_moveToTrash_success() {
		// given
		Employee sender = createAndSaveEmployee("senderX", "senderx@test.com");
		Employee receiver = createAndSaveEmployee("receiverX", "receiverx@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(inboxBox.getBoxId()))
			                             .targetType("TRASH")
			                             .build();
		
		// when
		mailService.moveMails(request, receiver.getUsername());
		
		// then
		Mailbox updated = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		assertThat(updated.getTypeId().getValue1()).isEqualTo("INBOX");   // 타입 유지
		assertThat(updated.getDeletedStatus()).isEqualTo((byte) 1);       // 휴지통 플래그
		assertThat(updated.getIsRead()).isFalse();
	}
	
	@Test
	@DisplayName("U: moveMails - 다른 사용자의 메일함 이동 시 AccessDeniedException")
	void moveMails_fail_otherUserMailbox() {
		// given
		Employee sender = createAndSaveEmployee("sender8", "sender8@test.com");
		Employee receiver = createAndSaveEmployee("receiver8", "receiver8@test.com");
		Employee otherUser = createAndSaveEmployee("otherUser", "otherUser@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(inboxBox.getBoxId()))
			                             .targetType("MYBOX")
			                             .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.moveMails(request, otherUser.getUsername()))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("권한이 없거나 존재하지 않는 항목이 포함되어 있습니다.");
	}
	
	@Test
	@DisplayName("U: moveMails - 잘못된 targetType이면 IllegalArgumentException")
	void moveMails_fail_invalidTargetType() {
		// given
		Employee sender = createAndSaveEmployee("sender9", "sender9@test.com");
		Employee receiver = createAndSaveEmployee("receiver9", "receiver9@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO request = MailMoveRequestDTO.builder()
			                             .mailIds(List.of(inboxBox.getBoxId()))
			                             .targetType("UNKNOWN")
			                             .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.moveMails(request, receiver.getUsername()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 타입은 존재하지 않습니다.");
	}
	
	// D ----------------------------------------------------------------------
	
	@Test
	@DisplayName("D: deleteMails - 휴지통 메일 삭제 시 deletedStatus=2")
	void deleteMails_success_softDeleteFromTrash() {
		// given
		Employee sender = createAndSaveEmployee("sender10", "sender10@test.com");
		Employee receiver = createAndSaveEmployee("receiver10", "receiver10@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO moveToTrash = MailMoveRequestDTO.builder()
			                                 .mailIds(List.of(inboxBox.getBoxId()))
			                                 .targetType("TRASH")
			                                 .build();
		mailService.moveMails(moveToTrash, receiver.getUsername());
		
		// when
		MailMoveRequestDTO deleteReq = MailMoveRequestDTO.builder()
			                               .mailIds(List.of(inboxBox.getBoxId()))
			                               .targetType("TRASH")
			                               .build();
		mailService.deleteMails(deleteReq, receiver.getUsername());
		
		// then
		Mailbox deleted = mailboxRepository.findById(inboxBox.getBoxId()).orElseThrow();
		assertThat(deleted.getDeletedStatus()).isEqualTo((byte) 2);
	}
	
	@Test
	@DisplayName("D: deleteMails - 휴지통이 아닌 메일 삭제 시 IllegalStateException")
	void deleteMails_fail_whenNotInTrash() {
		// given
		Employee sender = createAndSaveEmployee("sender11", "sender11@test.com");
		Employee receiver = createAndSaveEmployee("receiver11", "receiver11@test.com");
		
		sendSimpleMail(sender, receiver);
		Mailbox inboxBox = findSingleMailbox(receiver.getUsername(), "INBOX");
		
		MailMoveRequestDTO deleteReq = MailMoveRequestDTO.builder()
			                               .mailIds(List.of(inboxBox.getBoxId()))
			                               .targetType("TRASH")
			                               .build();
		
		// when & then
		assertThatThrownBy(() -> mailService.deleteMails(deleteReq, receiver.getUsername()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("메일이 휴지통에 존재하지 않습니다.");
	}
	
	// ------------------------------------------------------------------------
	// Stub 구현체들 (MockBean 사용 안함)
	// ------------------------------------------------------------------------
	
	static class StubAttachmentFileService implements AttachmentFileService {
		
		@Override
		public List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFile, Long ownerTypeId, Long ownerId) {
			return Collections.emptyList();
		}
		
		@Override
		public List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId) {
			return Collections.emptyList();
		}
		
		@Override
		public ResponseEntity<Resource> downloadFile(Long fileId) {
			return null;
		}
		
		@Override
		public void deleteFile(Long fileId) {
			// no-op
		}
	}
	
	static class StubEmlService implements EmlService {
		@Override
		public String generate(Mail mail,
		                       List<String> to,
		                       List<String> cc,
		                       List<String> bcc,
		                       List<AttachmentFileResponseDTO> attachmentFiles,
		                       Employee sender) {
			Long id = mail.getMailId() == null ? 0L : mail.getMailId();
			return "mail-eml/test/" + id + ".eml";
		}
	}
	
	static class StubNotificationService implements NotificationService {
		
		private int createdCount = 0;
		
		int getCreatedCount() {
			return createdCount;
		}
		
		@Override
		public NotificationResponseDTO create(NotificationRequestDTO requestDTO) {
			createdCount++;
			return null;
		}
		
		@Override
		public Page<NotificationResponseDTO> list(String username, Pageable pageable) {
			return Page.empty(pageable);
		}
		
		@Override
		public String getUrl(Long notificationId, String username) {
			return null;
		}
		
		@Override
		public void softDelete(Long notificationId, String username) {
			// no-op
		}
		
		@Override
		public long getUnreadNotiCount(String username) {
			return 0L;
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
