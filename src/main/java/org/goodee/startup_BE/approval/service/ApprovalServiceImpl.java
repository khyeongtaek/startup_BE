package org.goodee.startup_BE.approval.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.goodee.startup_BE.approval.dto.*;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.approval.entity.ApprovalLine;
import org.goodee.startup_BE.approval.entity.ApprovalReference;
import org.goodee.startup_BE.approval.enums.ApprovalDocStatus;
import org.goodee.startup_BE.approval.enums.ApprovalLineStatus;
import org.goodee.startup_BE.approval.repository.ApprovalDocRepository;
import org.goodee.startup_BE.approval.repository.ApprovalLineRepository;
import org.goodee.startup_BE.approval.repository.ApprovalReferenceRepository;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.enums.OwnerType;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.notification.dto.NotificationRequestDTO;
import org.goodee.startup_BE.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalDocRepository approvalDocRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalReferenceRepository approvalReferenceRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final NotificationService notificationService;

    // --- 공통 코드 Prefix 정의 ---
    private static final String DOC_STATUS_PREFIX = ApprovalDocStatus.PREFIX;
    private static final String LINE_STATUS_PREFIX = ApprovalLineStatus.PREFIX;

    // --- 공통 코드 Value1 정의 ---
    // 문서 상태
    private static final String DOC_STATUS_IN_PROGRESS = ApprovalDocStatus.IN_PROGRESS.name(); // 진행중
    private static final String DOC_STATUS_APPROVED = ApprovalDocStatus.APPROVED.name(); // 최종 승인
    private static final String DOC_STATUS_REJECTED = ApprovalDocStatus.REJECTED.name(); // 최종 반려
    // 결재선 상태
    private static final String LINE_STATUS_PENDING = ApprovalLineStatus.PENDING.name(); // 미결 (AL1)
    private static final String LINE_STATUS_AWAITING = ApprovalLineStatus.AWAITING.name(); // 대기 (AL2)
    private static final String LINE_STATUS_APPROVED = ApprovalLineStatus.APPROVED.name(); // 승인 (AL3)
    private static final String LINE_STATUS_REJECTED = ApprovalLineStatus.REJECTED.name(); // 반려 (AL4)


    /**
     * 상신 (결재 문서 생성)
     */
    @Override
    @Transactional
    public ApprovalDocResponseDTO createApproval(ApprovalDocRequestDTO request, String username) {
        Employee creator = getCurrentEmployee(username);

        // 1. 공통 코드 조회
        CommonCode docStatus = getCommonCode(DOC_STATUS_PREFIX, DOC_STATUS_IN_PROGRESS); // '진행중'
        CommonCode linePendingStatus = getCommonCode(LINE_STATUS_PREFIX, LINE_STATUS_PENDING); // '미결' (AL1)
        CommonCode lineAwaitingStatus = getCommonCode(LINE_STATUS_PREFIX, LINE_STATUS_AWAITING); // '대기' (AL2)
        CommonCode ownerCode = commonCodeRepository                                            // 결재 모듈
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.APPROVAL.name())
                .get(0);

        // 2. 문서(Doc) 생성 및 저장 (ID 자동 생성)
        ApprovalDoc doc = approvalDocRepository.save(request.toEntity(creator, docStatus));

        // 3. 결재선(Lines) 생성
        List<ApprovalLineRequestDTO> lineRequests = request.getApprovalLines();
        List<ApprovalLine> lineList = new ArrayList<>();

        for (ApprovalLineRequestDTO lineDto : lineRequests) {
            Employee approver = employeeRepository.findById(lineDto.getApproverId())
                    .orElseThrow(() -> new EntityNotFoundException("결재자를 찾을 수 없습니다: " + lineDto.getApproverId()));

            // 첫 번째 결재자는 '대기'(AWAITING), 나머지는 '미결'(PENDING)로 설정
            CommonCode initialLineStatus;
            if (lineDto.getApprovalOrder() == 1L) {
                initialLineStatus = lineAwaitingStatus;
            } else {
                initialLineStatus = linePendingStatus;
            }

            ApprovalLine line = lineDto.toEntity(doc, approver, initialLineStatus);
            lineList.add(line);
        }
        approvalLineRepository.saveAll(lineList);

        // 4. 참조자가 있는 경우 ApprovalReference 생성
        List<ApprovalReference> refList = new ArrayList<>(); // 알림 발송을 위해 리스트 생성
        if (request.getApprovalReferences() != null && !request.getApprovalReferences().isEmpty()) {
            for (ApprovalReferenceRequestDTO refDto : request.getApprovalReferences()) {
                Employee referrer = employeeRepository.findById(refDto.getReferrerId())
                        .orElseThrow(() -> new EntityNotFoundException("참조자를 찾을 수 없습니다: " + refDto.getReferrerId()));

                ApprovalReference reference = refDto.toEntity(doc, referrer);
                refList.add(reference); // 리스트에 추가
            }
            approvalReferenceRepository.saveAll(refList); // 참조자 일괄 저장
        }

        // 5. 알림 발송
        sendCreationNotifications(doc, lineList, refList, ownerCode);

        // 6. 저장된 전체 문서를 DTO로 변환하여 반환
        return convertToDocResponseDTO(doc);
    }


    /**
     * 결재 승인 / 반려
     */
    @Override
    @Transactional
    public ApprovalDocResponseDTO decideApproval(ApprovalLineRequestDTO request, String username) {
        Employee loginUser = getCurrentEmployee(username);
        Long lineId = request.getLineId();
        Long newStatusCodeId = request.getStatusCodeId();
        String comment = request.getComment();
        CommonCode finalDocStatus = null; // 최종 승인/최종반려 여부.
        CommonCode ownerCode = commonCodeRepository  // 결재 모듈
                .findByCodeStartsWithAndKeywordExactMatchInValues(OwnerType.PREFIX, OwnerType.APPROVAL.name())
                .get(0);
        ApprovalLine nextLine = null;   // 다음 결재선 정보

        // 1. 전달 받은 상태 id를 공통 코드에서 조회 (승인 또는 반려)
        CommonCode newStatus = commonCodeRepository.findById(newStatusCodeId)
                .orElseThrow(() -> new EntityNotFoundException("결재 상태 코드를 찾을 수 없습니다: " + newStatusCodeId));

        String newStatusValue = newStatus.getValue1(); // APPROVED 또는 REJECTED

        // 2. 결재선(Line) 조회 및 검증
        ApprovalLine line = approvalLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("결재선을 찾을 수 없습니다: " + lineId));

        // 2-1. 본인이 맞는지 검증
        if (!line.getEmployee().getEmployeeId().equals(loginUser.getEmployeeId())) {
            throw new AccessDeniedException("이 결재를 처리할 권한이 없습니다.");
        }

        // 2-2. '대기'(결재처리가 가능한 AWAITING 상태) 상태가 맞는지 검증
        if (line.getApprovalStatus() == null || !line.getApprovalStatus().getValue1().equals(LINE_STATUS_AWAITING)) {
            throw new IllegalStateException("이미 처리되었거나 결재가능한 상태가 아닙니다.");
        }

        // 3. 결재선 상태(승인/반려)와 결재 의견 업데이트
        line.updateApprovalStatus(newStatus);
        line.updateComment(comment);

        // 4. 문서(Doc) 및 다음 결재선 상태 업데이트
        ApprovalDoc doc = line.getDoc();
        doc.updateUpdater(loginUser); // 문서 최종 수정자 업데이트

        if (newStatusValue.equals(LINE_STATUS_REJECTED)) {  // 4-1. '반려'된 경우: 문서 상태를 '최종 반려'로 변경
            finalDocStatus = getCommonCode(DOC_STATUS_PREFIX, DOC_STATUS_REJECTED);
            doc.updateDocStatus(finalDocStatus);

        } else if (newStatusValue.equals(LINE_STATUS_APPROVED)) {   // 4-2. '승인'된 경우
            // 다음 결재 순서 찾기
            long nextOrder = line.getApprovalOrder() + 1;

            // 레포지토리에서 다음 순서 결재선 조회
            Optional<ApprovalLine> nextLineOpt = approvalLineRepository.findByDocAndApprovalOrder(doc, nextOrder);

            if (nextLineOpt.isPresent()) {  // 4-2-1. 다음 결재자가 있으면: 다음 결재선의 상태를 '미결'(PENDING) -> '대기'(AWAITING)로 변경
                nextLine = nextLineOpt.get();
                CommonCode awaitingStatus = getCommonCode(LINE_STATUS_PREFIX, LINE_STATUS_AWAITING);    //대기 코드 가져옴

                nextLine.updateApprovalStatus(awaitingStatus);                // 대기로 변경
            } else {
                // 4-2-2. 다음 결재자가 없으면 (최종 승인): 문서 상태를 '최종 승인'으로 변경
                finalDocStatus = getCommonCode(DOC_STATUS_PREFIX, DOC_STATUS_APPROVED);
                doc.updateDocStatus(finalDocStatus);
            }
        }

        // 5. 알림 발송
        sendDecisionNotifications(doc, nextLine, finalDocStatus, ownerCode);

        // 6. 업데이트된 결재 문서 상세 반환
        return convertToDocResponseDTO(doc);
    }


    /**
     * 결재 상세 조회
     */
    @Override
    @Transactional
    public ApprovalDocResponseDTO getApproval(Long approvalDocId, String username) {
        ApprovalDoc doc = approvalDocRepository.findDocWithDetailsById(approvalDocId)
                .orElseThrow(() -> new EntityNotFoundException("문서를 찾을 수 없습니다: " + approvalDocId));

        // 참조자인 경우 '열람' 처리
        Employee currentUser = getCurrentEmployee(username);

        // 레포지토리에서 doc과 employee로 참조자 정보 조회

        doc.getApprovalReferenceList()
                .stream()
                .filter(ref -> ref.getEmployee().getEmployeeId().equals(currentUser.getEmployeeId()))
                .findAny()
                .ifPresent(myReference -> {
                    if (myReference.getViewedAt() == null) {    // 최초 열람만 기록
                        myReference.update(LocalDateTime.now());
                    }
                });

        // 헬퍼 메서드를 통해 DTO 변환
        return convertToDocResponseDTO(doc);
    }

    /**
     * 결재 대기 문서 조회 (내가 결재자이면서, 아직 최종 결과가 나오지 않은 결재)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalDocResponseDTO> getPendingApprovals(Pageable pageable, String username) {
        Employee currentUser = getCurrentEmployee(username);

        return approvalDocRepository.findPendingDocsForEmployeeWithSort(
                currentUser,
                List.of(DOC_STATUS_APPROVED, DOC_STATUS_REJECTED),
                DOC_STATUS_PREFIX,
                LINE_STATUS_AWAITING, // "대기"
                LINE_STATUS_PENDING,  // "미결"
                LINE_STATUS_APPROVED, // "승인"
                pageable
        ).map(this::convertToDocResponseDTO);
    }

    /**
     * 결재 기안 문서 조회 (내가 올린 문서)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalDocResponseDTO> getDraftedDocuments(Pageable pageable, String username) {
        Employee currentUser = getCurrentEmployee(username);

        return approvalDocRepository
                .findByCreatorWithDetails(currentUser, pageable)
                .map(this::convertToDocResponseDTO);
    }

    /**
     * 결재 참조 문서 조회 (내가 참조된 문서)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalDocResponseDTO> getReferencedDocuments(Pageable pageable, String username) {
        Employee currentUser = getCurrentEmployee(username);

        return approvalDocRepository
                .findReferencedDocsForEmployee(currentUser, pageable)
                .map(this::convertToDocResponseDTO);
    }

    /**
     * 결재 완료 문서 조회 (내 결재가 포함된 완료/반려 문서)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalDocResponseDTO> getCompletedDocuments(Pageable pageable, String username) {
        Employee currentUser = getCurrentEmployee(username);

        return approvalDocRepository.findCompletedDocsForEmployee(
                currentUser,
                List.of(DOC_STATUS_APPROVED, DOC_STATUS_REJECTED),
                DOC_STATUS_PREFIX, // 'AD' prefix
                pageable
        ).map(this::convertToDocResponseDTO);
    }


    /**
     * 현재 로그인된 사용자 정보 조회
     */
    private Employee getCurrentEmployee(String username) {
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 공통 코드 조회
     *
     * @param codePrefix (예: "AD", "AL")
     * @param value1     (예: "IN_PROGRESS", "PENDING")
     * @return CommonCode 엔티티
     */
    private CommonCode getCommonCode(String codePrefix, String value1) {
        try {
            List<CommonCode> codes = commonCodeRepository.findByCodeStartsWithAndKeywordExactMatchInValues(
                    codePrefix,
                    value1
            );
            if (codes.isEmpty()) {
                throw new EntityNotFoundException("공통 코드를 찾을 수 없습니다: " + codePrefix + ", " + value1);
            }
            return codes.get(0);
        } catch (Exception e) {
            log.error("공통 코드 조회 중 오류 발생: {} / {}", codePrefix, value1, e);
            throw new EntityNotFoundException("공통 코드 조회 실패: " + codePrefix + ", " + value1);
        }
    }

    /**
     * DTO 변환 (엔티티 -> DTO) - (*** 핵심 수정 사항 ***)
     * N+1 문제 해결을 위해 리포지토리 재조회 로직 제거
     *
     * @param doc 변환할 ApprovalDoc 엔티티 (JOIN FETCH로 Lines, Refs가 채워져 있어야 함)
     * @return ApprovalDocResponseDTO
     */
    private ApprovalDocResponseDTO convertToDocResponseDTO(ApprovalDoc doc) {

        // 1. 엔티티의 Getter를 사용하여 이미 로드된 결재선 목록 변환 (수정됨)
        // (JOIN FETCH로 미리 로드되었거나, Lazy Loading으로 여기서 조회됨)
        List<ApprovalLineResponseDTO> lineDTOs =
                doc.getApprovalLineList()
                        .stream()
                        .sorted(Comparator.comparing(ApprovalLine::getApprovalOrder)) // 순서 보장
                        .map(ApprovalLineResponseDTO::toDTO)
                        .collect(Collectors.toList());

        // 2. 엔티티의 Getter를 사용하여 이미 로드된 참조자 목록 변환 (수정됨)
        List<ApprovalReferenceResponseDTO> refDTOs =
                doc.getApprovalReferenceList()
                        .stream()
                        .map(ApprovalReferenceResponseDTO::toDTO)
                        .collect(Collectors.toList());

        // 3. 최종 DTO 조합
        return ApprovalDocResponseDTO.toDTO(doc, lineDTOs, refDTOs);
    }

    /**
     * [헬퍼] 신규 결재 생성 시 알림 발송
     */
    private void sendCreationNotifications(ApprovalDoc doc, List<ApprovalLine> lines, List<ApprovalReference> references, CommonCode ownerCode) {
        // 결재자에게 알림 발송
        lines.forEach(line -> {
            notificationService.create(
                    NotificationRequestDTO
                            .builder()
                            .employeeId(line.getEmployee().getEmployeeId())
                            .ownerTypeCommonCodeId(ownerCode.getCommonCodeId())
                            .url("/approval/detail/" + doc.getDocId())
                            .title("새로운 결재의 결재자로 등록되었습니다.")
                            .content(doc.getTitle())
                            .build());
            if (line.getApprovalOrder() == 1L) {  // 첫번째 결재자라면 바로 결재 차례를 추가로 알림
                notificationService.create(
                        NotificationRequestDTO
                                .builder()
                                .employeeId(line.getEmployee().getEmployeeId())
                                .ownerTypeCommonCodeId(ownerCode.getCommonCodeId())
                                .url("/approval/detail/" + doc.getDocId())
                                .title("결재 대기중인 문서가 있습니다.")
                                .content(doc.getTitle())
                                .build());
            }

        });

        // 참조자에게 알림 발송
        references.forEach(reference -> {
            notificationService.create(
                    NotificationRequestDTO
                            .builder()
                            .employeeId(reference.getEmployee().getEmployeeId())
                            .ownerTypeCommonCodeId(ownerCode.getCommonCodeId())
                            .url("/approval/detail/" + doc.getDocId())
                            .title("새로운 결재의 참조자로 등록되었습니다.")
                            .content(doc.getTitle())
                            .build());
        });
    }

    /**
     * [헬퍼] 결재 승인/반려 시 알림 발송
     */
    private void sendDecisionNotifications(ApprovalDoc doc, ApprovalLine nextLine, CommonCode finalDocStatus, CommonCode ownerCode) {
        // 최종 완료 상태 변경이 있다면 기안자에게 알림 발송
        if (finalDocStatus != null) {
            notificationService.create(
                    NotificationRequestDTO
                            .builder()
                            .employeeId(doc.getCreator().getEmployeeId())
                            .ownerTypeCommonCodeId(ownerCode.getCommonCodeId())
                            .url("/approval/detail/" + doc.getDocId())
                            .title("상신한 결재가 " + finalDocStatus.getValue1() + "되었습니다.") // APPROVED 또는 REJECTED
                            .content(doc.getTitle())
                            .build());
        } else { // 최종 완료가 아니라면 다음 결재자에게만 알림 보냄.
            // (로직상 nextLine은 null이 될 수 없음)
            notificationService.create(
                    NotificationRequestDTO
                            .builder()
                            .employeeId(nextLine.getEmployee().getEmployeeId())
                            .ownerTypeCommonCodeId(ownerCode.getCommonCodeId())
                            .url("/approval/detail/" + doc.getDocId())
                            .title("결재 대기중인 문서가 있습니다.")
                            .content(doc.getTitle())
                            .build());
        }
    }

}