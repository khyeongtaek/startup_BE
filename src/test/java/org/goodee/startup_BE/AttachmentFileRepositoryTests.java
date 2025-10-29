package org.goodee.startup_BE;

import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
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
public class AttachmentFileRepositoryTests {
	@Autowired
	private AttachmentFileRepository attachmentFileRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	private CommonCode ownerTypeWorklog;
	private CommonCode ownerTypeMail;
	
	@BeforeEach
	void setUp() {
		attachmentFileRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		ownerTypeWorklog = CommonCode.createCommonCode(
			"OT_WORKLOG", "업무일지", "WORKLOG", null, null, 1L, null
		);
		ownerTypeMail = CommonCode.createCommonCode(
			"OT_MAIL", "메일", "MAIL", null, null, 2L, null
		);
		commonCodeRepository.saveAll(List.of(ownerTypeWorklog, ownerTypeMail));
	}
	
	// 헬퍼: 새 첨부파일 엔티티 생성
	private AttachmentFile newFile(String original, String ext, long size, String path, String mime,
	                               CommonCode ownerType, Long ownerId) {
		return AttachmentFile.createAttachmentFile(original, ext, size, path, mime, ownerType, ownerId);
	}
	
	// =========================
	// C R U D 기본 동작
	// =========================
	
	@Test
	@DisplayName("C: 첨부파일 저장 성공 - 필수값 세팅 및 @PrePersist 동작 확인")
	void save_success() {
		// given
		AttachmentFile f = newFile("a.txt", "txt", 10L, "WORKLOG/2025/10/29/a.txt",
			"text/plain", ownerTypeWorklog, 1001L);
		
		// when
		AttachmentFile saved = attachmentFileRepository.saveAndFlush(f);
		
		// then
		assertThat(saved.getFileId()).isNotNull();
		assertThat(saved.getOriginalName()).isEqualTo("a.txt");
		assertThat(saved.getStoragePath()).isEqualTo("WORKLOG/2025/10/29/a.txt");
		assertThat(saved.getOwnerType()).isEqualTo(ownerTypeWorklog);
		assertThat(saved.getOwnerId()).isEqualTo(1001L);
		assertThat(saved.getCreatedAt()).isNotNull(); // @PrePersist로 세팅
		assertThat(saved.getIsDeleted()).isFalse();   // @PrePersist 기본 false
	}
	
	@Test
	@DisplayName("C: ownerId는 null 허용 - 저장 성공")
	void save_ownerIdNullable_success() {
		// given
		AttachmentFile f = newFile("b.txt", "txt", 20L, "WORKLOG/2025/10/29/b.txt",
			"text/plain", ownerTypeWorklog, null);
		
		// when
		AttachmentFile saved = attachmentFileRepository.saveAndFlush(f);
		
		// then
		assertThat(saved.getFileId()).isNotNull();
		assertThat(saved.getOwnerId()).isNull(); // ownerId는 nullable
	}
	
	@Test
	@DisplayName("R: ownerType+ownerId 기준 조회 - isDeleted=false만 반환")
	void findAll_byOwnerTypeAndOwnerId_onlyAlive() {
		// given
		Long ownerId1 = 2001L;
		Long ownerId2 = 2002L;
		
		AttachmentFile alive1 = newFile("c.txt", "txt", 10L, "WORKLOG/2025/10/29/c.txt", "text/plain", ownerTypeWorklog, ownerId1);
		AttachmentFile alive2 = newFile("d.txt", "txt", 11L, "WORKLOG/2025/10/29/d.txt", "text/plain", ownerTypeWorklog, ownerId1);
		AttachmentFile otherOwner = newFile("e.txt", "txt", 12L, "WORKLOG/2025/10/29/e.txt", "text/plain", ownerTypeWorklog, ownerId2);
		AttachmentFile otherType = newFile("f.txt", "txt", 13L, "MAIL/2025/10/29/f.txt", "text/plain", ownerTypeMail, ownerId1);
		
		attachmentFileRepository.saveAll(List.of(alive1, alive2, otherOwner, otherType));
		
		// 삭제 케이스 추가
		AttachmentFile deleted = newFile("g.txt", "txt", 14L, "WORKLOG/2025/10/29/g.txt", "text/plain", ownerTypeWorklog, ownerId1);
		deleted.deleteAttachmentFile(); // isDeleted=true
		attachmentFileRepository.saveAndFlush(deleted);
		
		// when
		List<AttachmentFile> result = attachmentFileRepository
			                              .findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(ownerTypeWorklog, ownerId1);
		
		// then
		assertThat(result).extracting(AttachmentFile::getOriginalName)
			.containsExactlyInAnyOrder("c.txt", "d.txt"); // 삭제/다른 owner/type 제외
	}
	
	@Test
	@DisplayName("R: fileId로 단일 조회 - isDeleted=false이면 조회 성공")
	void findByFileId_alive_success() {
		// given
		AttachmentFile f = attachmentFileRepository.saveAndFlush(
			newFile("h.txt", "txt", 10L, "WORKLOG/2025/10/29/h.txt", "text/plain", ownerTypeWorklog, 3001L)
		);
		
		// when
		Optional<AttachmentFile> found = attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId());
		
		// then
		assertThat(found).isPresent();
		assertThat(found.get().getOriginalName()).isEqualTo("h.txt");
	}
	
	@Test
	@DisplayName("R: fileId로 단일 조회 - isDeleted=true이면 조회되지 않음")
	void findByFileId_deleted_empty() {
		// given
		AttachmentFile f = attachmentFileRepository.saveAndFlush(
			newFile("i.txt", "txt", 10L, "WORKLOG/2025/10/29/i.txt", "text/plain", ownerTypeWorklog, 3002L)
		);
		f.deleteAttachmentFile();
		attachmentFileRepository.saveAndFlush(f);
		
		// when
		Optional<AttachmentFile> found = attachmentFileRepository.findByFileIdAndIsDeletedFalse(f.getFileId());
		
		// then
		assertThat(found).isEmpty();
	}
	
	@Test
	@DisplayName("R: fileId로 단일 조회 - 존재하지 않는 ID면 빈 Optional")
	void findByFileId_notFound_empty() {
		// when
		Optional<AttachmentFile> found = attachmentFileRepository.findByFileIdAndIsDeletedFalse(999_999L);
		
		// then
		assertThat(found).isEmpty();
	}
	
	@Test
	@DisplayName("D: deleteById 하드삭제 - 이후 조회 불가")
	void deleteById_hardDelete() {
		// given
		AttachmentFile f = attachmentFileRepository.saveAndFlush(
			newFile("j.txt", "txt", 10L, "WORKLOG/2025/10/29/j.txt", "text/plain", ownerTypeWorklog, 4001L)
		);
		Long id = f.getFileId();
		assertThat(attachmentFileRepository.findById(id)).isPresent();
		
		// when
		attachmentFileRepository.deleteById(id);
		attachmentFileRepository.flush();
		
		// then
		assertThat(attachmentFileRepository.findById(id)).isNotPresent();
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(id)).isEmpty();
	}
	
	// =========================
	// 제약 / 예외 테스트
	// =========================
	
	@Test
	@DisplayName("EX: storagePath 유니크 위반 시 DataIntegrityViolationException")
	void unique_storagePath_violation() {
		// given
		AttachmentFile first = newFile("u1.txt", "txt", 10L, "WORKLOG/2025/10/29/unique.txt", "text/plain", ownerTypeWorklog, 5001L);
		attachmentFileRepository.saveAndFlush(first);
		
		// when
		AttachmentFile dup = newFile("u2.txt", "txt", 20L, "WORKLOG/2025/10/29/unique.txt", "text/plain", ownerTypeWorklog, 5002L);
		
		// then
		assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(dup))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("EX: NOT NULL 위반 - originalName null 저장 시 예외")
	void notnull_originalName_violation() {
		AttachmentFile f = newFile(null, "txt", 10L, "WORKLOG/2025/10/29/n1.txt", "text/plain", ownerTypeWorklog, 6001L);
		assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(f))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("EX: NOT NULL 위반 - size null 저장 시 예외")
	void notnull_size_violation() {
		AttachmentFile f = newFile("n2.txt", "txt", /*size*/0L, "WORKLOG/2025/10/29/n2.txt", "text/plain", ownerTypeWorklog, 6002L);
		// size는 primitive가 아니라 Long이고 NOT NULL이므로, null을 주입해야 제약이 걸림
		// -> 팩토리로는 null을 직접 전달해야 함
		AttachmentFile fNull = AttachmentFile.createAttachmentFile("n2.txt", "txt", null, "WORKLOG/2025/10/29/n2.txt", "text/plain", ownerTypeWorklog, 6002L);
		assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(fNull))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("EX: NOT NULL 위반 - storagePath null 저장 시 예외")
	void notnull_storagePath_violation() {
		AttachmentFile f = AttachmentFile.createAttachmentFile("n3.txt", "txt", 10L, null, "text/plain", ownerTypeWorklog, 6003L);
		assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(f))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
	
	@Test
	@DisplayName("EX: NOT NULL 위반 - ownerType null 저장 시 예외")
	void notnull_ownerType_violation() {
		AttachmentFile f = AttachmentFile.createAttachmentFile("n4.txt", "txt", 10L, "WORKLOG/2025/10/29/n4.txt", "text/plain", null, 6004L);
		assertThatThrownBy(() -> attachmentFileRepository.saveAndFlush(f))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
