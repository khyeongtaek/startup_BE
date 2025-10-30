package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  // <#entity>

  // 카테고리 별 기준으로 조회
  List<Post> findByCommonCode_CommonCodeId(Long commonCodeId);

  // 작성자
  List<Post> findByEmployee_EmployeeId(Long employeeId);

  // 제목
  // findByTitleContaining - 제목(title)에 특정 단어가 포함된 게시글을 찾는다.
  List<Post> findByTitleContaining(String title);

  // 공지글만 조회
  List<Post> findByIsNotificationTrue();

  // 일반글(공지x)만 조회
  List<Post> findByIsNotificationFalse();

  // 삭제되지 않은 게시글만 조회
  List<Post> findByIsDeletedFalse();

  // 삭제되지 않은 게시글 최신순 정령
  List<Post> findByIsDeletedFalseOrderByCreatedAtDesc();

  // 공지글 - 최신순 정렬
  List<Post> findByIsNotificationTrueOrderByCreatedAtDesc();

  // 제목 검색 - 삭제되지 않은 게시글만
  List<Post> findByTitleContainingAndIsDeletedFalse(String title);

}
