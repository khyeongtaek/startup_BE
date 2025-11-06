package org.goodee.startup_BE.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentFileServiceImpl implements AttachmentFileService{
	private final AttachmentFileRepository attachmentFileRepository;
	private final CommonCodeRepository commonCodeRepository;
	private static final String DEFAULT_MIME = MediaType.APPLICATION_OCTET_STREAM_VALUE;
	
	@Value("${file.storage.root}")
	private String storageRoot;
	
	// 확장자 추출
	private String getExt(String name) {
		if (!StringUtils.hasText(name)) return "";
		int i = name.lastIndexOf('.');
		return (i > -1 && i < name.length() - 1) ? name.substring(i + 1).toLowerCase() : "";
	}
	
	// 경로 지정
	private String makeRelPath(String ownerCode, String ext) {
		LocalDate d = LocalDate.now();
		String id = UUID.randomUUID().toString().replace("-", "");
		ownerCode = StringUtils.hasText(ownerCode) ? ownerCode : "UNKNOWN";
		return ownerCode + "/" + d.getYear() + "/" +
			       String.format("%02d", d.getMonthValue()) + "/" +
			       String.format("%02d", d.getDayOfMonth()) + "/" +
			       id + (StringUtils.hasText(ext) ? "." + ext : "");
	}
	
	// 파일 업로드
	@Override
	public List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFile, Long ownerTypeId, Long ownerId) {
		if (multipartFile == null || multipartFile.isEmpty()) {
			throw new IllegalArgumentException("업로드할 파일이 없습니다.");
		}
		
		CommonCode ownerType = commonCodeRepository.findById(ownerTypeId)
			                       .orElseThrow(() -> new NoSuchElementException("분류가 존재하지 않습니다."));
		
		List<Path> savedPaths = new ArrayList<>();
		List<AttachmentFile> entities = new ArrayList<>();
		
		try {
			for(MultipartFile file : multipartFile) {
				if(file == null || file.isEmpty()) continue;
				
				// 파일 데이터 저장
				String original = StringUtils.cleanPath(file.getOriginalFilename());    // DB에 저장(상대경로)
				String ext = getExt(original);
				Long size = file.getSize();
				String mime = StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_MIME;
				
				// 저장 경로
				String relative = makeRelPath(ownerType.getValue1(), ext);
				Path abs = Paths.get(storageRoot).resolve(relative).normalize();
				Files.createDirectories(abs.getParent());
				file.transferTo(abs.toFile());
				savedPaths.add(abs);
				
				// 엔티티
				entities.add(AttachmentFile.createAttachmentFile(original, ext, size, relative, mime, ownerType, ownerId));
			}
			
			// DB 저장
			return attachmentFileRepository.saveAll(entities).stream()
				       .map(AttachmentFileResponseDTO::toDTO)
				       .peek(dto -> {if (!StringUtils.hasText(dto.getMimeType())) dto.setMimeType(DEFAULT_MIME);})
				       .collect(Collectors.toList());
		} catch (IOException e) {
			// 물리파일 정리( DB는 트랜잭션 롤백 )
			for (Path p : savedPaths) try { Files.deleteIfExists(p); } catch (IOException ignore) {}
			throw new IllegalStateException("파일 업로드 실패", e);
		} catch (RuntimeException e) {
			// DB 예외 등 기타 런타임에도 물리파일 정리
			for (Path p : savedPaths) try { Files.deleteIfExists(p); } catch (IOException ignore) {}
			throw e;
		}
	}
	
	// 파일 리스트 조회
	@Override
	@Transactional(readOnly = true)
	public List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId) {
		CommonCode ownerType = commonCodeRepository.findById(ownerTypeId)
			                       .orElseThrow(() -> new NoSuchElementException("분류가 존재하지 않습니다."));

		List<AttachmentFile> files = attachmentFileRepository.findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(ownerType, ownerId);
		
		return files.stream()
			       .sorted(Comparator.comparing(AttachmentFile::getCreatedAt).reversed())
			       .map(AttachmentFileResponseDTO::toDTO)
			       .peek(dto -> {
							 if(dto.getMimeType() == null || dto.getMimeType().isEmpty()) {
								 dto.setMimeType(DEFAULT_MIME);
							 }
			       })
			       .toList();
	}
	
	// 파일 다운로드
	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<Resource> downloadFile(Long fileId) {
		// 파일 존재 여부 확인
		AttachmentFile file = attachmentFileRepository.findByFileIdAndIsDeletedFalse(fileId)
			                      .orElseThrow(() -> new NoSuchElementException("파일이 존재하지 않습니다."));
		
		// MIME 타입 지정
		String mime = file.getMimeType() == null || file.getMimeType().isEmpty()
			              ? DEFAULT_MIME : file.getMimeType();
		
		// 물리 경로
		Path filePath = Paths.get(storageRoot).resolve(file.getStoragePath()).normalize();
		System.out.println(filePath.toAbsolutePath().toString() );
		
		try {
			Resource resource = new UrlResource(filePath.toUri());
			if(!resource.exists() || !resource.isReadable()) {
				throw new NoSuchElementException("파일을 읽을 수 없습니다.");
			}
			
			String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
			
			return ResponseEntity.ok()
				       .contentType(MediaType.parseMediaType(mime))
				       .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
				       .body(resource);
			
		} catch (MalformedURLException e){
			throw new IllegalStateException("파일 경로가 잘못되었습니다.", e);
		}
	}
	
	// 파일 삭제
	@Override
	public void deleteFile(Long fileId) {
		AttachmentFile file = attachmentFileRepository.findByFileIdAndIsDeletedFalse(fileId)
			                      .orElseThrow(() -> new NoSuchElementException("파일이 존재하지 않습니다."));
		
		file.deleteAttachmentFile();
	}
}
