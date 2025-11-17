package org.goodee.startup_BE.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostViewLog;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostViewLogRequestDTO {

    private Long postId;

    public PostViewLog toEntity(Post post, Employee employee) {
        return PostViewLog.builder()
                .post(post)
                .employee(employee)
                .viewedAt(LocalDateTime.now())
                .build();

    }

}
