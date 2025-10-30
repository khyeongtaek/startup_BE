package org.goodee.startup_BE.post.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

  private final PostRepository postRepository;
  private final CommonCodeRepository commonCodeRepository;

  // 전체 게시글 (삭제되지 않는)
  @Override
  public List<PostResponseDTO> getAllPosts() {
    return List.of();
  }

  // 카테고리별 조회
  @Override
  public List<PostResponseDTO> getPostsByCategory(Long commonCodeId) {
    return List.of();
  }

  // 작성자 기준 조회
  @Override
  public List<PostResponseDTO> getPostsByEmployee(Long employeeId) {
    return List.of();
  }

  // 제목 조회
  @Override
  public List<PostResponseDTO> searchPostsByTitle(String title) {
    return List.of();
  }

  // 공지글 조회
  @Override
  public List<PostResponseDTO> getNotificationPosts() {
    return List.of();
  }

  // 일반글 조회
  @Override
  public List<PostResponseDTO> getNormalPosts() {
    return List.of();
  }

  // 게시글 생성
  @Override
  public PostResponseDTO createPost(PostRequestDTO postRequestDTO) {
    return null;
  }

  // 게시글 수정
  @Override
  public PostResponseDTO updatePost(Long postId, PostRequestDTO postRequestDTO) {
    return null;
  }

  // 게시글 삭제
  @Override
  public boolean deletePost(Long postId) {
    return false;
  }
}
