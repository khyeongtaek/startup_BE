package org.goodee.startup_BE.attachmentFile;

import jakarta.persistence.EntityManager;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManagerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AttachmentFileRepositoryTests {
	
	@Autowired
	AttachmentFileRepository attachmentFileRepository;
	
	@Autowired
	CommonCodeRepository commonCodeRepository;
	
	@Autowired
	EntityManager em;
	
	private CommonCode OT_MAIL;
	private CommonCode OT_WORKLOG;
	
	private static final Long OWNER_ID_1 = 101L;
	private static final Long OWNER_ID_2 = 202L;
	
	@BeforeEach
	void setUp() {
		attachmentFileRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		OT_MAIL = CommonCode.createCommonCode("OT_MAIL", "OwnerType.Mail", "MAIL", null, null, 1L, null);
		OT_WORKLOG = CommonCode.createCommonCode("OT_WORKLOG", "OwnerType.WorkLog", "WORKLOG", null, null, 2L, null);
		commonCodeRepository.saveAll(List.of(OT_MAIL, OT_WORKLOG));
	}
	
	// ===== Helpers =====
	private AttachmentFile build(String original, String ext, long size, String path, String mime, CommonCode ownerType, Long ownerId) {
		return AttachmentFile.createAttachmentFile(original, ext, size, path, mime, ownerType, ownerId);
	}
	
	private AttachmentFile save(AttachmentFile f) {
		return attachmentFileRepository.saveAndFlush(f);
	}
	
	// ===== CRUD =====
	
	@Test
	@DisplayName("C/R: save -> findById (정상 저장 & 조회, @PrePersist 동작)")
	void saveAndFindById_success() {
		AttachmentFile f = save(build("a.txt", "txt", 3L, "MAIL/2025/11/06/aaa.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		
		assertThat(f.getFileId()).isNotNull();
		assertThat(f.getCreatedAt()).isNotNull();
		assertThat(f.getIsDeleted()).isFalse();
		
		assertThat(attachmentFileRepository.findById(f.getFileId()))
			.isPresent()
			.get()
			.extracting(AttachmentFile::getStoragePath)
			.isEqualTo("MAIL/2025/11/06/aaa.txt");
	}
	
	@Test
	@DisplayName("U: 엔티티 필드 변경 후 flush 시 업데이트 반영")
	void update_success() {
		AttachmentFile f = save(build("a.txt", "txt", 3L, "MAIL/2025/11/06/bbb.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		
		// 변경
		f.deleteAttachmentFile(); // is_deleted = true 로 변경
		attachmentFileRepository.flush();
		
		// @Where 로 인해 findById는 제외되어야 함
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId())).isEmpty();
		
		// 네이티브로 실제 행의 is_deleted 확인
		Object flag = em.createNativeQuery("SELECT is_deleted FROM tbl_file WHERE file_id = :id")
			              .setParameter("id", f.getFileId())
			              .getSingleResult();
		boolean isDeleted = (flag instanceof Boolean) ? (Boolean) flag : Integer.valueOf(flag.toString()) == 1;
		assertThat(isDeleted).isTrue();
	}
	
	@Test
	@DisplayName("D: deleteById(물리삭제) - 수행 시 존재 여부 false")
	void delete_success() {
		AttachmentFile f = save(build("a.txt", "txt", 3L, "MAIL/2025/11/06/ccc.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		
		Long id = f.getFileId();
		attachmentFileRepository.deleteById(id);
		attachmentFileRepository.flush();
		
		assertThat(attachmentFileRepository.existsById(id)).isFalse();
	}
	
	// ===== Custom Queries =====
	
	@Test
	@DisplayName("findByFileIdAndIsDeletedFalse: 미삭제 파일만 조회")
	void findByFileIdAndIsDeletedFalse_success() {
		AttachmentFile f = save(build("a.txt", "txt", 3L, "MAIL/2025/11/06/ddd.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId())).isPresent();
		
		// 소프트 삭제 후에는 제외
		f.deleteAttachmentFile();
		attachmentFileRepository.flush();
		
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId())).isEmpty();
	}
	
	@Test
	@DisplayName("findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse: 조건 일치 + 미삭제만 반환")
	void findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse_filtering() {
		AttachmentFile a = save(build("m1.txt", "txt", 1L, "MAIL/2025/11/06/m1.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		AttachmentFile b = save(build("m2.txt", "txt", 2L, "MAIL/2025/11/06/m2.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		AttachmentFile c = save(build("w1.txt", "txt", 3L, "WORKLOG/2025/11/06/w1.txt", "text/plain", OT_WORKLOG, OWNER_ID_1));
		AttachmentFile d = save(build("m3.txt", "txt", 4L, "MAIL/2025/11/06/m3.txt", "text/plain", OT_MAIL, OWNER_ID_2));
		AttachmentFile e = save(build("m4.txt", "txt", 5L, "MAIL/2025/11/06/m4.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		e.deleteAttachmentFile();
		attachmentFileRepository.flush();
		
		List<AttachmentFile> result = attachmentFileRepository
			                              .findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(OT_MAIL, OWNER_ID_1);
		
		assertThat(result)
			.extracting(AttachmentFile::getOriginalName)
			.containsExactlyInAnyOrder("m1.txt", "m2.txt")
			.doesNotContain("w1.txt", "m3.txt", "m4.txt");
	}
	
	// ===== Exceptions / Constraints =====
	
	@Test
	@DisplayName("제약: storage_path UNIQUE - 중복 저장 시 DataIntegrityViolationException")
	void constraint_unique_storagePath() {
		AttachmentFile f1 = build("x.txt", "txt", 1L, "MAIL/dup/path.txt", "text/plain", OT_MAIL, OWNER_ID_1);
		AttachmentFile f2 = build("y.txt", "txt", 2L, "MAIL/dup/path.txt", "text/plain", OT_MAIL, OWNER_ID_1);
		
		save(f1);
		assertThatThrownBy(() -> save(f2))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("제약: owner_type_id NOT NULL - null 저장 시 예외")
	void constraint_ownerType_notNull() {
		AttachmentFile f = build("a.txt", "txt", 1L, "MAIL/nt/1.txt", "text/plain", null, OWNER_ID_1);
		assertThatThrownBy(() -> save(f))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("제약: original_name NOT NULL - null 저장 시 예외")
	void constraint_originalName_notNull() {
		AttachmentFile f = build(null, "txt", 1L, "MAIL/nt/2.txt", "text/plain", OT_MAIL, OWNER_ID_1);
		assertThatThrownBy(() -> save(f))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("제약: size NOT NULL - null 저장 시 예외")
	void constraint_size_notNull() {
		AttachmentFile f = build("a.txt", "txt", 0L, "MAIL/nt/3.txt", "text/plain", OT_MAIL, OWNER_ID_1);
		// size 컬럼은 primitive가 아닌 Long 이므로 null을 직접 테스트
		AttachmentFile nullSize = AttachmentFile.createAttachmentFile("b.txt", "txt", null, "MAIL/nt/4.txt", "text/plain", OT_MAIL, OWNER_ID_1);
		
		save(f); // 0L은 허용(비즈니스 규칙이 아니므로 DB 제약 위반 아님)
		assertThatThrownBy(() -> save(nullSize))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("제약: storage_path NOT NULL - null 저장 시 예외")
	void constraint_storagePath_notNull() {
		AttachmentFile f = AttachmentFile.createAttachmentFile("a.txt", "txt", 1L, null, "text/plain", OT_MAIL, OWNER_ID_1);
		assertThatThrownBy(() -> save(f))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("@Where: soft delete 후 findById 결과 제외")
	void where_excludes_softDeleted_on_findById() {
		AttachmentFile f = save(build("z.txt", "txt", 1L, "MAIL/2025/11/06/zz.txt", "text/plain", OT_MAIL, OWNER_ID_1));
		
		f.deleteAttachmentFile();
		attachmentFileRepository.flush();
		
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId())).isEmpty();
		
		Object flag = em.createNativeQuery("SELECT is_deleted FROM tbl_file WHERE file_id = :id")
			              .setParameter("id", f.getFileId())
			              .getSingleResult();
		boolean isDeleted = (flag instanceof Boolean) ? (Boolean) flag : Integer.valueOf(flag.toString()) == 1;
		assertThat(isDeleted).isTrue();
	}
}
