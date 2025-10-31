package org.goodee.startup_BE.employee.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Comment("이력 고유 ID")
    private Long historyId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("로그인을 시도한 아이디")
    private String username;

    @Column(nullable = false)
    @Comment("로그인 시도 시간")
    private LocalDateTime loginTimestamp;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("접속 IP 주소")
    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @Comment("로그인 상태")
    private CommonCode loginStatus;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("사용자 클라이언트 정보")
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @Comment("로그인 성공 시 참조할 직원 ID")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Employee employee;

    // --- 생성 팩토리 메서드 ---
    public static LoginHistory createLoginHistory(
            String username, String ipAddress,
            String userAgent, CommonCode status
    ) {
        LoginHistory history = new LoginHistory();
        history.username = username;
        history.ipAddress = ipAddress;
        history.userAgent = userAgent;
        history.loginStatus = status;
        history.loginTimestamp = LocalDateTime.now();
        return history;
    }

    public void updateEmployee(CommonCode loginStatus, Employee employee) {
        this.loginStatus = loginStatus;
        this.employee = employee;
    }
}