package org.goodee.startup_BE.common.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<CommonCode, Long> {

    // 최상위 부서 조회
    @Query("SELECT c FROM CommonCode c WHERE c.value1 IS NULL AND c.isDeleted = false ORDER BY c.sortOrder ASC")
    List<CommonCode> findRootDepartments();

    // 하위 부서 조회
    // :parentCode - jpql에서 쓰는 바인드 변수
    @Query("SELECT c FROM CommonCode c WHERE c.value1 = :parentCode AND c.isDeleted = false ORDER BY c.sortOrder ASC")
    List<CommonCode> findSubDepartments(@Param("parentCode")String parentCode);

    Optional<CommonCode> findByCodeAndIsDeletedFalse(String code);

}
