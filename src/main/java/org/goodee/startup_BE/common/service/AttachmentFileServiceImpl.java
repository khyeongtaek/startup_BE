package org.goodee.startup_BE.common.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;
import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.AttachmentFileRepository;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentFileServiceImpl implements AttachmentFileService{
	private final AttachmentFileRepository attachmentFileRepository;
	private final CommonCodeRepository commonCodeRepository;
	private static final String DEFAULT_MIME = MediaType.APPLICATION_OCTET_STREAM_VALUE;
	
	// 파일 업로드
	@Override
	public List<AttachmentFileResponseDTO> uploadFiles(AttachmentFileRequestDTO request) {
		return List.of();
	}
	
	// 파일 리스트 조회
	@Override
	public List<AttachmentFileResponseDTO> listFiles(AttachmentFileRequestDTO request) {
//		CommonCode ownerType = commonCodeRepository.findById(request.get)

//		AttachmentFile file = attachmentFileRepository.findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse()
		return List.of();
	}
	
	// 파일 다운로드
	@Override
	@Transactional(readOnly = true)
	public AttachmentFileResponseDTO resolveFile(AttachmentFileRequestDTO request) {
		AttachmentFile file = attachmentFileRepository.findByFileIdAndIsDeletedFalse(request.getFileId())
			                      .orElseThrow(() -> new NoSuchElementException("파일이 존재하지 않습니다."));
		
		String mime = file.getMimeType() == null || file.getMimeType().isEmpty()
			              ? DEFAULT_MIME : file.getMimeType();
		AttachmentFileResponseDTO attachmentFile = AttachmentFileResponseDTO.toDTO(file);
		attachmentFile.setMimeType(mime);
		return attachmentFile;
	}
}
