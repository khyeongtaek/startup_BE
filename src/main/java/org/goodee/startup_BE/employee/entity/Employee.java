package org.goodee.startup_BE.employee.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// 다른 패키지의 엔티티 임포트
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
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
public class Employee implements UserDetails{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("직원 고유 ID")
    private Long employeeId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("로그인 아이디")
    private String username;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("암호화된 비밀번호")
    private String password;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("이름")
    private String name;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("이메일")
    private String email;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Comment("연락처")
    private String phoneNumber;

    @Column(nullable = false)
    @Comment("입사일")
    private LocalDate hireDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    @Comment("재직 상태")
    private CommonCode status;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("프로필 이미지")
    private String profileImg;

    @Column(nullable = false)
    @ColumnDefault("true")
    @Comment("초기비밀번호 여부")
    private Boolean isInitialPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("소속 부서")
    private CommonCode department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("직급")
    private CommonCode position;

    // security의 권한 조회 시점이 db연결이 끊긴 후라 Lazy 사용안함.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    @Comment("권한")
    private CommonCode role;

    @Column(nullable = false)
    @Comment("생성일")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정일")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", updatable = false)
    @Comment("생성자")
    private Employee creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updater_id")
    @Comment("수정자")
    private Employee updater;



    // --- 생성 팩토리 메서드 ---
    public static Employee createEmployee(
            String username, String name, String email
            , String phoneNumber, LocalDate hireDate, CommonCode status , String profileImg
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
        employee.profileImg = profileImg == null ? "default_profile.png" : profileImg;
        employee.role = role;
        employee.department = department;
        employee.position = position;
        employee.creator = creator;
        return employee;
    }

    public void update(String username
            , String phoneNumber, CommonCode status, String profileImg, CommonCode role, CommonCode department, CommonCode position
            , Employee updater
    ) {
        this.username = username;
        this.phoneNumber = phoneNumber;
        updateStatus(status, updater);
        updateProfileImg(profileImg, updater);
        updateRole(role, updater);
        updateDepartment(department, updater);
        updatePosition(position, updater);
        this.updater = updater;
    }

    public void updateStatus(CommonCode status, Employee updater) {
        this.status = status;
        this.updater = updater;
    }

    public void updateProfileImg(String profileImg, Employee updater) {
        this.profileImg = profileImg == null ? "default_profile.png" : profileImg;
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
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getValue1()));
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}