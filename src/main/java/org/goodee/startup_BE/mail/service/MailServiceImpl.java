package org.goodee.startup_BE.mail.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class MailServiceImpl implements MailService{
	private final MailRepository mailRepository;
	private final MailReceiverRepository mailReceiverRepository;
	private final MailboxRepository mailboxRepository;
	private final EmployeeRepository employeeRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final AttachmentFileService attachmentFileService;
	private final EmlService emlService;
	private final NotificationService notificationService;
	
	
	// 메일 수신함 insert 메소드 및 count 반환 - 메일 작성
	private int insertReceivers(Mail mail, List<String> emails, CommonCode typeCode) {
		if (emails == null || emails.isEmpty()) return 0;
		
		Set<String> seen = new HashSet<>(); // lower-case 키로 중복 제거
		List<MailReceiver> batch = new ArrayList<>();
		
		for (String raw : emails) {
			if (raw == null) continue;
			String trimmed = raw.trim();
			if (trimmed.isEmpty()) continue;
			
			String key = trimmed.toLowerCase();  // 비교용
			if (seen.add(key)) {
				// 저장은 원본 그대로
				batch.add(MailReceiver.createMailReceiver(mail, trimmed, typeCode));
			}
		}
		
		if (!batch.isEmpty()) mailReceiverRepository.saveAll(batch);
		return batch.size();
	}
	
	// 이메일 1개를 정제해서 Set에 추가 (null/공백 제거, trim, 소문자 통일) - 메일 작성
	private void addSanitized(Set<String> acc, String raw) {
		if(raw == null) return;
		String sanitized = raw.trim();
		if(sanitized.isEmpty()) return;
		acc.add(sanitized.toLowerCase());
	}
	
	// 메일함 리스트 전제를 정제해서 Set에 추가 - 메일 작성
	private void addAllSanitized(Set<String> acc, List<String> src) {
		if(src == null) return;
		for(String raw : src) {
			addSanitized(acc, raw);
		}
	}
	
	// 리스트 첫 요소 or 예외 - 메일 수정
	private CommonCode firstOrNotFound(List<CommonCode> list, String msg) {
		if (list == null || list.isEmpty()) throw new ResourceNotFoundException(msg);
		return list.get(0);
	}
	
	// 수신자 이메일 -> 수신자 이름으로 가져오기 - 메일 리스트 조회
	private List<String> resolveReceiverNames(Mail mail, CommonCode toCode) {
		// 1) 이 메일의 TO 수신자 이메일들
		List<String> receiverEmails = mailReceiverRepository
			                              .findAllByMailAndType(mail, toCode)
			                              .stream()
			                              .map(MailReceiver::getEmail)
			                              .filter(Objects::nonNull)
			                              .map(String::trim)
			                              .filter(s -> !s.isEmpty())
			                              .toList();
		
		if (receiverEmails.isEmpty()) {
			return Collections.emptyList();
		}
		
		// 2) 이메일로 직원 목록 조회
		List<Employee> receiverEmployees = employeeRepository.findAllByEmailIn(receiverEmails);
		
		// 3) email(lowercase) -> 이름 매핑
		Map<String, String> nameByEmail = receiverEmployees.stream()
			                                  .filter(e -> e.getEmail() != null)
			                                  .collect(Collectors.toMap(
				                                  e -> e.getEmail().trim().toLowerCase(),
				                                  Employee::getName,
				                                  (a, b) -> a
			                                  ));
		
		// 4) 이메일 순서 유지하면서 이름 리스트 생성
		return receiverEmails.stream()
			       .map(email -> {
				       String key = email.trim().toLowerCase();
				       return nameByEmail.getOrDefault(key, email); // 이름 없으면 이메일 그대로
			       })
			       .toList();
	}
	
	// MailReceiver → "email (이름)" 리스트 - 메일 상세
	private List<String> mapEmailWithName(Mail mail, CommonCode typeCode) {
		// 1) 해당 타입(TO/CC/BCC) 수신자 이메일 목록
		List<String> receiverEmails = mailReceiverRepository
			                              .findAllByMailAndType(mail, typeCode)
			                              .stream()
			                              .map(MailReceiver::getEmail)
			                              .filter(Objects::nonNull)
			                              .map(String::trim)
			                              .filter(s -> !s.isEmpty())
			                              .toList();
		
		if (receiverEmails.isEmpty()) {
			return Collections.emptyList();
		}
		
		// 2) 이메일로 직원 목록 조회
		List<Employee> receiverEmployees = employeeRepository.findAllByEmailIn(receiverEmails);
		
		// 3) email(lowercase) -> name 매핑
		Map<String, String> nameByEmail = receiverEmployees.stream()
			                                  .filter(e -> e.getEmail() != null)
			                                  .collect(Collectors.toMap(
				                                  e -> e.getEmail().trim().toLowerCase(),
				                                  Employee::getName,
				                                  (a, b) -> a
			                                  ));
		
		// 4) "email (이름)" 형식으로 변환 (이름 없으면 email만)
		return receiverEmails.stream()
			       .map(email -> {
				       String key = email.trim().toLowerCase();
				       String name = nameByEmail.get(key);
				       if (name == null || name.isBlank()) {
					       return email;          // 매칭되는 직원 없으면 이메일만
				       }
				       return email + " (" + name + ")";
			       })
			       .toList();
	}
	
	
	// 메일 작성
	@Override
	public MailSendResponseDTO sendMail(MailSendRequestDTO mailSendRequestDTO , String username, List<MultipartFile> multipartFile) {
		// 0. 직원 조회
		Employee employee = employeeRepository.findByUsername(username)
			                    .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다"));
		
		CommonCode ownerTypeCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.MAIL.name()),"분류 타입 코드 없음");

		
		// 1. 메일 insert
		Mail mail = mailRepository.save(mailSendRequestDTO.toEntity(employee, LocalDateTime.now()));
		
		
		// 2. 수신자 메일함 insert
		CommonCode toCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name()),"TO 코드 없음");
		CommonCode ccCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.CC.name()),"CC 코드 없음");
		CommonCode bccCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.BCC.name()),"BCC 코드 없음");
		
		int toCount = insertReceivers(mail, mailSendRequestDTO.getTo(), toCode);
		int ccCount = insertReceivers(mail, mailSendRequestDTO.getCc(), ccCode);
		int bccCount = insertReceivers(mail, mailSendRequestDTO.getBcc(), bccCode);
		
		if(toCount + ccCount + bccCount == 0) {
			throw new IllegalArgumentException("수신자(to/cc/bcc) 중 최소 1명은 필요합니다.");
		}
		
		
		// 3. 발신자 보낸편지함 생성 (보낸편지함은 항상 읽음 처리 false - UI read 클래스 추가 x)
		CommonCode inboxCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(MailboxType.PREFIX, MailboxType.INBOX.name()),"INBOX 코드 없음");
		CommonCode sentCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(MailboxType.PREFIX, MailboxType.SENT.name()),"SENT 코드 없음");
		
		Mailbox sentBox = mailboxRepository.save(Mailbox.createMailbox(employee, mail, sentCode, false, (byte) 0));
		
		
		// 4. 수신자 받은편지함 생성
		Set<String> allEmails = new LinkedHashSet<>();
		addAllSanitized(allEmails, mailSendRequestDTO.getTo());
		addAllSanitized(allEmails, mailSendRequestDTO.getCc());
		addAllSanitized(allEmails, mailSendRequestDTO.getBcc());
		
		// 이메일로 직원 정보 조회
		List<Employee> receivers = employeeRepository.findAllByEmailIn(allEmails);
		
		// 미등록 이메일 체크
		Map<String, Employee> byEmail = new HashMap<>();
		for(Employee receiver : receivers) {
			if (receiver.getEmail() == null) continue;
			byEmail.put(receiver.getEmail().trim().toLowerCase(), receiver);
		}
		
		List<String> missing = new ArrayList<>();
		for(String email : allEmails) {
			if(!byEmail.containsKey(email)) {
				missing.add(email);
			}
		}
		
		if(!missing.isEmpty()) {
			throw new ResourceNotFoundException("사내 미등록 이멜 : " + String.join(", ", missing));
		}
		
		// 받은메일함 생성
		for(String email : allEmails) {
			Employee receiver = byEmail.get(email.trim().toLowerCase());
			mailboxRepository.save(Mailbox.createMailbox(receiver, mail, inboxCode, false, (byte) 0));
		}
		
		
		// 5. 파일첨부 업로드
		List<AttachmentFileResponseDTO> uploadFiles = Collections.emptyList();
		if(multipartFile != null && !multipartFile.isEmpty()) {
			uploadFiles = attachmentFileService.uploadFiles(multipartFile, ownerTypeCode.getCommonCodeId(), mail.getMailId());
		}
		
		
		// 6. EML 생성
		String emlPath = emlService.generate(mail, mailSendRequestDTO.getTo(), mailSendRequestDTO.getCc(), mailSendRequestDTO.getBcc(), uploadFiles, employee);
		mail.updateEmlPath(emlPath);
		
		
		// 7. 알림 서비스
		// 수신 타입별 이메일 리스트
		Set<String> toEmails = new LinkedHashSet<>();
		Set<String> ccEmails = new LinkedHashSet<>();
		Set<String> bccEmails = new LinkedHashSet<>();
		
		addAllSanitized(toEmails, mailSendRequestDTO.getTo());
		addAllSanitized(ccEmails, mailSendRequestDTO.getCc());
		addAllSanitized(bccEmails, mailSendRequestDTO.getBcc());
		
		// 수신 타입별 employeeId 리스트 생성
		List<Long> toEmployeeIds = toEmails.stream()
			                           .map(e -> byEmail.get(e).getEmployeeId())
			                           .toList();
		List<Long> ccEmployeeIds = ccEmails.stream()
			                           .map(e -> byEmail.get(e).getEmployeeId())
			                           .toList();
		List<Long> bccEmployeeIds = bccEmails.stream()
			                            .map(e -> byEmail.get(e).getEmployeeId())
			                            .toList();
		
		// 전체 알림 받을 대상자 리스트
		List<Long> receiverIds = Stream.of(toEmployeeIds, ccEmployeeIds, bccEmployeeIds)
				.flatMap(Collection::stream)
				.distinct()
				.toList();
		
		// 알림 요청 반복 호출
		for (Long empId : receiverIds) {
			NotificationRequestDTO dto = NotificationRequestDTO.builder()
				                             .employeeId(empId)
				                             .ownerTypeCommonCodeId(ownerTypeCode.getCommonCodeId())
				                             .url("/mail/detail/" + mail.getMailId())
				                             .title("새로운 메일이 도착했습니다.")
				                             .content(mail.getTitle())
				                             .build();
			notificationService.create(dto);
		}
		
		
		return MailSendResponseDTO.toDTO(mail, toCount, ccCount, bccCount, uploadFiles == null ? 0 : uploadFiles.size());
	}
	
	// 메일 상세 조회 및 읽음 처리
	@Override
	public MailDetailResponseDTO getMailDetail(Long mailId, String username, boolean isRead) {
		// 직원 정보, 메일함, 메일 정보 가져오기
		Employee employee = employeeRepository.findByUsername(username)
			                    .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
		Mailbox mailbox = mailboxRepository.findFirstByEmployeeEmployeeIdAndMailMailId(employee.getEmployeeId(), mailId)
			                  .orElseThrow(() -> new ResourceNotFoundException("해당 메일을 조회할 권한이 없습니다."));
		Mail mail = mailbox.getMail();
		
		// 삭제된 메일 조회 X (조회는 안되지만 url 조작)
		if(mailbox.getDeletedStatus() != null && mailbox.getDeletedStatus() == 2) {
			throw new ResourceNotFoundException("삭제된 메일입니다.");
		}
		
		// 읽음 처리
		if(isRead && (mailbox.getIsRead() == null || !mailbox.getIsRead())) {
			mailbox.markAsRead();
		}
		
		// 수신자 목록 조회
		CommonCode toCode = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name()), "TO 코드가 존재하지 않습니다."
		);
		CommonCode ccCode = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.CC.name()), "CC 코드가 존재하지 않습니다."
		);
		CommonCode bccCode = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.BCC.name()), "BCC 코드가 존재하지 않습니다."
		);
		
		List<String> toList = mapEmailWithName(mail, toCode);
		List<String> ccList = mapEmailWithName(mail, ccCode);
		List<String> bccList = null;
		
		boolean ISender = mail.getEmployee() != null && mail.getEmployee().getEmployeeId().equals(employee.getEmployeeId());
		if (ISender) {
			// 숨은참조는 작성자만 볼 수 있음
			bccList = mapEmailWithName(mail, bccCode);
		}
		
		// 첨부파일 조회
		CommonCode ownerType = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.MAIL.name()), "분류 코드가 존재하지 않습니다."
		);
		
		List<AttachmentFileResponseDTO> files = attachmentFileService.listFiles(ownerType.getCommonCodeId(), mail.getMailId());
		
		return MailDetailResponseDTO.toDTO(mail, toList, ccList, bccList, mailbox, files);
	}
	
	
	// 메일 이동
	@Override
	public void moveMails(MailMoveRequestDTO requestDTO, String username) {
		List<Mailbox> mailboxes = mailboxRepository.findAllByBoxIdInAndEmployeeUsername(requestDTO.getMailIds(), username);
		
		if(mailboxes.size() != requestDTO.getMailIds().size()) {
			throw new AccessDeniedException("권한이 없거나 존재하지 않는 항목이 포함되어 있습니다.");
		}
		
		CommonCode targetType;
		
		switch(requestDTO.getTargetType().toUpperCase()) {
			case "MYBOX" :
				targetType = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(MailboxType.PREFIX, MailboxType.MYBOX.name()), "분류 타입 코드 없음");
				break;
			case "TRASH" :
				targetType = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(MailboxType.PREFIX, MailboxType.TRASH.name()), "분류 타입 코드 없음");
				break;
			default :
				throw new IllegalArgumentException("해당 타입은 존재하지 않습니다.");
		}
		
		mailboxes.forEach(mail -> mail.moveMail(targetType));
	}
	
	
	// 메일 삭제
	@Override
	public void deleteMails(MailMoveRequestDTO requestDTO, String username) {
		List<Mailbox> mailboxes = mailboxRepository.findAllByBoxIdInAndEmployeeUsername(requestDTO.getMailIds(), username);
		
		if(mailboxes.size() != requestDTO.getMailIds().size()) {
			throw new AccessDeniedException("권한이 없거나 존재하지 않는 항목이 포함되어 있습니다.");
		}
		
		boolean checkInTrash = mailboxes.stream().allMatch(mail -> "TRASH".equals(mail.getTypeId().getValue1()));
		if(!checkInTrash) {
			throw new IllegalStateException("메일이 휴지통에 존재하지 않습니다.");
		}
		
		mailboxes.forEach(mail -> mail.deleteFromTrash());
	}
	
	
	// 메일함 리스트 조회
	@Override
	@Transactional(readOnly = true)
	public Page<MailboxListDTO> getMailboxList(String username, String boxType, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "mail.sendAt"));
		String type = boxType.toUpperCase();
		byte deleted = (byte) ("TRASH".equals(type) ? 1 : 0);
		
		Page<Mailbox> mailboxList = mailboxRepository
			                            .findByEmployeeUsernameAndTypeIdValue1AndDeletedStatus(
				                            username, boxType.toUpperCase(), deleted, pageable);
		
		CommonCode toCode = commonCodeRepository
			                    .findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name())
			                    .get(0);
		
		return mailboxList.map(mb -> {
			Mail mail = mb.getMail();
			
			List<String> receiverNames = resolveReceiverNames(mail, toCode);
			
			return MailboxListDTO.builder()
				       .boxId(mb.getBoxId())
				       .mailId(mail.getMailId())
				       .senderName(mail.getEmployee().getName())
				       .title(mail.getTitle())
				       .receivedAt(mail.getSendAt())
				       .isRead(Boolean.TRUE.equals(mb.getIsRead()))
				       .receivers(receiverNames)   // 🔹 이제 이름 리스트
				       .build();
		});
	}
}
