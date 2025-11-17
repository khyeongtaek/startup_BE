package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {

  // 게시글 검색
  List<PostResponseDTO> searchPost(PostRequestDTO postRequestDTO, Pageable pageable);

  // 게시글 생성
  PostResponseDTO createPost(PostRequestDTO postRequestDTO);

  // 게시글 수정
  PostResponseDTO updatePost(PostRequestDTO postRequestDTO);

  // 게시글 삭제
  void deletePost(Long postId);

}
