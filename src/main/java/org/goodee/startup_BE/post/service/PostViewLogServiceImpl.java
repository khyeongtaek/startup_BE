package org.goodee.startup_BE.post.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostViewLogRequestDTO;
import org.goodee.startup_BE.post.dto.PostViewLogResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostViewLog;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.goodee.startup_BE.post.repository.PostViewLogRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PostViewLogServiceImpl implements PostViewLogService {

    private final PostViewLogRepository postViewLogRepository;
    private final PostRepository postrepository;
    private final EmployeeRepository employeeRepository;

    // 조회 로그 저장(게시글을 보면 +1 되게끔)
    @Override
    public PostViewLogResponseDTO createPostViewLog(PostViewLogRequestDTO postViewLogRequestDTO, Long employeeId) {

        // 게시글 조회
        Post post = postrepository.findById(postViewLogRequestDTO.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        // 직원 조회
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));

        // 작성자가 본인의 글 보면 조회수 증가 X
        if (post.getEmployee().getEmployeeId().equals(employeeId)) {
            return null;
        }

        // 이미 조회한 기록이 있다면 추가 저장 X
        boolean alreadyViewed = postViewLogRepository.existsByPost_PostIdAndEmployee_EmployeeId(
                post.getPostId(), employee.getEmployeeId()
        );

        if (alreadyViewed) { return null; }

        // 조회 기록
        PostViewLog postViewLog = PostViewLog.createPostViewLog(post, employee);

        postViewLogRepository.save(postViewLog);

        return PostViewLogResponseDTO.toDTO(postViewLog);
    }

    // 조회수 (저장된 기록의 총 개수)
    @Override
    public long getViewCount(Long postId) {

        return postViewLogRepository.countByPost_PostId(postId);
    }
}
