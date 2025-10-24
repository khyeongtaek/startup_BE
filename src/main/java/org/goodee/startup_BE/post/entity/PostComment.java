package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "content", nullable = false,columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static PostComment createPostComment (
            Post post,
            Employee employee,
            String content,
            boolean isDeleted
    ) {
        return PostComment.builder()
                .post(post)
                .employee(employee)
                .content(content)
                .isDeleted(false)
                .build();
    }

    // 댓글 수정
    public void update (String content) {
        this.content = content;
    }

    // 댓글 삭제
    public void  delete () {
        this.isDeleted = true;
    }

}
