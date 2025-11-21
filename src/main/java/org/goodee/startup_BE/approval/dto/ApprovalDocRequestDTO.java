package org.goodee.startup_BE.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.goodee.startup_BE.approval.entity.ApprovalDoc;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.validation.ValidationGroups;
import org.goodee.startup_BE.employee.entity.Employee;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결재 문서를 위한 통합 Request DTO
 * (Doc + Lines + References)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ApprovalDocRequestDTO {

    @NotNull(message = "문서 ID는 필수입니다.", groups = {ValidationGroups.Update.class})
    private Long docId;

    @NotNull(message = "양식 코드는 필수 입니다.", groups = {ValidationGroups.Create.class})
    private String templateCode;

    @NotEmpty(message = "제목은 필수입니다.", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String title;

    @NotEmpty(message = "내용은 필수입니다.", groups = ValidationGroups.Create.class)
    private String content;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    // 휴가(AT1)
    private Long vacationTypeCode;   // CommonCode ID
    private Double vacationDays;
    private String vacationReason;

    // 출장(AT2)
    private String tripLocation;
    private String transportation;
    private String tripPurpose;
    private String tripRemark;


    @Valid // 이 리스트 내부의 DTO들도 유효성 검사를 수행
    // '결재 요청' 시에는 결재선 리스트가 null이 아니고, 1명 이상이어야 함
    @NotNull(message = "결재선 정보는 필수입니다.", groups = ValidationGroups.Create.class)
    @Size(min = 1, message = "결재선은 최소 1명 이상 지정해야 합니다.", groups = ValidationGroups.Create.class)
    private List<ApprovalLineRequestDTO> approvalLines;

    @Valid // 이 리스트 내부의 DTO들도 유효성 검사를 수행
    // 참조 리스트는 0명일 수 있으므로(null 허용)
    private List<ApprovalReferenceRequestDTO> approvalReferences;

    @Schema(description = "결재 첨부파일용 MultipartFile")
    private List<MultipartFile> multipartFile;

    /**
     * DTO를 ApprovalDoc 엔티티로 변환
     *
     * @param creator        기안자 엔티티 (현재 로그인한 사용자)
     * @param template       결재 양식 CommonCode 엔티티 (예: '휴가계획서')
     * @param docStatus      문서 상태 CommonCode 엔티티 (예: '결재중' 또는 '임시저장')
     * @return ApprovalDoc 엔티티
     */
    public ApprovalDoc toEntity(
            Employee creator,
            CommonCode template,
            CommonCode docStatus,
            CommonCode vacationTypeCodeEntity  // 휴가 종류 코드(연차, 오전 반차, 오후 반차)
    ) {
        ApprovalDoc doc = ApprovalDoc.createApprovalDoc(
                this.title,
                this.content,
                creator,
                template,
                this.startDate,
                this.endDate,
                docStatus
        );

        // 휴가 신청서
        if (template.getCode().equals("AT1")){
            doc.updateVacationInfo(
                    vacationTypeCodeEntity,
                    this.vacationDays,
                    this.vacationReason
            );
        }

        // 출장 신청서
        if (template.getCode().equals("AT2")){
            doc.updateTripInfo(
                    this.tripLocation,
                    this.transportation,
                    this.tripPurpose,
                    this.tripRemark
            );
        }
        return doc;
    }
}