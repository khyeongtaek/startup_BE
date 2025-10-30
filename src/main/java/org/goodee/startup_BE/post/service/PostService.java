package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;

import java.util.List;

public interface PostService {

  // 전체 게시글 (삭제 X)
  List<PostResponseDTO> getAllPosts();

  // 카테고리별 조회
  List<PostResponseDTO> getPostsByCategory(Long commonCodeId);

  // 작성자 기준 조회
  List<PostResponseDTO> getPostsByEmployee(Long employeeId);

  // 제목 조회
  List<PostResponseDTO> searchPostsByTitle(String title);

  // 공지글 조회
  List<PostResponseDTO> getNotificationPosts();

  // 일반글 조회
  List<PostResponseDTO> getNormalPosts();

  // 게시글 생성
  PostResponseDTO createPost(PostRequestDTO postRequestDTO);

  // 게시글 수정
  PostResponseDTO updatePost(Long postId,PostRequestDTO postRequestDTO);

  // 게시글 삭제
  // 삭제는 결과 응답만으로 충분, 불필요한 DTO 방지
  boolean deletePost(Long postId);

}
