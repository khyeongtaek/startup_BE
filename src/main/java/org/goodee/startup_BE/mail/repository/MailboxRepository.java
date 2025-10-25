package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.mail.entity.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    // INSERT
}
