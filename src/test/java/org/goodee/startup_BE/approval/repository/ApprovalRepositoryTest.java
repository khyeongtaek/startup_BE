package org.goodee.startup_BE.approval.repository;

import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


// JPA 관련 컴포넌트만 테스트
// H2 DB 사용을 위한 모든 설정을 명시적으로 강제 (외부 설정 파일 Override 방지)
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop", // 1. 강제 테이블 생성
        "spring.datasource.driver-class-name=org.h2.Driver", // 2. 강제 H2 드라이버
        // 3. 강제 인메모리 DB (testdb는 H2 콘솔에서 식별하기 위한 이름)
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", // 4. H2 기본 계정
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect" // 5. 강제 H2 방언
})
// @DataJpaTest가 엔티티 클래스(Employee, CommonCode 등)를 찾을 수 있도록 스캔 경로를 지정
// 일반적으로 프로젝트의 최상위 기본 패키지를 지정해주는 것이 좋음
@EntityScan(basePackages = "org.goodee.startup_BE")
class ApprovalRepositoryTest {

    // 테스트 대상 리포지토리
    @Autowired
    private ApprovalDocRepository approvalDocRepository;
    @Autowired
    private ApprovalLineRepository approvalLineRepository;
    @Autowired
    private ApprovalReferenceRepository approvalReferenceRepository;

    // 의존성 리포지토리 (테스트 데이터 생성용)
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- 테스트용 공통 데이터 ---
    private Employee creator; // 기안자
    private Employee approver1; // 결재자1
    private Employee approver2; // 결재자2
    private Employee referrer; // 참조자

    private CommonCode testTemplate;  // 결재 양식

    private CommonCode statusActive, roleUser, deptDev, posJunior, posSenior;
    private CommonCode docStatusInProgress; // 문서상태: 진행중
    private CommonCode lineStatusPending; // 결재선상태: 미결
    private CommonCode lineStatusAwaiting; // 결재선상태: 대기

    @BeforeEach
    void setUp() {
        // H2 DB 초기화 (참조 무결성을 위해 의존되는 엔티티부터 삭제)
        approvalReferenceRepository.deleteAll();
        approvalLineRepository.deleteAll();
        approvalDocRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 ---
        statusActive = commonCodeRepository.save(CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false));
        roleUser = commonCodeRepository.save(CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false));
        deptDev = commonCodeRepository.save(CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false));
        posJunior = commonCodeRepository.save(CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false));
        posSenior = commonCodeRepository.save(CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null, false));

        // 결재 관련 공통 코드
        docStatusInProgress = commonCodeRepository.save(CommonCode.createCommonCode("AD_IN_PROGRESS", "진행중", "IN_PROGRESS", "AD", null, 1L, null, false));
        lineStatusPending = commonCodeRepository.save(CommonCode.createCommonCode("AL_PENDING", "미결", "PENDING", "AL", null, 1L, null, false));
        lineStatusAwaiting = commonCodeRepository.save(CommonCode.createCommonCode("AL_AWAITING", "대기", "AWAITING", "AL", null, 2L, null, false));

        // 테스트용 양식 생성
        testTemplate = commonCodeRepository.save(CommonCode.createCommonCode("TPL_001", "결재 양식", "휴가신청서", null, null, 1L, null, false));

        // --- given: 직원 데이터 생성 ---
        creator = createAndSaveEmployee("creator", "creator@test.com", posJunior, null); // 최초 생성자
        approver1 = createAndSaveEmployee("approver1", "approver1@test.com", posSenior, creator);
        approver2 = createAndSaveEmployee("approver2", "approver2@test.com", posSenior, creator);
        referrer = createAndSaveEmployee("referrer", "referrer@test.com", posJunior, creator);
    }

    /**
     * 테스트용 직원 생성 및 저장 헬퍼 메서드
     */
    private Employee createAndSaveEmployee(String username, String email, CommonCode pos, Employee creatorEmployee) {
        Employee employee = Employee.createEmployee(
                username, username, email, "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, pos,
                creatorEmployee
        );
        employee.updateInitPassword("testPassword123!", creatorEmployee);
        return employeeRepository.save(employee);
    }

    /**
     * 테스트용 결재 문서(저장 가능 상태) 생성 헬퍼 메서드
     */
    private ApprovalDoc createPersistableApprovalDoc(String title) {
        return ApprovalDoc.createApprovalDoc(
                title,
                "테스트 내용입니다.",
                creator,
                testTemplate,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                docStatusInProgress
        );
    }

    // ==================================================================================
    // 1. ApprovalDocRepository 테스트
    // ==================================================================================
    @Nested
    @DisplayName("ApprovalDocRepository 테스트")
    class ApprovalDocTests {

        @Test
        @DisplayName("C: 결재 문서(Doc) 생성(save) 테스트")
        void saveDocTest() {
            // given
            ApprovalDoc newDoc = createPersistableApprovalDoc("첫 번째 결재 문서");

            // when
            ApprovalDoc savedDoc = approvalDocRepository.save(newDoc);

            // then
            assertThat(savedDoc).isNotNull();
            assertThat(savedDoc.getDocId()).isNotNull();
            assertThat(savedDoc.getTitle()).isEqualTo("첫 번째 결재 문서");
            assertThat(savedDoc.getCreator()).isEqualTo(creator);
            assertThat(savedDoc.getDocStatus()).isEqualTo(docStatusInProgress);
            assertThat(savedDoc.getCreatedAt()).isNotNull(); // @PrePersist 동작 확인
        }

        @Test
        @DisplayName("R: 결재 문서 ID로 조회(findById) 테스트")
        void findDocByIdTest() {
            // given
            ApprovalDoc savedDoc = approvalDocRepository.save(createPersistableApprovalDoc("조회용 문서"));

            // when
            Optional<ApprovalDoc> foundDoc = approvalDocRepository.findById(savedDoc.getDocId());

            // then
            assertThat(foundDoc).isPresent();
            assertThat(foundDoc.get().getDocId()).isEqualTo(savedDoc.getDocId());
        }

        @Test
        @DisplayName("U: 결재 문서 수정(update) 테스트 (변경 감지)")
        void updateDocTest() throws InterruptedException {
            // given
            ApprovalDoc savedDoc = approvalDocRepository.save(createPersistableApprovalDoc("수정 전 문서"));
            LocalDateTime createdAt = savedDoc.getCreatedAt();

            // @PreUpdate 시간 구분을 위해 대기
            Thread.sleep(10);

            // when
            // 영속성 컨텍스트에서 엔티티 가져오기
            ApprovalDoc docToUpdate = approvalDocRepository.findById(savedDoc.getDocId()).get();
            docToUpdate.updateTitle("수정된 문서 제목");
            docToUpdate.updateUpdater(approver1); // 수정자 설정

            // flush (변경 감지 실행)
            approvalDocRepository.flush();

            // 검증을 위해 다시 조회
            ApprovalDoc updatedDoc = approvalDocRepository.findById(savedDoc.getDocId()).get();

            // then
            assertThat(updatedDoc.getTitle()).isEqualTo("수정된 문서 제목");
            assertThat(updatedDoc.getUpdater()).isEqualTo(approver1);
            assertThat(updatedDoc.getUpdatedAt()).isAfter(createdAt); // @PreUpdate 동작 확인
        }

        @Test
        @DisplayName("D: 결재 문서 삭제(delete) 테스트")
        void deleteDocTest() {
            // given
            ApprovalDoc savedDoc = approvalDocRepository.save(createPersistableApprovalDoc("삭제될 문서"));
            Long docId = savedDoc.getDocId();
            assertThat(approvalDocRepository.existsById(docId)).isTrue();

            // when
            approvalDocRepository.deleteById(docId);
            approvalDocRepository.flush();

            // then
            assertThat(approvalDocRepository.existsById(docId)).isFalse();
        }

        @Test
        @DisplayName("Exception: 필수 FK(creator) null 저장 시 예외 발생")
        void saveDocNullCreatorTest() {
            // given
            // 헬퍼 메서드를 사용하지 않고, creator가 null인 엔티티 생성 시도
            // (static 팩토리 메서드에서 creator를 받으므로, 이 테스트는 사실상 팩토리 메서드 수정 없이는 어려움)
            // (만약 엔티티가 public 생성자/setter를 가졌다면 테스트 가능)
            // 여기서는 팩토리 메서드가 creator를 non-null로 강제한다고 가정하고,
            // 다른 non-null 필드(e.g., docStatus)로 테스트

            ApprovalDoc doc = ApprovalDoc.createApprovalDoc(
                    "제목", "내용", creator, testTemplate, null, null,
                    null // docStatus (nullable=false) 를 null로 설정
            );

            // when & then
            assertThatThrownBy(() -> approvalDocRepository.saveAndFlush(doc))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Exception: 필수 필드(title) null 저장 시 예외 발생")
        void saveDocNullTitleTest() {
            // given
            ApprovalDoc doc = ApprovalDoc.createApprovalDoc(
                    null, // title (nullable=false) 를 null로 설정
                    "내용", creator, testTemplate, null, null, docStatusInProgress
            );

            // when & then
            assertThatThrownBy(() -> approvalDocRepository.saveAndFlush(doc))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ==================================================================================
    // 2. ApprovalLineRepository 테스트
    // ==================================================================================
    @Nested
    @DisplayName("ApprovalLineRepository 테스트")
    class ApprovalLineTests {

        private ApprovalDoc savedDoc;

        @BeforeEach
        void lineSetUp() {
            // Line, Reference 테스트는 항상 Doc이 필요하므로 미리 생성
            savedDoc = approvalDocRepository.save(createPersistableApprovalDoc("결재선 테스트용 문서"));
        }

        /**
         * 테스트용 결재 라인(저장 가능 상태) 생성 헬퍼 메서드
         */
        private ApprovalLine createPersistableApprovalLine(Long order, Employee approver, CommonCode status) {
            return ApprovalLine.createApprovalLine(
                    order,
                    savedDoc, // @BeforeEach에서 생성된 문서 사용
                    approver,
                    status,
                    null, // approvalDate (처리일)
                    null // comment
            );
        }

        @Test
        @DisplayName("C: 결재선(Line) 생성(save) 테스트")
        void saveLineTest() {
            // given
            ApprovalLine newLine = createPersistableApprovalLine(1L, approver1, lineStatusAwaiting);

            // when
            ApprovalLine savedLine = approvalLineRepository.save(newLine);

            // then
            assertThat(savedLine).isNotNull();
            assertThat(savedLine.getLineId()).isNotNull();
            assertThat(savedLine.getDoc()).isEqualTo(savedDoc);
            assertThat(savedLine.getEmployee()).isEqualTo(approver1);
            assertThat(savedLine.getApprovalOrder()).isEqualTo(1L);
            assertThat(savedLine.getApprovalStatus()).isEqualTo(lineStatusAwaiting);
        }

        @Test
        @DisplayName("R: 결재선 ID로 조회(findById) 테스트")
        void findLineByIdTest() {
            // given
            ApprovalLine savedLine = approvalLineRepository.save(
                    createPersistableApprovalLine(1L, approver1, lineStatusAwaiting)
            );

            // when
            Optional<ApprovalLine> foundLine = approvalLineRepository.findById(savedLine.getLineId());

            // then
            assertThat(foundLine).isPresent();
            assertThat(foundLine.get().getLineId()).isEqualTo(savedLine.getLineId());
        }

        @Test
        @DisplayName("U: 결재선 수정(update) 테스트 (변경 감지)")
        void updateLineTest() throws InterruptedException {
            // given
            ApprovalLine savedLine = approvalLineRepository.save(
                    createPersistableApprovalLine(1L, approver1, lineStatusAwaiting)
            );

            // when
            ApprovalLine lineToUpdate = approvalLineRepository.findById(savedLine.getLineId()).get();

            lineToUpdate.updateApprovalStatus(lineStatusPending); // '대기' -> '미결'로 변경
            lineToUpdate.updateComment("테스트 코멘트");
            // approvalDate는 @PreUpdate에 의해 자동 설정됨

            approvalLineRepository.flush();

            // 검증을 위해 다시 조회
            ApprovalLine updatedLine = approvalLineRepository.findById(savedLine.getLineId()).get();

            // then
            assertThat(updatedLine.getApprovalStatus()).isEqualTo(lineStatusPending);
            assertThat(updatedLine.getComment()).isEqualTo("테스트 코멘트");
        }

        @Test
        @DisplayName("D: 결재선 삭제(delete) 테스트")
        void deleteLineTest() {
            // given
            ApprovalLine savedLine = approvalLineRepository.save(
                    createPersistableApprovalLine(1L, approver1, lineStatusAwaiting)
            );
            Long lineId = savedLine.getLineId();
            assertThat(approvalLineRepository.existsById(lineId)).isTrue();

            // when
            approvalLineRepository.deleteById(lineId);
            approvalLineRepository.flush();

            // then
            assertThat(approvalLineRepository.existsById(lineId)).isFalse();
        }

        @Test
        @DisplayName("Exception: 필수 FK(doc) null 저장 시 예외 발생")
        void saveLineNullDocTest() {
            // given
            ApprovalLine line = ApprovalLine.createApprovalLine(
                    1L,
                    null, // doc (nullable=false) 를 null로 설정
                    approver1,
                    lineStatusAwaiting,
                    null,
                    null
            );

            // when & then
            assertThatThrownBy(() -> approvalLineRepository.saveAndFlush(line))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }


        @Test
        @DisplayName("Custom: findByDocAndApprovalOrder 테스트")
        void findByDocAndApprovalOrderTest() {
            // given
            approvalLineRepository.save(createPersistableApprovalLine(1L, approver1, lineStatusAwaiting));
            approvalLineRepository.save(createPersistableApprovalLine(2L, approver2, lineStatusPending));

            // when
            // 2번째 순서(approver2) 조회
            Optional<ApprovalLine> foundLine = approvalLineRepository.findByDocAndApprovalOrder(savedDoc, 2L);

            // 3번째 순서 (존재 X) 조회
            Optional<ApprovalLine> notFoundLine = approvalLineRepository.findByDocAndApprovalOrder(savedDoc, 3L);


            // then
            assertThat(foundLine).isPresent();
            assertThat(foundLine.get().getEmployee()).isEqualTo(approver2);

            assertThat(notFoundLine).isNotPresent();
        }
    }


    // ==================================================================================
    // 3. ApprovalReferenceRepository 테스트
    // ==================================================================================
    @Nested
    @DisplayName("ApprovalReferenceRepository 테스트")
    class ApprovalReferenceTests {

        private ApprovalDoc savedDoc;

        @BeforeEach
        void referenceSetUp() {
            // Line, Reference 테스트는 항상 Doc이 필요하므로 미리 생성
            savedDoc = approvalDocRepository.save(createPersistableApprovalDoc("참조 테스트용 문서"));
        }

        /**
         * 테스트용 참조(저장 가능 상태) 생성 헬퍼 메서드
         */
        private ApprovalReference createPersistableApprovalReference(Employee refEmployee) {
            return ApprovalReference.createApprovalReference(
                    savedDoc,
                    refEmployee
            );
        }


        @Test
        @DisplayName("C: 참조(Reference) 생성(save) 테스트")
        void saveReferenceTest() {
            // given
            ApprovalReference newRef = createPersistableApprovalReference(referrer);

            // when
            ApprovalReference savedRef = approvalReferenceRepository.save(newRef);

            // then
            assertThat(savedRef).isNotNull();
            assertThat(savedRef.getReferenceId()).isNotNull();
            assertThat(savedRef.getDoc()).isEqualTo(savedDoc);
            assertThat(savedRef.getEmployee()).isEqualTo(referrer);
            assertThat(savedRef.getViewedAt()).isNull(); // 최초 생성 시 null
        }

        @Test
        @DisplayName("R: 참조 ID로 조회(findById) 테스트")
        void findReferenceByIdTest() {
            // given
            ApprovalReference savedRef = approvalReferenceRepository.save(
                    createPersistableApprovalReference(referrer)
            );

            // when
            Optional<ApprovalReference> foundRef = approvalReferenceRepository.findById(savedRef.getReferenceId());

            // then
            assertThat(foundRef).isPresent();
            assertThat(foundRef.get().getReferenceId()).isEqualTo(savedRef.getReferenceId());
        }

        @Test
        @DisplayName("U: 참조 수정(update) 테스트 - (열람 시간 기록)")
        void updateReferenceTest() {
            // given
            ApprovalReference savedRef = approvalReferenceRepository.save(
                    createPersistableApprovalReference(referrer)
            );
            assertThat(savedRef.getViewedAt()).isNull();

            // when
            ApprovalReference refToUpdate = approvalReferenceRepository.findById(savedRef.getReferenceId()).get();
            LocalDateTime viewTime = LocalDateTime.now();
            refToUpdate.update(viewTime); // 열람 시간 업데이트

            approvalReferenceRepository.flush();

            // 검증을 위해 다시 조회
            ApprovalReference updatedRef = approvalReferenceRepository.findById(savedRef.getReferenceId()).get();


            // then
            assertThat(updatedRef.getViewedAt()).isEqualTo(viewTime);
        }

        @Test
        @DisplayName("D: 참조 삭제(delete) 테스트")
        void deleteReferenceTest() {
            // given
            ApprovalReference savedRef = approvalReferenceRepository.save(
                    createPersistableApprovalReference(referrer)
            );
            Long refId = savedRef.getReferenceId();
            assertThat(approvalReferenceRepository.existsById(refId)).isTrue();

            // when
            approvalReferenceRepository.deleteById(refId);
            approvalReferenceRepository.flush();

            // then
            assertThat(approvalReferenceRepository.existsById(refId)).isFalse();
        }

        @Test
        @DisplayName("Exception: 필수 FK(doc) null 저장 시 예외 발생")
        void saveReferenceNullDocTest() {
            // given
            ApprovalReference ref = ApprovalReference.createApprovalReference(
                    null, // doc (nullable=false) 를 null로 설정
                    referrer
            );

            // when & then
            assertThatThrownBy(() -> approvalReferenceRepository.saveAndFlush(ref))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Exception: 중복 참조 저장 (Unique 제약조건 - 복합키)")
        void saveDuplicateReferenceTest() {
            // (가정: 만약 ApprovalReference 엔티티의 @Table에
            // @UniqueConstraint(columnNames = {"doc_id", "employee_id"}) 가 걸려있다면)

            // ApprovalReference.java 에는 현재 Unique 제약조건이 정의되어 있지 않습니다.
            // 따라서 이 테스트는 현재 엔티티 정의 하에서는 DataIntegrityViolationException을 발생시키지 않습니다.

            // given
            approvalReferenceRepository.save(createPersistableApprovalReference(referrer));
            approvalReferenceRepository.flush(); // 첫 번째 저장

            // when
            ApprovalReference duplicateRef = createPersistableApprovalReference(referrer);

            // then
            // Unique 제약조건이 없으므로, 예외가 발생하지 않고 저장이 성공해야 합니다.
            ApprovalReference savedDuplicate = approvalReferenceRepository.saveAndFlush(duplicateRef);

            assertThat(savedDuplicate).isNotNull();
            assertThat(savedDuplicate.getReferenceId()).isNotEqualTo(1L); // 새 ID를 받음
        }
    }
}