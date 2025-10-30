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

  @BeforeEach
  void setUp() {
    commonCodeRepository.deleteAll();

    dept1 = CommonCode.createCommonCode(
            "DP1", "부서 코드 - 개발팀", "DEV",
            null, null, 1L, (Employee) null
    );

    dept2 = CommonCode.createCommonCode(
            "DP2", "부서 코드 - 인사팀", "HR",
            null, null, 2L, (Employee) null
    );

    commonCodeRepository.saveAll(List.of(dept1, dept2));
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
                      "DP3", "부서 코드 - 총무팀", "GEN",
                      null, null, 3L, null
              )
      );

      assertThat(saved.getCode()).isEqualTo("DP3");
      assertThat(saved.getCodeDescription()).contains("총무팀");
    }

    @Test
    @DisplayName("부서 코드 전체 조회 성공")
    void findAllDepartments() {
      List<CommonCode> list = commonCodeRepository.findAll();
      assertThat(list).hasSize(2);
      assertThat(list).extracting(CommonCode::getCode)
              .containsExactlyInAnyOrder("DP1", "DP2");
    }

    @Test
    @DisplayName("부서 코드 단건 조회 성공")
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

      found.update("DP1", "부서 코드 - 개발팀(수정)", "DEVOPS",
              null, null, 1L, null);

      CommonCode updated = commonCodeRepository.save(found);
      assertThat(updated.getValue1()).isEqualTo("DEVOPS");
      assertThat(updated.getCodeDescription()).contains("개발팀(수정)");
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
