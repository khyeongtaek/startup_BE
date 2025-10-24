package org.goodee.startup_BE.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_common_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("공통 코드 관리 테이블")
public class CommonCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "common_code_id")
    @Comment("PK")
    private Long commonCodeId;

    @Column(name = "code", nullable = false, unique = true, columnDefinition = "LONGTEXT")
    @Comment("코드 (비즈니스 키)")
    private String code;

    @Column(name = "code_description", nullable = false, columnDefinition = "LONGTEXT")
    @Comment("코드설명")
    private String codeDescription;

    @Column(columnDefinition = "LONGTEXT")
    @Comment("참조값1")
    private String value1;

    @Column(columnDefinition = "LONGTEXT")
    @Comment("참조값2")
    private String value2;

    @Column(columnDefinition = "LONGTEXT")
    @Comment("참조값3")
    private String value3;

    @Comment("우선순위 (정렬용)")
    private Long sortOrder;

    //추후 Employee entity 추가시 ManyToOne 연결 예정
//    @Comment("작성자 (직원 ID)")
//    @JoinColumn(name = "employee_id", nullable = true)
//    @ManyToOne(fetch = FetchType.LAZY)
//    private Employee employee;

    @Column(nullable = false, updatable = false)
    @Comment("생성일")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Comment("삭제여부")
    private boolean isDeleted = false;

    public static CommonCode createCommonCode(
            String code
            , String codeDescription
            , String value1
            , String value2
            , String value3
            , Long sortOrder
//            , Employee employee
    ) {
        CommonCode commonCode = new CommonCode();
        commonCode.code = code;
        commonCode.codeDescription = codeDescription;
        commonCode.value1 = value1;
        commonCode.value2 = value2;
        commonCode.value3 = value3;
        commonCode.sortOrder = sortOrder;
//        commonCode.employee = employee;
        commonCode.createdAt = LocalDateTime.now();
        commonCode.updatedAt = LocalDateTime.now();
        return commonCode;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void update(
            String code
            , String codeDescription
            , String value1
            , String value2
            , String value3
            , Long sortOrder
    ){
        this.code = code;
        this.codeDescription = codeDescription;
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.sortOrder = sortOrder;
//        commonCode.employee = employee;
        this.updatedAt = LocalDateTime.now();

    }

}