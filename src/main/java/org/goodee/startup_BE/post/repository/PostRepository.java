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

            SELECT p
          FROM Post p
          LEFT JOIN p.employee e
         WHERE p.isDeleted = false
           AND p.commonCode.code = :categoryCode
           AND (
                 (:keyword IS NULL OR :keyword = '' )
                 OR p.title LIKE %:keyword%
                 OR p.content LIKE %:keyword%
               )
         ORDER BY p.createdAt DESC
        """)
    Page<Post> searchPost(
            @Param("categoryCode") String categoryCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    }

