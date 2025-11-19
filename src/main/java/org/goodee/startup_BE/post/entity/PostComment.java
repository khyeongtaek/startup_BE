package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_post_comment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id",  nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Column(name = "content", nullable = false,columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PostComment createPostComment (
            Post post,
            Employee employee,
            String content
    ) {
        return PostComment.builder()
                .post(post)
                .employee(employee)
                .content(content)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 댓글 수정
    public void update (String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    // 댓글 삭제
    public void  delete () {
        this.isDeleted = true;
        this.updatedAt = LocalDateTime.now();
    }

}
