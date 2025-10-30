package org.goodee.startup_BE.approval.repository;

import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // 사용하지 않으므로 제거 가능
import org.springframework.data.repository.query.Param; // 사용하지 않으므로 제거 가능

import java.util.List;
import java.util.Optional;

public interface ApprovalReferenceRepository extends JpaRepository<ApprovalReference, Long> {
}