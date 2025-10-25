# SET FOREIGN_KEY_CHECKS = 0;
#
# DROP TABLE IF EXISTS tbl_approval_doc;
# DROP TABLE IF EXISTS tbl_approval_line;
# DROP TABLE IF EXISTS tbl_approval_reference;
# DROP TABLE IF EXISTS tbl_common_code;
# DROP TABLE IF EXISTS tbl_employee;
# DROP TABLE IF EXISTS tbl_login_history;
#
# SET FOREIGN_KEY_CHECKS = 1;

/*
* =============================================
* ApprovalDoc (문서 상태)
* code: AD + 번호
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('AD1', '문서 상태 - 임시저장', 'DRAFT', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('AD2', '문서 상태 - 진행중', 'IN_PROGRESS', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('AD3', '문서 상태 - 최종 승인', 'APPROVED', NULL, NULL, 3, NULL, NOW(), NOW(), false),
    ('AD4', '문서 상태 - 최종 반려', 'REJECTED', NULL, NULL, 4, NULL, NOW(), NOW(), false);


/*
* =============================================
* ApprovalLine (결재선 상태)
* code: AL + 번호
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('AL1', '결재선 상태 - 대기', 'PENDING', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('AL2', '결재선 상태 - 승인', 'APPROVED', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('AL3', '결재선 상태 - 반려', 'REJECTED', NULL, NULL, 3, NULL, NOW(), NOW(), false);


/*
* =============================================
* Employee (재직 상태) - '퇴사' 제거됨
* code: ES + 번호 (Employee Status)
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('ES1', '재직 상태 - 재직중', 'ACTIVE', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('ES2', '재직 상태 - 휴직', 'ON_LEAVE', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('ES3', '재직 상태 - 계정 잠김', 'LOCKED', NULL, NULL, 3, NULL, NOW(), NOW(), false);


/*
* =============================================
* Department (부서) - '스타트업' 최상위 루트로 추가
* code: DP + 번호
* code_description: '부서' (카테고리명)
* value1: 부서명
* value2: 상위 부서의 code (DP1이 최상위)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('DP1', '부서', '스타트업', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('DP2', '부서', '경영지원본부', 'DP1', NULL, 2, NULL, NOW(), NOW(), false),
    ('DP3', '부서', '인사팀', 'DP2', NULL, 3, NULL, NOW(), NOW(), false),
    ('DP4', '부서', '재무회계팀', 'DP2', NULL, 4, NULL, NOW(), NOW(), false),
    ('DP5', '부서', '총무팀', 'DP2', NULL, 5, NULL, NOW(), NOW(), false),
    ('DP6', '부서', 'R&D 본부', 'DP1', NULL, 10, NULL, NOW(), NOW(), false),
    ('DP7', '부서', '백엔드개발팀', 'DP6', NULL, 11, NULL, NOW(), NOW(), false),
    ('DP8', '부서', '프론트엔드개발팀', 'DP6', NULL, 12, NULL, NOW(), NOW(), false),
    ('DP9', '부서', 'UI/UX 디자인팀', 'DP6', NULL, 13, NULL, NOW(), NOW(), false),
    ('DP10', '부서', 'QA팀', 'DP6', NULL, 14, NULL, NOW(), NOW(), false),
    ('DP11', '부서', '사업본부', 'DP1', NULL, 20, NULL, NOW(), NOW(), false),
    ('DP12', '부서', '영업1팀', 'DP11', NULL, 21, NULL, NOW(), NOW(), false),
    ('DP13', '부서', '영업2팀', 'DP11', NULL, 22, NULL, NOW(), NOW(), false),
    ('DP14', '부서', '마케팅팀', 'DP11', NULL, 23, NULL, NOW(), NOW(), false),
    ('DP15', '부서', 'C-Level', 'DP1', NULL, 30, NULL, NOW(), NOW(), false),
    ('DP16', '부서', '대표이사', 'DP15', NULL, 31, NULL, NOW(), NOW(), false);


/*
* =============================================
* Position (직급)
* code: PS + 번호
* code_description: '직급' (카테고리명)
* value1: 직급명
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('PS1', '직급', '사원', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('PS2', '직급', '주임', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('PS3', '직급', '대리', NULL, NULL, 3, NULL, NOW(), NOW(), false),
    ('PS4', '직급', '과장', NULL, NULL, 4, NULL, NOW(), NOW(), false),
    ('PS5', '직급', '차장', NULL, NULL, 5, NULL, NOW(), NOW(), false),
    ('PS6', '직급', '부장', NULL, NULL, 6, NULL, NOW(), NOW(), false),
    ('PS7', '직급', '이사', NULL, NULL, 7, NULL, NOW(), NOW(), false),
    ('PS8', '직급', '대표이사', NULL, NULL, 8, NULL, NOW(), NOW(), false);


/*
* =============================================
* Authority (권한)
* code: AU + 번호 (Authority)
* value1: 권한 값 (ROLE_)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('AU1', '권한 - 관리자', 'ROLE_ADMIN', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('AU2', '권한 - 일반 사용자', 'ROLE_USER', NULL, NULL, 2, NULL, NOW(), NOW(), false);


/*
* =============================================
* Login Status (로그인 상태)
* code: LS + 번호 (Login Status)
* value1: 상태 값
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('LS1', '로그인 상태 - 성공', 'SUCCESS', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('LS2', '로그인 상태 - 실패', 'FAIL', NULL, NULL, 2, NULL, NOW(), NOW(), false);


/*
* =============================================
* Employee (직원) - 최초 관리자
* employeeId: 1 (수동 할당)
* =============================================
*/

INSERT INTO tbl_employee (
    employee_id,
    username,
    password,
    name,
    email,
    phone_number,
    hire_date,
    status,
    profile_img,
    is_initial_password,
    department_common_code_id,
    position_common_code_id,
    role_common_code_id,
    created_at,
    updated_at,
    creator_id,
    updater_id
)
VALUES (
           1, -- employee_id (Employee.java에 @GeneratedValue가 없으므로 수동 할당)
           'admin', -- username (로그인 아이디)
           'admin', -- password (!!주의: 실제로는 암호화된 값이어야 함)
           '관리자', -- name
           'admin@company.com', -- email
           '010-0000-0000', -- phone_number
           CURDATE(), -- hire_date (오늘 날짜)
           (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'), -- status (ES1: 재직중)
           'default_profile.png', -- profile_img
           true, -- is_initial_password (초기 비밀번호 맞음)
           (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP16'), -- department_common_code_id (DP16: 대표이사)
           (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS8'), -- position_common_code_id (PS8: 대표이사)
           (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU1'), -- role_common_code_id (AU1: 관리자)
           NOW(), -- created_at
           NOW(), -- updated_at
           NULL, -- creator_id (최초의 관리자이므로 생성자 없음)
           NULL -- updater_id (최초의 관리자이므로 수정자 없음)
       );


