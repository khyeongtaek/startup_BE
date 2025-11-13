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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
@Import({MailServiceImpl.class, MailServiceTests.TestConfig.class})
class MailServiceTests {
	
	@Autowired private MailService mailService;
	
	@Autowired private MailRepository mailRepository;
	@Autowired private MailboxRepository mailboxRepository;
	@Autowired private MailReceiverRepository mailReceiverRepository;
	@Autowired private EmployeeRepository employeeRepository;
	@Autowired private CommonCodeRepository commonCodeRepository;
	
	@Autowired private AttachmentFileService attachmentFileService;
	@Autowired private EmlService emlService;
	
	private Employee admin;
	private Employee dev1;
	private Employee dev2;
	
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private CommonCode ownerMail;
	private CommonCode rtTo, rtCc, rtBcc;
	private CommonCode mtInbox, mtSent, mtMybox, mtTrash;
	
	private static final String TEST_PW = "Pw1234!";
	
	@TestConfiguration
	static class TestConfig {
		
		@Bean
		AttachmentFileService attachmentFileService() {
			return new InMemoryAttachmentFileService();
		}
		
		@Bean
		EmlService emlService() {
			return (mail, to, cc, bcc, attachments, sender) ->
				       "mail-eml/2025/11/03/" + (mail.getMailId() == null ? 0L : mail.getMailId()) + ".eml";
		}
		
		static class InMemoryAttachmentFileService implements AttachmentFileService {
			private final Map<String, List<AttachmentFileResponseDTO>> store = new ConcurrentHashMap<>();
			
			private String key(Long ownerTypeId, Long ownerId) {
				return ownerTypeId + ":" + ownerId;
			}
			
			@Override
			public List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFiles, Long ownerTypeId, Long ownerId) {
				if (multipartFiles == null || multipartFiles.isEmpty()) {
					throw new IllegalArgumentException("업로드할 파일이 없습니다.");
				}
				List<AttachmentFileResponseDTO> curr =
					store.computeIfAbsent(key(ownerTypeId, ownerId), k -> new ArrayList<>());
				long baseId = curr.stream()
					              .map(AttachmentFileResponseDTO::getFileId)
					              .filter(Objects::nonNull)
					              .mapToLong(Long::longValue)
					              .max()
					              .orElse(100L);
				
				List<AttachmentFileResponseDTO> created = new ArrayList<>();
				for (int i = 0; i < multipartFiles.size(); i++) {
					long id = baseId + i + 1;
					AttachmentFileResponseDTO dto = AttachmentFileResponseDTO.builder()
						                                .fileId(id)
						                                .originalName("upload_" + id)
						                                .storagePath("files/upload_" + id)
						                                .mimeType("application/octet-stream")
						                                .build();
					curr.add(dto);
					created.add(dto);
				}
				return created;
			}
			
			@Override
			public List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId) {
				return new ArrayList<>(store.getOrDefault(key(ownerTypeId, ownerId), Collections.emptyList()));
			}
			
			@Override
			public ResponseEntity<Resource> downloadFile(Long fileId) {
				throw new UnsupportedOperationException("downloadFile는 이 테스트에서 사용하지 않습니다.");
			}
			
			@Override
			public void deleteFile(Long fileId) {
				for (List<AttachmentFileResponseDTO> list : store.values()) {
					list.removeIf(f -> Objects.equals(f.getFileId(), fileId));
				}
			}
		}
	}
	
	// ===== 유틸 메서드 =====
	
	private CommonCode cc(String code, String desc, String v1, String v2, String v3, long sort) {
		return CommonCode.createCommonCode(code, desc, v1, v2, v3, sort, null);
	}
	
	private Employee newEmp(String username, String name, String email,
	                        CommonCode role, CommonCode dept, CommonCode pos,
	                        Employee creator) {
		Employee e = Employee.createEmployee(
			username,
			name,
			email,
			"010-0000-0000",
			LocalDate.now(),
			statusActive,
			role,       // 권한
			dept,       // 부서
			pos,        // 직급
			creator
		);
		e.updateInitPassword(TEST_PW, creator);
		return e;
	}
	
	private void seedCommonCodes() {
		statusActive = cc("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L);
		roleAdmin    = cc("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L);
		roleUser     = cc("ROLE_USER", "사용자", "USER", null, null, 2L);
		deptDev      = cc("DEPT_DEV", "개발팀", "DEV", null, null, 1L);
		deptHr       = cc("DEPT_HR", "인사팀", "HR", null, null, 2L);
		posJunior    = cc("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L);
		posSenior    = cc("POS_SENIOR", "대리", "SENIOR", null, null, 2L);
		
		ownerMail = cc(OwnerType.PREFIX + "_MAIL", "OWNER_TYPE", OwnerType.MAIL.name(), null, null, 1L);
		
		rtTo  = cc(ReceiverType.PREFIX + "_TO",  "RECEIVER_TYPE", ReceiverType.TO.name(),  null, null, 1L);
		rtCc  = cc(ReceiverType.PREFIX + "_CC",  "RECEIVER_TYPE", ReceiverType.CC.name(),  null, null, 2L);
		rtBcc = cc(ReceiverType.PREFIX + "_BCC", "RECEIVER_TYPE", ReceiverType.BCC.name(), null, null, 3L);
		
		mtInbox = cc(MailboxType.PREFIX + "_INBOX", "MAILBOX_TYPE", MailboxType.INBOX.name(), null, null, 1L);
		mtSent  = cc(MailboxType.PREFIX + "_SENT",  "MAILBOX_TYPE", MailboxType.SENT.name(),  null, null, 2L);
		mtMybox = cc(MailboxType.PREFIX + "_MYBOX","MAILBOX_TYPE", MailboxType.MYBOX.name(), null, null, 3L);
		mtTrash = cc(MailboxType.PREFIX + "_TRASH","MAILBOX_TYPE", MailboxType.TRASH.name(), null, null, 4L);
		
		commonCodeRepository.saveAll(Arrays.asList(
			statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior,
			ownerMail, rtTo, rtCc, rtBcc, mtInbox, mtSent, mtMybox, mtTrash
		));
	}
	
	@BeforeEach
	void setUp() {
		mailboxRepository.deleteAll();
		mailReceiverRepository.deleteAll();
		mailRepository.deleteAll();
		employeeRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		seedCommonCodes();
		
		admin = newEmp("admin", "관리자", "admin@test.com", roleAdmin, deptHr, posSenior, null);
		employeeRepository.save(admin);
		
		dev1 = newEmp("dev1", "개발자1", "dev1@test.com", roleUser, deptDev, posJunior, admin);
		dev2 = newEmp("dev2", "개발자2", "dev2@test.com", roleUser, deptDev, posJunior, admin);
		employeeRepository.saveAll(List.of(dev1, dev2));
	}
	
	// ===== sendMail 테스트 =====
	
	@Test
	@DisplayName("sendMail: 정상 발송 - 수신자 정제/중복제거 + 보낸/받은 편지함 생성 + EML 경로 설정")
	void sendMail_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("회의 자료")
			                         .content("<p>내용</p>")
			                         .to(Arrays.asList("dev1@test.com", " DEV1@TEST.COM ", "dev2@test.com"))
			                         .cc(Collections.emptyList())
			                         .bcc(Collections.emptyList())
			                         .build();
		
		MailSendResponseDTO res = mailService.sendMail(dto, "admin", null);
		
		assertThat(res.getMailId()).isNotNull();
		assertThat(res.getTitle()).isEqualTo("회의 자료");
		assertThat(res.getToCount()).isEqualTo(2);
		assertThat(res.getCcCount()).isZero();
		assertThat(res.getBccCount()).isZero();
		assertThat(res.getAttachmentCount()).isZero();
		assertThat(res.getEmlPath()).isEqualTo("mail-eml/2025/11/03/" + res.getMailId() + ".eml");
		
		List<Mailbox> boxes = mailboxRepository.findAll();
		assertThat(boxes).hasSize(3); // 보낸함 1 + 받은함 2
		
		Optional<Mailbox> senderBox =
			mailboxRepository.findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), res.getMailId());
		assertThat(senderBox).isPresent();
		assertThat(senderBox.get().getTypeId().getValue1()).isEqualTo(MailboxType.SENT.name());
		
		Mail mail = mailRepository.findById(res.getMailId()).orElseThrow();
		CommonCode toCode = commonCodeRepository
			                    .findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name())
			                    .get(0);
		List<MailReceiver> toList = mailReceiverRepository.findAllByMailAndType(mail, toCode);
		assertThat(toList).hasSize(2);
	}
	
	@Test
	@DisplayName("sendMail: 첨부파일 업로드 시 attachmentCount 반영")
	void sendMail_withAttachments_success() {
		MultipartFile file1 = org.mockito.Mockito.mock(MultipartFile.class);
		MultipartFile file2 = org.mockito.Mockito.mock(MultipartFile.class);
		List<MultipartFile> files = List.of(file1, file2);
		
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("첨부 메일")
			                         .content("내용")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		
		MailSendResponseDTO res = mailService.sendMail(dto, "admin", files);
		
		assertThat(res.getAttachmentCount()).isEqualTo(2);
	}
	
	@Test
	@DisplayName("sendMail: 수신자(to/cc/bcc) 총합이 0이면 IllegalArgumentException")
	void sendMail_noRecipients_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("수신자 없음")
			                         .content("내용")
			                         .to(Collections.emptyList())
			                         .cc(Collections.emptyList())
			                         .bcc(Collections.emptyList())
			                         .build();
		
		assertThatThrownBy(() -> mailService.sendMail(dto, "admin", null))
			.isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	@DisplayName("sendMail: 사내에 존재하지 않는 이메일이 포함되면 ResourceNotFoundException")
	void sendMail_missingEmployeeEmail_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("미등록 수신자 포함")
			                         .content("내용")
			                         .to(Arrays.asList("dev1@test.com", "unknown@test.com"))
			                         .build();
		
		assertThatThrownBy(() -> mailService.sendMail(dto, "admin", null))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	@Test
	@DisplayName("sendMail: 존재하지 않는 발신자 username이면 ResourceNotFoundException")
	void sendMail_unknownSender_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .content("내용")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		
		assertThatThrownBy(() -> mailService.sendMail(dto, "no-user", null))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ===== getMailDetail 테스트 =====
	
	@Test
	@DisplayName("getMailDetail: 발신자가 조회하면 읽음 처리되고 BCC도 조회됨")
	void getMailDetail_asSender_marksRead_andShowsBcc() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목A")
			                         .content("본문A")
			                         .to(List.of("dev1@test.com"))
			                         .cc(List.of("dev2@test.com"))
			                         .bcc(List.of("dev2@test.com"))
			                         .build();
		
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		MailDetailResponseDTO detail = mailService.getMailDetail(sent.getMailId(), "admin", true);
		
		assertThat(detail.getMailId()).isEqualTo(sent.getMailId());
		assertThat(detail.getTitle()).isEqualTo("제목A");
		assertThat(detail.getSenderEmail()).isEqualTo("admin@test.com");
		assertThat(detail.getMailboxType()).isEqualTo(MailboxType.SENT.name());
		assertThat(detail.getIsRead()).isTrue();
		assertThat(detail.getBcc()).contains("dev2@test.com");
		
		Mailbox senderBox =
			mailboxRepository.findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
				.orElseThrow();
		assertThat(senderBox.getIsRead()).isTrue();
	}
	
	@Test
	@DisplayName("getMailDetail: 수신자는 BCC를 볼 수 없다")
	void getMailDetail_asReceiver_hidesBcc() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목B")
			                         .content("본문B")
			                         .to(List.of("dev1@test.com"))
			                         .cc(Collections.emptyList())
			                         .bcc(List.of("dev2@test.com"))
			                         .build();
		
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		MailDetailResponseDTO detail = mailService.getMailDetail(sent.getMailId(), "dev1", true);
		
		assertThat(detail.getMailboxType()).isEqualTo(MailboxType.INBOX.name());
		assertThat(detail.getIsRead()).isTrue();
		assertThat(detail.getBcc()).isEmpty();
	}
	
	@Test
	@DisplayName("getMailDetail: 존재하지 않는 사용자 이름으로 조회 시 ResourceNotFoundException")
	void getMailDetail_unknownUser_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		assertThatThrownBy(() -> mailService.getMailDetail(sent.getMailId(), "no-user", true))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	@Test
	@DisplayName("getMailDetail: 해당 메일함이 없는 사용자 조회 시 ResourceNotFoundException")
	void getMailDetail_noMailboxForUser_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		assertThatThrownBy(() -> mailService.getMailDetail(sent.getMailId(), "dev2", true))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	@Test
	@DisplayName("getMailDetail: deletedStatus=2(영구삭제) 메일 조회 시 ResourceNotFoundException")
	void getMailDetail_deletedMail_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		mailService.moveMails(MailMoveRequestDTO.builder()
			                      .mailIds(List.of(boxId))
			                      .targetType("TRASH")
			                      .build(),
			"admin");
		
		mailService.deleteMails(MailMoveRequestDTO.builder()
			                        .mailIds(List.of(boxId))
			                        .targetType("TRASH")
			                        .build(),
			"admin");
		
		assertThatThrownBy(() -> mailService.getMailDetail(sent.getMailId(), "admin", true))
			.isInstanceOf(ResourceNotFoundException.class);
	}
	
	// ===== moveMails 테스트 =====
	
	@Test
	@DisplayName("moveMails: MYBOX로 이동 → type=MYBOX, deletedStatus=0")
	void moveMails_toMybox_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		MailMoveRequestDTO move = MailMoveRequestDTO.builder()
			                          .mailIds(List.of(boxId))
			                          .targetType("MYBOX")
			                          .build();
		
		mailService.moveMails(move, "admin");
		
		Mailbox moved = mailboxRepository.findById(boxId).orElseThrow();
		assertThat(moved.getTypeId().getValue1()).isEqualTo(MailboxType.MYBOX.name());
		assertThat(moved.getDeletedStatus()).isEqualTo((byte) 0);
	}
	
	@Test
	@DisplayName("moveMails: TRASH로 이동 → type=TRASH, deletedStatus=1")
	void moveMails_toTrash_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		MailMoveRequestDTO move = MailMoveRequestDTO.builder()
			                          .mailIds(List.of(boxId))
			                          .targetType("TRASH")
			                          .build();
		
		mailService.moveMails(move, "admin");
		
		Mailbox moved = mailboxRepository.findById(boxId).orElseThrow();
		assertThat(moved.getTypeId().getValue1()).isEqualTo(MailboxType.TRASH.name());
		assertThat(moved.getDeletedStatus()).isEqualTo((byte) 1);
	}
	
	@Test
	@DisplayName("moveMails: 잘못된 타입 → IllegalArgumentException")
	void moveMails_invalidType_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		MailMoveRequestDTO move = MailMoveRequestDTO.builder()
			                          .mailIds(List.of(boxId))
			                          .targetType("UNKNOWN")
			                          .build();
		
		assertThatThrownBy(() -> mailService.moveMails(move, "admin"))
			.isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	@DisplayName("moveMails: 남의 메일함 이동 시도 → AccessDeniedException")
	void moveMails_wrongOwner_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long dev1InboxId = mailboxRepository.findAll().stream()
			                   .filter(mb -> Objects.equals(mb.getEmployee().getEmployeeId(), dev1.getEmployeeId()))
			                   .map(Mailbox::getBoxId)
			                   .findFirst()
			                   .orElseThrow();
		
		MailMoveRequestDTO move = MailMoveRequestDTO.builder()
			                          .mailIds(List.of(dev1InboxId))
			                          .targetType("MYBOX")
			                          .build();
		
		assertThatThrownBy(() -> mailService.moveMails(move, "dev2"))
			.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}
	
	// ===== deleteMails 테스트 =====
	
	@Test
	@DisplayName("deleteMails: TRASH에 있는 항목만 삭제(deletedStatus=2)")
	void deleteMails_fromTrash_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		mailService.moveMails(MailMoveRequestDTO.builder()
			                      .mailIds(List.of(boxId))
			                      .targetType("TRASH")
			                      .build(),
			"admin");
		
		mailService.deleteMails(MailMoveRequestDTO.builder()
			                        .mailIds(List.of(boxId))
			                        .targetType("TRASH")
			                        .build(),
			"admin");
		
		Mailbox mb = mailboxRepository.findById(boxId).orElseThrow();
		assertThat(mb.getDeletedStatus()).isEqualTo((byte) 2);
		assertThat(mb.getTypeId().getValue1()).isEqualTo(MailboxType.TRASH.name());
	}
	
	@Test
	@DisplayName("deleteMails: TRASH가 아니면 삭제 불가 → IllegalStateException")
	void deleteMails_notInTrash_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long boxId = mailboxRepository
			             .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId)
			             .orElseThrow();
		
		assertThatThrownBy(() -> mailService.deleteMails(MailMoveRequestDTO.builder()
			                                                 .mailIds(List.of(boxId))
			                                                 .targetType("TRASH")
			                                                 .build(), "admin"))
			.isInstanceOf(IllegalStateException.class);
	}
	
	@Test
	@DisplayName("deleteMails: 남의 메일함 삭제 시도 → AccessDeniedException")
	void deleteMails_wrongOwner_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long dev1InboxId = mailboxRepository.findAll().stream()
			                   .filter(mb -> Objects.equals(mb.getEmployee().getEmployeeId(), dev1.getEmployeeId()))
			                   .map(Mailbox::getBoxId)
			                   .findFirst()
			                   .orElseThrow();
		
		MailMoveRequestDTO req = MailMoveRequestDTO.builder()
			                         .mailIds(List.of(dev1InboxId))
			                         .targetType("TRASH")
			                         .build();
		
		assertThatThrownBy(() -> mailService.deleteMails(req, "dev2"))
			.isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
	}
	
	// ===== getMailboxList 테스트 =====
	
	@Test
	@DisplayName("getMailboxList: INBOX / SENT / MYBOX 필터링 동작 확인")
	void getMailboxList_basicFolders() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Page<MailboxListDTO> inbox = mailService.getMailboxList("dev1", "INBOX", 0, 10);
		assertThat(inbox.getTotalElements()).isEqualTo(1);
		MailboxListDTO inboxItem = inbox.getContent().get(0);
		assertThat(inboxItem.getSenderName()).isEqualTo("관리자");
		assertThat(inboxItem.getTitle()).isEqualTo("제목");
		assertThat(inboxItem.getReceivers()).contains("dev1@test.com");
		
		Page<MailboxListDTO> sentPage = mailService.getMailboxList("admin", "SENT", 0, 10);
		assertThat(sentPage.getTotalElements()).isEqualTo(1);
		MailboxListDTO sentItem = sentPage.getContent().get(0);
		assertThat(sentItem.getSenderName()).isEqualTo("관리자");
		assertThat(sentItem.getReceivers()).contains("dev1@test.com");
		
		Long adminBoxId = mailboxRepository
			                  .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			                  .map(Mailbox::getBoxId)
			                  .orElseThrow();
		
		mailService.moveMails(MailMoveRequestDTO.builder()
			                      .mailIds(List.of(adminBoxId))
			                      .targetType("MYBOX")
			                      .build(),
			"admin");
		
		Page<MailboxListDTO> myboxPage = mailService.getMailboxList("admin", "MYBOX", 0, 10);
		assertThat(myboxPage.getTotalElements()).isEqualTo(1);
		assertThat(myboxPage.getContent().get(0).getBoxId()).isEqualTo(adminBoxId);
	}
	
	@Test
	@DisplayName("getMailboxList: TRASH 조회시 deletedStatus=1만 조회, 영구삭제 후에는 제외")
	void getMailboxList_trash_before_and_after_delete() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin", null);
		
		Long adminBoxId = mailboxRepository
			                  .findFirstByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			                  .map(Mailbox::getBoxId)
			                  .orElseThrow();
		
		mailService.moveMails(MailMoveRequestDTO.builder()
			                      .mailIds(List.of(adminBoxId))
			                      .targetType("TRASH")
			                      .build(),
			"admin");
		
		Page<MailboxListDTO> trashBefore = mailService.getMailboxList("admin", "TRASH", 0, 10);
		assertThat(trashBefore.getContent())
			.extracting(MailboxListDTO::getBoxId)
			.contains(adminBoxId);
		
		mailService.deleteMails(MailMoveRequestDTO.builder()
			                        .mailIds(List.of(adminBoxId))
			                        .targetType("TRASH")
			                        .build(),
			"admin");
		
		Page<MailboxListDTO> trashAfter = mailService.getMailboxList("admin", "TRASH", 0, 10);
		assertThat(trashAfter.getContent())
			.extracting(MailboxListDTO::getBoxId)
			.doesNotContain(adminBoxId);
	}
}
