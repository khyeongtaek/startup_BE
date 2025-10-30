package org.goodee.startup_BE.approval.repository;

import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    /**
     * (승인 처리 시) 특정 문서의 다음 결재 순서(order)에 해당하는 결재선 조회
     */
    Optional<ApprovalLine> findByDocAndApprovalOrder(ApprovalDoc doc, Long order);
}