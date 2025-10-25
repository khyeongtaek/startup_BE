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

}
