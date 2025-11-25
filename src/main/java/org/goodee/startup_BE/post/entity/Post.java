package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
    private CommonCode commonCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    @Column(name = "employee_name", nullable = false, columnDefinition = "LONGTEXT")
    private String employeeName;

    @Column(name = "title", nullable = false, columnDefinition = "LONGTEXT")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_notification", nullable = false)
    private Boolean isNotification;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "alert", nullable = false)
    @Builder.Default
    private Boolean alert = false;

    @Column(name = "view_count", nullable = false)
    @Builder.Default // Lombok @Builder 사용 시 기본값을 유지하기 위함.
    private Long viewCount = 0L;


    public static Post create(CommonCode commonCode, Employee employee, String title, String content, Boolean isNotification, Boolean alert) {
        return Post.builder()
                .commonCode(commonCode)
                .employee(employee)
                .employeeName(employee.getName())
                .title(title)
                .content(content)
                .isNotification(isNotification != null ? isNotification : false)
                .alert(alert != null ? alert : false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // 게시글 수정
    public void update (String title, String content, boolean isNotification) {
        this.title = title;
        this.content = content;
        this.isNotification = isNotification;
        this.updatedAt = LocalDateTime.now();
    }

    // 게시글 삭제
    public void delete() {
        this.isDeleted = true;
    }

}
