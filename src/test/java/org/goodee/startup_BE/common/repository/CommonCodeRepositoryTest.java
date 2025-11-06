package org.goodee.startup_BE.common.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class CommonCodeRepositoryTest {

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    private CommonCode dept1;
    private CommonCode dept2;
    // [추가] 새 쿼리 테스트를 위한 변수
    private CommonCode template1;
    private CommonCode template2;


    @BeforeEach
    void setUp() {
        commonCodeRepository.deleteAll();

        // [수정] codeDescription을 '부서 코드'로 통일
        dept1 = CommonCode.createCommonCode(
                "DP1", "부서 코드", "DEV",
                null, null, 1L, (Employee) null
        );

        dept2 = CommonCode.createCommonCode(
                "DP2", "부서 코드", "HR",
                null, null, 2L, (Employee) null
        );

        // [추가] 새 쿼리(findByCodeDescription...) 테스트를 위한 데이터
        // 정렬 순서를 확인하기 위해 일부러 2번 -> 1번 순서로 생성
        template2 = CommonCode.createCommonCode(
                "TPL_002", "결재 양식", "연차신청서",
                null, null, 2L, (Employee) null
        );

        template1 = CommonCode.createCommonCode(
                "TPL_001", "결재 양식", "휴가신청서",
                null, null, 1L, (Employee) null
        );

        commonCodeRepository.saveAll(List.of(dept1, dept2, template2, template1));
    }

    // -------------------------------
    // CRUD 테스트
    // -------------------------------
    @Nested
    @DisplayName("조직도용 CRUD 테스트")
    class CRUDTests {

        @Test
        @DisplayName("부서 코드 등록 성공")
        void createDepartmentCode() {
            CommonCode saved = commonCodeRepository.save(
                    CommonCode.createCommonCode(
                            "DP3", "부서 코드", "GEN", // '부서 코드'
                            null, null, 3L, null
                    )
            );

            assertThat(saved.getCode()).isEqualTo("DP3");
            assertThat(saved.getCodeDescription()).isEqualTo("부서 코드");
        }

        @Test
        @DisplayName("부서 코드 전체 조회 성공")
        void findAllDepartments() {
            List<CommonCode> list = commonCodeRepository.findAll();
            // '결재 양식' 2개가 추가되어 총 4개
            assertThat(list).hasSize(4);
        }

        @Test
        @DisplayName("부서 코드 단건 조회 성공 (findById)")
        void findByIdDepartment() {
            Optional<CommonCode> found = commonCodeRepository.findById(dept1.getCommonCodeId());
            assertThat(found).isPresent();
            assertThat(found.get().getValue1()).isEqualTo("DEV");
        }

        @Test
        @DisplayName("부서 코드 수정 성공")
        void updateDepartmentCode() {
            CommonCode found = commonCodeRepository.findById(dept1.getCommonCodeId())
                    .orElseThrow();

            found.update("DP1", "부서 코드(수정)", "DEVOPS",
                    null, null, 1L, null);

            CommonCode updated = commonCodeRepository.save(found);
            assertThat(updated.getValue1()).isEqualTo("DEVOPS");
            assertThat(updated.getCodeDescription()).contains("부서 코드(수정)");
        }

        @Test
        @DisplayName("부서 코드 삭제 성공")
        void deleteDepartmentCode() {
            commonCodeRepository.deleteById(dept2.getCommonCodeId());
            Optional<CommonCode> deleted = commonCodeRepository.findById(dept2.getCommonCodeId());
            assertThat(deleted).isEmpty();
        }
    }

    // -------------------------------
    //  Custom Query 테스트
    // -------------------------------
    @Nested
    @DisplayName("Custom Query 테스트")
    class CustomQueryTests {

        @Test
        @DisplayName("findByCodeDescription...: '결재 양식' 조회 및 SortOrder 정렬 테스트")
        void findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc_Success() {
            // given
            // setUp()에서 "결재 양식" 2개 (sortOrder 1, 2), "부서 코드" 2개 저장
            // (참고: IsDeletedFalse 테스트는 CommonCode 엔티티에 soft-delete 로직과
            //       해당 로직을 호출하는 코드가 setUp()에 추가로 필요합니다.)

            // when
            List<CommonCode> results = commonCodeRepository.findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc("결재 양식");

            // then
            // "결재 양식" 2개만 조회되어야 함
            assertThat(results).hasSize(2);

            // 1L, 2L 순서로 정렬되었는지 확인
            assertThat(results).extracting(CommonCode::getCode)
                    .containsExactly("TPL_001", "TPL_002");
            assertThat(results).extracting(CommonCode::getSortOrder)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("findByCodeDescription...: '부서 코드' 조회 및 SortOrder 정렬 테스트")
        void findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc_DeptSuccess() {
            // when
            List<CommonCode> results = commonCodeRepository.findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc("부서 코드");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(CommonCode::getCode)
                    .containsExactly("DP1", "DP2");
        }

        @Test
        @DisplayName("findByCodeDescription...: 조회 결과 없음")
        void findByCodeDescription_NoResult() {
            // when
            List<CommonCode> results = commonCodeRepository.findByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc("없는 설명");

            // then
            assertThat(results).isEmpty();
        }
    }


    // -------------------------------
    // 예외 테스트
    // -------------------------------
    @Nested
    @DisplayName("조직도 예외 테스트")
    class ExceptionTests {

        @Test
        @DisplayName("부서 코드 중복 저장 시 예외 발생")
        void duplicateDepartmentCode() {
            CommonCode duplicate = CommonCode.createCommonCode(
                    "DP1", "중복 부서", "DUP",
                    null, null, 3L, null
            );

            assertThatThrownBy(() -> commonCodeRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("필수 필드 누락 시 예외 발생 (code 누락)")
        void missingRequiredField() {
            CommonCode invalid = CommonCode.createCommonCode(
                    null, "부서 코드 누락", "MISSING",
                    null, null, 5L, null
            );

            assertThatThrownBy(() -> commonCodeRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}