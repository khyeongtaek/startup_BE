package org.goodee.startup_BE.mail.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.mail.entity.Mail;

import java.util.List;

public interface EmlService {
	// EML 생성 후 "상대경로" 반환
	String generate(
		Mail mail, List<String> to, List<String> cc, List<String> bcc, List<AttachmentFileResponseDTO> attachmentFiles, Employee sender
	);
}
