package org.goodee.startup_BE.common.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {
    @Query("SELECT c FROM CommonCode c " +
            "WHERE c.code LIKE CONCAT(:codePrefix, '%') " + // code가 codePrefix로 시작
            "  AND (c.value1 = :keyword OR " + // keyword와 정확히 일치
            "       c.value2 = :keyword OR " +
            "       c.value3 = :keyword) " +
            "  AND c.isDeleted = false") // 삭제되지 않음
    List<CommonCode> findByCodeStartsWithAndKeywordExactMatchInValues(
            @Param("codePrefix") String codePrefix,
            @Param("keyword") String keyword
    );

    // 부모 자식 관계가 모두 담긴 부서 List를 List<CommonCode>로 반환
    @Query("SELECT c FROM CommonCode c " +
            "WHERE c.codeDescription = '부서' " +
            "AND c.isDeleted = false " +
            "ORDER BY c.sortOrder ASC")
    List<CommonCode> findAllDepartments();

    // 삭제 되지 않은 결재 양식 조회를 위한 쿼리 메소드
    List<CommonCode> findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc(String codeDescription);

    // code가 codePrefix로 시작되는 전체를 조회
    List<CommonCode> findByCodeStartsWithAndIsDeletedFalse(String codePrefix);

}
