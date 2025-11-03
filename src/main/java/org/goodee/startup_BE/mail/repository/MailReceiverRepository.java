package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.mail.entity.Mail;
import org.goodee.startup_BE.mail.entity.MailReceiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MailReceiverRepository extends JpaRepository<MailReceiver, Long> {
	// 메일의 수신자 타입에 따른 조회 (리스트)
	List<MailReceiver> findAllByMailAndType(Mail mail, CommonCode type);
}
