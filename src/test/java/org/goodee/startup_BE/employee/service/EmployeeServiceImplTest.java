//
package org.goodee.startup_BE.employee.service;

// [추가] 신규 의존성 import
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;
// [추가] ArgumentMatchers import
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
// BDDMockito static import
import static org.mockito.BDDMockito.given;
// Mockito static import
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class EmployeeServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private EmployeeServiceImpl employeeServiceImpl;

    @Mock // Mock 객체로 생성
    private EmployeeRepository employeeRepository;

    @Mock // Mock 객체로 생성
    private CommonCodeRepository commonCodeRepository;

    @Mock // Mock 객체로 생성
    private PasswordEncoder passwordEncoder;

    @Mock // [추가] 신규 의존성 Mock 객체
    private AttachmentFileService attachmentFileService;

    @Mock
    EmployeeHistoryService employeeHistoryService;

    // 테스트에서 공통으로 사용할 Mock 객체 선언
    private Employee mockEmployee;
    private Employee mockAdmin;
    private CommonCode mockStatus;
    private CommonCode mockRole;
    private CommonCode mockDept;
    private CommonCode mockPos;

    @BeforeEach // 각 테스트 실행 전 공통 설정
    void setUp() {
        // Mock 객체 초기화
        mockEmployee = mock(Employee.class);
        mockAdmin = mock(Employee.class);
        mockStatus = mock(CommonCode.class);
        mockRole = mock(CommonCode.class);
        mockDept = mock(CommonCode.class);
        mockPos = mock(CommonCode.class);

        // EmployeeResponseDTO.toDTO() 메서드 실행 시 NPE 방지를 위한 기본 Mocking
        // lenient() : 해당 Mocking이 모든 테스트에서 사용되지 않더라도(e.g. 실패 테스트) 경고/오류를 발생시키지 않음
        lenient().when(mockEmployee.getStatus()).thenReturn(mockStatus);
        lenient().when(mockEmployee.getRole()).thenReturn(mockRole);
        lenient().when(mockEmployee.getDepartment()).thenReturn(mockDept);
        lenient().when(mockEmployee.getPosition()).thenReturn(mockPos);

        lenient().when(mockStatus.getCommonCodeId()).thenReturn(101L);
        lenient().when(mockRole.getValue1()).thenReturn("ROLE_USER");
        lenient().when(mockDept.getValue1()).thenReturn("Test Dept");
        lenient().when(mockPos.getValue1()).thenReturn("Test Pos");

        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getUsername()).thenReturn("user");
        lenient().when(mockEmployee.getName()).thenReturn("Test User");
        lenient().when(mockEmployee.getEmail()).thenReturn("user@test.com");
        lenient().when(mockEmployee.getPhoneNumber()).thenReturn("010-1111-1111");
        lenient().when(mockEmployee.getHireDate()).thenReturn(LocalDate.now());
        lenient().when(mockEmployee.getProfileImg()).thenReturn("default.png");
    }


    @Nested // 테스트 그룹화
    @DisplayName("updateEmployeeByUser (사용자 정보 수정)")
    class UpdateEmployeeByUser {

        @Test
        @DisplayName("성공")
        void updateEmployeeByUser_Success() {
            // given
            String username = "user";
            String newPhoneNumber = "010-2222-2222";
            EmployeeRequestDTO request = new EmployeeRequestDTO();
            // request.username은 더 이상 서비스에서 사용되지 않음
            request.setUsername(username);
            request.setPhoneNumber(newPhoneNumber);
            // request.getMultipartFile()은 null이므로 파일 업로드 로직은 실행되지 않음

            // DTO 변환 시 업데이트된 번호를 반환하도록 설정
            lenient().when(mockEmployee.getPhoneNumber()).thenReturn(newPhoneNumber);

            // [수정] 서비스가 인증된 username으로 조회함
            given(employeeRepository.findByUsername(username)).willReturn(Optional.of(mockEmployee));

            // [추가] CommonCode (OwnerType) 조회를 Mocking (IndexOutOfBoundsException 방지)
            // 서비스 로직의 .get(0) 호출을 위함
            CommonCode mockOwnerCode = mock(CommonCode.class);
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    anyString(), // OwnerType.PREFIX
                    anyString()  // OwnerType.EMPLOYEE.name()
            )).willReturn(List.of(mockOwnerCode));

            // when
            EmployeeResponseDTO result = employeeServiceImpl.updateEmployeeByUser(username, request);

            // then
            // 엔티티의 updatePhoneNumber 메서드가 정확한 인자와 함께 호출되었는지 검증
            verify(mockEmployee).updatePhoneNumber(newPhoneNumber, mockEmployee);
            // [추가] 파일이 없었으므로 updateProfileImg는 호출되지 않았는지 검증
            verify(mockEmployee, never()).updateProfileImg(anyString(), any(Employee.class));
            // 반환된 DTO의 값이 요청 값과 일치하는지 확인
            assertThat(result.getPhoneNumber()).isEqualTo(newPhoneNumber);
        }

        @Test
        @DisplayName("실패 - 사용자 없음 (orElseThrow 처리)")
        void updateEmployeeByUser_Fail_UserNotFound() {
            // given
            String username = "user";
            EmployeeRequestDTO request = new EmployeeRequestDTO();
            request.setUsername(username);

            // [수정] findByUsername이 Optional.empty()를 반환하면
            // 서비스 로직의 orElseThrow가 예외를 발생시킴
            given(employeeRepository.findByUsername(username)).willReturn(Optional.empty());

            // when & then
            // employee가 null일 때 BadCredentialsException이 발생하는지 확인
            assertThatThrownBy(() -> employeeServiceImpl.updateEmployeeByUser(username, request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("회원 정보 수정 권한이 없습니다.");

            // 이 테스트는 예외가 먼저 발생하므로 CommonCodeRepository를 호출하지 않음
        }

        // [삭제] updateEmployeeByUser_Fail_Unauthorized 테스트
        // 서비스 로직 변경으로 인해 더 이상 유효하지 않은 테스트 케이스이므로 삭제
    }

    @Nested
    @DisplayName("updateEmployeeByAdmin (관리자 정보 수정)")
    class UpdateEmployeeByAdmin {

        // ... (기존과 동일)
        private EmployeeRequestDTO request;
        private String adminUsername = "admin";
        private String targetUsername = "user";
        private Long targetEmployeeId = 1L; // 대상 직원 ID

        @BeforeEach
        void adminSetup() {
            // 관리자 수정 요청 DTO 공통 설정
            // 이 DTO는 Success, Fail 케이스 모두에서 사용됨
            request = new EmployeeRequestDTO();
            request.setUsername(targetUsername); // DTO 변환 로직 등에 사용될 수 있음
            request.setEmployeeId(targetEmployeeId); // [수정] 서비스 로직 변경으로 ID 추가
            request.setStatus(101L);
            request.setRole(901L);
            request.setDepartment(201L);
            request.setPosition(301L);

            // [수정] DTO 변환(toDTO) 관련 Mocking을 Success 테스트로 이동
        }

        @Test
        @DisplayName("성공")
        void updateEmployeeByAdmin_Success() {
            // given
            // 관리자 조회
            given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
            // [수정] 대상 직원을 ID로 조회
            given(employeeRepository.findById(targetEmployeeId)).willReturn(Optional.of(mockEmployee));
            // CommonCode 조회
            given(commonCodeRepository.findById(101L)).willReturn(Optional.of(mockStatus));
            given(commonCodeRepository.findById(901L)).willReturn(Optional.of(mockRole));
            given(commonCodeRepository.findById(201L)).willReturn(Optional.of(mockDept));
            given(commonCodeRepository.findById(301L)).willReturn(Optional.of(mockPos));

            // [수정] DTO 변환(toDTO) 시 사용될 Mocking (Success Case 전용)
            // 이 Mocking은 toDTO가 호출될 때(즉, 성공 시)만 필요
            when(mockRole.getValue1()).thenReturn("ROLE_ADMIN");
            when(mockDept.getValue1()).thenReturn("Admin Dept");
            when(mockPos.getValue1()).thenReturn("Manager");

            // [수정] DTO 변환 시 getter 호출 설정 (Success Case 전용)
            // lenient()를 사용한 global setup을 덮어씀
            when(mockEmployee.getStatus()).thenReturn(mockStatus);
            when(mockEmployee.getRole()).thenReturn(mockRole);
            when(mockEmployee.getDepartment()).thenReturn(mockDept);
            when(mockEmployee.getPosition()).thenReturn(mockPos);


            // when
            EmployeeResponseDTO result = employeeServiceImpl.updateEmployeeByAdmin(adminUsername, request);

            // then
            // 엔티티의 각 update 메서드가 올바른 CommonCode와 admin 객체로 호출되었는지 검증
            verify(mockEmployee).updateStatus(mockStatus, mockAdmin);
            verify(mockEmployee).updateRole(mockRole, mockAdmin);
            verify(mockEmployee).updateDepartment(mockDept, mockAdmin);
            verify(mockEmployee).updatePosition(mockPos, mockAdmin);

            // 반환된 DTO 검증
            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("실패 - 대상 사원 없음")
        void updateEmployeeByAdmin_Fail_TargetNotFound() {
            // given
            given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
            // [수정] 대상 직원 조회 실패 (ID 기준)
            given(employeeRepository.findById(targetEmployeeId)).willReturn(Optional.empty());

            // when & then
            // ResourceNotFoundException 발생 확인
            assertThatThrownBy(() -> employeeServiceImpl.updateEmployeeByAdmin(adminUsername, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("사원 정보를 찾을 수 없습니다.");

            // toDTO 관련 Mocking이 없으므로 UnnecessaryStubbingException 발생 안 함
        }

        @Test
        @DisplayName("실패 - Status 코드 없음")
        void updateEmployeeByAdmin_Fail_StatusCodeNotFound() {
            // given
            given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
            // [수정] 대상 직원 조회 성공 (ID 기준)
            given(employeeRepository.findById(targetEmployeeId)).willReturn(Optional.of(mockEmployee));
            // Status 코드 조회 실패
            given(commonCodeRepository.findById(101L)).willReturn(Optional.empty());

            // when & then
            // ResourceNotFoundException 발생 및 메시지 확인
            assertThatThrownBy(() -> employeeServiceImpl.updateEmployeeByAdmin(adminUsername, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("status code: 101");
        }
    }

    @Nested
    @DisplayName("initPassword (비밀번호 초기화)")
    class InitPassword {

        // ... (기존과 동일)
        private EmployeeRequestDTO request;
        private String adminUsername = "admin";
        private String targetUsername = "user";
        private Long targetEmployeeId = 1L; // 대상 직원 ID
        private String encodedPassword = "encodedPassword";

        @BeforeEach
        void initSetup() {
            request = new EmployeeRequestDTO();
            request.setUsername(targetUsername); // DTO 변환 로직 등에 사용될 수 있음
            request.setEmployeeId(targetEmployeeId); // [수정] 서비스 로직 변경으로 ID 추가

            // [수정] getUsername() Mocking을 Success 테스트로 이동
            // when(mockEmployee.getUsername()).thenReturn(targetUsername);
        }

        @Test
        @DisplayName("성공")
        void initPassword_Success() {
            // given
            given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
            // [수정] 대상 직원을 ID로 조회
            given(employeeRepository.findById(targetEmployeeId)).willReturn(Optional.of(mockEmployee));

            // [수정] SUT가 employee.getUsername()을 호출하므로 Mocking (Success Case 전용)
            // 1. passwordEncoder.encode(employee.getUsername())
            // 2. EmployeeResponseDTO.toDTO(employee) -> employee.getUsername()
            when(mockEmployee.getUsername()).thenReturn(targetUsername);

            // passwordEncoder가 targetUsername(초기 비밀번호)을 인코딩한 값을 반환하도록 설정
            given(passwordEncoder.encode(targetUsername)).willReturn(encodedPassword);

            // when
            EmployeeResponseDTO result = employeeServiceImpl.initPassword(adminUsername, request);

            // then
            // updateInitPassword가 인코딩된 비밀번호와 admin 객체로 호출되었는지 검증
            verify(mockEmployee).updateInitPassword(encodedPassword, mockAdmin);
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(targetUsername);
        }

        @Test
        @DisplayName("실패 - 대상 사원 없음")
        void initPassword_Fail_TargetNotFound() {
            // given
            given(employeeRepository.findByUsername(adminUsername)).willReturn(Optional.of(mockAdmin));
            // [수정] 대상 직원 조회 실패 (ID 기준)
            given(employeeRepository.findById(targetEmployeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> employeeServiceImpl.initPassword(adminUsername, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("사원 정보를 찾을 수 없습니다.");

            // mockEmployee.getUsername() 스터빙이 없으므로 UnnecessaryStubbingException 발생 안 함
        }
    }

    @Nested
    @DisplayName("getEmployee (사원 조회)")
    class GetEmployee {

        // ... (기존과 동일)
        @Test
        @DisplayName("성공 - ID로 조회")
        void getEmployeeById_Success() {
            // given
            Long employeeId = 1L;
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));
            // setUp()에서 DTO 변환을 위한 Mocking 이미 완료됨 (lenient)

            // when
            EmployeeResponseDTO result = employeeServiceImpl.getEmployee(employeeId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmployeeId()).isEqualTo(employeeId);
            assertThat(result.getUsername()).isEqualTo("user");
            assertThat(result.getDepartment()).isEqualTo("Test Dept");
        }

        @Test
        @DisplayName("실패 - ID로 조회 (사원 없음)")
        void getEmployeeById_Fail_NotFound() {
            // given
            Long employeeId = 1L;
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> employeeServiceImpl.getEmployee(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("사원 정보를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("성공 - Username으로 조회")
        void getEmployeeByUsername_Success() {
            // given
            String username = "user";
            given(employeeRepository.findByUsername(username)).willReturn(Optional.of(mockEmployee));
            // setUp()에서 DTO 변환을 위한 Mocking 이미 완료됨 (lenient)

            // when
            EmployeeResponseDTO result = employeeServiceImpl.getEmployee(username);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(username);
            assertThat(result.getDepartment()).isEqualTo("Test Dept");
        }

        @Test
        @DisplayName("실패 - Username으로 조회 (사원 없음)")
        void getEmployeeByUsername_Fail_NotFound() {
            // given
            String username = "user";
            given(employeeRepository.findByUsername(username)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> employeeServiceImpl.getEmployee(username))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("사원 정보를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("getDepartmentMembers (부서원 조회)")
    class GetDepartmentMembers {

        // ... (기존과 동일)
        @Test
        @DisplayName("성공 - 부서원 있음")
        void getDepartmentMembers_Success_MembersFound() {
            // given
            Long departmentId = 201L;
            // mockEmployee 1명을 포함한 리스트 반환
            given(employeeRepository.findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(departmentId))
                    .willReturn(List.of(mockEmployee));
            // setUp()에서 DTO 변환을 위한 Mocking 이미 완료됨 (lenient)

            // when
            List<EmployeeResponseDTO> resultList = employeeServiceImpl.getDepartmentMembers(departmentId);

            // then
            assertThat(resultList).isNotNull();
            assertThat(resultList).hasSize(1);
            assertThat(resultList.get(0).getUsername()).isEqualTo("user");
            assertThat(resultList.get(0).getDepartment()).isEqualTo("Test Dept");
        }

        @Test
        @DisplayName("성공 - 부서원 없음")
        void getDepartmentMembers_Success_NoMembers() {
            // given
            Long departmentId = 201L;
            // 빈 리스트 반환
            given(employeeRepository.findByDepartmentCommonCodeIdOrderByPositionSortOrderDesc(departmentId))
                    .willReturn(List.of()); // 빈 리스트

            // when
            List<EmployeeResponseDTO> resultList = employeeServiceImpl.getDepartmentMembers(departmentId);

            // then
            assertThat(resultList).isNotNull();
            // 리스트가 비어있는지 확인
            assertThat(resultList).isEmpty();
        }
    }
}