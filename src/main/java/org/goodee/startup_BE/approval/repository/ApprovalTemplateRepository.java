package org.goodee.startup_BE.approval.repository;

import org.goodee.startup_BE.approval.entity.ApprovalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, Long>{

    /**
     * 삭제되지 않은(is_deleted = false) 모든 템플릿을 조회합니다.
     * @return 템플릿 엔티티 리스트
     */
    List<ApprovalTemplate> findAllByIsDeletedFalse();

    Optional<ApprovalTemplate> findByTemplateIdAndIsDeletedFalse(Long templateId);
}
