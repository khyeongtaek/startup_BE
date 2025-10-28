package org.goodee.startup_BE.approval.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.entity.Employee;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결재 문서를 위한 통합 Request DTO
 * (Doc + Lines + References)
 */
@Getter
@ToString
@NoArgsConstructor
public class ApprovalDocRequestDTO {

    @NotNull(message = "문서 ID는 필수입니다.", groups = {ValidationGroups.Update.class})
    private Long docId;

    @NotEmpty(message = "제목은 필수입니다.", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String title;

    @NotEmpty(message = "내용은 필수입니다.", groups = ValidationGroups.Create.class)
    private String content;

    private LocalDateTime startDate;

    private LocalDateTime endDate;


    @Valid // 이 리스트 내부의 DTO들도 유효성 검사를 수행
    // '결재 요청' 시에는 결재선 리스트가 null이 아니고, 1명 이상이어야 함
    @NotNull(message = "결재선 정보는 필수입니다.", groups = ValidationGroups.Create.class)
    @Size(min = 1, message = "결재선은 최소 1명 이상 지정해야 합니다.", groups = ValidationGroups.Create.class)
    private List<ApprovalLineRequestDTO> approvalLines;

    @Valid // 이 리스트 내부의 DTO들도 유효성 검사를 수행
    // 참조 리스트는 0명일 수 있으므로(null 허용)
    private List<ApprovalReferenceRequestDTO> approvalReferences;

    /**
     * DTO를 ApprovalDoc 엔티티로 변환
     *
     * @param creater        기안자 엔티티 (현재 로그인한 사용자)
     * @param docStatus      문서 상태 CommonCode 엔티티 (예: '결재중' 또는 '임시저장')
     * @return ApprovalDoc 엔티티
     */
    public ApprovalDoc toEntity(
            Employee creater, // (주석) 현재 로그인한 사용자(기안자)
            CommonCode docStatus // (주석) 초기 문서 상태 (예: '임시저장' 또는 '결재중')
    ) {
        // (주석) 엔티티의 정적 팩토리 메서드 호출
        return ApprovalDoc.createApprovalDoc(
                this.title,
                this.content,
                creater,
                this.startDate,
                this.endDate,
                docStatus
        );
    }
}