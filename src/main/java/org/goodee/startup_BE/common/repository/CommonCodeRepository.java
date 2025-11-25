package org.goodee.startup_BE.common.repository;

import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommonCodeRepository extends JpaRepository<CommonCode, Long> {
    @Query("SELECT c FROM CommonCode c " +
            "WHERE c.code LIKE CONCAT(:codePrefix, '%') " + // code가 codePrefix로 시작
            "  AND (c.value1 = :keyword OR " + // keyword와 정확히 일치
            "       c.value2 = :keyword OR " +
            "       c.value3 = :keyword) " +
            "  AND c.isDisabled = false") // 활성화 되어있는
    List<CommonCode> findByCodeStartsWithAndKeywordExactMatchInValues(
            @Param("codePrefix") String codePrefix,
            @Param("keyword") String keyword
    );

    // code가 codePrefix로 시작되는 활성화된 전체를 조회
    List<CommonCode> findByCodeStartsWithAndIsDisabledFalse(String codePrefix);

    // code가 codePrefix로 시작되는 비활성화 포함 전체를 조회
    List<CommonCode> findByCodeStartsWith(String codePrefix);


    /**
     * 접두사(codePrefix)와 해당 접두사 'root' 코드 (ex: "DP0")의 정보를 DTO로 조회
     * DTO의 code 필드에는 접두사(ex: "DP")를, 나머지 필드에는 "DP0"의 데이터가 담김.
     *
     * @return List<CommonCodeResponseDTO>
     */
    @Query("SELECT new org.goodee.startup_BE.common.dto.CommonCodeResponseDTO(" +
            "   c.commonCodeId, " +
            "   SUBSTRING(c.code, 1, 2), " + // DTO의 code 필드에 접두사를 매핑
            "   c.codeDescription, " +
            "   c.value1, " +
            "   c.value2, " +
            "   c.value3, " +
            "   c.sortOrder, " +
            "   c.createdAt, " +
            "   c.updatedAt," +
            "   c.isDisabled," +
            "   cr.username, " + // creator
            "   up.username  " + // updater
            ") " +
            "FROM CommonCode c " +
            "LEFT JOIN c.creator cr " +
            "LEFT JOIN c.updater up " +
            "WHERE c.code LIKE '__0' " + // 'DP0', 'ES0' 등 2글자 접두사 + 0 인 코드
            "ORDER BY SUBSTRING(c.code, 1, 2) ASC")
    List<CommonCodeResponseDTO> findDistinctCodePrefixes();

    // 게시판전용 find
    Optional<CommonCode> findByCode(String code);
}
