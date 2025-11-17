package org.goodee.startup_BE.post.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;

    // 게시글 검색
    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDTO> searchPost(PostRequestDTO dto, Pageable pageable) {
        Page<Post> postPage =
                postRepository.searchPost(dto.getCommonCodeId(), dto.getTitle(), dto.getContent(), dto.getEmployeeName(), pageable);

        return postPage.getContent()
                .stream()
                .map(PostResponseDTO::toDTO)
                .collect(Collectors.toList());
    }

    // 게시글 생성
    @Override
    public PostResponseDTO createPost(PostRequestDTO dto) {

        CommonCode commonCode = commonCodeRepository.findById(dto.getCommonCodeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판 ID입니다."));

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작성자입니다."));

        Post post = Post.builder()
                .commonCode(commonCode)
                .employee(employee)
                .employeeName(dto.getEmployeeName())   // ★ 반드시 포함
                .title(dto.getTitle())
                .content(dto.getContent())
                .isNotification(dto.getIsNotification() != null ? dto.getIsNotification() : false)
                .alert(dto.getAlert() != null ? dto.getAlert() : false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return PostResponseDTO.toDTO(postRepository.save(post));
    }

    // 게시글 수정
    @Override
    public PostResponseDTO updatePost(PostRequestDTO dto) {

        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.getIsDeleted())
            throw new IllegalStateException("삭제된 게시글입니다.");

        post.update(
                dto.getTitle(),
                dto.getContent(),
                dto.getIsNotification()
        );

        return PostResponseDTO.toDTO(post);
    }

    // 게시글 삭제
    @Override
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        post.delete();
    }
}
