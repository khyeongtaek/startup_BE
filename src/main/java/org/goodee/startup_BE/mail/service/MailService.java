package org.goodee.startup_BE.mail.service;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.goodee.startup_BE.mail.dto.MailSendRequestDTO;
import org.goodee.startup_BE.mail.dto.MailSendResponseDTO;
import org.goodee.startup_BE.mail.dto.MailUpdateRequestDTO;

public interface MailService {
	// 메일 작성
	MailSendResponseDTO sendMail(MailSendRequestDTO mailSendRequestDTO, String username);
	
	// 메일 수정
	MailSendResponseDTO updateMail(Long mailId, MailUpdateRequestDTO requestDTO, String username);
}
