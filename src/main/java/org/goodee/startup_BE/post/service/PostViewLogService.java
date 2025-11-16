package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.post.dto.PostViewLogRequestDTO;
import org.goodee.startup_BE.post.dto.PostViewLogResponseDTO;

public interface PostViewLogService {

    // 조회 로그 저장
    PostViewLogResponseDTO createPostViewLog(PostViewLogRequestDTO postViewLogRequestDTO, Long employeeId);

    // 조회수
    long getViewCount(Long postId);

}
