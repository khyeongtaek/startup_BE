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


/*
* =============================================
* Receiver Type (수신자 타입)
* code: RT + 번호 (Receiver Type)
* value1: 타입 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('RT1', '수신', 'TO', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('RT2', '참조', 'CC', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('RT3', '숨은 참조', 'BCC', NULL, NULL, 3, NULL, NOW(), NOW(), false);


/*
* =============================================
* Mailbox Type (메일함 타입)
* code: MT + 번호 (Mailbox Type)
* value1: 타입 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('MT1', '수신함', 'INBOX', NULL, NULL, 1, NULL, NOW(), NOW(), false),
    ('MT2', '발신함', 'SENT', NULL, NULL, 2, NULL, NOW(), NOW(), false),
    ('MT3', '개인 보관함', 'MYBOX', NULL, NULL, 3, NULL, NOW(), NOW(), false);


/*
* =============================================
* Work Type (업무 분류)
* code: WT + 번호 (Work Type)
* value1: 분류 코드 (영문)
* value2: 분류 이름 (한글)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('WT01', '업무 분류 : 프로젝트', 'PROJECT', '프로젝트', NULL, 1, NULL, NOW(), NOW(), false),
    ('WT02', '업무 분류 : 연구', 'STUDY', '연구', NULL, 0, NULL, NOW(), NOW(), false),
    ('WT03', '업무 분류 : 회의', 'MEETING', '회의', NULL, 0, NULL, NOW(), NOW(), false),
    ('WT04', '업무 분류 : 기타 업무', 'ETC', '기타 업무', NULL, 0, NULL, NOW(), NOW(), false);


/*
* =============================================
* Work Type Option (업무 분류별 세무 항목)
* code: WO + 번호 (Work Type Option)
* value1: 옵션 코드 (영문)
* value2: 옵션 이름 (한글)
* value3: 상위 분류 (상위 코드 값)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('WO01', '메일 기능 개발', 'MAIL', '메일', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO02', '업무일지 기능 개발', 'WORKLOG', '업무일지', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO03', '게시판 기능 개발', 'BOARD', '게시판', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO04', '조직도 기능 개발', 'ORGANIZATION', '조직도', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO05', '회원가입 기능 개발', 'SIGNUP', '회원가입', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO06', '로그인 기능 개발', 'LOGIN', '로그인', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO07', '전자결재 기능 개발', 'APPROVAL', '전자결재', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO08', '메신저 기능 개발', 'CHAT', '메신저', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO09', '알림 기능 개발', 'NOTIFICATION', '알림', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO10', '일정 기능 개발', 'CALENDAR', '일정', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO11', '근태관리 기능 개발', 'ATTENDANCE', '근태관리', 'WT01', 0, NULL, NOW(), NOW(), false),
    ('WO12', '옵션 더미 데이터', 'TECH_STUDY', '기술 스터디', 'WT02', 0, NULL, NOW(), NOW(), false),
    ('WO13', '옵션 더미 데이터', 'DOC', '문서 정리', 'WT02', 0, NULL, NOW(), NOW(), false),
    ('WO14', '옵션 더미 데이터', 'MEETING_PREP', '회의 준비', 'WT03', 0, NULL, NOW(), NOW(), false),
    ('WO15', '옵션 더미 데이터', 'MINUTES', '회의록 작성', 'WT03', 0, NULL, NOW(), NOW(), false),
    ('WO16', '옵션 더미 데이터', 'REPORT', '보고서 작성', 'WT04', 0, NULL, NOW(), NOW(), false),
    ('WO17', '옵션 더미 데이터', 'SUPPORT', '업무 지원', 'WT04', 0, NULL, NOW(), NOW(), false);


/*
* =============================================
* Owner Type (출처 모듈 타입)
* code: OT + 번호 (Owner Type)
* value1: 모듈 값 (OwnerType 값)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, employee_id, created_at, updated_at, is_deleted)
VALUES
    ('OT01', '출처 모듈 : 메일', 'MAIL', '', '', 0, NULL, NOW(), NOW(), false),
    ('OT02', '출처 모듈 : 업무일지', 'WORKLOG', '', '', 0, NULL, NOW(), NOW(), false);