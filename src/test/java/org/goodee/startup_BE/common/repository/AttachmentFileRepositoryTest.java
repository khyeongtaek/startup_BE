package org.goodee.startup_BE.common.repository;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
class AttachmentFileRepositoryTest {

    @Autowired
    private AttachmentFileRepository attachmentFileRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EntityManager em;

    // --- 테스트용 공통 데이터 ---
    private CommonCode typeNotice; // 공지사항 첨부파일 타입
    private CommonCode typeBoard;  // 게시판 첨부파일 타입
    private final Long OWNER_ID_1 = 100L;
    private final Long OWNER_ID_2 = 200L;
    private final Long OWNER_ID_3 = 300L;

    @BeforeEach
    void setUp() {
        // 1. 데이터 초기화
        attachmentFileRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // 2. CommonCode (OwnerType) 생성
        typeNotice = createAndSaveCode("TYPE_NOTICE", "공지사항파일", "NOTICE", 1L);
        typeBoard = createAndSaveCode("TYPE_BOARD", "게시판파일", "BOARD", 2L);
    }

    // --- Helper Methods ---

    private CommonCode createAndSaveCode(String code, String name, String val1, Long seq) {
        CommonCode c = CommonCode.createCommonCode(code, name, val1, null, null, seq, null, false);
        return commonCodeRepository.save(c);
    }

    private AttachmentFile createAndSaveFile(String name, String path, CommonCode type, Long ownerId) {
        AttachmentFile file = AttachmentFile.createAttachmentFile(
                name, "txt", 1024L, path, "text/plain", type, ownerId
        );
        return attachmentFileRepository.save(file);
    }

    // --- CRUD Tests ---

    @Test
    @DisplayName("C: 첨부파일 저장(save) 테스트 - @PrePersist 동작 확인")
    void saveAttachmentFileTest() {
        // given
        AttachmentFile file = AttachmentFile.createAttachmentFile(
                "test.txt", "txt", 500L, "/upload/test.txt", "text/plain", typeNotice, OWNER_ID_1
        );

        // when
        AttachmentFile savedFile = attachmentFileRepository.save(file);

        // then
        assertThat(savedFile.getFileId()).isNotNull();
        assertThat(savedFile.getOriginalName()).isEqualTo("test.txt");
        assertThat(savedFile.getCreatedAt()).isNotNull(); // @PrePersist
        assertThat(savedFile.getIsDeleted()).isFalse();   // @PrePersist
    }

    @Test
    @DisplayName("R: ID로 조회(findById) 테스트")
    void findByIdTest() {
        // given
        AttachmentFile saved = createAndSaveFile("read.txt", "/upload/read.txt", typeNotice, OWNER_ID_1);

        // when
        Optional<AttachmentFile> found = attachmentFileRepository.findById(saved.getFileId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getStoragePath()).isEqualTo("/upload/read.txt");
    }

    @Test
    @DisplayName("U: 소프트 삭제(Soft Delete) 테스트 - @Where 동작 확인")
    void softDeleteTest() {
        // given
        AttachmentFile target = createAndSaveFile("del.txt", "/upload/del.txt", typeNotice, OWNER_ID_1);
        Long id = target.getFileId();

        // when
        // 엔티티 메서드를 통한 소프트 삭제
        target.deleteAttachmentFile();
        attachmentFileRepository.flush(); // DB 반영
        em.clear(); // 영속성 컨텍스트 비우기 (DB에서 다시 조회하기 위해)

        // then
        // 1. Repository 조회 시 결과가 없어야 함 (@Where(clause = "is_deleted = false") 적용 확인)
        Optional<AttachmentFile> deletedFile = attachmentFileRepository.findById(id);
        assertThat(deletedFile).isEmpty();

        // 2. Native Query로 실제 DB 확인 시 데이터는 존재하고 is_deleted가 true여야 함
        Boolean isDeletedInDb = (Boolean) em.createNativeQuery("SELECT is_deleted FROM tbl_file WHERE file_id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(isDeletedInDb).isTrue();
    }

    @Test
    @DisplayName("D: 물리적 삭제(delete) 테스트")
    void hardDeleteTest() {
        // given
        AttachmentFile target = createAndSaveFile("hard.txt", "/upload/hard.txt", typeNotice, OWNER_ID_1);
        Long id = target.getFileId();

        // when
        attachmentFileRepository.delete(target);
        attachmentFileRepository.flush();

        // then
        // 물리적으로도 없어야 함 (Native Query로 확인)
        Long count = (Long) em.createNativeQuery("SELECT count(*) FROM tbl_file WHERE file_id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(count).isEqualTo(0L);
    }

    // --- Exception Tests ---

    @Test
    @DisplayName("Exception: 필수값(OwnerType) 누락 시 예외")
    void saveNullOwnerTypeTest() {
        // given
        AttachmentFile invalidFile = AttachmentFile.createAttachmentFile(
                "fail.txt", "txt", 100L, "/fail/path", "text/plain", null, OWNER_ID_1
        );

        // when & then
        assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(invalidFile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 저장 경로(storagePath) 중복 시 예외 (Unique Constraint)")
    void saveDuplicateStoragePathTest() {
        // given
        createAndSaveFile("orig.txt", "/duplicate/path", typeNotice, OWNER_ID_1);

        AttachmentFile duplicateFile = AttachmentFile.createAttachmentFile(
                "copy.txt", "txt", 200L, "/duplicate/path", "text/plain", typeNotice, OWNER_ID_2
        );

        // when & then
        assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(duplicateFile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Custom Repository Queries Tests ---

    @Test
    @DisplayName("Custom: findByFileIdAndIsDeletedFalse - 미삭제 파일 단건 조회")
    void findByFileIdAndIsDeletedFalseTest() {
        // given
        AttachmentFile file = createAndSaveFile("active.txt", "/active", typeNotice, OWNER_ID_1);
        AttachmentFile deleted = createAndSaveFile("deleted.txt", "/deleted", typeNotice, OWNER_ID_1);

        deleted.deleteAttachmentFile();
        attachmentFileRepository.save(deleted);

        // when
        Optional<AttachmentFile> resActive = attachmentFileRepository.findByFileIdAndIsDeletedFalse(file.getFileId());
        Optional<AttachmentFile> resDeleted = attachmentFileRepository.findByFileIdAndIsDeletedFalse(deleted.getFileId());

        // then
        assertThat(resActive).isPresent();
        assertThat(resDeleted).isEmpty();
    }

    @Test
    @DisplayName("Custom: findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse - 특정 모듈의 특정 ID 파일 목록")
    void findAllByOwnerTypeAndOwnerIdTest() {
        // given
        // Notice / ID 100 : 파일 2개 (하나는 삭제됨)
        AttachmentFile f1 = createAndSaveFile("n1.txt", "/n1", typeNotice, OWNER_ID_1);
        AttachmentFile f2 = createAndSaveFile("n2.txt", "/n2", typeNotice, OWNER_ID_1);
        f2.deleteAttachmentFile(); // 삭제 처리

        // Notice / ID 200 : 파일 1개
        createAndSaveFile("n3.txt", "/n3", typeNotice, OWNER_ID_2);

        // Board / ID 100 : 파일 1개
        createAndSaveFile("b1.txt", "/b1", typeBoard, OWNER_ID_1);

        attachmentFileRepository.flush();

        // when
        List<AttachmentFile> result = attachmentFileRepository.findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(typeNotice, OWNER_ID_1);

        // then
        assertThat(result).hasSize(1); // n1.txt만 나와야 함 (n2는 삭제, n3는 ID다름, b1은 타입다름)
        assertThat(result.get(0).getOriginalName()).isEqualTo("n1.txt");
    }

    @Test
    @DisplayName("Custom: findAllByOwnerTypeAndOwnerIdIn... - 여러 ID(IN절) 파일 일괄 조회")
    void findAllByOwnerIdInTest() {
        // given
        // [Notice]
        // ID 100: file_100_A (Active), file_100_D (Deleted)
        AttachmentFile f1A = createAndSaveFile("100A", "/100A", typeNotice, OWNER_ID_1);
        AttachmentFile f1D = createAndSaveFile("100D", "/100D", typeNotice, OWNER_ID_1);
        f1D.deleteAttachmentFile();

        // ID 200: file_200_A (Active)
        AttachmentFile f2A = createAndSaveFile("200A", "/200A", typeNotice, OWNER_ID_2);

        // ID 300: file_300_A (Active) - 조회 대상에서 뺄 예정
        createAndSaveFile("300A", "/300A", typeNotice, OWNER_ID_3);

        // [Board] - 타입 다른 파일 (조회되면 안됨)
        createAndSaveFile("Board100", "/B100", typeBoard, OWNER_ID_1);

        attachmentFileRepository.flush();

        // when
        // Notice 타입이면서 ID가 100 또는 200인 파일 조회
        List<Long> targetIds = List.of(OWNER_ID_1, OWNER_ID_2);
        List<AttachmentFile> results = attachmentFileRepository.findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(typeNotice, targetIds);

        // then
        assertThat(results).hasSize(2); // 100A, 200A 만 조회되어야 함
        assertThat(results).extracting("originalName")
                .containsExactlyInAnyOrder("100A", "200A");

        assertThat(results).extracting("originalName")
                .doesNotContain("100D", "300A", "Board100");
    }
}