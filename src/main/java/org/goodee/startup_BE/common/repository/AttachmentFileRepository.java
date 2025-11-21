package org.goodee.startup_BE.common.repository;

import org.goodee.startup_BE.common.entity.AttachmentFile;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentFileRepository extends JpaRepository<AttachmentFile, Long> {
	// 파일첨부 리스트 조회 (출처 모듈 + 해당 내 모듈 PK)
	List<AttachmentFile> findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(CommonCode ownerType, Long ownerId);
	
	// 단일 조회 / 다운로드
	Optional<AttachmentFile> findByFileIdAndIsDeletedFalse(Long fileId);

    // 여러 ownerId에 해당하는 모든 첨부파일을 한번에 조회
    List<AttachmentFile> findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(CommonCode ownerType, List<Long> ownerIds);
}
