package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.mail.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailRepository extends JpaRepository<Mail, Long> {

}
