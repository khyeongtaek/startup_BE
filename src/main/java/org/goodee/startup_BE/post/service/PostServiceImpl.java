package org.goodee.startup_BE.post.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.AttachmentFileResponseDTO;          // ★ 추가
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;         // ★ 추가
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;
    private final AttachmentFileService attachmentFileService;

    // 게시판 모듈에 해당하는 OwnerType(OT7)의 commonCodeId 가져오기
    private Long getPostOwnerTypeId() {
        CommonCode ownerType = commonCodeRepository.findByCode("OT10")
                .orElseThrow(() -> new IllegalArgumentException("게시판 OwnerType 코드(OT10)를 찾을 수 없습니다."));
        // CommonCode 엔티티의 PK 필드명이 commonCodeId라고 가정
        return ownerType.getCommonCodeId();
    }

    // 로그인한 사용자 조회
    private Employee getCurrentEmployee(String username) {
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    // 게시글 검색
    @Override
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> searchPost(String commonCodeCode, String keyword, Pageable pageable) {

        Page<Post> postPage =
                postRepository.searchPost(commonCodeCode, keyword, pageable);

        // 목록에서는 첨부파일 필요 없으니 기존 방식 유지
        return postPage.map(PostResponseDTO::toDTO);
    }

    // 게시글 생성
    @Override
    public PostResponseDTO createPost(
            PostRequestDTO dto,
            String commonCodeCode,
            String username) {

        Employee currentEmployee = getCurrentEmployee(username);

        CommonCode commonCode = commonCodeRepository.findByCode(commonCodeCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글 ID입니다."));

        // 게시글 엔티티 생성
        Post post = Post.builder()
                .commonCode(commonCode)
                .employee(currentEmployee)
                .employeeName(currentEmployee.getName())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isNotification(dto.getIsNotification() != null ? dto.getIsNotification() : false)
                .alert(dto.getAlert() != null ? dto.getAlert() : false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 먼저 게시글 저장
        Post savedPost = postRepository.save(post);

        // 첨부파일이 있다면 업로드 처리
        List<AttachmentFileResponseDTO> attachmentFiles = List.of();
        if (dto.getMultipartFile() != null && !dto.getMultipartFile().isEmpty()) {
            Long ownerTypeId = getPostOwnerTypeId();  // OT7의 common_code_id
            attachmentFiles = attachmentFileService.uploadFiles(
                    dto.getMultipartFile(),
                    ownerTypeId,
                    savedPost.getPostId()
            );
        }

        // 첨부파일 정보까지 포함해서 응답
        return PostResponseDTO.toDTO(savedPost, attachmentFiles);
    }

    // 게시글 수정
    @Override
    public PostResponseDTO updatePost(PostRequestDTO dto, String username) {

        // Employee currentEmployee = getCurrentEmployee(username);

        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.getIsDeleted())
            throw new IllegalStateException("삭제된 게시글입니다.");

        // 제목/내용 수정
        post.update(dto.getTitle(), dto.getContent(), dto.getIsNotification());

        Long ownerTypeId = getPostOwnerTypeId();

        // 기존 파일 삭제
        if (dto.getDeleteFileIds() != null) {
            for (Long fileId : dto.getDeleteFileIds()) {
                attachmentFileService.deleteFile(fileId);
            }
        }

        // 2) 새 파일 업로드
        if (dto.getMultipartFile() != null && !dto.getMultipartFile().isEmpty()) {
            attachmentFileService.uploadFiles(
                    dto.getMultipartFile(),
                    ownerTypeId,
                    post.getPostId()
            );
        }

        // 최신 첨부파일 목록 가져와서 DTO로 내려줌
        List<AttachmentFileResponseDTO> files =
                attachmentFileService.listFiles(ownerTypeId, post.getPostId());

        return PostResponseDTO.toDTO(post, files);
    }



    // 게시글 삭제
    @Override
    public void deletePost(Long postId, String username) {

        Employee currentEmployee = getCurrentEmployee(username);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        if (post.getIsDeleted()) {
            throw new IllegalStateException("이미 삭제된 게시글입니다.");
        }

        if (!post.getEmployee().getEmployeeId().equals(currentEmployee.getEmployeeId())) {
            throw new SecurityException("본인이 작성한 글만 삭제할 수 있습니다.");
        }
        post.delete();
    }

    // 게시글 상세
    @Override
    @Transactional(readOnly = true)
    public PostResponseDTO getPostDetail(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 이 게시글에 연결된 첨부파일 목록 조회
        Long ownerTypeId = getPostOwnerTypeId(); // OT7
        List<AttachmentFileResponseDTO> attachmentFiles =
                attachmentFileService.listFiles(ownerTypeId, postId);

        // 게시글 + 첨부파일 함께 내려줌
        return PostResponseDTO.toDTO(post, attachmentFiles);
    }
}
