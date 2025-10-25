package org.goodee.startup_BE.mail.repository;

import org.goodee.startup_BE.mail.entity.MailReceiver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailReceiverRepository extends JpaRepository<MailReceiver, Long> {
    // INSERT
}
