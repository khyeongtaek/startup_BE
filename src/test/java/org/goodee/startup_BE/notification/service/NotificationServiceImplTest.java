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
        // Mock 객체 초기화 (lenient: 모든 테스트에서 사용되지 않아도 경고 미발생)
        mockEmployee = mock(Employee.class);
        mockOwnerType = mock(CommonCode.class);
        mockNotification = mock(Notification.class);

        // 공통 Mocking 설정
        lenient().when(mockEmployee.getUsername()).thenReturn(testUsername);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(testEmployeeId);
    }

    // --- Create Notification 테스트 수정 ---
    @Nested
    @DisplayName("create (알림 생성)")
    class CreateNotification {

        private NotificationRequestDTO requestDTO;
        private Long ownerTypeCodeId = 101L;

        @BeforeEach
        void createSetup() {
            // 알림 생성 요청 DTO 준비 (employeeId 사용)
            // *가정: NotificationRequestDTO에 employeeId 필드가 Long 타입으로 존재*
            requestDTO = new NotificationRequestDTO(
                    testEmployeeId, // 받는 사람 Employee ID
                    ownerTypeCodeId, // CommonCode ID
                    "/mail/1",
                    "테스트 제목",
                    "테스트 내용"
            );
        }

        @Test
        @DisplayName("성공")
        void create_Success() {
            // given: 사원 조회(ID 기준), CommonCode 조회, 알림 저장, DTO 변환 과정 Mocking
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findById(ownerTypeCodeId)).willReturn(Optional.of(mockOwnerType));
            given(mockOwnerType.getCodeDescription()).willReturn("메일");
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> invocation.getArgument(0));
            // WebSocket 푸시에 사용할 username Mocking (findById로 찾은 employee 객체에서 가져옴)
            given(mockEmployee.getUsername()).willReturn(testUsername);


            // when: 알림 생성 실행 (파라미터 변경)
            NotificationResponseDTO resultDTO = notificationService.create(requestDTO);

            // then: 결과 검증
            // 1. DTO 변환 검증
            assertThat(resultDTO).isNotNull();
            assertThat(resultDTO.getTitle()).isEqualTo("테스트 제목");
            assertThat(resultDTO.getOwnerTypeDescription()).isEqualTo("메일");

            // 2. Repository 및 Template 호출 검증
            verify(employeeRepository, times(1)).findById(testEmployeeId);
            verify(commonCodeRepository, times(1)).findById(ownerTypeCodeId);
            verify(notificationRepository, times(1)).save(any(Notification.class));
            // WebSocket 푸시 username 검증 (mockEmployee에서 가져온 값)
            verify(simpMessagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUsername), eq("/queue/noti"), any(NotificationResponseDTO.class)
            );
        }

        @Test
        @DisplayName("실패 - 사원 없음")
        void create_Fail_UserNotFound() {
            // given: 사원 조회 실패 (ID 기준)
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.empty());

            // when & then: UsernameNotFoundException 발생 확인 (호출 방식 변경)
            assertThatThrownBy(() -> notificationService.create(requestDTO))
                    .isInstanceOf(UsernameNotFoundException.class) // Service 내부 Exception 타입 확인 필요 (UsernameNotFoundException or EntityNotFoundException)
                    .hasMessageContaining("존재 하지 않는 사원 입니다");
        }

        @Test
        @DisplayName("실패 - CommonCode 없음")
        void create_Fail_CommonCodeNotFound() {
            // given: 사원 조회는 성공 (ID 기준), CommonCode 조회 실패
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(mockEmployee));
            given(commonCodeRepository.findById(ownerTypeCodeId)).willReturn(Optional.empty());

            // when & then: EntityNotFoundException 발생 확인 (호출 방식 변경)
            assertThatThrownBy(() -> notificationService.create(requestDTO))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("유효하지 않은 알림 출처 CommonCode Id 입니다");
        }
    }

    // ... (list, checkRole, getUrl, softDelete, getUnreadNotiCount, readAll, softDeleteAll 테스트는 변경 없음) ...
    @Nested
    @DisplayName("list (목록 조회)")
    class ListNotification {

        @Test
        @DisplayName("성공")
        void list_Success() {
            // given: Pageable 객체 및 Repository 반환값 설정
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

            // DTO 변환에 필요한 Mocking
            given(mockOwnerType.getCodeDescription()).willReturn("메일");
            given(mockNotification.getOwnerType()).willReturn(mockOwnerType);

            List<Notification> notificationList = List.of(mockNotification);
            Page<Notification> mockPage = new PageImpl<>(notificationList, pageable, notificationList.size());

            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockEmployee));
            given(notificationRepository.findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable))
                    .willReturn(mockPage);

            // when: 목록 조회 실행
            Page<NotificationResponseDTO> resultPage = notificationService.list(testUsername, pageable);

            // then: 결과 검증
            verify(employeeRepository, times(1)).findByUsername(testUsername);
            verify(notificationRepository, times(1)).findByEmployeeEmployeeIdAndIsDeletedFalseOrderByCreatedAtDesc(testEmployeeId, pageable);
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).getOwnerTypeDescription()).isEqualTo("메일");
        }
    }

    @Nested
    @DisplayName("checkRole (공통 메서드 - getUrl, softDelete에서 호출)")
    class CheckRole {

        private Long notificationId = 1L;

        @Test
        @DisplayName("실패 - 알림 없음")
        void checkRole_Fail_NotificationNotFound() {
            // given: 알림 조회 실패
            given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

            // when & then: getUrl 호출 시 EntityNotFoundException 발생 확인
            assertThatThrownBy(() -> notificationService.getUrl(notificationId, testUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("존재하지 않는 알림입니다.");
        }

        @Test
        @DisplayName("실패 - 권한 없음 (소유자 불일치)")
        void checkRole_Fail_AccessDenied() {
            // given: 알림은 찾았으나, 소유자 username이 다름
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee);
            given(mockEmployee.getUsername()).willReturn("anotherUser"); // 소유자 username

            // when & then: softDelete 호출 시 AccessDeniedException 발생 확인
            assertThatThrownBy(() -> notificationService.softDelete(notificationId, testUsername)) // 요청자: testUsername
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("해당 알림에 접근할 권한이 없습니다.");
        }

        @Test
        @DisplayName("실패 - 이미 삭제됨")
        void checkRole_Fail_AlreadyDeleted() {
            // given: 알림 찾았고, 소유자도 일치하나, isDeleted = true
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee);
            given(mockEmployee.getUsername()).willReturn(testUsername); // 소유자 일치
            given(mockNotification.getIsDeleted()).willReturn(true);    // 이미 삭제됨

            // when & then: getUrl 호출 시 IllegalStateException 발생 확인
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
            given(mockNotification.getEmployee()).willReturn(mockEmployee);
            given(mockEmployee.getUsername()).willReturn(testUsername);
            given(mockNotification.getIsDeleted()).willReturn(false);
            given(mockNotification.getUrl()).willReturn(expectedUrl);

            // when: getUrl 실행
            String resultUrl = notificationService.getUrl(notificationId, testUsername);

            // then: 결과 검증
            // 1. 알림 읽음 처리 메서드가 호출되었는지 검증
            verify(mockNotification, times(1)).readNotification();
            // 2. 반환된 URL이 일치하는지 검증
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
            given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));
            given(mockNotification.getEmployee()).willReturn(mockEmployee);
            given(mockEmployee.getUsername()).willReturn(testUsername);
            given(mockNotification.getIsDeleted()).willReturn(false);

            // when: softDelete 실행
            notificationService.softDelete(notificationId, testUsername);

            // then: 알림 삭제 처리 메서드가 호출되었는지 검증
            verify(mockNotification, times(1)).deleteNotification();
        }
    }

    @Nested
    @DisplayName("getUnreadNotiCount (읽지 않은 알림 개수)")
    class GetUnreadCount {
        @Test
        @DisplayName("성공")
        void getUnreadNotiCount_Success() {
            // given: Repository가 5L 반환하도록 설정
            given(notificationRepository.countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .willReturn(5L);

            // when: 개수 조회 실행
            long count = notificationService.getUnreadNotiCount(testUsername);

            // then: 결과 검증
            assertThat(count).isEqualTo(5L);
            verify(notificationRepository, times(1)).countByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername);
        }
    }

    @Nested
    @DisplayName("readAll (모든 알림 읽음 처리)")
    class ReadAll {
        @Test
        @DisplayName("성공")
        void readAll_Success() {
            // given: 읽지 않은 알림 2개 Mocking
            Notification noti1 = mock(Notification.class);
            Notification noti2 = mock(Notification.class);
            List<Notification> unreadList = List.of(noti1, noti2);

            given(notificationRepository.findByEmployeeUsernameAndReadAtIsNullAndIsDeletedFalse(testUsername))
                    .willReturn(unreadList);

            // when: 모두 읽음 실행
            notificationService.readAll(testUsername);

            // then: 각 알림의 readNotification()이 호출되었는지 검증
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
            // given: 삭제되지 않은 알림 2개 Mocking
            Notification noti1 = mock(Notification.class);
            Notification noti2 = mock(Notification.class);
            List<Notification> allList = List.of(noti1, noti2);

            given(notificationRepository.findByEmployeeUsernameAndIsDeletedFalse(testUsername))
                    .willReturn(allList);

            // when: 모두 삭제 실행
            notificationService.softDeleteAll(testUsername);

            // then: 각 알림의 deleteNotification()이 호출되었는지 검증
            verify(noti1, times(1)).deleteNotification();
            verify(noti2, times(1)).deleteNotification();
        }
    }
}