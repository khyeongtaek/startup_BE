package org.goodee.startup_BE.common.repository;

// 필요한 import
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

    // 테스트에 사용될 공통 데이터
    private CommonCode dept1;
    private CommonCode dept2;
    private CommonCode pos1;
    private CommonCode pos2;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 모든 데이터 삭제
        commonCodeRepository.deleteAll();

        // '부서' 2개 생성 (정렬 순서 2, 1)
        dept1 = CommonCode.createCommonCode(
                "DP1", "부서", "DEV",
                "Development", "개발팀", 2L, (Employee) null, false
        );

        dept2 = CommonCode.createCommonCode(
                "DP2", "부서", "HR",
                "Human Resources", "인사팀", 1L, (Employee) null, false
        );

        // '직위' 2개 생성
        pos1 = CommonCode.createCommonCode(
                "PO1", "직위", "STAFF",
                "Employee", "사원", 1L, (Employee) null, false
        );

        pos2 = CommonCode.createCommonCode(
                "PO2", "직위", "MANAGER",
                "Team Leader", "팀장", 2L, (Employee) null, false
        );

        // 총 4개의 데이터 저장
        commonCodeRepository.saveAll(List.of(dept1, dept2, pos1, pos2));
    }

    // -------------------------------
    // CRUD 테스트
    // -------------------------------
    @Nested
    @DisplayName("조직도용 CRUD 테스트")
    class CRUDTests {

        @Test
        @DisplayName("공통 코드 등록 성공")
        void createDepartmentCode() {
            CommonCode saved = commonCodeRepository.save(
                    CommonCode.createCommonCode(
                            "DP3", "부서 코드 - 총무팀", "GEN",
                            null, null, 3L, null, false
                    )
            );

            assertThat(saved.getCode()).isEqualTo("DP3");
            assertThat(saved.getCodeDescription()).contains("총무팀");
        }

        @Test
        @DisplayName("공통 코드 전체 조회 성공 (findAll)")
        void findAllCodes() {
            // setUp에서 4개의 데이터를 저장했으므로, findAll()은 4개를 반환해야 함
            List<CommonCode> list = commonCodeRepository.findAll();
            assertThat(list).hasSize(4);
            assertThat(list).extracting(CommonCode::getCode)
                    .containsExactlyInAnyOrder("DP1", "DP2", "PO1", "PO2");
        }

        @Test
        @DisplayName("공통 코드 단건 조회 성공 (findById)")
        void findByIdDepartment() {
            // dept1의 ID로 조회
            Optional<CommonCode> found = commonCodeRepository.findById(dept1.getCommonCodeId());
            assertThat(found).isPresent();
            assertThat(found.get().getValue1()).isEqualTo("DEV");
        }

        @Test
        @DisplayName("공통 코드 수정 성공")
        void updateDepartmentCode() {
            CommonCode found = commonCodeRepository.findById(dept1.getCommonCodeId())
                    .orElseThrow();

            // dept1의 내용 수정
            found.update("DP1", "부서 코드 - 개발팀(수정)", "DEVOPS",
                    null, null, 1L, null, false);

            CommonCode updated = commonCodeRepository.save(found);
            assertThat(updated.getValue1()).isEqualTo("DEVOPS");
            assertThat(updated.getCodeDescription()).contains("개발팀(수정)");
        }

        @Test
        @DisplayName("공통 코드 삭제 성공")
        void deleteDepartmentCode() {
            // dept2 삭제
            commonCodeRepository.deleteById(dept2.getCommonCodeId());
            Optional<CommonCode> deleted = commonCodeRepository.findById(dept2.getCommonCodeId());
            assertThat(deleted).isEmpty();

            // 남은 데이터는 3개
            List<CommonCode> list = commonCodeRepository.findAll();
            assertThat(list).hasSize(3);
        }
    }

    // -------------------------------
    // 예외 테스트
    // -------------------------------
    @Nested
    @DisplayName("조직도 예외 테스트")
    class ExceptionTests {

        @Test
        @DisplayName("공통 코드 중복 저장 시 예외 발생")
        void duplicateDepartmentCode() {
            // setUp에서 "DP1" 코드가 이미 저장됨
            CommonCode duplicate = CommonCode.createCommonCode(
                    "DP1", "중복 부서", "DUP",
                    null, null, 3L, null, false
            );

            // 동일한 code("DP1")로 저장 시도 시 예외 발생 (Unique 제약 조건)
            assertThatThrownBy(() -> commonCodeRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("필수 필드 누락 시 예외 발생 (code 누락)")
        void missingRequiredField() {
            CommonCode invalid = CommonCode.createCommonCode(
                    null, "부서 코드 누락", "MISSING", // code 필드가 null
                    null, null, 5L, null, false
            );

            // code는 @Column(nullable = false)이므로 예외 발생
            assertThatThrownBy(() -> commonCodeRepository.saveAndFlush(invalid))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // -------------------------------
    // 새로운 커스텀 쿼리 메소드 테스트
    // -------------------------------
    @Nested
    @DisplayName("커스텀 쿼리 메소드 테스트")
    class CustomQueryTests {

        // --- testFindByCodeDescriptionAndIsDeletedFalseOrderBySortOrderAsc() ---
        // CommonCodeRepository.java에서 해당 메소드가 삭제되었으므로 테스트 케이스 제거
        // -----------------------------------------------------------------

        @Test
        @DisplayName("findByCodeStartsWithAndIsDeletedFalse: codePrefix로 시작하는 코드 조회")
        void testFindByCodeStartsWithAndIsDeletedFalse() {
            // when: "DP"로 시작하는 코드 조회
            List<CommonCode> dpList = commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse("DP");
            // then: dept1, dept2 조회
            assertThat(dpList).hasSize(2);
            assertThat(dpList).containsExactlyInAnyOrder(dept1, dept2);

            // when: "PO"로 시작하는 코드 조회
            List<CommonCode> poList = commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse("PO");
            // then: pos1, pos2 조회
            assertThat(poList).hasSize(2);
            assertThat(poList).containsExactlyInAnyOrder(pos1, pos2);

            // when: "XX"로 시작하는 코드 조회
            List<CommonCode> xxList = commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse("XX");
            // then: 조회 결과 없음
            assertThat(xxList).isEmpty();
        }

        @Test
        @DisplayName("findByCodeStartsWithAndKeywordExactMatchInValues: codePrefix와 value 키워드 일치 조회")
        void testFindByCodeStartsWithAndKeywordExactMatchInValues() {
            // Case 1: value1 (DEV) 매칭
            List<CommonCode> result1 = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("DP", "DEV");
            assertThat(result1).hasSize(1);
            assertThat(result1).contains(dept1);

            // Case 2: value2 (Team Leader) 매칭
            List<CommonCode> result2 = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("PO", "Team Leader");
            assertThat(result2).hasSize(1);
            assertThat(result2).contains(pos2);

            // Case 3: value3 (인사팀) 매칭
            List<CommonCode> result3 = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("DP", "인사팀");
            assertThat(result3).hasSize(1);
            assertThat(result3).contains(dept2);

            // Case 4: codePrefix는 맞지만 keyword가 틀린 경우
            List<CommonCode> result4 = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("DP", "NoMatchKeyword");
            assertThat(result4).isEmpty();

            // Case 5: keyword는 맞지만 codePrefix가 틀린 경우
            List<CommonCode> result5 = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("XX", "DEV");
            assertThat(result5).isEmpty();
        }
    }
}