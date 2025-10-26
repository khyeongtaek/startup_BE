package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_post")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@ToString
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_category_id", nullable = false)
    private PostCategory postCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "title", nullable = false, columnDefinition = "LONGTEXT")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_notification", nullable = false)
    private Boolean isNotification;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Post createPost (
            PostCategory postCategory,
            Employee employee,
            String title,
            String content,
            boolean isNotification
    ) {
        return Post.builder()
                .postCategory(postCategory)
                .employee(employee)
                .title(title)
                .content(content)
                .isNotification(isNotification)
                .build();
    }

    // 게시글 수정
    public void update (String title, String content, boolean isNotification) {
        this.title = title;
        this.content = content;
        this.isNotification = isNotification;
    }

    // 게시글 삭제
    public void delete() {
        this.isDeleted = true;
    }

}
