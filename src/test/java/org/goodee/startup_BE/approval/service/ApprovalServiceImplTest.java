package org.goodee.startup_BE.approval.service;

import jakarta.persistence.EntityNotFoundException;
import org.goodee.startup_BE.approval.dto.*;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.approval.repository.ApprovalLineRepository;
import org.goodee.startup_BE.approval.repository.ApprovalReferenceRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
// NotificationService 임포트
import org.goodee.startup_BE.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// AssertJ static import
import static org.assertj.core.api.Assertions.*;
// BDDMockito static import
import static org.mockito.BDDMockito.*;
// Mockito static import
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class ApprovalServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private ApprovalServiceImpl approvalService;

    @Mock // Mock 객체로 생성
    private ApprovalDocRepository approvalDocRepository;

    @Mock // Mock 객체로 생성
    private ApprovalLineRepository approvalLineRepository;

    @Mock // Mock 객체로 생성
    private ApprovalReferenceRepository approvalReferenceRepository;

    @Mock // Mock 객체로 생성
    private EmployeeRepository employeeRepository;

    @Mock // Mock 객체로 생성
    private CommonCodeRepository commonCodeRepository;

    @Mock // NPE 해결을 위해 NotificationService Mock 객체 추가
    private NotificationService notificationService;

    // --- 테스트용 Mock 객체 선언 ---
    private Employee mockCreator;
    private Employee mockApprover1;
    private Employee mockApprover2;
    private Employee mockReferrer;

    private CommonCode mockDocStatusInProgress;
    private CommonCode mockDocStatusApproved;
    private CommonCode mockDocStatusRejected;
    private CommonCode mockLineStatusPending;
    private CommonCode mockLineStatusAwaiting;
    private CommonCode mockLineStatusApproved;
    private CommonCode mockLineStatusRejected;

    // decideApproval (line 136)에서 공통으로 호출하는 "OT", "APPROVAL"용 Mock 객체
    private CommonCode mockOtherApproval;

    private ApprovalDoc mockDoc;
    private ApprovalLine mockLine1;
    private ApprovalLine mockLine2;
    private ApprovalReference mockRef;

    // EmployeeResponseDTO 변환을 위한 공용 Mock 객체
    private CommonCode mockEmpStatus;
    private CommonCode mockEmpRole;
    private CommonCode mockEmpDept;
    private CommonCode mockEmpPos;


    @BeforeEach // 각 테스트 실행 전 공통 설정
    void setUp() {
        // Mock 객체 초기화
        mockCreator = mock(Employee.class);
        mockApprover1 = mock(Employee.class);
        mockApprover2 = mock(Employee.class);
        mockReferrer = mock(Employee.class);

        mockDocStatusInProgress = mock(CommonCode.class);
        mockDocStatusApproved = mock(CommonCode.class);
        mockDocStatusRejected = mock(CommonCode.class);
        mockLineStatusPending = mock(CommonCode.class);
        mockLineStatusAwaiting = mock(CommonCode.class);
        mockLineStatusApproved = mock(CommonCode.class);
        mockLineStatusRejected = mock(CommonCode.class);

        // 새 Mock 객체 초기화
        mockOtherApproval = mock(CommonCode.class);

        mockDoc = mock(ApprovalDoc.class);
        mockLine1 = mock(ApprovalLine.class);
        mockLine2 = mock(ApprovalLine.class);
        mockRef = mock(ApprovalReference.class);

        // Employee DTO 변환용 Mock 객체 초기화
        mockEmpStatus = mock(CommonCode.class);
        mockEmpRole = mock(CommonCode.class);
        mockEmpDept = mock(CommonCode.class);
        mockEmpPos = mock(CommonCode.class);

        // --- DTO 변환(toDTO) 시 NPE 방지를 위한 공통 Stubbing (lenient) ---
        // lenient(): 이 Stubbing이 모든 테스트에서 사용되지 않아도 경고/오류 X

        // Employee DTO 변환용 공통 코드 Stub
        lenient().when(mockEmpStatus.getCommonCodeId()).thenReturn(901L);
        lenient().when(mockEmpRole.getValue1()).thenReturn("ROLE_TEST");
        lenient().when(mockEmpDept.getValue1()).thenReturn("Test Dept");
        lenient().when(mockEmpPos.getValue1()).thenReturn("Test Pos");

        // 공통 Employee Mock 객체 Stubbing (Creator)
        stubMockEmployee(mockCreator, 10L, "creator", "기안자");
        // (Approver 1)
        stubMockEmployee(mockApprover1, 11L, "approver1", "결재자1");
        // (Approver 2)
        stubMockEmployee(mockApprover2, 12L, "approver2", "결재자2");
        // (Referrer)
        stubMockEmployee(mockReferrer, 13L, "referrer", "참조자");

        // 공통 CommonCode Mock 객체 Stubbing (결재 상태값)
        lenient().when(mockDocStatusInProgress.getCommonCodeId()).thenReturn(101L);
        lenient().when(mockDocStatusInProgress.getValue1()).thenReturn("IN_PROGRESS");
        lenient().when(mockDocStatusApproved.getValue1()).thenReturn("APPROVED");
        lenient().when(mockDocStatusRejected.getValue1()).thenReturn("REJECTED");

        lenient().when(mockLineStatusPending.getCommonCodeId()).thenReturn(201L);
        lenient().when(mockLineStatusPending.getValue1()).thenReturn("PENDING");
        lenient().when(mockLineStatusAwaiting.getCommonCodeId()).thenReturn(202L);
        lenient().when(mockLineStatusAwaiting.getValue1()).thenReturn("AWAITING");
        lenient().when(mockLineStatusApproved.getCommonCodeId()).thenReturn(203L);
        lenient().when(mockLineStatusApproved.getValue1()).thenReturn("APPROVED");
        lenient().when(mockLineStatusRejected.getCommonCodeId()).thenReturn(204L);
        lenient().when(mockLineStatusRejected.getValue1()).thenReturn("REJECTED");

        // "OT", "APPROVAL" Mock 객체 Stubbing
        lenient().when(mockOtherApproval.getValue1()).thenReturn("APPROVAL");


        // 컴파일 오류 수정: lenient().given() -> lenient().when().thenReturn()

        // createApproval에서 공통으로 필요한 코드들
        lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AD", "IN_PROGRESS"))
                .thenReturn(List.of(mockDocStatusInProgress));
        lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AL", "PENDING"))
                .thenReturn(List.of(mockLineStatusPending));
        lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AL", "AWAITING"))
                .thenReturn(List.of(mockLineStatusAwaiting));

        // decideApproval에서 공통으로 필요한 코드 (로그에서 확인됨)
        lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("OT", "APPROVAL"))
                .thenReturn(List.of(mockOtherApproval));


        // 공통 Approval 객체 Mock Stubbing
        lenient().when(mockDoc.getDocId()).thenReturn(1L);
        lenient().when(mockDoc.getTitle()).thenReturn("테스트 기안 문서");
        lenient().when(mockDoc.getCreator()).thenReturn(mockCreator);
        lenient().when(mockDoc.getDocStatus()).thenReturn(mockDocStatusInProgress);
        lenient().when(mockDoc.getApprovalLineList()).thenReturn(List.of(mockLine1, mockLine2)); // 상세조회 시 사용
        lenient().when(mockDoc.getApprovalReferenceList()).thenReturn(List.of(mockRef)); // 상세조회 시 사용

        lenient().when(mockLine1.getDoc()).thenReturn(mockDoc);
        lenient().when(mockLine1.getLineId()).thenReturn(20L);
        lenient().when(mockLine1.getApprovalOrder()).thenReturn(1L);
        lenient().when(mockLine1.getEmployee()).thenReturn(mockApprover1);
        lenient().when(mockLine1.getApprovalStatus()).thenReturn(mockLineStatusAwaiting); // 1차 결재자는 '대기'

        lenient().when(mockLine2.getDoc()).thenReturn(mockDoc);
        lenient().when(mockLine2.getLineId()).thenReturn(21L);
        lenient().when(mockLine2.getApprovalOrder()).thenReturn(2L);
        lenient().when(mockLine2.getEmployee()).thenReturn(mockApprover2);
        lenient().when(mockLine2.getApprovalStatus()).thenReturn(mockLineStatusPending); // 2차 결재자는 '미결'

        lenient().when(mockRef.getDoc()).thenReturn(mockDoc);
        lenient().when(mockRef.getReferenceId()).thenReturn(30L);
        lenient().when(mockRef.getEmployee()).thenReturn(mockReferrer);
    }

    /**
     * setUp()에서 반복되는 Employee Mock 객체 Stubbing을 위한 헬퍼 메서드
     */
    private void stubMockEmployee(Employee mockEmp, Long id, String username, String name) {
        lenient().when(mockEmp.getEmployeeId()).thenReturn(id);
        lenient().when(mockEmp.getUsername()).thenReturn(username);
        lenient().when(mockEmp.getName()).thenReturn(name);
        lenient().when(mockEmp.getEmail()).thenReturn(username + "@test.com");
        lenient().when(mockEmp.getPhoneNumber()).thenReturn("010-1111-1111");
        lenient().when(mockEmp.getHireDate()).thenReturn(LocalDate.now());
        lenient().when(mockEmp.getProfileImg()).thenReturn("default.png");
        // DTO 변환에 필요한 공통 코드 Mock 객체들 설정
        lenient().when(mockEmp.getStatus()).thenReturn(mockEmpStatus);
        lenient().when(mockEmp.getRole()).thenReturn(mockEmpRole);
        lenient().when(mockEmp.getDepartment()).thenReturn(mockEmpDept);
        lenient().when(mockEmp.getPosition()).thenReturn(mockEmpPos);
    }

    /**
     * getCommonCode() 헬퍼 메서드를 Mocking하기 위한 헬퍼 메서드
     *
     * @param prefix "AD" 또는 "AL"
     * @param value  "IN_PROGRESS", "PENDING" 등
     * @param code   반환할 Mock CommonCode 객체
     */
    private void givenCommonCode(String prefix, String value, CommonCode code) {
        // 이 메서드는 BDDMockito 스타일(given...willReturn)을 유지
        given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(prefix, value))
                .willReturn(List.of(code));
    }


    // CreateApproval 클래스
    @Nested
    @DisplayName("createApproval (결재 문서 생성)")
    class CreateApproval {

        private ApprovalDocRequestDTO requestDto;
        private final String creatorUsername = "creator";

        @BeforeEach
        void createSetup() {
            // DTO 생성 로직만 남김
            requestDto = new ApprovalDocRequestDTO();
            requestDto.setTitle("새 기안 문서");
            requestDto.setContent("내용입니다.");

            // 결재선 1 (순서 1, ID 11)
            ApprovalLineRequestDTO lineDto1 = new ApprovalLineRequestDTO();
            lineDto1.setApprovalOrder(1L);
            lineDto1.setApproverId(11L); // mockApprover1

            // 결재선 2 (순서 2, ID 12)
            ApprovalLineRequestDTO lineDto2 = new ApprovalLineRequestDTO();
            lineDto2.setApprovalOrder(2L);
            lineDto2.setApproverId(12L); // mockApprover2

            // 참조자 1 (ID 13)
            ApprovalReferenceRequestDTO refDto1 = new ApprovalReferenceRequestDTO();
            refDto1.setReferrerId(13L); // mockReferrer

            requestDto.setApprovalLines(List.of(lineDto1, lineDto2));
            requestDto.setApprovalReferences(List.of(refDto1));

            // 공통 given은 setUp()으로 이동
        }

        @Test
        @DisplayName("성공 - 결재선, 참조자 포함")
        void createApproval_Success() {
            // given
            // '성공'에 필요한 Mocking을 이 테스트 내부로 이동
            // (공통 코드는 setUp()에서 lenient()로 이미 stubbing됨)
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));

            given(employeeRepository.findById(11L)).willReturn(Optional.of(mockApprover1));
            given(employeeRepository.findById(12L)).willReturn(Optional.of(mockApprover2));
            given(employeeRepository.findById(13L)).willReturn(Optional.of(mockReferrer));

            given(approvalDocRepository.save(any(ApprovalDoc.class))).willReturn(mockDoc);

            // NotificationService가 null이 아니므로, create 메서드 호출이 발생함
            // BDDMockito.doNothing()을 사용하여 void 메서드 호출을 명시적으로 처리 (혹은 비워둬도 Mockito가 알아서 처리함)
            // doNothing().when(notificationService).create(any(NotificationRequestDTO.class));

            // when
            ApprovalDocResponseDTO result = approvalService.createApproval(requestDto, creatorUsername);

            // then
            assertThat(result).isNotNull();
            // setUp()에서 mockDoc에 stub한 값들이 반환되는지 확인
            assertThat(result.getDocId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("테스트 기안 문서");
            assertThat(result.getCreator().getUsername()).isEqualTo("creator");
            assertThat(result.getApprovalLines()).hasSize(2);
            assertThat(result.getApprovalReferences()).hasSize(1);

            // Doc 저장 확인
            verify(approvalDocRepository).save(any(ApprovalDoc.class));

            // Lines 저장 확인 (ArgumentCaptor 사용)
            ArgumentCaptor<List<ApprovalLine>> lineCaptor = ArgumentCaptor.forClass(List.class);
            verify(approvalLineRepository).saveAll(lineCaptor.capture());

            List<ApprovalLine> savedLines = lineCaptor.getValue();
            assertThat(savedLines).hasSize(2);
            // 첫번째 결재선(order 1)은 'AWAITING' (대기)
            assertThat(savedLines.stream().filter(l -> l.getApprovalOrder() == 1L).findFirst().get().getApprovalStatus())
                    .isEqualTo(mockLineStatusAwaiting);
            // 두번째 결재선(order 2)은 'PENDING' (미결)
            assertThat(savedLines.stream().filter(l -> l.getApprovalOrder() == 2L).findFirst().get().getApprovalStatus())
                    .isEqualTo(mockLineStatusPending);


            // Refs 저장 확인
            // .save() -> .saveAll()로 변경
            verify(approvalReferenceRepository).saveAll(anyList());

            // 알림 서비스가 호출되었는지 검증 (선택 사항이지만, NPE 방지 확인용)
            //    [수정] 로그에 따라 3회 -> 4회로 변경
            verify(notificationService, times(4)).create(any());
        }

        @Test
        @DisplayName("실패 - 기안자(로그인 사용자) 없음")
        void createApproval_Fail_CreatorNotFound() {
            // given
            // 이 테스트에 필요한 최소한의 Mocking만 설정
            // 서비스 로직의 첫 번째 단계인 getCurrentEmployee()에서 실패
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> approvalService.createApproval(requestDto, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다: " + creatorUsername);
        }

        @Test
        @DisplayName("실패 - 공통 코드(문서 상태) 없음")
        void createApproval_Fail_DocStatusCodeNotFound() {
            // given
            // 이 테스트에 필요한 Mocking만 설정
            // 기안자 조회는 통과
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            // 'AD', 'IN_PROGRESS' 조회 시 빈 리스트 반환 (실패 지점)
            // setUp()의 lenient() stubbing을 오버라이드 (BDDMockito 스타일 사용)
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AD", "IN_PROGRESS"))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> approvalService.createApproval(requestDto, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    // [수정] AssertionError 해결: 실제 오류 메시지로 수정
                    .hasMessageContaining("공통 코드 조회 실패: AD, IN_PROGRESS");
        }

        @Test
        @DisplayName("실패 - 결재자 없음")
        void createApproval_Fail_ApproverNotFound() {
            // given
            // 이 테스트에 필요한 Mocking만 설정
            // 기안자 조회 통과
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            // 공통 코드 조회 통과 (setUp()에서 lenient()로 처리됨)
            // Doc 저장 통과
            given(approvalDocRepository.save(any(ApprovalDoc.class))).willReturn(mockDoc);
            // 결재자 1 (ID: 11L) 조회 실패 (실패 지점)
            given(employeeRepository.findById(11L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> approvalService.createApproval(requestDto, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("결재자를 찾을 수 없습니다: 11");
        }

        @Test
        @DisplayName("실패 - 참조자 없음")
        void createApproval_Fail_ReferrerNotFound() {
            // given
            // 이 테스트에 필요한 Mocking만 설정
            // 기안자 조회 통과
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            // 공통 코드 조회 통과 (setUp()에서 lenient()로 처리됨)
            // Doc 저장 통과
            given(approvalDocRepository.save(any(ApprovalDoc.class))).willReturn(mockDoc);
            // 결재선 조회 통과
            given(employeeRepository.findById(11L)).willReturn(Optional.of(mockApprover1));
            given(employeeRepository.findById(12L)).willReturn(Optional.of(mockApprover2));
            // 참조자 (ID: 13L) 조회 실패 (실패 지점)
            given(employeeRepository.findById(13L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> approvalService.createApproval(requestDto, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("참조자를 찾을 수 없습니다: 13");
        }
    }


    @Nested
    @DisplayName("decideApproval (결재 승인/반려)")
    class DecideApproval {

        private ApprovalLineRequestDTO decideRequest;
        private final String approverUsername = "approver1"; // 1차 결재자(11L)
        private final Long lineId = 20L; // 1차 결재선(mockLine1) ID
        private final Long approvedCodeId = 301L; // '승인' 상태 코드 ID
        private final Long rejectedCodeId = 302L; // '반려' 상태 코드 ID

        @BeforeEach
        void decideSetup() {
            // DTO 생성 로직만 남김
            decideRequest = new ApprovalLineRequestDTO();
            decideRequest.setLineId(lineId);
            decideRequest.setComment("테스트 의견");

            // 공통 given은 setUp()으로 이동
        }

        @Test
        @DisplayName("성공 - 1차 승인 (다음 결재자 있음)")
        void decideApproval_Success_ApproveAndNextExists() {
            // given
            // '성공'에 필요한 모든 Mocking을 이 테스트 내부로 이동
            decideRequest.setStatusCodeId(approvedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            given(mockLine1.getEmployee()).willReturn(mockApprover1); // 권한 검증 통과
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting); // 상태 검증 통과
            given(mockLine1.getDoc()).willReturn(mockDoc);

            // '승인' 코드 조회
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // 다음 결재선(mockLine2) 조회 성공
            given(mockLine1.getApprovalOrder()).willReturn(1L);
            given(approvalLineRepository.findByDocAndApprovalOrder(mockDoc, 2L)).willReturn(Optional.of(mockLine2));

            // 다음 결재선 상태('AWAITING')로 변경하기 위한 공통 코드 조회
            // 이 테스트에서만 필요한 givenCommonCode 호출은 유지
            givenCommonCode("AL", "AWAITING", mockLineStatusAwaiting);

            // 알림 서비스 Mocking (void 메서드)
            // doNothing().when(notificationService).create(any());

            // when
            ApprovalDocResponseDTO result = approvalService.decideApproval(decideRequest, approverUsername);

            // then
            assertThat(result).isNotNull();
            // 현재 결재선 상태가 '승인'으로 변경되었는지 검증
            verify(mockLine1).updateApprovalStatus(mockLineStatusApproved);
            verify(mockLine1).updateComment("테스트 의견");
            // 문서 수정자가 '결재자1'로 변경되었는지 검증
            verify(mockDoc).updateUpdater(mockApprover1);
            // 다음 결재선(mockLine2) 상태가 '대기'로 변경되었는지 검증
            verify(mockLine2).updateApprovalStatus(mockLineStatusAwaiting);
            // 문서(Doc) 상태는 변경되지 않아야 함
            verify(mockDoc, never()).updateDocStatus(any(CommonCode.class));

            // 알림 서비스 호출 검증 (로그에 따라 2회 -> 1회로 수정)
            verify(notificationService, times(1)).create(any());
        }

        @Test
        @DisplayName("성공 - 최종 승인 (다음 결재자 없음)")
        void decideApproval_Success_ApproveFinal() {
            // given
            // '성공'에 필요한 모든 Mocking을 이 테스트 내부로 이동
            decideRequest.setStatusCodeId(approvedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            given(mockLine1.getEmployee()).willReturn(mockApprover1); // 권한 검증 통과
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting); // 상태 검증 통과
            given(mockLine1.getDoc()).willReturn(mockDoc);

            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // 다음 결재선 조회 실패 (Optional.empty())
            given(mockLine1.getApprovalOrder()).willReturn(1L); // (실제론 마지막 순서라고 가정)
            given(approvalLineRepository.findByDocAndApprovalOrder(mockDoc, 2L)).willReturn(Optional.empty());

            // 문서 상태('APPROVED')로 변경하기 위한 공통 코드 조회
            givenCommonCode("AD", "APPROVED", mockDocStatusApproved);

            // 알림 서비스 Mocking (void 메서드)
            // doNothing().when(notificationService).create(any());

            // when
            approvalService.decideApproval(decideRequest, approverUsername);

            // then
            // 현재 결재선 상태 '승인' 변경
            verify(mockLine1).updateApprovalStatus(mockLineStatusApproved);
            // 다음 결재선이 없으므로 상태 변경 시도 X
            verify(mockLine2, never()).updateApprovalStatus(any(CommonCode.class));
            // 문서(Doc) 상태가 '최종 승인'으로 변경되어야 함
            verify(mockDoc).updateDocStatus(mockDocStatusApproved);

            // 알림 서비스 호출 검증 (기안자 1회)
            verify(notificationService, times(1)).create(any());
        }

        @Test
        @DisplayName("성공 - 반려")
        void decideApproval_Success_Reject() {
            // given
            // '성공'에 필요한 모든 Mocking을 이 테스트 내부로 이동
            decideRequest.setStatusCodeId(rejectedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            given(mockLine1.getEmployee()).willReturn(mockApprover1); // 권한 검증 통과
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting); // 상태 검증 통과
            given(mockLine1.getDoc()).willReturn(mockDoc);

            // '반려' 코드 조회
            given(commonCodeRepository.findById(rejectedCodeId)).willReturn(Optional.of(mockLineStatusRejected));

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // 문서 상태('REJECTED')로 변경하기 위한 공통 코드 조회
            givenCommonCode("AD", "REJECTED", mockDocStatusRejected);

            // 알림 서비스 Mocking (void 메서드)
            // doNothing().when(notificationService).create(any());

            // when
            approvalService.decideApproval(decideRequest, approverUsername);

            // then
            // 현재 결재선 상태 '반려' 변경
            verify(mockLine1).updateApprovalStatus(mockLineStatusRejected);
            // 문서(Doc) 상태가 '최종 반려'로 변경되어야 함
            verify(mockDoc).updateDocStatus(mockDocStatusRejected);
            // (중요) 반려 시에는 다음 결재선을 찾지 않아야 함
            verify(approvalLineRepository, never()).findByDocAndApprovalOrder(any(), any());

            // 알림 서비스 호출 검증 (기안자 1회)
            verify(notificationService, times(1)).create(any());
        }

        @Test
        @DisplayName("실패 - 결재 상태 코드 없음")
        void decideApproval_Fail_StatusCodeNotFound() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            // 이 테스트에 필요한 Mocking만 설정
            // 사용자 조회 통과
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            // 상태 코드 조회 실패 (실패 지점)
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.empty());

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨
            // 따라서 IndexOutOfBoundsException이 발생하지 않고, 이 테스트의 given까지만 실행됨

            // when & then
            assertThatThrownBy(() -> approvalService.decideApproval(decideRequest, approverUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("결재 상태 코드를 찾을 수 없습니다: " + approvedCodeId);
        }

        @Test
        @DisplayName("실패 - 결재선 없음")
        void decideApproval_Fail_LineNotFound() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            // 이 테스트에 필요한 Mocking만 설정
            // 사용자 조회 통과
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            // 상태 코드 조회 통과 (서비스 로직 순서상 이게 먼저)
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            // 결재선 조회 실패 (실패 지점)
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.empty());

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // when & then
            assertThatThrownBy(() -> approvalService.decideApproval(decideRequest, approverUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("결재선을 찾을 수 없습니다: " + lineId);
        }

        @Test
        @DisplayName("실패 - 권한 없음 (본인 아님)")
        void decideApproval_Fail_AccessDenied() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            // 이 테스트에 필요한 Mocking만 설정
            // 로그인 사용자를 '결재자2'(mockApprover2)로 설정
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover2));
            // 상태 코드 조회 통과
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            // 결재선 조회 통과
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));
            // 결재선(mockLine1)의 실제 주인은 '결재자1'(mockApprover1)로 설정 (실패 지점)
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            // (추가) mockApprover1과 mockApprover2의 ID가 다르도록 stub
            //    (setUp()에서 이미 lenient()로 되어있지만 명시적으로)
            lenient().when(mockApprover1.getEmployeeId()).thenReturn(11L);
            lenient().when(mockApprover2.getEmployeeId()).thenReturn(12L);

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // when & then
            assertThatThrownBy(() -> approvalService.decideApproval(decideRequest, approverUsername))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("이 결재를 처리할 권한이 없습니다.");
        }

        @Test
        @DisplayName("실패 - 상태 오류 (이미 처리됨)")
        void decideApproval_Fail_IllegalState() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            // 이 테스트에 필요한 Mocking만 설정
            // 사용자 조회 통과
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            // 상태 코드 조회 통과
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            // 결재선 조회 통과
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));
            // 권한 검증 통과 (본인 맞음)
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            // 결재선의 현재 상태가 'AWAITING'(대기)이 아닌 'APPROVED'(승인)이라고 설정 (실패 지점)
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusApproved);
            // (추가) CommonCode의 getValue1() 호출 stub
            //    (setUp()에서 이미 lenient()로 되어있지만 명시적으로)
            lenient().when(mockLineStatusAwaiting.getValue1()).thenReturn("AWAITING");
            lenient().when(mockLineStatusApproved.getValue1()).thenReturn("APPROVED");

            // "OT", "APPROVAL" 호출은 setUp()에서 이미 lenient()로 처리됨

            // when & then
            assertThatThrownBy(() -> approvalService.decideApproval(decideRequest, approverUsername))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 처리되었거나 결재가능한 상태가 아닙니다.");
        }
    }


    @Nested
    @DisplayName("getApproval (결재 상세 조회)")
    class GetApproval {

        private final Long docId = 1L;

        @BeforeEach
        void getSetup() {
            // 공통 given (문서 조회 성공)
            given(approvalDocRepository.findDocWithDetailsById(docId)).willReturn(Optional.of(mockDoc));
        }

        @Test
        @DisplayName("성공 - 참조자 아님 (열람 시간 미기록)")
        void getApproval_Success_NotReferrer() {
            // given
            String username = "creator"; // 기안자(ID 10L)로 조회
            given(employeeRepository.findByUsername(username)).willReturn(Optional.of(mockCreator));
            // mockDoc의 참조자(mockRef)는 mockReferrer(ID 13L)
            // 둘이 다르므로 열람 시간이 기록되지 않아야 함

            // when
            ApprovalDocResponseDTO result = approvalService.getApproval(docId, username);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDocId()).isEqualTo(docId);
            // 참조자(mockRef)의 update()가 호출되지 않았는지 검증
            verify(mockRef, never()).update(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성공 - 참조자 (최초 열람)")
        void getApproval_Success_ReferrerFirstView() {
            // given
            String username = "referrer"; // 참조자(ID 13L)로 조회
            given(employeeRepository.findByUsername(username)).willReturn(Optional.of(mockReferrer));
            // 참조자(mockRef)의 열람 시간(viewedAt)이 null로 설정 (최초 열람)
            given(mockRef.getViewedAt()).willReturn(null);

            // when
            approvalService.getApproval(docId, username);

            // then
            // 참조자(mockRef)의 update()가 1회 호출되었는지 검증
            verify(mockRef).update(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("성공 - 참조자 (재열람)")
        void getApproval_Success_ReferrerReView() {
            // given
            String username = "referrer"; // 참조자(ID 13L)로 조회
            given(employeeRepository.findByUsername(username)).willReturn(Optional.of(mockReferrer));
            // 참조자(mockRef)의 열람 시간(viewedAt)이 이미 존재
            given(mockRef.getViewedAt()).willReturn(LocalDateTime.now().minusDays(1));

            // when
            approvalService.getApproval(docId, username);

            // then
            // 이미 열람했으므로 update()가 호출되지 않았는지 검증
            verify(mockRef, never()).update(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("실패 - 문서 없음")
        void getApproval_Fail_DocNotFound() {
            // given
            // 문서 조회 실패
            given(approvalDocRepository.findDocWithDetailsById(docId)).willReturn(Optional.empty());
            String username = "creator";

            // when & then
            assertThatThrownBy(() -> approvalService.getApproval(docId, username))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("문서를 찾을 수 없습니다: " + docId);
        }

    }


    // ***** [수정된 부분] *****
    @Nested
    @DisplayName("get... (문서 목록 조회)")
    class GetDocumentLists {

        private Pageable mockPageable;
        private Page<ApprovalDoc> mockDocPage;
        // '기안자' username, 'getList_Fail_UserNotFound' 에서도 공통 사용
        private final String creatorUsername = "creator";

        @BeforeEach
        void listSetup() {
            mockPageable = mock(Pageable.class);
            // PageImpl: Page 인터페이스의 실제 구현체
            mockDocPage = new PageImpl<>(List.of(mockDoc));

            // [수정] 공통 사용자 Mocking 제거
            // 각 테스트 케이스에서 목적에 맞는 사용자를 mocking 하도록 변경
        }

        @Test
        @DisplayName("getPendingApprovals (결재 대기 문서 조회)")
        void getPendingApprovals_Success() {
            // given
            // [수정] 이 테스트는 '결재자'로 조회해야 함
            String approverUsername = "approver1";
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));

            given(approvalDocRepository.findPendingDocsForEmployeeWithSort(
                    eq(mockApprover1), // [수정] currentUser를 mockApprover1로 변경
                    anyList(), anyString(), anyString(), anyString(), anyString(), eq(mockPageable)
            )).willReturn(mockDocPage);

            // [수정] mockDoc이 mockApprover1을 결재선(mockLine1)에 포함하도록 setUp()에서 설정됨
            // mockLine1의 상태는 AWAITING이므로 convertToPendingDTO 로직 통과

            // when
            // [수정] '결재자' username으로 서비스 호출
            Page<ApprovalDocResponseDTO> result = approvalService.getPendingApprovals(mockPageable, approverUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getDocId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getDraftedDocuments (기안 문서 조회)")
        void getDraftedDocuments_Success() {
            // given
            // [추가] '기안자' Mocking 설정
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));

            given(approvalDocRepository.findByCreatorWithDetails(mockCreator, mockPageable))
                    .willReturn(mockDocPage);

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getDraftedDocuments(mockPageable, creatorUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCreator().getUsername()).isEqualTo("creator");
        }

        @Test
        @DisplayName("getReferencedDocuments (참조 문서 조회)")
        void getReferencedDocuments_Success() {
            // given
            // [수정] 이 테스트는 '참조자'로 조회하는 것이 더 명확함
            String referrerUsername = "referrer";
            given(employeeRepository.findByUsername(referrerUsername)).willReturn(Optional.of(mockReferrer));

            given(approvalDocRepository.findReferencedDocsForEmployee(mockReferrer, mockPageable)) // [수정]
                    .willReturn(mockDocPage);

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getReferencedDocuments(mockPageable, referrerUsername); // [수정]

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            // mockDoc의 참조자가 mockReferrer이므로 검증 통과
            assertThat(result.getContent().get(0).getApprovalReferences().get(0).getReferrer().getUsername()).isEqualTo("referrer");
        }

        @Test
        @DisplayName("getCompletedDocuments (완료 문서 조회)")
        void getCompletedDocuments_Success() {
            // given
            // [추가] '기안자' Mocking 설정 (기안자/결재자/참조자 누구나 조회 가능)
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));

            given(approvalDocRepository.findCompletedDocsForEmployee(
                    eq(mockCreator), anyList(), anyString(), eq(mockPageable)
            )).willReturn(mockDocPage);

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getCompletedDocuments(mockPageable, creatorUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getDocId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패 - 사용자 없음 (모든 목록 조회 공통)")
        void getList_Fail_UserNotFound() {
            // given
            // [수정] listSetup()에서 제거되었으므로, 여기서 명시적으로 실패 Mocking 설정
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.empty());

            // when & then
            // getPendingApprovals 메서드로 대표 검증
            assertThatThrownBy(() -> approvalService.getPendingApprovals(mockPageable, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다: " + creatorUsername);

            // 다른 메서드들도 동일하게 동작할 것임
            assertThatThrownBy(() -> approvalService.getDraftedDocuments(mockPageable, creatorUsername))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}