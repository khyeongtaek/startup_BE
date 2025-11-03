package org.goodee.startup_BE.notification.service;

import jakarta.persistence.EntityNotFoundException;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.dto.NotificationResponseDTO;
import org.goodee.startup_BE.notification.entity.Notification;
import org.goodee.startup_BE.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

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

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화
        mockEmployee = mock(Employee.class);
        mockOwnerType = mock(CommonCode.class);
        mockNotification = mock(Notification.class);

        // 공통 Mocking 설정 - lenient()를 적용하여 테스트 간 충돌 방지
        lenient().when(mockEmployee.getUsername()).thenReturn(testUsername);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(testEmployeeId);

        // DTO 변환(toDTO) 시 사용되는 핵심 Mocking
        lenient().when(mockNotification.getOwnerType()).thenReturn(mockOwnerType);
        // DTO가 getValue1()을 사용하므로 "MAIL"(영어)을 반환하도록 설정
        lenient().when(mockOwnerType.getValue1()).thenReturn("MAIL");
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
        @DisplayName("성공")
        void create_Success() {
            // given: 사원 조회, CommonCode 조회, 알림 저장 Mocking
            // lenient()를 사용하여 다른 테스트와의 Mocking 충돌을 방지합니다.
            lenient().when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(mockEmployee));
            lenient().when(commonCodeRepository.findById(ownerTypeCodeId)).thenReturn(Optional.of(mockOwnerType));

            // DTO 변환 및 WebSocket 푸시에 필요한 Mocking (setUp에서 이미 설정됨)
            // lenient().when(mockOwnerType.getValue1()).thenReturn("MAIL");
            // lenient().when(mockEmployee.getUsername()).thenReturn(testUsername);

            // toDTO에 전달될 mockNotification 객체의 필드 값들을 mocking
            lenient().when(mockNotification.getNotificationId()).thenReturn(1L);
            lenient().when(mockNotification.getUrl()).thenReturn(requestDTO.getUrl());
            lenient().when(mockNotification.getTitle()).thenReturn(requestDTO.getTitle());
            lenient().when(mockNotification.getContent()).thenReturn(requestDTO.getContent());
            lenient().when(mockNotification.getCreatedAt()).thenReturn(LocalDateTime.now());
            lenient().when(mockNotification.getReadAt()).thenReturn(null); // toDTO에서 false로 변환됨

            // save가 호출되면 mockNotification을 반환하도록 설정
            lenient().when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

            // when: 알림 생성 실행
            NotificationResponseDTO resultDTO = notificationService.create(requestDTO);

            // then: 결과 검증 (NotificationResponseDTO.toDTO의 로직 기반)
            assertThat(resultDTO).isNotNull();
            assertThat(resultDTO.getTitle()).isEqualTo("테스트 제목");

            // *** 수정된 부분 ***
            // DTO는 value1 ("MAIL")을 사용합니다.
            // 로그의 "메일"(codeDescription)은 잘못된 assertion입니다.
            assertThat(resultDTO.getOwnerType()).isEqualTo("MAIL");
            assertThat(resultDTO.getReadAt()).isFalse(); // toDTO 로직 (null -> false)

            // Repository 및 Template 호출 검증
            verify(employeeRepository, times(1)).findById(testEmployeeId);
            verify(commonCodeRepository, times(1)).findById(ownerTypeCodeId);
            verify(notificationRepository, times(1)).save(any(Notification.class));
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername), eq("/queue/noti"), any(NotificationResponseDTO.class)
            );
        }

        @Test
        @DisplayName("실패 - 사원 없음 (ID 조회)")
        void create_Fail_UserNotFound() {
            // given
            lenient().when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.create(requestDTO))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("존재 하지 않는 사원 입니다");
        }

        @Test
        @DisplayName("실패 - CommonCode 없음")
        void create_Fail_CommonCodeNotFound() {
            // given
            lenient().when(employeeRepository.findById(testEmployeeId)).thenReturn(Optional.of(mockEmployee));
            lenient().when(commonCodeRepository.findById(ownerTypeCodeId)).thenReturn(Optional.empty());

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

            // DTO 변환에 필요한 Mocking (setUp에서 이미 설정됨)
            // lenient().when(mockOwnerType.getValue1()).thenReturn("MAIL");
            // lenient().when(mockNotification.getOwnerType()).thenReturn(mockOwnerType);

            // 추가 Mocking (toDTO)
            lenient().when(mockNotification.getReadAt()).thenReturn(null); // readAt = false
            lenient().when(mockNotification.getCreatedAt()).thenReturn(LocalDateTime.now());

            List<Notification> notificationList = List.of(mockNotification);
            Page<Notification> mockPage = new PageImpl<>(notificationList, pageable, notificationList.size());

            // *** 수정된 부분 ***
            // 모든 given/when에 lenient() 적용
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockEmployee));
            lenient().when(notificationRepository.findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable))
                    .thenReturn(mockPage);

            // when: 목록 조회 실행
            Page<NotificationResponseDTO> resultPage = notificationService.list(testUsername, pageable);

            // then: 결과 검증
            verify(employeeRepository, times(1)).findByUsername(testUsername);
            verify(notificationRepository, times(1)).findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable);
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);

            // *** 수정된 부분 ***
            // DTO는 value1 ("MAIL")을 사용합니다.
            assertThat(resultPage.getContent().get(0).getOwnerType()).isEqualTo("MAIL");
            assertThat(resultPage.getContent().get(0).getReadAt()).isFalse();
        }

        @Test
        @DisplayName("실패 - 사원 없음 (username 조회)")
        void list_Fail_UserNotFound() {
            // given
            Pageable pageable = PageRequest.of(0, 5);
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

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
            lenient().when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.getUrl(notificationId, testUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 알림입니다.");
        }

        @Test
        @DisplayName("실패 - 권한 없음 (소유자 불일치)")
        void checkRole_Fail_AccessDenied() {
            // given
            lenient().when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(mockNotification));
            lenient().when(mockNotification.getEmployee()).thenReturn(mockEmployee);
            lenient().when(mockEmployee.getUsername()).thenReturn("anotherUser"); // 소유자 username

            // when & then
            assertThatThrownBy(() -> notificationService.softDelete(notificationId, testUsername))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("해당 알림에 접근할 권한이 없습니다.");
        }

        @Test
        @DisplayName("실패 - 이미 삭제됨")
        void checkRole_Fail_AlreadyDeleted() {
            // given
            lenient().when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(mockNotification));
            lenient().when(mockNotification.getEmployee()).thenReturn(mockEmployee);
            lenient().when(mockEmployee.getUsername()).thenReturn(testUsername);
            lenient().when(mockNotification.getIsDeleted()).thenReturn(true);    // 이미 삭제됨

            // when & then
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
            lenient().when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(mockNotification));
            lenient().when(mockNotification.getEmployee()).thenReturn(mockEmployee);
            // mockEmployee.getUsername()는 setUp에서 이미 Mocking 됨
            lenient().when(mockNotification.getIsDeleted()).thenReturn(false);
            lenient().when(mockNotification.getUrl()).thenReturn(expectedUrl);

            // when: getUrl 실행
            String resultUrl = notificationService.getUrl(notificationId, testUsername);

            // then:
            verify(mockNotification, times(1)).readNotification();
            assertThat(resultUrl).isEqualTo(expectedUrl);
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
            lenient().when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(mockNotification));
            lenient().when(mockNotification.getEmployee()).thenReturn(mockEmployee);
            // mockEmployee.getUsername()는 setUp에서 이미 Mocking 됨
            lenient().when(mockNotification.getIsDeleted()).thenReturn(false);

            // when: softDelete 실행
            notificationService.softDelete(notificationId, testUsername);

            // then:
            verify(mockNotification, times(1)).deleteNotification();
        }
    }

    @Nested
    @DisplayName("getUnreadNotiCount (읽지 않은 알림 개수)")
    class GetUnreadCount {
        @Test
        @DisplayName("성공")
        void getUnreadNotiCount_Success() {
            // given:
            // *** 수정된 부분 ***
            // 이 테스트에서만 사용되지만, 다른 테스트와의 충돌을 피하기 위해 lenient() 사용
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockEmployee));
            lenient().when(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .thenReturn(5L);

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
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.getUnreadNotiCount(testUsername))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 사원 입니다.");
        }
    }


    @Nested
    @DisplayName("countUndeletedAll (삭제되지 않은 모든 알림 개수)")
    class CountUndeletedAll {
        @Test
        @DisplayName("성공")
        void countUndeletedAll_Success() {
            // given
            // *** 수정된 부분 ***
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.of(mockEmployee));
            lenient().when(notificationRepository.countByEmployeeUsernameAndIsDeletedFalse(testUsername))
                    .thenReturn(10L);

            // when
            long count = notificationService.countUndeletedAll(testUsername);

            // then
            assertThat(count).isEqualTo(10L);
            verify(employeeRepository, times(1)).findByUsername(testUsername);
            verify(notificationRepository, times(1)).countByEmployeeUsernameAndIsDeletedFalse(testUsername);
        }

        @Test
        @DisplayName("실패 - 사원 없음")
        void countUndeletedAll_Fail_UserNotFound() {
            // given
            lenient().when(employeeRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.countUndeletedAll(testUsername))
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

            lenient().when(notificationRepository.findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .thenReturn(unreadList);

            // when
            notificationService.readAll(testUsername);

            // then
            verify(noti1, times(1)).readNotification();
            verify(noti2, times(1)).readNotification();
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

            lenient().when(notificationRepository.findByEmployeeUsernameAndIsDeletedFalse(testUsername))
                    .thenReturn(allList);

            // when
            notificationService.softDeleteAll(testUsername);

            // then
            verify(noti1, times(1)).deleteNotification();
            verify(noti2, times(1)).deleteNotification();
        }
    }
}