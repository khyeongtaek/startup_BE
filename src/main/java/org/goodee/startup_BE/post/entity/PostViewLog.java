package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_post_view_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_view_log_id")
    private Long postViewLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public static PostViewLog createPostViewLog(Post post, Employee employee) {
        return PostViewLog.builder()
                .post(post)
                .employee(employee)
                .viewedAt(LocalDateTime.now())
                .build();
    }
}
