package org.goodee.startup_BE.mail.service;

import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

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
	
	@Autowired private AttachmentFileService attachmentFileService; // In-memory bean
	@Autowired private EmlService emlService;                       // In-memory bean
	
	private Employee admin;
	private Employee dev1;
	private Employee dev2;
	
	private CommonCode statusActive, roleAdmin, roleUser, deptDev, deptHr, posJunior, posSenior;
	private CommonCode ownerMail = cc("OT_MAIL", "OWNER_TYPE", "MAIL", null, null, 1L);
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
			// mailId 기반으로 고정 경로 생성(테스트에서 검증 용이)
			return (mail, to, cc, bcc, attachments, sender) ->
				       "mail-eml/2025/11/03/" + (mail.getMailId() == null ? 0L : mail.getMailId()) + ".eml";
		}
	}
	
	// 간단한 인메모리 구현 (업로드/목록/삭제만 사용)
	static class InMemoryAttachmentFileService implements AttachmentFileService {
		private final Map<String, List<AttachmentFileResponseDTO>> store = new ConcurrentHashMap<>();
		private String key(Long ownerTypeId, Long ownerId) { return ownerTypeId + ":" + ownerId; }
		
		// 테스트에서 초기 파일 주입용
		void seedFiles(Long ownerTypeId, Long ownerId, List<AttachmentFileResponseDTO> files) {
			store.put(key(ownerTypeId, ownerId), new ArrayList<>(files));
		}
		
		@Override
		public List<AttachmentFileResponseDTO> uploadFiles(AttachmentFileRequestDTO request, Long ownerTypeId, Long ownerId) {
			if (request == null || request.getFiles() == null || request.getFiles().isEmpty()) return Collections.emptyList();
			List<AttachmentFileResponseDTO> curr = store.computeIfAbsent(key(ownerTypeId, ownerId), k -> new ArrayList<>());
			long baseId = curr.stream().map(AttachmentFileResponseDTO::getFileId).filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(100L);
			List<AttachmentFileResponseDTO> created = new ArrayList<>();
			for (int i = 0; i < request.getFiles().size(); i++) {
				long id = baseId + i + 1;
				AttachmentFileResponseDTO dto = AttachmentFileResponseDTO.builder()
					                                .fileId(id)
					                                .originalName("upload_" + id)
					                                .storagePath("files/upload_" + id)
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
		public AttachmentFileResponseDTO resolveFile(AttachmentFileRequestDTO request) {
			throw new UnsupportedOperationException("not used in this test");
		}
		
		@Override
		public void deleteFile(AttachmentFileRequestDTO request) {
			if (request == null || request.getFileId() == null) return;
			for (List<AttachmentFileResponseDTO> list : store.values()) {
				list.removeIf(f -> request.getFileId().equals(f.getFileId()));
			}
		}
	}
	
	// ====== 유틸 ======
	private CommonCode cc(String code, String desc, String v1, String v2, String v3, long sort) {
		return CommonCode.createCommonCode(code, desc, v1, v2, v3, sort, null);
	}
	
	private Employee newEmp(String username, String name, String email, CommonCode role, CommonCode dept, CommonCode pos, Employee creator) {
		Employee e = Employee.createEmployee(
			username, name, email, "010-0000-0000",
			LocalDate.now(), statusActive, "default.png", role, dept, pos, creator
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
	
	// ====== 테스트 케이스 ======
	
	@Test
	@DisplayName("sendMail: 정상 발송 - 수신자 정제/중복제거 + 보낸/받은 편지함 생성 + EML 경로 설정")
	void sendMail_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("회의 자료")
			                         .content("<p>내용</p>")
			                         .to(Arrays.asList("dev1@test.com", " DEV1@TEST.COM ", "dev2@test.com"))
			                         .cc(Collections.emptyList())
			                         .bcc(Collections.emptyList())
			                         .attachmentFiles(Collections.emptyList())
			                         .build();
		
		MailSendResponseDTO res = mailService.sendMail(dto, "admin");
		
		assertThat(res.getMailId()).isNotNull();
		assertThat(res.getTitle()).isEqualTo("회의 자료");
		assertThat(res.getToCount()).isEqualTo(2);
		assertThat(res.getCcCount()).isZero();
		assertThat(res.getBccCount()).isZero();
		assertThat(res.getEmlPath()).isEqualTo("mail-eml/2025/11/03/" + res.getMailId() + ".eml");
		
		List<Mailbox> boxes = mailboxRepository.findAll();
		assertThat(boxes).hasSize(3);
		
		Optional<Mailbox> senderBox = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), res.getMailId());
		assertThat(senderBox).isPresent();
		assertThat(senderBox.get().getTypeId().getValue1()).isEqualTo(MailboxType.SENT.name());
		
		Mail mail = mailRepository.findById(res.getMailId()).orElseThrow();
		CommonCode toCode = commonCodeRepository
			                    .findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name()).get(0);
		List<MailReceiver> toList = mailReceiverRepository.findAllByMailAndType(mail, toCode);
		assertThat(toList).hasSize(2);
	}
	
	@Test
	@DisplayName("sendMail: 수신자 비어있으면 IllegalArgumentException")
	void sendMail_noRecipients_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("수신자 없음")
			                         .content("내용")
			                         .to(Collections.emptyList()) // cc, bcc 생략 -> 총합 0명
			                         .build();
		
		assertThatThrownBy(() -> mailService.sendMail(dto, "admin"))
			.isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	@DisplayName("sendMail: 사내 미등록 이메일 포함 -> ResourceNotFoundException")
	void sendMail_missingEmployeeEmail_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("미등록 수신자 포함")
			                         .content("내용")
			                         .to(Arrays.asList("dev1@test.com", "unknown@test.com"))
			                         .build();
		
		assertThatThrownBy(() -> mailService.sendMail(dto, "admin"))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("unknown@test.com");
	}
	
	@Test
	@DisplayName("updateMail: 제목/본문/수신자/첨부 변경 + EML 재생성")
	void updateMail_success() {
		// given: 먼저 메일 생성
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("원본 제목")
			                         .content("원본 본문")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		// 실제 서비스 내부 조회 기준으로 ownerType을 가져오기
		CommonCode realOwnerType = commonCodeRepository
			                           .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.MAIL.name())
			                           .get(0);
		
		InMemoryAttachmentFileService mem = (InMemoryAttachmentFileService) attachmentFileService;
		mem.seedFiles(
			realOwnerType.getCommonCodeId(), sent.getMailId(),
			List.of(
				AttachmentFileResponseDTO.builder().fileId(101L).originalName("a.pdf").storagePath("files/a.pdf").build(),
				AttachmentFileResponseDTO.builder().fileId(102L).originalName("b.png").storagePath("files/b.png").build()
			)
		);
		
		// when: 수정 호출 (to 교체, cc 추가, 첨부 101 삭제)
		MailUpdateRequestDTO upd = MailUpdateRequestDTO.builder()
			                           .title("수정된 제목")
			                           .content("수정된 본문")
			                           .to(Arrays.asList("dev2@test.com", "DEV2@TEST.COM")) // dev2로 교체(중복 제거)
			                           .cc(List.of("dev1@test.com"))
			                           .bcc(Collections.emptyList())
			                           .deleteAttachmentFileIds(List.of(101L))              // 101 삭제 지시
			                           .attachmentFiles(Collections.emptyList())            // 업로드 없음
			                           .build();
		
		MailSendResponseDTO res = mailService.updateMail(sent.getMailId(), upd, "admin");
		
		// then
		assertThat(res.getMailId()).isEqualTo(sent.getMailId());
		assertThat(res.getTitle()).isEqualTo("수정된 제목");
		assertThat(res.getToCount()).isEqualTo(1);   // dev2만 남음
		assertThat(res.getCcCount()).isEqualTo(1);
		assertThat(res.getBccCount()).isEqualTo(0);
		
		// 삭제 이후 listFiles 기준 → 102만 남아 1개여야 함
		assertThat(res.getAttachmentCount()).isEqualTo(1);
		assertThat(res.getEmlPath()).isEqualTo("mail-eml/2025/11/03/" + res.getMailId() + ".eml");
		
		// 실제 인메모리 스토어에서도 1개만 남는지 확인 (102만 존재)
		List<AttachmentFileResponseDTO> latest =
			attachmentFileService.listFiles(realOwnerType.getCommonCodeId(), sent.getMailId());
		assertThat(latest).hasSize(1);
		assertThat(latest).extracting(AttachmentFileResponseDTO::getFileId).containsExactly(102L);
		
		// 수신자 실제 반영 확인
		Mail mail = mailRepository.findById(res.getMailId()).orElseThrow();
		CommonCode toCode = commonCodeRepository
			                    .findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name())
			                    .get(0);
		List<MailReceiver> toList = mailReceiverRepository.findAllByMailAndType(mail, toCode);
		assertThat(toList).extracting(MailReceiver::getEmail).containsExactly("dev2@test.com");
	}
	
	@Test
	@DisplayName("updateMail: 작성자가 아니면 AccessDeniedException")
	void updateMail_wrongUser_throws() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("원본")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		MailUpdateRequestDTO upd = MailUpdateRequestDTO.builder().title("변경").build();
		assertThatThrownBy(() -> mailService.updateMail(sent.getMailId(), upd, "dev1"))
			.isInstanceOf(AccessDeniedException.class);
	}
	
	@Test
	@DisplayName("getMailDetail: (보낸사람으로 조회) 읽음표시 적용 + 상세 DTO 매핑")
	void getMailDetail_asSender_marksRead_and_maps() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목A")
			                         .content("본문A")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		MailDetailResponseDTO detail = mailService.getMailDetail(sent.getMailId(), "admin", true);
		
		assertThat(detail.getMailId()).isEqualTo(sent.getMailId());
		assertThat(detail.getTitle()).isEqualTo("제목A");
		assertThat(detail.getSenderEmail()).isEqualTo("admin@test.com");
		assertThat(detail.getMailboxType()).isEqualTo(MailboxType.SENT.name());
		assertThat(detail.getIsRead()).isTrue();
		
		Mailbox senderBox = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId()).orElseThrow();
		assertThat(senderBox.getIsRead()).isTrue();
	}
	
	@Test
	@DisplayName("moveMails: MYBOX로 이동 → type=MYBOX, deletedStatus=0")
	void moveMails_toMybox_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
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
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
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
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
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
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long dev1InboxId = mailboxRepository.findAll().stream()
			                   .filter(mb -> Objects.equals(mb.getEmployee().getEmployeeId(), dev1.getEmployeeId()))
			                   .map(Mailbox::getBoxId)
			                   .findFirst().orElseThrow();
		
		MailMoveRequestDTO move = MailMoveRequestDTO.builder()
			                          .mailIds(List.of(dev1InboxId))
			                          .targetType("MYBOX")
			                          .build();
		
		assertThatThrownBy(() -> mailService.moveMails(move, "dev2"))
			.isInstanceOf(AccessDeniedException.class);
	}
	
	@Test
	@DisplayName("deleteMails: TRASH에 있는 항목만 삭제(deletedStatus=2)")
	void deleteMails_fromTrash_success() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
		mailService.moveMails(MailMoveRequestDTO.builder().mailIds(List.of(boxId)).targetType("TRASH").build(), "admin");
		mailService.deleteMails(MailMoveRequestDTO.builder().mailIds(List.of(boxId)).build(), "admin");
		
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
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
		assertThatThrownBy(() -> mailService.deleteMails(MailMoveRequestDTO.builder().mailIds(List.of(boxId)).build(), "admin"))
			.isInstanceOf(IllegalStateException.class);
	}
	
	@Test
	@DisplayName("getMailboxList: TRASH 조회 (서비스 로직: deletedStatus != 1)")
	void getMailboxList_trash_afterDelete() {
		MailSendRequestDTO dto = MailSendRequestDTO.builder()
			                         .title("제목")
			                         .to(List.of("dev1@test.com"))
			                         .build();
		MailSendResponseDTO sent = mailService.sendMail(dto, "admin");
		
		Long boxId = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(admin.getEmployeeId(), sent.getMailId())
			             .map(Mailbox::getBoxId).orElseThrow();
		
		// TRASH로 이동 후 영구삭제(deletedStatus = 2)
		mailService.moveMails(MailMoveRequestDTO.builder().mailIds(List.of(boxId)).targetType("TRASH").build(), "admin");
		mailService.deleteMails(MailMoveRequestDTO.builder().mailIds(List.of(boxId)).build(), "admin");
		
		Page<MailboxListDTO> page = mailService.getMailboxList("admin", "TRASH", 0, 10);
		
		assertThat(page.getContent()).extracting(MailboxListDTO::getBoxId).contains(boxId);
		assertThat(page.getContent()).allMatch(it -> it.getIsRead() != null);
	}
}
