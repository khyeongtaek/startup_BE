package org.goodee.startup_BE.notification.service;

import jakarta.persistence.EntityNotFoundException;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationCountDTO;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.goodee.startup_BE.notification.entity.Notification;
import org.goodee.startup_BE.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;
// BDDMockito static import
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    // 테스트용 공통 객체
    private Employee mockEmployee;
    private CommonCode mockOwnerType;
    private Notification mockNotification;
    private String testUsername = "testUser";
    private Long testEmployeeId = 1L;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화
        mockEmployee = mock(Employee.class);
        mockOwnerType = mock(CommonCode.class);
        mockNotification = mock(Notification.class);
        testTime = LocalDateTime.now();

        // 공통 Mocking 설정 - lenient()를 적용하여 테스트 간 충돌 방지
        lenient().when(mockEmployee.getUsername()).thenReturn(testUsername);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(testEmployeeId);

        // DTO 변환(toDTO) 시 사용되는 핵심 Mocking
        // NotificationResponseDTO.toDTO는
        // notification.getOwnerType().getValue1()을 호출합니다.
        lenient().when(mockNotification.getOwnerType()).thenReturn(mockOwnerType);
        lenient().when(mockOwnerType.getValue1()).thenReturn("MAIL"); //

        // DTO 변환에 필요한 나머지 getter들
        lenient().when(mockNotification.getNotificationId()).thenReturn(1L);
        lenient().when(mockNotification.getUrl()).thenReturn("/mail/1");
        lenient().when(mockNotification.getTitle()).thenReturn("테스트 제목");
        lenient().when(mockNotification.getContent()).thenReturn("테스트 내용");
        lenient().when(mockNotification.getCreatedAt()).thenReturn(testTime);
        lenient().when(mockNotification.getReadAt()).thenReturn(null); // toDTO는 null을 false로 변환
    }


    @Nested
    @DisplayName("create (알림 생성)")
    class CreateNotification {

        private NotificationRequestDTO requestDTO;
        private Long ownerTypeCodeId = 101L;

        @BeforeEach
        void createSetup() {
            requestDTO = new NotificationRequestDTO(
                    testEmployeeId,
                    ownerTypeCodeId,
                    "/mail/1",
                    "테스트 제목",
                    "테스트 내용"
            );
        }

        @Test
        @DisplayName("성공 - DTO 반환 및 WebSocket 메시지 2개 전송")
        void create_Success() {
            // given: 사원 조회, CommonCode 조회, 알림 저장 Mocking
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findById(ownerTypeCodeId)).willReturn(Optional.of(mockOwnerType));

            // [FIX] Notification.createNotification이 static이므로, save될 엔티티를 직접 mock
            // (toDTO에 필요한 모든 getter는 @BeforeEach에서 이미 stubbing됨)
            given(notificationRepository.save(any(Notification.class))).willReturn(mockNotification);

            // sendNotificationCounts 내부 Mocking (WebSocket 검증용)
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(1L); // 방금 생성됨

            // ArgumentCaptor 준비
            ArgumentCaptor<NotificationCountDTO> countDTOCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);
            ArgumentCaptor<NotificationResponseDTO> responseDTOCaptor = ArgumentCaptor.forClass(NotificationResponseDTO.class);

            // when: 알림 생성 실행
            NotificationResponseDTO resultDTO = notificationService.create(requestDTO);

            // then: 결과 검증 (NotificationResponseDTO.toDTO의 로직 기반)
            assertThat(resultDTO).isNotNull();
            assertThat(resultDTO.getTitle()).isEqualTo("테스트 제목");
            assertThat(resultDTO.getOwnerType()).isEqualTo("MAIL"); // getValue1()
            assertThat(resultDTO.getReadAt()).isFalse(); // toDTO 로직 (null -> false)

            // Repository 호출 검증
            verify(employeeRepository, times(1)).findById(testEmployeeId);
            verify(commonCodeRepository, times(1)).findById(ownerTypeCodeId);
            verify(notificationRepository, times(1)).save(any(Notification.class));

            // [FIX] WebSocket 호출 검증 (총 2회)
            // 1. sendNotificationCounts가 호출한 /queue/notifications 검증
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/notifications"),
                    countDTOCaptor.capture() // DTO 캡처
            );
            assertThat(countDTOCaptor.getValue().getUnreadCount()).isEqualTo(1L);

            // 2. [신규] create가 직접 호출한 /queue/new-notifications 검증
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/new-notifications"),
                    responseDTOCaptor.capture() // DTO 캡처
            );
            // 전송된 DTO가 toDTO로 생성된 resultDTO와 동일한지 확인
            assertThat(responseDTOCaptor.getValue()).isSameAs(resultDTO);
            assertThat(responseDTOCaptor.getValue().getNotificationId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패 - 사원 없음 (ID 조회)")
        void create_Fail_UserNotFound() {
            // given
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.create(requestDTO))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("존재 하지 않는 사원 입니다");
        }

        @Test
        @DisplayName("실패 - CommonCode 없음")
        void create_Fail_CommonCodeNotFound() {
            // given
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findById(ownerTypeCodeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.create(requestDTO))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("유효하지 않은 알림 출처 CommonCode Id 입니다");
        }
    }


    @Nested
    @DisplayName("list (목록 조회)")
    class ListNotification {

        @Test
        @DisplayName("성공")
        void list_Success() {
            // given: Pageable 객체 및 Repository 반환값 설정
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

            // DTO 변환에 필요한 Mocking (@BeforeEach에서 이미 설정됨)
            // (getOwnerType, getValue1, getReadAt, getCreatedAt)

            List<Notification> notificationList = List.of(mockNotification);
            Page<Notification> mockPage = new PageImpl<>(notificationList, pageable, notificationList.size());

            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable))
                    .willReturn(mockPage);

            // when: 목록 조회 실행
            // service의 .map(NotificationResponseDTO::toDTO)가 실행됨
            Page<NotificationResponseDTO> resultPage = notificationService.list(testUsername, pageable);

            // then: 결과 검증
            verify(employeeRepository, times(1)).findByUsername(testUsername);
            verify(notificationRepository, times(1)).findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable);
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            // toDTO 로직 검증
            assertThat(resultPage.getContent().get(0).getOwnerType()).isEqualTo("MAIL");
            assertThat(resultPage.getContent().get(0).getReadAt()).isFalse();
            assertThat(resultPage.getContent().get(0).getCreatedAt()).isEqualTo(testTime);
        }

        @Test
        @DisplayName("실패 - 사원 없음 (username 조회)")
        void list_Fail_UserNotFound() {
            // given
            Pageable pageable = PageRequest.of(0, 5);
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.list(testUsername, pageable))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("존재 하지 않는 사원 입니다");
        }
    }

    @Nested
    @DisplayName("checkRole (공통 메서드 - getUrl, softDelete에서 호출)")
    class CheckRole {

        private Long notificationId = 1L;

        @Test
        @DisplayName("실패 - 알림 없음")
        void checkRole_Fail_NotificationNotFound() {
            // given
            given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

            // when & then (getUrl로 테스트)
            assertThatThrownBy(() -> notificationService.getUrl(notificationId, testUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 알림입니다.");
        }

        @Test
        @DisplayName("실패 - 권한 없음 (소유자 불일치)")
        void checkRole_Fail_AccessDenied() {
            // given
            Employee anotherEmployee = mock(Employee.class);
            given(anotherEmployee.getUsername()).willReturn("anotherUser"); // 다른 소유자

            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(anotherEmployee);

            // when & then (softDelete로 테스트)
            assertThatThrownBy(() -> notificationService.softDelete(notificationId, testUsername))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("해당 알림에 접근할 권한이 없습니다.");
        }

        @Test
        @DisplayName("실패 - 이미 삭제됨")
        void checkRole_Fail_AlreadyDeleted() {
            // given
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee); // 소유자는 맞음
            given(mockNotification.getIsDeleted()).willReturn(true);    // 이미 삭제됨

            // when & then (getUrl로 테스트)
            assertThatThrownBy(() -> notificationService.getUrl(notificationId, testUsername))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 삭제된 알림입니다.");
        }
    }

    @Nested
    @DisplayName("getUrl (알림 읽음 및 URL 반환)")
    class GetUrl {
        @Test
        @DisplayName("성공")
        void getUrl_Success() {
            // given: checkRole 성공 및 URL 설정
            Long notificationId = 1L;
            String expectedUrl = "/mail/1";
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee); // checkRole 통과
            given(mockNotification.getIsDeleted()).willReturn(false);      // checkRole 통과
            given(mockNotification.getUrl()).willReturn(expectedUrl);

            // sendNotificationCounts 내부 Mocking
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(4L); // 1개 읽음

            // ArgumentCaptor 준비
            ArgumentCaptor<NotificationCountDTO> countDTOCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);

            // when: getUrl 실행
            String resultUrl = notificationService.getUrl(notificationId, testUsername);

            // then:
            // 1. 엔티티 메서드 호출 검증
            verify(mockNotification, times(1)).readNotification(); //
            // 2. URL 반환 검증
            assertThat(resultUrl).isEqualTo(expectedUrl);

            // 3. WebSocket 호출 검증 (sendNotificationCounts)
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/notifications"),
                    countDTOCaptor.capture()
            );

            // 4. 캡처된 DTO 내용 검증
            NotificationCountDTO capturedDTO = countDTOCaptor.getValue();
            assertThat(capturedDTO.getUnreadCount()).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("softDelete (알림 단일 삭제)")
    class SoftDelete {
        @Test
        @DisplayName("성공")
        void softDelete_Success() {
            // given: checkRole 성공
            Long notificationId = 1L;
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee); // checkRole 통과
            given(mockNotification.getIsDeleted()).willReturn(false);      // checkRole 통과

            // sendNotificationCounts 내부 Mocking
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(5L);

            // ArgumentCaptor 준비
            ArgumentCaptor<NotificationCountDTO> countDTOCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);

            // when: softDelete 실행
            notificationService.softDelete(notificationId, testUsername);

            // then:
            // 1. 엔티티 메서드 호출 검증
            verify(mockNotification, times(1)).deleteNotification(); //

            // 2. WebSocket 호출 검증 (sendNotificationCounts)
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/notifications"),
                    countDTOCaptor.capture()
            );

            // 3. 캡처된 DTO 내용 검증
            NotificationCountDTO capturedDTO = countDTOCaptor.getValue();
            assertThat(capturedDTO.getUnreadCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("getUnreadNotiCount (읽지 않은 알림 개수)")
    class GetUnreadCount {
        @Test
        @DisplayName("성공")
        void getUnreadNotiCount_Success() {
            // given:
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .willReturn(5L);

            // when: 개수 조회 실행
            long count = notificationService.getUnreadNotiCount(testUsername);

            // then: 결과 검증
            assertThat(count).isEqualTo(5L);
            verify(employeeRepository, times(1)).findByUsername(testUsername);
            verify(notificationRepository, times(1)).countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername);
        }

        @Test
        @DisplayName("실패 - 사원 없음")
        void getUnreadNotiCount_Fail_UserNotFound() {
            // given
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.getUnreadNotiCount(testUsername))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 사원 입니다.");
        }
    }

    @Nested
    @DisplayName("readAll (모든 알림 읽음 처리)")
    class ReadAll {
        @Test
        @DisplayName("성공")
        void readAll_Success() {
            // given
            Notification noti1 = mock(Notification.class);
            Notification noti2 = mock(Notification.class);
            List<Notification> unreadList = List.of(noti1, noti2);

            given(notificationRepository.findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .willReturn(unreadList);

            // sendNotificationCounts 내부 Mocking
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(0L); // 모두 읽음

            // ArgumentCaptor 준비
            ArgumentCaptor<NotificationCountDTO> countDTOCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);

            // when
            notificationService.readAll(testUsername);

            // then
            // 1. 엔티티 메서드 호출 검증
            verify(noti1, times(1)).readNotification(); //
            verify(noti2, times(1)).readNotification(); //

            // 2. WebSocket 호출 검증
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/notifications"),
                    countDTOCaptor.capture()
            );

            // 3. 캡처된 DTO 내용 검증
            NotificationCountDTO capturedDTO = countDTOCaptor.getValue();
            assertThat(capturedDTO.getUnreadCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("softDeleteAll (모든 알림 삭제)")
    class SoftDeleteAll {
        @Test
        @DisplayName("성공")
        void softDeleteAll_Success() {
            // given
            Notification noti1 = mock(Notification.class);
            Notification noti2 = mock(Notification.class);
            List<Notification> allList = List.of(noti1, noti2);

            given(notificationRepository.findByEmployeeUsernameAndIsDeletedFalse(testUsername))
                    .willReturn(allList);

            // sendNotificationCounts 내부 Mocking
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(0L); // 삭제시 읽지않은수 0

            // ArgumentCaptor 준비
            ArgumentCaptor<NotificationCountDTO> countDTOCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);

            // when
            notificationService.softDeleteAll(testUsername);

            // then
            // 1. 엔티티 메서드 호출 검증
            verify(noti1, times(1)).deleteNotification(); //
            verify(noti2, times(1)).deleteNotification(); //

            // 2. WebSocket 호출 검증
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),
                    eq("/queue/notifications"),
                    countDTOCaptor.capture()
            );

            // 3. 캡처된 DTO 내용 검증
            NotificationCountDTO capturedDTO = countDTOCaptor.getValue();
            assertThat(capturedDTO.getUnreadCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("sendNotificationCounts (공통 메서드 - WebSocket 전송)")
    class SendNotificationCounts {

        @Test
        @DisplayName("성공")
        void sendNotificationCounts_Success() {
            // given
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername)).willReturn(3L);

            // ArgumentCaptor: DTO를 캡처하여 검증하기 위함
            ArgumentCaptor<NotificationCountDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationCountDTO.class);

            // when
            notificationService.sendNotificationCounts(testUsername);

            // then
            // 1. simpMessagingTemplate이 정확한 인자들로 호출되었는지 검증
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername),           // username
                    eq("/queue/notifications"), // 경로
                    dtoCaptor.capture()         // DTO 캡처
            );

            // 2. 캡처된 DTO의 내용 검증
            NotificationCountDTO capturedDTO = dtoCaptor.getValue();
            assertThat(capturedDTO).isNotNull();
            assertThat(capturedDTO.getUnreadCount()).isEqualTo(3L);
        }
    }
}