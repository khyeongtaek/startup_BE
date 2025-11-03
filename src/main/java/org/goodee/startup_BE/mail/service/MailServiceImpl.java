package org.goodee.startup_BE.mail.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
	
	// 수신자 변경 적용: null=변경없음, []=전부 제거, 값 있으면 교체 - 메일 수정
	private void receiverChange(Mail mail, CommonCode typeCode, List<MailReceiver> currentList, List<String> newList) {
		// 현재 집합(소문자)
		Set<String> curr = new LinkedHashSet<>();
		for (MailReceiver r : currentList) {
			if (r == null || r.getEmail() == null) continue;
			String s = r.getEmail().trim().toLowerCase();
			if (!s.isEmpty()) curr.add(s);
		}
		
		// 변경 없음
		if (newList == null) return;
		
		// 목표 집합(소문자)
		Set<String> next = new LinkedHashSet<>();
		for (String raw : newList) {
			if (raw == null) continue;
			String s = raw.trim().toLowerCase();
			if (!s.isEmpty()) next.add(s);
		}
		
		// 제거 대상 = curr - next
		Set<String> toRemove = new LinkedHashSet<>(curr);
		toRemove.removeAll(next);
		
		// 추가 대상 = next - curr
		Set<String> toAdd = new LinkedHashSet<>(next);
		toAdd.removeAll(curr);
		
		// 제거 실행
		if (!toRemove.isEmpty()) {
			List<MailReceiver> del = new ArrayList<>();
			for (MailReceiver r : currentList) {
				String key = r.getEmail() == null ? "" : r.getEmail().trim().toLowerCase();
				if (toRemove.contains(key)) del.add(r);
			}
			if (!del.isEmpty()) mailReceiverRepository.deleteAll(del);
		}
		
		// 추가 실행 (원본 케이스 유지 위해 newList 원소 사용)
		if (!toAdd.isEmpty()) {
			List<MailReceiver> ins = new ArrayList<>();
			for (String raw : newList) {
				if (raw == null) continue;
				String key = raw.trim().toLowerCase();
				if (!key.isEmpty() && toAdd.contains(key)) {
					ins.add(MailReceiver.createMailReceiver(mail, raw.trim(), typeCode));
				}
			}
			if (!ins.isEmpty()) mailReceiverRepository.saveAll(ins);
		}
	}
	
	// MailReceiver → email 리스트 - 메일 수정
	private List<String> mapEmails(List<MailReceiver> list) {
		List<String> out = new ArrayList<>();
		for (MailReceiver r : list) {
			if (r == null || r.getEmail() == null) continue;
			String s = r.getEmail().trim();
			if (!s.isEmpty()) out.add(s);
		}
		return out;
	}
	
	
	// 메일 작성
	@Override
	public MailSendResponseDTO sendMail(MailSendRequestDTO mailSendRequestDTO , String username) {
		// 0. 직원 조회
		Employee employee = employeeRepository.findByUsername(username)
			                    .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다"));
		
		CommonCode ownerTypeCode = firstOrNotFound(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.MAIL.name()),"뷴류 타입 코드 없음");

		
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
		if(mailSendRequestDTO.getAttachmentFiles() != null && !mailSendRequestDTO.getAttachmentFiles().isEmpty()) {
			AttachmentFileRequestDTO fileDTO = AttachmentFileRequestDTO.builder()
				                            .files(mailSendRequestDTO.getAttachmentFiles())
																		.build();
			uploadFiles = attachmentFileService.uploadFiles(fileDTO, ownerTypeCode.getCommonCodeId(), mail.getMailId());
		}
		
		
		// 6. EML 생성
		String emlPath = emlService.generate(mail, mailSendRequestDTO.getTo(), mailSendRequestDTO.getCc(), mailSendRequestDTO.getBcc(), uploadFiles, employee);
		mail.updateEmlPath(emlPath);
		
		return MailSendResponseDTO.toDTO(mail, toCount, ccCount, bccCount, uploadFiles == null ? 0 : uploadFiles.size());
	}
	
	
	// 메일 수정
	@Override
	public MailSendResponseDTO updateMail(Long mailId, MailUpdateRequestDTO requestDTO, String username) {
		// 0. 수정 권한 확인 (메일 정보의 직원 ID 와 접속중인 사용자의 ID를 비교)
		Employee employee = employeeRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
		Mail mail = mailRepository.findById(mailId).orElseThrow(() -> new ResourceNotFoundException("메일이 존재하지 않습니다."));
		if(!mail.getEmployee().getEmployeeId().equals(employee.getEmployeeId())) {
			throw new AccessDeniedException("수정 권한이 없습니다.");
		}
		
		// 1. 분류 코드 가져오기
		CommonCode toCode = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.TO.name()),"TO 코드 없음"
		);
		CommonCode ccCode  = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.CC.name()), "CC 코드 없음"
		);
		CommonCode bccCode = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(ReceiverType.PREFIX, ReceiverType.BCC.name()), "BCC 코드 없음"
		);
		CommonCode ownerType = firstOrNotFound(
			commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.MAIL.name()), "분류 코드가 존재하지 않습니다."
		);
		
		// 2. 제목/ 본문 변경사항 업데이트
		if(requestDTO.getTitle() != null) mail.updateTitle(requestDTO.getTitle());
		if(requestDTO.getContent() != null) mail.updateContent(requestDTO.getContent());
		
		// 3. 수신자 변경사항 반영 (TO, CC, BCC)
		List<MailReceiver> currentTo = mailReceiverRepository.findAllByMailAndType(mail, toCode);
		List<MailReceiver> currentCc = mailReceiverRepository.findAllByMailAndType(mail, ccCode);
		List<MailReceiver> currentBcc = mailReceiverRepository.findAllByMailAndType(mail, bccCode);
		
		receiverChange(mail, toCode, currentTo, requestDTO.getTo());
		receiverChange(mail, ccCode, currentCc, requestDTO.getCc());
		receiverChange(mail, bccCode, currentBcc, requestDTO.getBcc());
		
		// 4. 첨부 삭제 -> 추가
		if(requestDTO.getDeleteAttachmentFileIds() != null && !requestDTO.getDeleteAttachmentFileIds().isEmpty()) {
			for(Long id : requestDTO.getDeleteAttachmentFileIds()) {
				if(id == null) continue;
				AttachmentFileRequestDTO delFile = new AttachmentFileRequestDTO();
				delFile.setFileId(id);
				attachmentFileService.deleteFile(delFile);
			}
		}
		List<AttachmentFileResponseDTO> upload = Collections.emptyList();
		if(requestDTO.getAttachmentFiles() != null && !requestDTO.getAttachmentFiles().isEmpty()) {
			AttachmentFileRequestDTO uploadFiles = AttachmentFileRequestDTO.builder()
				                                      .files(requestDTO.getAttachmentFiles())
				                                      .build();
			upload = attachmentFileService.uploadFiles(uploadFiles, ownerType.getCommonCodeId(), mail.getMailId());
		}
		
		// 5. 최신 수신자 목록 재조회
		List<MailReceiver> newTo = mailReceiverRepository.findAllByMailAndType(mail, toCode);
		List<MailReceiver> newCc = mailReceiverRepository.findAllByMailAndType(mail, ccCode);
		List<MailReceiver> newBcc = mailReceiverRepository.findAllByMailAndType(mail, bccCode);
		List<String> toList = mapEmails(newTo);
		List<String> ccList = mapEmails(newCc);
		List<String> bccList = mapEmails(newBcc);
		
		// 6. EML 생성
		List<AttachmentFileResponseDTO> currentFiles = attachmentFileService.listFiles(ownerType.getCommonCodeId(), mail.getMailId());
		String emlPath = emlService.generate(mail, toList, ccList, bccList, currentFiles, employee);
		mail.updateEmlPath(emlPath);
		
		return MailSendResponseDTO.toDTO(mail, newTo.size(), newCc.size(), newBcc.size(), currentFiles.size());
	}
	
	
	// 메일 상세 조회 및 읽음 처리
	@Override
	public MailDetailResponseDTO getMailDetail(Long mailId, String username, boolean isRead) {
		// 직원 정보, 메일함, 메일 정보 가져오기
		Employee employee = employeeRepository.findByUsername(username)
			                    .orElseThrow(() -> new ResourceNotFoundException("직원이 존재하지 않습니다."));
		Mailbox mailbox = mailboxRepository.findByEmployeeEmployeeIdAndMailMailId(employee.getEmployeeId(), mailId)
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
		
		List<String> toList = mapEmails(mailReceiverRepository.findAllByMailAndType(mail, toCode));
		List<String> ccList = mapEmails(mailReceiverRepository.findAllByMailAndType(mail, ccCode));
		List<String> bccList = null;
		
		boolean ISender = mail.getEmployee() != null && mail.getEmployee().getEmployeeId().equals(employee.getEmployeeId());
		if(ISender) {
			// 숨은참조는 작성자만 볼수있음
			bccList = mapEmails(mailReceiverRepository.findAllByMailAndType(mail, bccCode));
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
		
		Page<Mailbox> mailboxList = mailboxRepository
			                       .findByEmployeeUsernameAndTypeIdValue1AndDeletedStatusNot(
				                       username, boxType.toUpperCase(), boxType.toUpperCase().equals("TRASH") ? 1 : 0, pageable);
		
		return mailboxList.map(mb -> MailboxListDTO.builder()
			                             .boxId(mb.getBoxId())
			                             .mailId(mb.getMail().getMailId())
			                             .senderName(mb.getEmployee().getName())
			                             .title(mb.getMail().getTitle())
			                             .receivedAt(mb.getMail().getSendAt())
			                             .isRead(Boolean.TRUE.equals(mb.getIsRead()))
			                             .build()
			                      );
	}
}
