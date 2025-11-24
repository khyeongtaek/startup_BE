package org.goodee.startup_BE.approval.service;

import jakarta.persistence.EntityNotFoundException;
import org.goodee.startup_BE.approval.dto.*;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.approval.enums.ApprovalTemplate;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.approval.repository.ApprovalLineRepository;
import org.goodee.startup_BE.approval.repository.ApprovalReferenceRepository;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.common.service.AttachmentFileService;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 확장 사용
class ApprovalServiceImplTest {

    @InjectMocks // 테스트 대상 클래스, Mock 객체들이 주입됨
    private ApprovalServiceImpl approvalService;

    // --- Mock 객체 ---
    @Mock
    private ApprovalDocRepository approvalDocRepository;
    @Mock
    private ApprovalLineRepository approvalLineRepository;
    @Mock
    private ApprovalReferenceRepository approvalReferenceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CommonCodeRepository commonCodeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private VacationApprovalService vacationApprovalService;

    // AttachmentFileService Mock 객체 추가
    @Mock
    private AttachmentFileService attachmentFileService;

    // --- 테스트용 Mock 엔티티 선언 ---
    private Employee mockCreator;
    private Employee mockApprover1;
    private Employee mockApprover2;
    private Employee mockReferrer;

    // CommonCode Mock 객체 사용
    private CommonCode mockTemplateCode;
    private CommonCode mockDocStatusInProgress;
    private CommonCode mockDocStatusApproved;
    private CommonCode mockDocStatusRejected;
    private CommonCode mockLineStatusPending;
    private CommonCode mockLineStatusAwaiting;
    private CommonCode mockLineStatusApproved;
    private CommonCode mockLineStatusRejected;
    private CommonCode mockOwnerCodeApproval; // 알림용 "APPROVAL" Owner Type

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
        // Mock 엔티티 초기화
        mockCreator = mock(Employee.class);
        mockApprover1 = mock(Employee.class);
        mockApprover2 = mock(Employee.class);
        mockReferrer = mock(Employee.class);

        // 결재 양식 Mock (CommonCode)
        mockTemplateCode = mock(CommonCode.class);
        lenient().when(mockTemplateCode.getCommonCodeId()).thenReturn(99L); // [수정] 테스트용 ID 추가

        // 상태 코드 Mock 초기화
        mockDocStatusInProgress = mock(CommonCode.class);
        mockDocStatusApproved = mock(CommonCode.class);
        mockDocStatusRejected = mock(CommonCode.class);
        mockLineStatusPending = mock(CommonCode.class);
        mockLineStatusAwaiting = mock(CommonCode.class);
        mockLineStatusApproved = mock(CommonCode.class);
        mockLineStatusRejected = mock(CommonCode.class);
        mockOwnerCodeApproval = mock(CommonCode.class);

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
        // Employee DTO 변환용
        lenient().when(mockEmpStatus.getCommonCodeId()).thenReturn(901L);
        lenient().when(mockEmpRole.getValue1()).thenReturn("ROLE_TEST");
        lenient().when(mockEmpDept.getValue1()).thenReturn("Test Dept");
        lenient().when(mockEmpPos.getValue1()).thenReturn("Test Pos");

        // 공통 Employee Mock 객체 Stubbing
        stubMockEmployee(mockCreator, 10L, "creator", "기안자");
        stubMockEmployee(mockApprover1, 11L, "approver1", "결재자1");
        stubMockEmployee(mockApprover2, 12L, "approver2", "결재자2");
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

        // 알림용 Owner Type Stubbing
        lenient().when(mockOwnerCodeApproval.getCommonCodeId()).thenReturn(301L);
        lenient().when(mockOwnerCodeApproval.getValue1()).thenReturn("APPROVAL");


        // --- 공통 Approval 객체 Mock Stubbing ---
        lenient().when(mockDoc.getDocId()).thenReturn(1L);
        lenient().when(mockDoc.getTitle()).thenReturn("테스트 기안 문서");
        lenient().when(mockDoc.getCreator()).thenReturn(mockCreator);
        lenient().when(mockDoc.getDocStatus()).thenReturn(mockDocStatusInProgress);
        lenient().when(mockDoc.getApprovalLineList()).thenReturn(List.of(mockLine1, mockLine2)); // 상세조회 시 사용
        lenient().when(mockDoc.getApprovalReferenceList()).thenReturn(List.of(mockRef)); // 상세조회 시 사용
        lenient().when(mockDoc.getApprovalTemplate()).thenReturn(mockTemplateCode);
        lenient().when(mockLine1.getDoc()).thenReturn(mockDoc);
        lenient().when(mockLine1.getLineId()).thenReturn(20L);
        lenient().when(mockLine1.getApprovalOrder()).thenReturn(1L);
        lenient().when(mockLine1.getEmployee()).thenReturn(mockApprover1);
        lenient().when(mockLine1.getApprovalStatus()).thenReturn(mockLineStatusAwaiting); // 1차 '대기'

        lenient().when(mockLine2.getDoc()).thenReturn(mockDoc);
        lenient().when(mockLine2.getLineId()).thenReturn(21L);
        lenient().when(mockLine2.getApprovalOrder()).thenReturn(2L);
        lenient().when(mockLine2.getEmployee()).thenReturn(mockApprover2);
        lenient().when(mockLine2.getApprovalStatus()).thenReturn(mockLineStatusPending); // 2차 '미결'

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
     */
    private void givenCommonCode(String prefix, String value, CommonCode code) {
        given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(prefix, value))
                .willReturn(List.of(code));
    }


    // ==================================================================================
    // getAllApprovalTemplates 테스트
    // ==================================================================================
    @Test
    @DisplayName("getAllApprovalTemplates: 결재 양식 목록 조회")
    void getAllApprovalTemplates_Success() {
        // given
        // '결재 양식'으로 조회 시 mockTemplateCode 반환
        given(commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse(ApprovalTemplate.PREFIX))
                .willReturn(List.of(mockTemplateCode));
        // DTO 변환을 위한 stub
        given(mockTemplateCode.getCommonCodeId()).willReturn(99L);
        given(mockTemplateCode.getCode()).willReturn("TPL_001");
        given(mockTemplateCode.getValue1()).willReturn("휴가신청서");


        // when
        List<CommonCodeResponseDTO> result = approvalService.getAllApprovalTemplates();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommonCodeId()).isEqualTo(99L);
        assertThat(result.get(0).getValue1()).isEqualTo("휴가신청서");
        then(commonCodeRepository).should(times(1)).findByCodeStartsWithAndIsDisabledFalse(ApprovalTemplate.PREFIX);
    }


    // ==================================================================================
    // createApproval 테스트
    // ==================================================================================
    @Nested
    @DisplayName("createApproval (결재 문서 생성)")
    class CreateApproval {

        private ApprovalDocRequestDTO requestDto;
        private final String creatorUsername = "creator";

        @BeforeEach
        void createSetup() {
            // DTO 생성
            requestDto = new ApprovalDocRequestDTO();
            requestDto.setTitle("새 기안 문서");
            requestDto.setContent("내용입니다.");
            requestDto.setVacationTypeCode(null);       // 휴가양식 아닌 상황 가정

            ApprovalLineRequestDTO lineDto1 = new ApprovalLineRequestDTO();
            lineDto1.setApprovalOrder(1L);
            lineDto1.setApproverId(11L); // mockApprover1

            ApprovalLineRequestDTO lineDto2 = new ApprovalLineRequestDTO();
            lineDto2.setApprovalOrder(2L);
            lineDto2.setApproverId(12L); // mockApprover2

            ApprovalReferenceRequestDTO refDto1 = new ApprovalReferenceRequestDTO();
            refDto1.setReferrerId(13L); // mockReferrer

            requestDto.setApprovalLines(List.of(lineDto1, lineDto2));
            requestDto.setApprovalReferences(List.of(refDto1));

            // --- 상태 공통 코드 Mocking
            lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AD", "IN_PROGRESS"))
                    .thenReturn(List.of(mockDocStatusInProgress));
            lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AL", "PENDING"))
                    .thenReturn(List.of(mockLineStatusPending));
            lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("AL", "AWAITING"))
                    .thenReturn(List.of(mockLineStatusAwaiting));
            lenient().when(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("OT", "APPROVAL"))
                    .thenReturn(List.of(mockOwnerCodeApproval));

            // --- templateCode 관련 Mocking
            // createApproval에서 사용하는 정확한 메서드:
            // commonCodeRepository.findByCodeStartsWithAndIsDeletedFalse(templateCode)


            // templateCode CommonCode Stub
            given(commonCodeRepository.findByCodeStartsWithAndIsDisabledFalse("AT2"))
                    .willReturn(List.of(mockTemplateCode));

            given(mockTemplateCode.getCode()).willReturn("AT2");
            given(mockTemplateCode.getValue1()).willReturn("출장신청서");
            given(mockTemplateCode.getValue2()).willReturn("BUSINESS_TRIP");

            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("OT", "APPROVAL"))
                    .willReturn(List.of(mockOwnerCodeApproval));  // 휴가 아님
            given(mockTemplateCode.getCommonCodeId()).willReturn(99L);

            given(approvalDocRepository.save(any(ApprovalDoc.class)))
                    .willReturn(mockDoc);


        }

        @Test
        @DisplayName("성공 - 출장 템플릿(AT2) 기반 결재 생성 성공")
        void createApproval_Success_BusinessTrip() {

            // === Given ===
            String creatorUsername = "creator";

            ApprovalDocRequestDTO requestDTO = new ApprovalDocRequestDTO();
            requestDTO.setTitle("출장 제목");
            requestDTO.setContent("출장 내용");
            requestDTO.setTemplateCode("AT2");

            // 출장 필수 값
            requestDTO.setTripLocation("서울 강남구");
            requestDTO.setTransportation("KTX");
            requestDTO.setTripPurpose("회의 참석");
            requestDTO.setTripRemark("비고");

            // 출장 날짜 필수
            requestDTO.setStartDate(LocalDateTime.now());
            requestDTO.setEndDate(LocalDateTime.now().plusDays(1));

            // 결재선
            ApprovalLineRequestDTO lineDto1 = new ApprovalLineRequestDTO();
            lineDto1.setApprovalOrder(1L);
            lineDto1.setApproverId(11L);

            ApprovalLineRequestDTO lineDto2 = new ApprovalLineRequestDTO();
            lineDto2.setApprovalOrder(2L);
            lineDto2.setApproverId(12L);

            requestDTO.setApprovalLines(List.of(lineDto1, lineDto2));

            // 참조자
            ApprovalReferenceRequestDTO refDto = new ApprovalReferenceRequestDTO();
            refDto.setReferrerId(13L);
            requestDTO.setApprovalReferences(List.of(refDto));

            // --- Mocking 영역 ---

            // creator, 결재자, 참조자 조회
            given(employeeRepository.findByUsername(creatorUsername))
                    .willReturn(Optional.of(mockCreator));
            given(employeeRepository.findById(11L)).willReturn(Optional.of(mockApprover1));
            given(employeeRepository.findById(12L)).willReturn(Optional.of(mockApprover2));
            given(employeeRepository.findById(13L)).willReturn(Optional.of(mockReferrer));

            // 문서 상태 / 결재선 상태
            givenCommonCode("AD", "IN_PROGRESS", mockDocStatusInProgress);
            givenCommonCode("AL", "PENDING", mockLineStatusPending);
            givenCommonCode("AL", "AWAITING", mockLineStatusAwaiting);
            givenCommonCode("OT", "APPROVAL", mockOwnerCodeApproval);

            // === 출장 템플릿 Mock ===
            given(mockTemplateCode.getCode()).willReturn("AT2");
            given(mockTemplateCode.getValue1()).willReturn("출장신청서");
            given(mockTemplateCode.getValue2()).willReturn("BUSINESS_TRIP");


            // 문서 저장 Mock
            given(approvalDocRepository.save(any(ApprovalDoc.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // === When ===
            ApprovalDocResponseDTO result =
                    approvalService.createApproval(requestDTO, creatorUsername);

            // === Then ===

            // 1) 전체 결과 객체가 정상 생성되었는지
            assertThat(result).isNotNull();

        // 2) 비즈니스적으로 중요한 필드 검증
            assertThat(result.getTitle()).isEqualTo("출장 제목");
            assertThat(result.getContent()).isEqualTo("출장 내용");
            assertThat(result.getCreator().getUsername()).isEqualTo("creator");

        // 3) 결재선
            ArgumentCaptor<List<ApprovalLine>> captor = ArgumentCaptor.forClass(List.class);
            then(approvalLineRepository).should(times(1)).saveAll(captor.capture());

            List<ApprovalLine> savedLines = captor.getValue();
            assertThat(savedLines).hasSize(2);

        // 4) (중요) docId는 JPA가 아니면 세팅되지 않으므로, 값 자체를 강제하지 않는다.
        // 필요하다면 "null이어도 된다" 정도로만 검증
        // assertThat(result.getDocId()).isNull();  // 이렇게 명시해도 됨
        // 혹은 그냥 docId에 대한 검증 자체를 제거해도 됨.

        // Mock 검증
            then(approvalDocRepository).should(times(1)).save(any());
            then(approvalLineRepository).should(times(1)).saveAll(anyList());
            then(approvalReferenceRepository).should(times(1)).saveAll(anyList());

            // 알림 총 4회
            then(notificationService).should(times(4)).create(any());
        }

    }



    // ==================================================================================
    // decideApproval 테스트
    // ==================================================================================
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
            decideRequest = new ApprovalLineRequestDTO();
            decideRequest.setLineId(lineId);
            decideRequest.setComment("테스트 의견");

            // --- 공통 Mocking (Service의 getCommonCode 및 알림 로직) ---
            givenCommonCode("OT", "APPROVAL", mockOwnerCodeApproval);
        }

        @Test
        @DisplayName("성공 - 1차 승인 (다음 결재자 있음)")
        void decideApproval_Success_ApproveAndNextExists() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved)); // '승인' 코드 조회
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            // 검증 로직 통과
            given(mockLine1.getEmployee()).willReturn(mockApprover1); // 1. 본인 맞음
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting); // 2. '대기' 상태 맞음
            given(mockLine1.getDoc()).willReturn(mockDoc);

            // 다음 결재선(mockLine2) 조회 성공
            given(mockLine1.getApprovalOrder()).willReturn(1L);
            given(approvalLineRepository.findByDocAndApprovalOrder(mockDoc, 2L)).willReturn(Optional.of(mockLine2));

            // 다음 결재선 상태('AWAITING')로 변경하기 위한 공통 코드 조회
            givenCommonCode("AL", "AWAITING", mockLineStatusAwaiting);

            // when
            ApprovalDocResponseDTO result = approvalService.decideApproval(decideRequest, approverUsername);

            // then
            assertThat(result).isNotNull();
            // 1. 현재 결재선 상태 '승인' 변경
            then(mockLine1).should(times(1)).updateApprovalStatus(mockLineStatusApproved);
            then(mockLine1).should(times(1)).updateComment("테스트 의견");
            // 2. 문서 수정자 변경
            then(mockDoc).should(times(1)).updateUpdater(mockApprover1);
            // 3. 다음 결재선(mockLine2) 상태 '대기' 변경
            then(mockLine2).should(times(1)).updateApprovalStatus(mockLineStatusAwaiting);
            // 4. 문서(Doc) 상태는 변경 X
            then(mockDoc).should(never()).updateDocStatus(any(CommonCode.class));
            // 5. 알림 (다음 결재자 1회)
            then(notificationService).should(times(1)).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("성공 - 최종 승인 (다음 결재자 없음)")
        void decideApproval_Success_ApproveFinal() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));
            given(mockDoc.getApprovalTemplate()).willReturn(mockTemplateCode);
            given(mockTemplateCode.getValue2()).willReturn("VACATION");
            doNothing().when(vacationApprovalService).handleApprovedVacation(anyLong());

            // 검증 통과
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting);
            given(mockLine1.getDoc()).willReturn(mockDoc);

            // 다음 결재선 조회 실패 (Optional.empty())
            given(mockLine1.getApprovalOrder()).willReturn(1L); // (마지막 순서라고 가정)
            given(approvalLineRepository.findByDocAndApprovalOrder(mockDoc, 2L)).willReturn(Optional.empty());

            // 문서 상태('APPROVED')로 변경하기 위한 공통 코드 조회
            givenCommonCode("AD", "APPROVED", mockDocStatusApproved);

            // when
            approvalService.decideApproval(decideRequest, approverUsername);

            // then
            // 1. 현재 결재선 '승인' 변경
            then(mockLine1).should(times(1)).updateApprovalStatus(mockLineStatusApproved);
            // 2. 다음 결재선 상태 변경 시도 X
            then(mockLine2).should(never()).updateApprovalStatus(any(CommonCode.class));
            // 3. 문서(Doc) 상태가 '최종 승인'으로 변경
            then(mockDoc).should(times(1)).updateDocStatus(mockDocStatusApproved);
            // 4. 알림 (기안자 1회)
            then(notificationService).should(times(1)).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("성공 - 반려")
        void decideApproval_Success_Reject() {
            // given
            decideRequest.setStatusCodeId(rejectedCodeId);

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(commonCodeRepository.findById(rejectedCodeId)).willReturn(Optional.of(mockLineStatusRejected)); // '반려' 코드 조회
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            // 검증 통과
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusAwaiting);
            given(mockLine1.getDoc()).willReturn(mockDoc);

            // 문서 상태('REJECTED')로 변경하기 위한 공통 코드 조회
            givenCommonCode("AD", "REJECTED", mockDocStatusRejected);

            // when
            approvalService.decideApproval(decideRequest, approverUsername);

            // then
            // 1. 현재 결재선 '반려' 변경
            then(mockLine1).should(times(1)).updateApprovalStatus(mockLineStatusRejected);
            // 2. 문서(Doc) 상태 '최종 반려' 변경
            then(mockDoc).should(times(1)).updateDocStatus(mockDocStatusRejected);
            // 3. (중요) 반려 시 다음 결재선 찾지 않음
            then(approvalLineRepository).should(never()).findByDocAndApprovalOrder(any(), any());
            // 4. 알림 (기안자 1회)
            then(notificationService).should(times(1)).create(any(NotificationRequestDTO.class));
        }

        @Test
        @DisplayName("실패 - 권한 없음 (본인 아님)")
        void decideApproval_Fail_AccessDenied() {
            // given
            decideRequest.setStatusCodeId(approvedCodeId);

            // 로그인 사용자를 '결재자2'(mockApprover2)로 설정
            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover2));
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            // 결재선(mockLine1)의 주인은 '결재자1'(mockApprover1) (실패 지점)
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            // ID가 다름
            lenient().when(mockApprover1.getEmployeeId()).thenReturn(11L);
            lenient().when(mockApprover2.getEmployeeId()).thenReturn(12L);

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

            given(employeeRepository.findByUsername(approverUsername)).willReturn(Optional.of(mockApprover1));
            given(commonCodeRepository.findById(approvedCodeId)).willReturn(Optional.of(mockLineStatusApproved));
            given(approvalLineRepository.findById(lineId)).willReturn(Optional.of(mockLine1));

            // 권한 검증 통과 (본인 맞음)
            given(mockLine1.getEmployee()).willReturn(mockApprover1);
            // 결재선 현재 상태가 'AWAITING'(대기)이 아닌 'APPROVED'(승인) (실패 지점)
            given(mockLine1.getApprovalStatus()).willReturn(mockLineStatusApproved);

            // when & then
            assertThatThrownBy(() -> approvalService.decideApproval(decideRequest, approverUsername))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 처리되었거나 결재가능한 상태가 아닙니다.");
        }
    }


    // ==================================================================================
    // getApproval (상세 조회) 테스트
    // ==================================================================================
    @Nested
    @DisplayName("getApproval (결재 상세 조회)")
    class GetApproval {

        private final Long docId = 1L;
        private final String creatorUsername = "creator";
        private final String referrerUsername = "referrer";

        @BeforeEach
        void getSetup() {
            // 공통 given (문서 조회 성공)
            // (참고: setUp()에서 mockDoc은 mockLine1, mockLine2, mockRef를 리스트로 반환하도록 stub됨)
            given(approvalDocRepository.findDocWithDetailsById(docId)).willReturn(Optional.of(mockDoc));

            // 추가된 로직(파일 서비스)을 위한 Mock Stubbing
            // getApproval() 메서드에서 'OT', 'APPROVAL' 코드를 조회하므로 stubbing 추가
            given(commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues("OT", "APPROVAL"))
                    .willReturn(List.of(mockOwnerCodeApproval));

            // attachmentFileService.listFiles()가 호출되므로 Mocking
            // lenient()를 사용하여 호출되지 않아도 오류가 발생하지 않도록 함
            lenient().when(attachmentFileService.listFiles(anyLong(), anyLong())).thenReturn(List.of());
        }

        @Test
        @DisplayName("성공 - 참조자 아님 (열람 시간 미기록)")
        void getApproval_Success_NotReferrer() {
            // given
            given(employeeRepository.findByUsername(creatorUsername)).willReturn(Optional.of(mockCreator));
            // (mockCreator(10L)는 mockRef의 주인(13L)이 아님)

            // when
            ApprovalDocResponseDTO result = approvalService.getApproval(docId, creatorUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getDocId()).isEqualTo(docId);
            // 참조자(mockRef)의 update()가 호출되지 않았는지 검증
            then(mockRef).should(never()).update(any(LocalDateTime.class));
            // 파일 서비스가 호출되었는지 검증
            then(attachmentFileService).should(times(2))
                    .listFiles(anyLong(), eq(docId));

        }

        @Test
        @DisplayName("성공 - 참조자 (최초 열람)")
        void getApproval_Success_ReferrerFirstView() {
            // given
            given(employeeRepository.findByUsername(referrerUsername)).willReturn(Optional.of(mockReferrer));
            // (mockReferrer(13L)는 mockRef의 주인(13L)이 맞음)

            // 참조자(mockRef)의 열람 시간(viewedAt)이 null (최초 열람)
            given(mockRef.getViewedAt()).willReturn(null);

            // when
            approvalService.getApproval(docId, referrerUsername);

            // then
            // 참조자(mockRef)의 update()가 1회 호출
            then(mockRef).should(times(1)).update(any(LocalDateTime.class));
            // 파일 서비스가 호출되었는지 검증
            then(attachmentFileService).should(times(2))
                    .listFiles(anyLong(), eq(docId));

        }

        @Test
        @DisplayName("성공 - 참조자 (재열람)")
        void getApproval_Success_ReferrerReView() {
            // given
            given(employeeRepository.findByUsername(referrerUsername)).willReturn(Optional.of(mockReferrer));
            // 참조자(mockRef)의 열람 시간(viewedAt)이 이미 존재
            given(mockRef.getViewedAt()).willReturn(LocalDateTime.now().minusDays(1));

            // when
            approvalService.getApproval(docId, referrerUsername);

            // then
            // 이미 열람했으므로 update()가 호출되지 않음
            then(mockRef).should(never()).update(any(LocalDateTime.class));
            // 파일 서비스가 호출되었는지 검증
            then(attachmentFileService).should(times(2))
                    .listFiles(anyLong(), eq(docId));

        }
    }


    // ==================================================================================
    // 목록 조회 테스트
    // ==================================================================================
    @Nested
    @DisplayName("get... (문서 목록 조회)")
    class GetDocumentLists {

        private Pageable mockPageable;
        private Page<ApprovalDoc> mockDocPage;
        private final String testUsername = "testUser";

        @BeforeEach
        void listSetup() {
            mockPageable = mock(Pageable.class);
            // PageImpl: Page 인터페이스의 실제 구현체
            // (참고: setUp()에서 mockDoc은 mockLine1(AWAITING), mockLine2(PENDING)를 반환)
            mockDocPage = new PageImpl<>(List.of(mockDoc));

            // 목록 조회 테스트는 'testUser' (mockCreator)로 대표 진행
            given(employeeRepository.findByUsername(testUsername)).willReturn(Optional.of(mockCreator));
        }

        @Test
        @DisplayName("getPendingApprovals (결재 대기 문서 조회)")
        void getPendingApprovals_Success() {
            // given
            given(approvalDocRepository.findPendingDocsForEmployeeWithSort(
                    eq(mockCreator), anyList(), anyString(), anyString(), anyString(), anyString(), eq(mockPageable)
            )).willReturn(mockDocPage);

            // (mockCreator가 mockApprover1이라고 가정)
            // convertToPendingDTO 헬퍼 메서드 테스트를 위해,
            // '내'가 '대기' 상태(mockLine1)를 갖도록 stub
            lenient().when(mockCreator.getEmployeeId()).thenReturn(11L); // mockApprover1의 ID

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getPendingApprovals(mockPageable, testUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            // convertToPendingDTO 로직에 의해,
            // '내'가 '대기'(AWAITING) 상태이므로 내 결재선(mockLine1)만 포함되어야 함
            assertThat(result.getContent().get(0).getApprovalLines()).hasSize(1);
            assertThat(result.getContent().get(0).getApprovalLines().get(0).getLineId()).isEqualTo(20L); // mockLine1
        }

        @Test
        @DisplayName("getDraftedDocuments (기안 문서 조회)")
        void getDraftedDocuments_Success() {
            // given
            given(approvalDocRepository.findByCreatorWithDetails(mockCreator, mockPageable))
                    .willReturn(mockDocPage);

            // (mockDoc의 상태는 IN_PROGRESS, mockLine1은 AWAITING)

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getDraftedDocuments(mockPageable, testUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            // convertToDraftedDTO 로직에 의해,
            // '진행중'이므로 '대기'중인 결재선(mockLine1)만 포함
            assertThat(result.getContent().get(0).getApprovalLines()).hasSize(1);
            assertThat(result.getContent().get(0).getApprovalLines().get(0).getLineId()).isEqualTo(20L); // mockLine1
        }

        @Test
        @DisplayName("getReferencedDocuments (참조 문서 조회)")
        void getReferencedDocuments_Success() {
            // given
            given(approvalDocRepository.findReferencedDocsForEmployee(mockCreator, mockPageable))
                    .willReturn(mockDocPage);

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getReferencedDocuments(mockPageable, testUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            // 참조 DTO가 포함되었는지 확인
            assertThat(result.getContent().get(0).getApprovalReferences()).hasSize(1);
            assertThat(result.getContent().get(0).getApprovalReferences().get(0).getReferenceId()).isEqualTo(30L); // mockRef
        }

        @Test
        @DisplayName("getCompletedDocuments (완료 문서 조회)")
        void getCompletedDocuments_Success() {
            // given
            given(approvalDocRepository.findCompletedDocsForEmployee(
                    eq(mockCreator), anyList(), anyString(), eq(mockPageable)
            )).willReturn(mockDocPage);

            // when
            Page<ApprovalDocResponseDTO> result = approvalService.getCompletedDocuments(mockPageable, testUsername);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            // convertToCompletedDTO 로직에 의해,
            // '승인' 또는 '반려'된 마지막 결재선만 포함 (여기서는 mockLine1/2가 아님 -> 0개)
            // (만약 mockLine1이 APPROVED였다면 1개)
            assertThat(result.getContent().get(0).getApprovalLines()).hasSize(0);
        }
    }
}