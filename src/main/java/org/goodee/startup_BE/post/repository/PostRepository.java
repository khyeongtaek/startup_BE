package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 검색
    @Query("""
    SELECT p FROM Post p
    JOIN p.employee e
    WHERE p.isDeleted = false
    AND (:categoryId IS NULL OR p.commonCode.commonCodeId = :categoryId)
    AND (
        (:title IS NULL AND :content IS NULL AND :employeeName IS NULL)
        OR (:title IS NOT NULL AND p.title LIKE CONCAT('%', :title, '%'))
        OR (:content IS NOT NULL AND p.content LIKE CONCAT('%', :content, '%'))
        OR (:employeeName IS NOT NULL AND e.name LIKE CONCAT('%', :employeeName, '%'))
    )
    """)
    Page<Post> searchPost(@Param("categoryId") Long categoryId,
                          @Param("title") String title,
                          @Param("content") String content,
                          @Param("employeeName") String employeeName,
                          Pageable pageable);

}
