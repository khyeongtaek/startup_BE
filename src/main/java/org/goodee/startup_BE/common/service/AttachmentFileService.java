package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentFileService {
	List<AttachmentFileResponseDTO> uploadFiles(List<MultipartFile> multipartFile, Long ownerTypeId, Long ownerId);
	List<AttachmentFileResponseDTO> listFiles(Long ownerTypeId, Long ownerId);
	ResponseEntity<Resource> downloadFile(Long fileId);
	void deleteFile(Long fileId);
}
