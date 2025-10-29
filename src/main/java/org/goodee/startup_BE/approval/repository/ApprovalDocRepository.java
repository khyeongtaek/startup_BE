package org.goodee.startup_BE.approval.repository;

import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph; // [수정] EntityGraph 임포트
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalDocRepository extends JpaRepository<ApprovalDoc, Long> {


    /**
     * (상세 조회용) ID로 문서를 조회하되, 결재선과 참조자를 모두 FETCH
     */
    @Query("SELECT d FROM ApprovalDoc d WHERE d.docId = :docId")
    Optional<ApprovalDoc> findDocWithDetailsById(@Param("docId") Long docId);

    /**
     * (기안 문서) 내가 기안한 문서 목록을 FETCH
     */
    @Query(value = "SELECT d FROM ApprovalDoc d " +
            "WHERE d.creator = :creator",
            countQuery = "SELECT COUNT(d) FROM ApprovalDoc d WHERE d.creator = :creator")
    Page<ApprovalDoc> findByCreatorWithDetails(Employee creator, Pageable pageable);


    /**
     * (결재 대기) 내가 결재선에 포함된 '완료/반려'되지 않은 문서 목록을 정렬(대기 > 미결 > 승인) 및 작성일 순(오래된 순)으로 조회
     */
    @Query(value = "SELECT d FROM ApprovalDoc d " +
            "JOIN d.approvalLineList l " +
            "WHERE l.employee = :employee " +
            // ---  '완료/반려'가 아닌 문서 ---
            "AND d.docStatus.value1 NOT IN :excludedDocStatuses " +
            "AND d.docStatus.code LIKE CONCAT(:docPrefix, '%') " +
            "ORDER BY " +
            "CASE WHEN l.approvalStatus.value1 = :statusAwaiting THEN 1 " +
            "     WHEN l.approvalStatus.value1 = :statusPending  THEN 2 " +
            "     WHEN l.approvalStatus.value1 = :statusApproved  THEN 3 " +
            "     ELSE 4 " +
            "END ASC, " +
            "d.createdAt ASC",
            countQuery = "SELECT COUNT(d) FROM ApprovalDoc d " +
                    "JOIN d.approvalLineList l " +
                    "WHERE l.employee = :employee " +
                    "AND d.docStatus.value1 NOT IN :excludedDocStatuses " +
                    "AND d.docStatus.code LIKE CONCAT(:docPrefix, '%')")
    Page<ApprovalDoc> findPendingDocsForEmployeeWithSort(
            @Param("employee") Employee employee,
            @Param("excludedDocStatuses") List<String> excludedDocStatuses,
            @Param("docPrefix") String docPrefix,
            @Param("statusAwaiting") String statusAwaiting,
            @Param("statusPending") String statusPending,
            @Param("statusApproved") String statusApproved,
            Pageable pageable
    );

    /**
     * (참조 문서) 내가 참조된 문서 목록을 FETCH
     */
    @Query(value = "SELECT d FROM ApprovalDoc d " +
            "JOIN d.approvalReferenceList r " +
            "WHERE r.employee = :employee",
            countQuery = "SELECT COUNT(d) FROM ApprovalDoc d " +
                    "JOIN d.approvalReferenceList r " +
                    "WHERE r.employee = :employee")
    Page<ApprovalDoc> findReferencedDocsForEmployee(
            @Param("employee") Employee employee,
            Pageable pageable
    );

    /**
     * (결재 완료) 내 결재가 포함된 완료/반려 문서 목록을 JOIN FETCH
     */
    @Query(value = "SELECT d FROM ApprovalDoc d " +
            "JOIN d.approvalLineList l " +
            "WHERE l.employee = :employee " +
            "AND d.docStatus.value1 IN :statusValues " +
            "AND d.docStatus.code LIKE CONCAT(:prefix, '%')",
            countQuery = "SELECT COUNT(d) FROM ApprovalDoc d " +
                    "JOIN d.approvalLineList l " +
                    "WHERE l.employee = :employee " +
                    "AND d.docStatus.value1 IN :statusValues " +
                    "AND d.docStatus.code LIKE CONCAT(:prefix, '%')")
    Page<ApprovalDoc> findCompletedDocsForEmployee(
            @Param("employee") Employee employee,
            @Param("statusValues") List<String> statusValues,
            @Param("prefix") String prefix,
            Pageable pageable
    );
}