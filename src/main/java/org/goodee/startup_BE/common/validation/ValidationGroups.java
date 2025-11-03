package org.goodee.startup_BE.common.validation;


/**
 * DTO 유효성 검사 그룹을 정의하기 위한 인터페이스
 * 이 인터페이스들은 마커(marker) 용도로만 사용됨
 * 아래와 같은 형식으로 사용
 */
// Controller에서 @Validated(ValidationGroups.Create.class) @RequestBody ApprovalDocRequestDto requestDto
// DTO 에서 @NotNull(message = "결재 문서 ID는 필수입니다.", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})

public class ValidationGroups {

    /**
     * '생성' (Create) 시 사용할 그룹
     * (예: POST /api/approvals)
     */
    public interface Create {}

    /**
     * '수정' (Update) 시 사용할 그룹
     * (예: PUT 또는 PATCH /api/approvals/{id})
     */
    public interface Update {}

    
    // 첨부파일
    public interface Attachment {
        interface Upload {}
        interface List {}
        interface Download {}
    }
    
    // 메일
    public interface Mail {
        interface Move {}
        interface Delete {}
    }
}