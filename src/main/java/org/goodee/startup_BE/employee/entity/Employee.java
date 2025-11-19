package org.goodee.startup_BE.employee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "tbl_employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "직원 엔티티")
public class Employee implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id", nullable = false)
    @Comment("직원 고유 ID")
    @Schema(description = "직원 고유 ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long employeeId;

    @Column(nullable = false, unique = true)
    @Comment("로그인 아이디")
    @Schema(description = "로그인 아이디", example = "user123")
    private String username;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("암호화된 비밀번호")
    @Schema(description = "암호화된 비밀번호", example = "password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("이름")
    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Lob
    @Column(nullable = false, unique = true, columnDefinition = "LONGTEXT")
    @Comment("이메일")
    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("연락처")
    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;

    @Column(nullable = false)
    @Comment("입사일")
    @Schema(description = "입사일", example = "2024-01-01")
    private LocalDate hireDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    @Comment("재직 상태")
    @Schema(description = "재직 상태 (CommonCode)")
    private CommonCode status;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("프로필 이미지")
    @Schema(description = "프로필 이미지 URL", example = "default_profile.png")
    private String profileImg;

    @Column(nullable = false)
    @ColumnDefault("true")
    @Comment("초기비밀번호 여부")
    @Schema(description = "초기비밀번호 여부", example = "true")
    private Boolean isInitialPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("소속 부서")
    @Schema(description = "소속 부서 (CommonCode)")
    private CommonCode department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("직급")
    @Schema(description = "직급 (CommonCode)")
    private CommonCode position;

    // security의 권한 조회 시점이 db연결이 끊긴 후라 Lazy 사용안함.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    @Comment("권한")
    @Schema(description = "권한 (CommonCode)")
    private CommonCode role;

    @Column(nullable = false)
    @Comment("생성일")
    @Schema(description = "생성일", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일")
    @Schema(description = "수정일", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", updatable = false)
    @Comment("생성자")
    @Schema(description = "생성자 (Employee)", accessMode = Schema.AccessMode.READ_ONLY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    @Comment("수정자")
    @Schema(description = "수정자 (Employee)", accessMode = Schema.AccessMode.READ_ONLY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee updater;


    // --- 생성 팩토리 메서드 ---
    public static Employee createEmployee(
            String username, String name, String email
            , String phoneNumber, LocalDate hireDate, CommonCode status
            , CommonCode role, CommonCode department, CommonCode position
            , Employee creator
    ) {
        Employee employee = new Employee();
        employee.username = username;

        employee.name = name;
        employee.email = email;
        employee.phoneNumber = phoneNumber;
        employee.hireDate = hireDate;
        employee.status = status;
        employee.role = role;
        employee.department = department;
        employee.position = position;
        employee.creator = creator;
        return employee;
    }

    public void update(
            String phoneNumber, CommonCode status, String profileImg, CommonCode role, CommonCode department, CommonCode position
            , Employee updater
    ) {
        updatePhoneNumber(phoneNumber, updater);
        updateStatus(status, updater);
        updateProfileImg(profileImg, updater);
        updateRole(role, updater);
        updateDepartment(department, updater);
        updatePosition(position, updater);
        this.updater = updater;
    }

    public void updatePhoneNumber(String phoneNumber, Employee updater) {
        this.phoneNumber = phoneNumber;
        this.updater = updater;
    }

    public void updateStatus(CommonCode status, Employee updater) {
        this.status = status;
        this.updater = updater;
    }

    public void updateProfileImg(String profileImg, Employee updater) {
        this.profileImg = profileImg;
        this.updater = updater;
    }


    public void updateRole(CommonCode role, Employee updater) {
        this.role = role;
        this.updater = updater;
    }

    public void updateDepartment(CommonCode department, Employee updater) {
        this.department = department;
        this.updater = updater;
    }

    public void updatePosition(CommonCode position, Employee updater) {
        this.position = position;
        this.updater = updater;
    }

    public void updatePassword(String password, Employee updater) {
        this.password = password;
        this.isInitialPassword = false;
        this.updater = updater;
    }

    public void updateInitPassword(String password, Employee updater) {
        this.password = password;
        this.isInitialPassword = true;
        this.updater = updater;
    }

    @PrePersist
    protected void onPrePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getValue1()));
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}