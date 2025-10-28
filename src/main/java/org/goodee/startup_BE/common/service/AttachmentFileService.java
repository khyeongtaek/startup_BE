package org.goodee.startup_BE.common.service;

import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;
import org.goodee.startup_BE.common.dto.AttachmentFileRequestDTO;

import java.util.List;

public interface AttachmentFileService {
	List<AttachmentFileResponseDTO> uploadFiles(AttachmentFileRequestDTO request);
	List<AttachmentFileResponseDTO> listFiles(AttachmentFileRequestDTO request);
	AttachmentFileResponseDTO resolveFile(AttachmentFileRequestDTO request);
}
