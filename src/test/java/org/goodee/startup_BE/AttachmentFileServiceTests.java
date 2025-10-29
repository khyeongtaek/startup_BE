package org.goodee.startup_BE;

import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "org.goodee.startup_BE")
@TestPropertySource(properties = {
	// H2(메모리) + MySQL 호환 모드 (LONGTEXT 등 DDL 호환성)
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	// 서비스가 참조하는 물리 저장 루트 (OS 임시 폴더 하위)
	"file.storage.root=${java.io.tmpdir}/uploads-test"
})
@Transactional
class AttachmentFileServiceTests {
	
	@Autowired
	private AttachmentFileService attachmentFileService;
	
	@Autowired
	private AttachmentFileRepository attachmentFileRepository;
	
	@Autowired
	private CommonCodeRepository commonCodeRepository;
	
	private CommonCode ownerTypeWorklog;
	private Long ownerTypeId;
	private final Long ownerId = 1001L;
	
	private Path storageRoot;
	
	// ===== 공통 헬퍼 =====
	private MockMultipartFile mock(String filename, String contentType, byte[] data) {
		// DTO의 필드명이 files 이므로 name="files"로 맞춰둠
		return new MockMultipartFile("files", filename, contentType, data);
	}
	
	private AttachmentFileRequestDTO uploadReq(MockMultipartFile... files) {
		return AttachmentFileRequestDTO.builder()
			       .files(Arrays.asList(files))
			       .build();
	}
	
	@BeforeEach
	void setUp() throws IOException {
		// 물리 경로
		storageRoot = Paths.get(System.getProperty("java.io.tmpdir")).resolve("uploads-test").normalize();
		// 깔끔히 초기화
		if (Files.exists(storageRoot)) {
			Files.walk(storageRoot)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> { try { Files.deleteIfExists(p);} catch (IOException ignored) {} });
		}
		Files.createDirectories(storageRoot);
		
		// DB 초기화
		attachmentFileRepository.deleteAll();
		commonCodeRepository.deleteAll();
		
		// OWNER 타입(업무일지) 준비
		ownerTypeWorklog = CommonCode.createCommonCode(
			"OWNER_WORKLOG", "업무일지", "WORKLOG",
			null, null, 1L, null
		);
		ownerTypeWorklog = commonCodeRepository.save(ownerTypeWorklog);
		ownerTypeId = ownerTypeWorklog.getCommonCodeId();
	}
	
	@AfterEach
	void tearDown() throws IOException {
		if (Files.exists(storageRoot)) {
			Files.walk(storageRoot)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> { try { Files.deleteIfExists(p);} catch (IOException ignored) {} });
		}
	}
	
	@Test
	@DisplayName("C: 업로드 성공 - 저장·엔티티 매핑·@PrePersist 동작")
	void upload_success() {
		// given
		MockMultipartFile f1 = mock("hello.txt", "text/plain", "HELLO".getBytes());
		MockMultipartFile f2 = mock("img.png", "image/png", new byte[]{1,2,3,4});
		
		// when
		var result = attachmentFileService.uploadFiles(uploadReq(f1, f2), ownerTypeId, ownerId);
		
		// then
		assertThat(result).hasSize(2);
		for (AttachmentFileResponseDTO dto : result) {
			assertThat(dto.getFileId()).isNotNull();
			assertThat(dto.getOriginalName()).isIn("hello.txt", "img.png");
			assertThat(dto.getStoragePath()).startsWith("WORKLOG/");
			assertThat(dto.getCreatedAt()).isNotNull();
			// 실제 파일 존재 확인
			Path abs = storageRoot.resolve(dto.getStoragePath()).normalize();
			assertThat(Files.exists(abs)).isTrue();
		}
	}
	
	@Test
	@DisplayName("C: 업로드 실패 - 파일 리스트 비어있음")
	void upload_fail_emptyFiles() {
		// given
		AttachmentFileRequestDTO req = AttachmentFileRequestDTO.builder()
			                               .files(Collections.emptyList())
			                               .build();
		
		// when & then
		assertThatThrownBy(() -> attachmentFileService.uploadFiles(req, ownerTypeId, ownerId))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("업로드할 파일이 없습니다");
	}
	
	@Test
	@DisplayName("C: 업로드 실패 - 존재하지 않는 ownerTypeId")
	void upload_fail_noOwnerType() {
		// given
		MockMultipartFile f = mock("a.bin", null, new byte[]{9,9});
		Long wrongOwnerTypeId = 99999L;
		
		// when & then
		assertThatThrownBy(() -> attachmentFileService.uploadFiles(uploadReq(f), wrongOwnerTypeId, ownerId))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("분류가 존재하지 않습니다");
	}
	
	@Test
	@DisplayName("R: 리스트 조회 - createdAt 내림차순 + MIME 기본값 보정")
	void list_success_desc_and_mimeDefault() {
		// given (두 파일 업로드: 하나는 contentType=null로 기본값 보정 유도)
		MockMultipartFile f1 = mock("a.bin", null, new byte[]{1}); // MIME null
		MockMultipartFile f2 = mock("b.txt", "text/plain", "B".getBytes());
		attachmentFileService.uploadFiles(uploadReq(f1), ownerTypeId, ownerId);
		attachmentFileService.uploadFiles(uploadReq(f2), ownerTypeId, ownerId);
		
		// when
		var list = attachmentFileService.listFiles(ownerTypeId, ownerId);
		
		// then
		assertThat(list).hasSize(2);
		// 최신(createdAt) 우선
		LocalDateTime first = list.get(0).getCreatedAt();
		LocalDateTime second = list.get(1).getCreatedAt();
		assertThat(first).isAfterOrEqualTo(second);
		
		// MIME 기본값 보정 확인 (null로 업로드한 a.bin이 octet-stream)
		assertThat(list.stream()
			           .filter(d -> d.getOriginalName().equals("a.bin"))
			           .findFirst().orElseThrow().getMimeType())
			.isEqualTo("application/octet-stream");
	}
	
	@Test
	@DisplayName("R: 리스트 조회 실패 - 잘못된 ownerTypeId")
	void list_fail_noOwnerType() {
		// when & then
		assertThatThrownBy(() -> attachmentFileService.listFiles(123456L, ownerId))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("분류가 존재하지 않습니다");
	}
	
	@Test
	@DisplayName("R: 단일 조회(다운로드) - 기본 MIME 보정 포함")
	void resolve_success() {
		// given
		MockMultipartFile f = mock("raw.bin", null, new byte[]{7,7,7}); // contentType=null
		var uploaded = attachmentFileService.uploadFiles(uploadReq(f), ownerTypeId, ownerId);
		Long fileId = uploaded.get(0).getFileId();
		
		AttachmentFileRequestDTO req = AttachmentFileRequestDTO.builder()
			                               .fileId(fileId)
			                               .build();
		
		// when
		AttachmentFileResponseDTO dto = attachmentFileService.resolveFile(req);
		
		// then
		assertThat(dto.getFileId()).isEqualTo(fileId);
		assertThat(dto.getOriginalName()).isEqualTo("raw.bin");
		assertThat(dto.getMimeType()).isEqualTo("application/octet-stream"); // 기본값 보정
	}
	
	@Test
	@DisplayName("R: 단일 조회 실패 - 존재하지 않는 파일ID")
	void resolve_fail_notFound() {
		// given
		AttachmentFileRequestDTO req = AttachmentFileRequestDTO.builder()
			                               .fileId(9999L)
			                               .build();
		
		// when & then
		assertThatThrownBy(() -> attachmentFileService.resolveFile(req))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("파일이 존재하지 않습니다");
	}
	
	@Test
	@DisplayName("D: 소프트삭제 - isDeleted=true로 전환되어 조회 불가")
	void deleteFile_success_softDelete() {
		// given
		MockMultipartFile f = mock("del.txt", "text/plain", "DEL".getBytes());
		var uploaded = attachmentFileService.uploadFiles(uploadReq(f), ownerTypeId, ownerId);
		Long fileId = uploaded.get(0).getFileId();
		
		AttachmentFileRequestDTO req = AttachmentFileRequestDTO.builder()
			                               .fileId(fileId)
			                               .build();
		
		// when
		attachmentFileService.deleteFile(req);
		
		// then
		// 현재 구현은 deleteFile에 readOnly=true라서 플래그 반영이 안 될 수 있음.
		// 통과시키려면 서비스의 @Transactional(readOnly=true) 제거/수정 필수.
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(fileId)).isEmpty();
	}
}
