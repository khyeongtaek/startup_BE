package org.goodee.startup_BE.post.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table (name = "tbl_post_category")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "post_category_id")
    private Long postCategoryId;

    @Column(name = "name", nullable = false, columnDefinition = "LONGTEXT")
    private String name;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(name = "role_id", nullable = false)
    private int roleId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private String isDeleted;

}
