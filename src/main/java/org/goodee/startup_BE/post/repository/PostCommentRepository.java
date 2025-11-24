package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.post.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    // 게시글의 댓글 목록
    Page<PostComment> findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtDesc(Long postId, Pageable pageable);


}
