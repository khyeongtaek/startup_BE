package org.goodee.startup_BE.approval.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.approval.entity.ApprovalTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE") // Entity 스캔 범위 설정
class ApprovalTemplateRepositoryTest {

    @Autowired
    private ApprovalTemplateRepository approvalTemplateRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트용 공통 데이터
    private ApprovalTemplate template1, template2, deletedTemplate;

    @BeforeEach
    void setUp() {
        // DB 초기화
        approvalTemplateRepository.deleteAll();

        // --- given: ApprovalTemplate 생성 ---
        template1 = createPersistableTemplate("휴가 신청서", "휴가 신청서 양식입니다.");
        template2 = createPersistableTemplate("출장 보고서", "출장 보고서 양식입니다.");
        deletedTemplate = createPersistableTemplate("지출 결의서", "지출 결의서 양식입니다.");

        approvalTemplateRepository.saveAll(List.of(template1, template2, deletedTemplate));

        // deletedTemplate는 삭제 처리
        deletedTemplate.softDelete();
        approvalTemplateRepository.save(deletedTemplate);

        // 영속성 컨텍스트 초기화 (softDelete 변경 감지를 확실히 DB에 반영하기 위해)
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 테스트용 양식 생성 헬퍼 메서드 (ApprovalTemplate 엔티티의 create 활용)
     */
    private ApprovalTemplate createPersistableTemplate(String templateName, String content) {
        return ApprovalTemplate.create(templateName, content);
    }

    @Test
    @DisplayName("C: 결재 양식 생성(save) 테스트")
    void saveTemplateTest() {
        // given
        ApprovalTemplate newTemplate = createPersistableTemplate("신규 양식", "테스트 내용");

        // when
        ApprovalTemplate savedTemplate = approvalTemplateRepository.save(newTemplate);

        // then
        assertThat(savedTemplate).isNotNull();
        assertThat(savedTemplate.getTemplateId()).isNotNull();
        assertThat(savedTemplate.getTemplateName()).isEqualTo("신규 양식");
        assertThat(savedTemplate.getContent()).isEqualTo("테스트 내용");
        assertThat(savedTemplate.getCreatedAt()).isNotNull();
        assertThat(savedTemplate.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("R: findAllByIsDeletedFalse (삭제되지 않은 모든 양식 조회)")
    void findAllByIsDeletedFalseTest() {
        // given: setUp에서 template1, template2, deletedTemplate 생성 및 삭제 처리 완료

        // when
        List<ApprovalTemplate> resultList = approvalTemplateRepository.findAllByIsDeletedFalse();

        // then
        assertThat(resultList).isNotNull();
        assertThat(resultList).hasSize(2);
        assertThat(resultList).extracting(ApprovalTemplate::getTemplateName)
                .containsExactlyInAnyOrder("휴가 신청서", "출장 보고서");
    }

    @Test
    @DisplayName("R: findByIdAndIsDeletedFalse (삭제되지 않은 특정 양식 조회) - 성공")
    void findByIdAndIsDeletedFalse_Success() {
        // given: template1 (삭제되지 않음)

        // when
        Optional<ApprovalTemplate> result = approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(template1.getTemplateId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTemplateId()).isEqualTo(template1.getTemplateId());
        assertThat(result.get().getTemplateName()).isEqualTo("휴가 신청서");
    }

    @Test
    @DisplayName("R: findByIdAndIsDeletedFalse - 실패 (존재하지 않는 ID)")
    void findByIdAndIsDeletedFalse_Fail_NotFound() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<ApprovalTemplate> result = approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(nonExistentId);

        // then
        assertThat(result).isNotPresent(); // (isEmpty()와 동일)
    }

    @Test
    @DisplayName("R: findByIdAndIsDeletedFalse - 실패 (이미 삭제된 ID)")
    void findByIdAndIsDeletedFalse_Fail_Deleted() {
        // given: deletedTemplate (삭제됨)

        // when
        Optional<ApprovalTemplate> result = approvalTemplateRepository.findByTemplateIdAndIsDeletedFalse(deletedTemplate.getTemplateId());

        // then
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("U/D: softDelete (양식 삭제) 테스트")
    void softDeleteTest() {
        // given: template1 (삭제되지 않음)
        Long targetId = template1.getTemplateId();

        // when: template1을 조회하여 삭제 처리
        ApprovalTemplate targetTemplate = approvalTemplateRepository.findById(targetId)
                .orElseThrow(() -> new AssertionError("테스트 데이터가 없습니다."));

        targetTemplate.softDelete();
        approvalTemplateRepository.save(targetTemplate);

        // 영속성 컨텍스트 비우기 (캐시가 아닌 DB에서 조회하기 위함)
        entityManager.flush();
        entityManager.clear();

        // then:
        // 1. 삭제된 엔티티의 is_deleted 필드 확인
        ApprovalTemplate deleted = approvalTemplateRepository.findById(targetId).get();
        assertThat(deleted.getIsDeleted()).isTrue();

        // 2. 삭제되지 않은 목록 조회 시 포함되지 않는지 확인
        List<ApprovalTemplate> activeList = approvalTemplateRepository.findAllByIsDeletedFalse();
        assertThat(activeList).hasSize(1); // template2만 남아야 함
        assertThat(activeList).extracting(ApprovalTemplate::getTemplateId)
                .doesNotContain(targetId);
    }
}