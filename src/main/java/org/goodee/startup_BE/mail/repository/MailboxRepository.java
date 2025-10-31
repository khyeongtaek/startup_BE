package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.mail.entity.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
	Optional<Mailbox> findByEmployeeEmployeeIdAndMailMailId(Long employeeId, Long mailId);
}
