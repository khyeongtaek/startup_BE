package org.goodee.startup_BE.employee.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @Column(nullable = false)
    @Comment("이력 고유 ID")
    private Long historyId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    @Comment("로그인을 시도한 아이디")
    private String username;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
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
    private Employee employee;

    // --- 생성 팩토리 메서드 ---
    public static LoginHistory createLoginHistory(
            Long historyId, String username, String ipAddress,
            CommonCode loginStatus, String userAgent, Employee employee
    ) {
        LoginHistory history = new LoginHistory();
        history.historyId = historyId;
        history.username = username;
        history.ipAddress = ipAddress;
        history.loginStatus = loginStatus;
        history.userAgent = userAgent;
        history.employee = employee;
        history.loginTimestamp = LocalDateTime.now();
        return history;
    }
}