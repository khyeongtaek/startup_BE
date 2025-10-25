package org.goodee.startup_BE.employee.repository;

import org.goodee.startup_BE.employee.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
}
