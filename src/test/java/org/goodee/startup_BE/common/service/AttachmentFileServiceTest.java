package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import(AttachmentFileServiceImpl.class)
class AttachmentFileServiceTest {
	
	@Autowired
	AttachmentFileServiceImpl attachmentFileService;
	
	@Autowired
	AttachmentFileRepository attachmentFileRepository;
	
	@Autowired
	CommonCodeRepository commonCodeRepository;
	
	@Autowired
	EntityManager em;
	
	@TempDir
	Path tempDir;
	
	private CommonCode ownerTypeMail;   // value1 = "MAIL"
	private Long ownerId = 100L;
	
	@BeforeEach
	void init() {
		// 서비스의 저장 루트 동적 주입
		ReflectionTestUtils.setField(attachmentFileService, "storageRoot", tempDir.toString());
		
		// CommonCode 준비 (파일 경로 생성 시 value1 사용됨)
		ownerTypeMail = CommonCode.createCommonCode(
			"OT_MAIL", "OwnerType.Mail", "MAIL", null, null, 1L, null, false
		);
		commonCodeRepository.save(ownerTypeMail);
	}
	
	// ========= Helpers =========
	
	private MockMultipartFile mockFile(String name, String contentType, byte[] bytes) {
		return new MockMultipartFile("files", name, contentType, bytes);
	}
	
	private List<AttachmentFileResponseDTO> doUpload(List<MultipartFile> files) {
		return attachmentFileService.uploadFiles(files, ownerTypeMail.getCommonCodeId(), ownerId);
	}
	
	// ========= uploadFiles() =========
	
	@Test
	@DisplayName("uploadFiles: 단일/다중 업로드 성공 + 물리파일/DB 저장 + DTO 반환")
	void uploadFiles_success() throws IOException {
		// given
		MockMultipartFile f1 = mockFile("report a.txt", "text/plain", "hello".getBytes());
		MockMultipartFile f2 = mockFile("image.png", "image/png", new byte[]{1,2,3,4});
		
		// when
		List<AttachmentFileResponseDTO> dtos = doUpload(List.of(f1, f2));
		
		// then
		assertThat(dtos).hasSize(2);
		List<AttachmentFile> saved = attachmentFileRepository.findAll();
		assertThat(saved).hasSize(2);
		
		for (AttachmentFileResponseDTO dto : dtos) {
			// 상대경로가 채워지고, 물리 파일이 존재해야 함
			assertThat(dto.getStoragePath()).isNotBlank();
			Path abs = tempDir.resolve(dto.getStoragePath()).normalize();
			assertThat(Files.exists(abs)).isTrue();
			assertThat(Files.size(abs)).isGreaterThan(0);
			// createdAt 자동 세팅
			assertThat(dto.getCreatedAt()).isNotNull();
		}
	}
	
	@Test
	@DisplayName("uploadFiles: 파일 리스트에 null/empty가 섞여 있어도 유효 파일만 저장")
	void uploadFiles_skipNullOrEmpty() {
		// given
		MockMultipartFile valid = mockFile("v.dat", "application/octet-stream", new byte[]{9});
		MockMultipartFile empty = new MockMultipartFile("files", "empty.txt", "text/plain", new byte[0]);
		
		// when
		List<AttachmentFileResponseDTO> dtos = doUpload(Arrays.asList(valid, null, empty));
		
		// then
		assertThat(dtos).hasSize(1);
		AttachmentFileResponseDTO dto = dtos.get(0);
		assertThat(dto.getOriginalName()).isEqualTo("v.dat");
	}
	
	@Test
	@DisplayName("uploadFiles: contentType null이면 DEFAULT_MIME으로 저장")
	void uploadFiles_defaultMimeWhenNull() {
		// given
		MockMultipartFile f = new MockMultipartFile("files", "raw.bin", null, new byte[]{1,2});
		
		// when
		List<AttachmentFileResponseDTO> dtos = doUpload(List.of(f));
		
		// then
		assertThat(dtos).hasSize(1);
		assertThat(dtos.get(0).getMimeType()).isEqualTo("application/octet-stream");
	}
	
	@Test
	@DisplayName("uploadFiles: ownerTypeId 미존재 시 NoSuchElementException")
	void uploadFiles_ownerTypeNotFound() {
		// given
		MockMultipartFile f = mockFile("a.txt", "text/plain", "x".getBytes());
		
		// when & then
		assertThatThrownBy(() ->
			                   attachmentFileService.uploadFiles(List.of(f), 9999L, ownerId)
		).isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("분류가 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("uploadFiles: 업로드 목록이 비어 있으면 IllegalArgumentException")
	void uploadFiles_emptyFiles() {
		// when & then
		assertThatThrownBy(() ->
			                   attachmentFileService.uploadFiles(Collections.emptyList(), ownerTypeMail.getCommonCodeId(), ownerId)
		).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("업로드할 파일이 없습니다.");
	}
	
	// ========= listFiles() =========
	
	@Test
	@DisplayName("listFiles: 생성일시 내림차순으로 반환 + DEFAULT_MIME 보정")
	void listFiles_sortedDescAndDefaultMime() throws Exception {
		// given: 업로드 타이밍 차이를 위해 약간 대기
		MockMultipartFile f1 = mockFile("old.txt", null, "1".getBytes());
		List<AttachmentFileResponseDTO> d1 = doUpload(List.of(f1));
		Thread.sleep(5);
		
		MockMultipartFile f2 = mockFile("new.txt", "text/plain", "22".getBytes());
		List<AttachmentFileResponseDTO> d2 = doUpload(List.of(f2));
		
		// when
		List<AttachmentFileResponseDTO> list = attachmentFileService.listFiles(ownerTypeMail.getCommonCodeId(), ownerId);
		
		// then: 최신(new.txt) 먼저
		assertThat(list).hasSize(2);
		assertThat(list.get(0).getOriginalName()).isEqualTo("new.txt");
		assertThat(list.get(1).getOriginalName()).isEqualTo("old.txt");
		
		// old.txt는 contentType null이었으므로 DEFAULT_MIME이어야 함
		assertThat(list.get(1).getMimeType()).isEqualTo("application/octet-stream");
	}
	
	@Test
	@DisplayName("listFiles: ownerTypeId 미존재 시 NoSuchElementException")
	void listFiles_ownerTypeNotFound() {
		assertThatThrownBy(() ->
			                   attachmentFileService.listFiles(123456789L, ownerId)
		).isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("분류가 존재하지 않습니다.");
	}
	
	// ========= downloadFile() =========
	
	@Test
	@DisplayName("downloadFile: 성공 시 Resource/헤더(Content-Disposition, Content-Type) 정상")
	void downloadFile_success() throws IOException {
		// given
		MockMultipartFile f = mockFile("file A.txt", "text/plain", "hello world".getBytes());
		List<AttachmentFileResponseDTO> dtos = doUpload(List.of(f));
		Long fileId = dtos.get(0).getFileId();
		
		// when
		ResponseEntity<Resource> resp = attachmentFileService.downloadFile(fileId);
		
		// then
		assertThat(resp.getBody()).isNotNull();
		assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
		String cd = resp.getHeaders().getFirst("Content-Disposition");
		assertThat(cd).contains("attachment;");
		// 공백이 %20으로 인코딩되어야 함
		assertThat(cd).contains("filename=\"file%20A.txt\"");
		assertThat(resp.getBody().exists()).isTrue();
		assertThat(resp.getBody().isReadable()).isTrue();
	}
	
	@Test
	@DisplayName("downloadFile: 파일 엔티티 미존재 시 NoSuchElementException")
	void downloadFile_notFoundEntity() {
		assertThatThrownBy(() -> attachmentFileService.downloadFile(987654321L))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("파일이 존재하지 않습니다.");
	}
	
	@Test
	@DisplayName("downloadFile: 물리 파일이 삭제되어 읽을 수 없으면 NoSuchElementException")
	void downloadFile_notReadablePhysicalFile() throws IOException {
		// given
		MockMultipartFile f = mockFile("gone.txt", "text/plain", "bye".getBytes());
		List<AttachmentFileResponseDTO> dtos = doUpload(List.of(f));
		AttachmentFileResponseDTO dto = dtos.get(0);
		
		// 물리 파일 삭제
		Files.deleteIfExists(tempDir.resolve(dto.getStoragePath()).normalize());
		
		// when & then
		assertThatThrownBy(() -> attachmentFileService.downloadFile(dto.getFileId()))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("파일을 읽을 수 없습니다.");
	}
	
	// ========= deleteFile() =========
	
	@Test
	@DisplayName("deleteFile: 소프트 삭제 - 이후 조회 API에서 제외됨(@Where + 쿼리 조건)")
	void deleteFile_softDelete() {
		// given
		MockMultipartFile f = mockFile("del.txt", "text/plain", "x".getBytes());
		List<AttachmentFileResponseDTO> dtos = doUpload(List.of(f));
		Long fileId = dtos.get(0).getFileId();
		
		// when
		attachmentFileService.deleteFile(fileId);
		
		// then: 서비스/리포지토리 조회에서 제외
		assertThat(attachmentFileRepository.findByFileIdAndIsDeletedFalse(fileId)).isEmpty();
		List<AttachmentFileResponseDTO> after = attachmentFileService.listFiles(ownerTypeMail.getCommonCodeId(), ownerId);
		assertThat(after).isEmpty();
		
		// native 쿼리로 is_deleted=true 확인 (엔티티 @Where 우회)
		Object flag = em.createNativeQuery("SELECT is_deleted FROM tbl_file WHERE file_id = :id")
			              .setParameter("id", fileId)
			              .getSingleResult();
		boolean isDeleted = (flag instanceof Boolean) ? (Boolean) flag : Integer.valueOf(flag.toString()) == 1;
		assertThat(isDeleted).isTrue();
	}
	
	@Test
	@DisplayName("deleteFile: 엔티티 미존재 시 NoSuchElementException")
	void deleteFile_notFound() {
		assertThatThrownBy(() -> attachmentFileService.deleteFile(111111L))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessageContaining("파일이 존재하지 않습니다.");
	}
}
