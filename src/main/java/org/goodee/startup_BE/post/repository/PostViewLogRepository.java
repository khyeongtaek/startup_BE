package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.post.entity.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    // PostViewLog 테이블에서 해당 게시글의 조회 기록 개수를 세어 조회수를 반환한다.
    long countByPost_PostId(Long postId);

    // 사용자가 해당 게시글을 조회한 적이 있는지 확인
    boolean existsByPost_PostIdAndEmployee_EmployeeId(Long postId, Long employeeId);

    // 같은 사용자가 해당 게시글을 조회한 기록을 가져오기
    Optional<PostViewLog> findByPost_PostIdAndEmployee_EmployeeId(Long postId, Long employeeId);
}
