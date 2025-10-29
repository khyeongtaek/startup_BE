package org.goodee.startup_BE.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NotificationActionDTO
 * STOMP 메시징 환경에서 이 DTO를 따로 사용하는 주된 이유는 메시지의 경량화와 명확한 목적 때문
 * 경량화: Rest API를 사용해 알림을 생성 할 때는 많은 정보가 필요하지만,
 * STOMP를 통해 삭제 명령 같은 경우는 해당 알림의 ID외에 다른 정보는 담을 필요가 없어 DTO를 최소화 하는 목적
 * 재사용성: 단일 알림을 대상으로 하는 모든 STOMP Action(단일 읽음, 단일 삭제 등)에서 재사용될 수 있어 코드가 깔끔해진다.
 */

@Getter
@Setter
@NoArgsConstructor
public class NotificationActionDTO {

    private Long notificationId; // 읽음, 삭제할 알림Id

}
