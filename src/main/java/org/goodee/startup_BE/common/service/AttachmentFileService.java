package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentFileService {
	List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFile, Long ownerTypeId, Long ownerId);
	List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId);
	AttachmentFileResponseDTO resolveFile(AttachmentFileRequestDTO request);
	void deleteFile(AttachmentFileRequestDTO request);
}
