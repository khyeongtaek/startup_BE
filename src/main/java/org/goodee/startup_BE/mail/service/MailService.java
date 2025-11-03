package org.goodee.startup_BE.mail.service;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.goodee.startup_BE.mail.dto.*;

public interface MailService {
	// 메일 작성
	MailSendResponseDTO sendMail(MailSendRequestDTO mailSendRequestDTO, String username);
	
	// 메일 수정
	MailSendResponseDTO updateMail(Long mailId, MailUpdateRequestDTO requestDTO, String username);
	
	// 메일 상세 조회 및 읽음 처리
	MailDetailResponseDTO getMailDetail(Long mailId, String username, boolean isRead);
	
	// 메일 이동 (개인보관함, 휴지통)
	void moveMails(MailMoveRequestDTO requestDTO, String username);
	
	// 메일 삭제 (휴지통에서 삭제 (소프트삭제))
	void deleteMails(MailMoveRequestDTO requestDTO, String username);
}
