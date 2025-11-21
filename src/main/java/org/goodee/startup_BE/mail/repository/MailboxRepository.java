package org.goodee.startup_BE.mail.repository;

import jakarta.validation.constraints.NotEmpty;
import org.goodee.startup_BE.mail.entity.Mailbox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
	
	Optional<Mailbox> findFirstByEmployeeEmployeeIdAndMailMailId(Long employeeId, Long mailId);
	
	List<Mailbox> findAllByEmployeeEmployeeIdAndMailMailId(Long employeeId, Long mailId);
	
	List<Mailbox> findAllByBoxIdInAndEmployeeUsername(List<Long> boxIds, String username);
	
	Page<Mailbox> findByEmployeeUsernameAndTypeIdValue1AndDeletedStatus(
		String username, String typeValue1, Byte deletedStatus, Pageable pageable);
	
	Page<Mailbox> findByEmployeeUsernameAndDeletedStatus(
		String username, Byte deletedStatus, Pageable pageable
	);
}

