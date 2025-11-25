SET FOREIGN_KEY_CHECKS = 0;
#
# DROP TABLE IF EXISTS tbl_approval_doc;
# DROP TABLE IF EXISTS tbl_approval_line;
# DROP TABLE IF EXISTS tbl_approval_reference;
# DROP TABLE IF EXISTS tbl_common_code;
# DROP TABLE IF EXISTS tbl_employee;
# DROP TABLE IF EXISTS tbl_login_history;
#

/*
* =============================================
* ApprovalDoc (문서 상태)
* code: AD + 번호
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('AD0', '문서 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('AD1', '임시저장', 'DRAFT', '임시 저장', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('AD2', '진행중', 'IN_PROGRESS', '진행중', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('AD3', '최종 승인', 'APPROVED', '최종 승인', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('AD4', '최종 반려', 'REJECTED', '반려', NULL, 4, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* ApprovalLine (결재선 상태)
* code: AL + 번호
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('AL0', '결재선 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('AL1', '미결', 'PENDING', '미결', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('AL2', '대기', 'AWAITING', '대기', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('AL3', '승인', 'APPROVED', '승인', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('AL4', '반려', 'REJECTED', '반려', NULL, 3, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Employee (재직 상태) - '퇴사' 제거됨
* code: ES + 번호 (Employee Status)
* value1: 상태 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('ES0', '재직 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('ES1', '재직중', 'ACTIVE', '재직', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('ES2', '휴직', 'ON_LEAVE', '휴직', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('ES3', '계정 잠김', 'LOCKED', '계정 잠김', NULL, 3, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Department (부서) - '스타트업' 최상위 루트로 추가
* code: DP + 번호
* code_description: 부서명 (DP0은 '부서' 카테고리명)
* value1: 부서명
* value2: 상위 부서의 code (DP1이 최상위)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('DP0', '부서', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('DP1', '스타트업', '스타트업', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('DP2', '경영지원본부', '경영지원본부', 'DP1', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('DP3', '인사팀', '인사팀', 'DP2', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('DP4', '재무회계팀', '재무회계팀', 'DP2', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('DP5', '총무팀', '총무팀', 'DP2', NULL, 5, 1, 1, NOW(), NOW(), false),
    ('DP6', 'R&D 본부', 'R&D 본부', 'DP1', NULL, 10, 1, 1, NOW(), NOW(), false),
    ('DP7', '백엔드개발팀', '백엔드개발팀', 'DP6', NULL, 11, 1, 1, NOW(), NOW(), false),
    ('DP8', '프론트엔드개발팀', '프론트엔드개발팀', 'DP6', NULL, 12, 1, 1, NOW(), NOW(), false),
    ('DP9', 'UI/UX 디자인팀', 'UI/UX 디자인팀', 'DP6', NULL, 13, 1, 1, NOW(), NOW(), false),
    ('DP10', 'QA팀', 'QA팀', 'DP6', NULL, 14, 1, 1, NOW(), NOW(), false),
    ('DP11', '사업본부', '사업본부', 'DP1', NULL, 20, 1, 1, NOW(), NOW(), false),
    ('DP12', '영업1팀', '영업1팀', 'DP11', NULL, 21, 1, 1, NOW(), NOW(), false),
    ('DP13', '영업2팀', '영업2팀', 'DP11', NULL, 22, 1, 1, NOW(), NOW(), false),
    ('DP14', '마케팅팀', '마케팅팀', 'DP11', NULL, 23, 1, 1, NOW(), NOW(), false),
    ('DP15', 'C-Level', 'C-Level', 'DP1', NULL, 30, 1, 1, NOW(), NOW(), false),
    ('DP16', '대표이사', '대표이사', 'DP15', NULL, 31, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Position (직급)
* code: PS + 번호
* code_description: 직급명 (PS0은 '직급' 카테고리명)
* value1: 직급명
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('PS0', '직급', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('PS1', '사원', '사원', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('PS2', '주임', '주임', NULL, NULL, 2, 1, 1, NOW(), NOW(), false),
    ('PS3', '대리', '대리', NULL, NULL, 3, 1, 1, NOW(), NOW(), false),
    ('PS4', '과장', '과장', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('PS5', '차장', '차장', NULL, NULL, 5, 1, 1, NOW(), NOW(), false),
    ('PS6', '부장', '부장', NULL, NULL, 6, 1, 1, NOW(), NOW(), false),
    ('PS7', '이사', '이사', NULL, NULL, 7, 1, 1, NOW(), NOW(), false),
    ('PS8', '대표이사', '대표이사', NULL, NULL, 8, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Authority (권한)
* code: AU + 번호 (Authority)
* value1: 권한 값 (ROLE_)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('AU0', '권한', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('AU1', '관리자', 'ROLE_ADMIN', '관리자', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('AU2', '일반 사용자', 'ROLE_USER', '사용자', NULL, 2, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Login Status (로그인 상태)
* code: LS + 번호 (Login Status)
* value1: 상태 값
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('LS0', '로그인 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('LS1', '성공', 'SUCCESS', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('LS2', '실패', 'FAIL', NULL, NULL, 2, 1, 1, NOW(), NOW(), false);



/*
* =============================================
* Receiver Type (수신자 타입)
* code: RT + 번호 (Receiver Type)
* value1: 타입 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('RT0', '수신자 타입', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('RT1', '수신', 'TO', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('RT2', '참조', 'CC', NULL, NULL, 2, 1, 1, NOW(), NOW(), false),
    ('RT3', '숨은 참조', 'BCC', NULL, NULL, 3, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Mailbox Type (메일함 타입)
* code: MT + 번호 (Mailbox Type)
* value1: 타입 값 (영문)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('MT0', '메일함 타입', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('MT1', '수신함', 'INBOX', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('MT2', '발신함', 'SENT', NULL, NULL, 2, 1, 1, NOW(), NOW(), false),
    ('MT3', '개인 보관함', 'MYBOX', NULL, NULL, 3, 1, 1, NOW(), NOW(), false),
    ('MT4', '휴지통', 'TRASH', NULL, NULL, 4, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Work Type (업무 분류)
* code: WT + 번호 (Work Type)
* value1: 분류 코드 (영문)
* value2: 분류 이름 (한글)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('WT0', '업무 분류', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('WT1', '프로젝트', 'PROJECT', '프로젝트', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('WT2', '연구', 'STUDY', '연구', NULL, 0, 1, 1, NOW(), NOW(), false),
    ('WT3', '회의', 'MEETING', '회의', NULL, 0, 1, 1, NOW(), NOW(), false),
    ('WT4', '기타 업무', 'ETC', '기타 업무', NULL, 0, 1, 1, NOW(), NOW(), false);


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
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('WO0', '업무 분류별 세무 항목', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('WO1', '메일 기능 개발', 'MAIL', '메일', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO2', '업무일지 기능 개발', 'WORKLOG', '업무일지', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO3', '게시판 기능 개발', 'BOARD', '게시판', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO4', '조직도 기능 개발', 'ORGANIZATION', '조직도', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO5', '회원가입 기능 개발', 'SIGNUP', '회원가입', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO6', '로그인 기능 개발', 'LOGIN', '로그인', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO7', '전자결재 기능 개발', 'APPROVAL', '전자결재', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO8', '메신저 기능 개발', 'CHAT', '메신저', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO9', '알림 기능 개발', 'NOTIFICATION', '알림', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO10', '일정 기능 개발', 'CALENDAR', '일정', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO11', '근태관리 기능 개발', 'ATTENDANCE', '근태관리', 'WT1', 0, 1, 1, NOW(), NOW(), false),
    ('WO12', '기술 스터디', 'TECH_STUDY', '기술 스터디', 'WT2', 0, 1, 1, NOW(), NOW(), false),
    ('WO13', '문서 정리', 'DOC', '문서 정리', 'WT2', 0, 1, 1, NOW(), NOW(), false),
    ('WO14', '회의 준비', 'MEETING_PREP', '회의 준비', 'WT3', 0, 1, 1, NOW(), NOW(), false),
    ('WO15', '회의록 작성', 'MINUTES', '회의록 작성', 'WT3', 0, 1, 1, NOW(), NOW(), false),
    ('WO16', '보고서 작성', 'REPORT', '보고서 작성', 'WT4', 0, 1, 1, NOW(), NOW(), false),
    ('WO17', '업무 지원', 'SUPPORT', '업무 지원', 'WT4', 0, 1, 1, NOW(), NOW(), false);


/*
* =============================================
* Owner Type (출처 모듈 타입)
* code: OT + 번호 (Owner Type)
* value1: 모듈 값 (OwnerType 값)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('OT0', '출처 모듈 타입', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('OT1', '메일', 'MAIL', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT2', '업무일지', 'WORKLOG', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT3', '사원', 'EMPLOYEE', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT4', '전자결재', 'APPROVAL', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT5', '채팅 초대', 'TEAMCHATNOTI', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT6', '일정 초대', 'SCHEDULEINVITE', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT7', '채팅 메시지', 'CHAT', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT8', '시스템 메시지', 'CHAT_SYSTEM', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT9', '사용자 메시지', 'CHAT_USER', '', '', 0, 1, 1, NOW(), NOW(), false),
    ('OT10', '게시판', 'POST', '', '', 0, 1, 1, NOW(), NOW(), false);

/*
* =============================================
* Schedule Color (일정 색상)
* code: CL + 번호 (Color)
* value1: 색상 코드 (영문)
* value2: HEX 코드
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('CL0', '색상 코드', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('CL1', '파란색', 'BLUE', '#3498db', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('CL2', '빨간색', 'RED', '#e74c3c', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('CL3', '초록색', 'GREEN', '#27ae60', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('CL4', '노란색', 'YELLOW', '#f1c40f', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('CL5', '회색', 'GRAY', '#7f8c8d', NULL, 5, 1, 1, NOW(), NOW(), false);



/*
* =============================================
* Work Status (근무 상태)
* code: WS + 번호 (Work Status)
* value1: 상태 코드 (영문)
* value2: 상태 이름 (한글)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('WS0', '근무 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('WS1', '정상근무', 'NORMAL', '정상근무', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('WS2', '지각', 'LATE', '지각', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('WS3', '조퇴', 'EARLY_LEAVE', '조퇴', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('WS4', '결근', 'ABSENT', '결근', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('WS5', '휴가', 'VACATION', '휴가', NULL, 5, 1, 1, NOW(), NOW(), false),
    ('WS6', '외근', 'OUT_ON_BUSINESS', '외근', NULL , 6 , 1, 1, NOW(), NOW(), false ),
    ('WS7', '퇴근', 'CLOCK_OUT', '퇴근', NULL , 6 , 1, 1, NOW(), NOW(), false ),
    ('WS8', '오전 반차', 'MORNING_HALF', '오전 반차', NULL,8, 1, 1, NOW(), NOW(), false),
    ( 'WS9', '오후 반차', 'AFTERNOON_HALF', '오후 반차',NULL ,9, 1, 1, NOW(), NOW(), false);

/*
* =============================================
* Schedule Category (일정 카테고리)
* code: SC + 번호 (Schedule Category)
* value1: 분류 코드 (영문)
* value2: 분류 이름 (한글)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('SC0', '일정 카테고리', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('SC1', '회의', 'MEETING', '회의', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('SC2', '출장', 'BUSINESS_TRIP', '출장', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('SC3', '휴가', 'VACATION', '휴가', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('SC4', '프로젝트', 'PROJECT', '프로젝트', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('SC5', '기타', 'ETC', '기타', NULL, 5, 1, 1, NOW(), NOW(), false);
/*
* =============================================
* Participant Status (참여 상태)
* code: SP + 번호 (Schedule Participant Status)
* value1: 상태 코드 (영문)
* value2: 상태 이름 (한글)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('SP0', '참여 상태', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('SP1', '참석', 'ATTEND', '참석', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('SP2', '거절', 'REJECT', '거절', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('SP3', '미응답', 'PENDING', '미응답', NULL, 3, 1, 1, NOW(), NOW(), false);


/*
* Employee (직원) 데이터 삽입 (스네이크 케이스)
* - 모든 부서(DP1~DP16)에 직원 배정
* - 2명의 관리자(AU1) 포함 (admin, ceo)
* - 모든 비밀번호는 '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K'로 고정
* - 첫 번째 관리자(admin)를 생성자/수정자로 지정 (employee_id = 1 가정)
* =============================================
*/
-- 첫 번째 사용자: 관리자 (ID: 1)
INSERT INTO tbl_employee (
    employee_id, username, password, name, email, phone_number, hire_date,
    status, profile_img, is_initial_password,
    department_common_code_id, position_common_code_id, role_common_code_id,
    created_at, updated_at, creator_id, updater_id
)
VALUES
    -- 1. 관리자 (R&D 본부 - 백엔드개발팀)
    (
        1, 'admin', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '김민수', 'kimminsu@startup.com', '010-0000-0001', '2024-01-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'), -- 재직중
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP7'), -- 백엔드개발팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS6'), -- 부장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU1'), -- 관리자
        NOW(), NOW(), NULL, NULL -- 첫 사용자는 creator/updater가 없음
    );

-- 두 번째 INSERT: 나머지 모든 사용자 (ID: 2 ~ 32)
-- employee_id 컬럼을 추가하고 각 VALUES에 ID 값을 직접 지정
INSERT INTO tbl_employee (
    employee_id, username, password, name, email, phone_number, hire_date,
    status, profile_img, is_initial_password,
    department_common_code_id, position_common_code_id, role_common_code_id,
    created_at, updated_at, creator_id, updater_id
)
VALUES
    -- 2. 대표이사 (C-Level - 대표이사)
    (
        2, 'ceo', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '이서연', 'leeseoyeon@startup.com', '010-1111-1111', '2024-01-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'), -- 재직중
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP16'), -- 대표이사(부서)
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS8'), -- 대표이사(직급)
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU1'), -- 관리자
        NOW(), NOW(), 1, 1 -- admin(1)이 생성
    ),
    -- 3. 인사팀
    (
        3, 'hr_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '박지훈', 'parkjihoon@startup.com', '010-2222-2222', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP3'), -- 인사팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS4'), -- 과장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'), -- 일반사용자
        NOW(), NOW(), 1, 1
    ),
    (
        4, 'hr_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '최유진', 'choiyujin@startup.com', '010-2222-2223', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP3'), -- 인사팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 4. 재무회계팀
    (
        5, 'finance_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '정우진', 'jungwoojin@startup.com', '010-3333-3333', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP4'), -- 재무회계팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS5'), -- 차장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        6, 'finance_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '한지민', 'hanjimin@startup.com', '010-3333-3334', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP4'), -- 재무회계팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS2'), -- 주임
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 5. 총무팀
    (
        7, 'ga_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '윤도현', 'yoondohyeon@startup.com', '010-4444-4444', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP5'), -- 총무팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS3'), -- 대리
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        8, 'ga_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '장예린', 'jangyerin@startup.com', '010-4444-4445', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP5'), -- 총무팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 6. 백엔드개발팀 (admin 외 추가)
    (
        9, 'backend_dev1', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '오승현', 'oseunghyun@startup.com', '010-5555-5551', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP7'), -- 백엔드개발팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS3'), -- 대리
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        10, 'backend_dev2', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '조민지', 'chominji@startup.com', '010-5555-5552', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP7'), -- 백엔드개발팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 7. 프론트엔드개발팀
    (
        11, 'frontend_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '배태훈', 'baetaehun@startup.com', '010-6666-6661', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP8'), -- 프론트엔드개발팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS4'), -- 과장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        12, 'frontend_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '강지호', 'kangjiho@startup.com', '010-6666-6662', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP8'), -- 프론트엔드개발팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 8. UI/UX 디자인팀
    (
        13, 'designer_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '신예은', 'shinyeeun@startup.com', '010-7777-7771', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP9'), -- UI/UX 디자인팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS3'), -- 대리
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        14, 'designer_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '임도윤', 'limdoyoon@startup.com', '010-7777-7772', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP9'), -- UI/UX 디자인팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS2'), -- 주임
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 9. QA팀
    (
        15, 'qa_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '서지훈', 'seojihun@startup.com', '010-8888-8881', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP10'), -- QA팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS3'), -- 대리
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        16, 'qa_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '권나연', 'kwonnayeon@startup.com', '010-8888-8882', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP10'), -- QA팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 10. 영업1팀
    (
        17, 'sales1_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '문시후', 'moonsihoo@startup.com', '010-9999-9991', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP12'), -- 영업1팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS5'), -- 차장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        18, 'sales1_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '하윤서', 'hayunseo@startup.com', '010-9999-9992', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP12'), -- 영업1팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 11. 영업2팀
    (
        19, 'sales2_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '양준호', 'yangjunho@startup.com', '010-1010-1011', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP13'), -- 영업2팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS4'), -- 과장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        20, 'sales2_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '홍가은', 'hongkaeun@startup.com', '010-1010-1012', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP13'), -- 영업2팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 12. 마케팅팀
    (
        21, 'marketing_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '유민재', 'yoominjae@startup.com', '010-1212-1211', '2024-02-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP14'), -- 마케팅팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS3'), -- 대리
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        22, 'marketing_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '노수진', 'nosujin@startup.com', '010-1212-1212', '2024-03-01',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP14'), -- 마케팅팀
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS1'), -- 사원
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),

    -- "모든 부서" 요청을 만족하기 위해 상위 부서에도 인원 배정 --
    -- 13. C-Level (상위)
    (
        23, 'clevel_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '백동현', 'baekdonghyun@startup.com', '010-1313-1313', '2024-01-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP15'), -- C-Level
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS7'), -- 이사
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        24, 'clevel_staff2', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '남서윤', 'namseoyoon@startup.com', '010-1313-1314', '2024-01-20',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP15'), -- C-Level
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS6'), -- 부장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 14. 사업본부 (상위)
    (
        25, 'biz_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '곽준영', 'kwakjunyoung@startup.com', '010-1414-1414', '2024-01-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP11'), -- 사업본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS7'), -- 이사
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        26, 'biz_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '표지우', 'pyojiwoo@startup.com', '010-1414-1415', '2024-02-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP11'), -- 사업본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS4'), -- 과장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 15. R&D 본부 (상위)
    (
        27, 'rnd_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '설민혁', 'seolminhyuk@startup.com', '010-1515-1515', '2024-01-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP6'), -- R&D 본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS7'), -- 이사
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        28, 'rnd_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '주예린', 'jooyerin@startup.com', '010-1515-1516', '2024-02-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP6'), -- R&D 본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS5'), -- 차장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 16. 경영지원본부 (상위)
    (
        29, 'mgmt_manager', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '라준서', 'rajunseo@startup.com', '010-1616-1616', '2024-01-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP2'), -- 경영지원본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS7'), -- 이사
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        30, 'mgmt_staff', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '도하늘', 'dohaneul@startup.com', '010-1616-1617', '2024-02-15',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP2'), -- 경영지원본부
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS5'), -- 차장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    -- 17. 스타트업 (최상위)
    (
        31, 'root_staff1', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '추지민', 'chujimin@startup.com', '010-1717-1717', '2024-01-10',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP1'), -- 스타트업
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS7'), -- 이사
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    ),
    (
        32, 'root_staff2', '$2a$10$GetZyZUrGR48sFt7WUL5yOLrp2r6pkVYqaGkv8TDowbflcqbku10K', '위서진', 'wiseojin@startup.com', '010-1717-1718', '2024-01-10',
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'ES1'),
        null, true,
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'DP1'), -- 스타트업
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'PS6'), -- 부장
        (SELECT common_code_id FROM tbl_common_code WHERE code = 'AU2'),
        NOW(), NOW(), 1, 1
    );

/*
* =============================================
* Approval Type (결재 양식)
* code: AT + 번호 (Approval Template)
* code_description: 양식 명 (AT0은 '결재 양식' 카테고리명)
* value1: 양식 명 (한글, UI에 노출될 이름)
* value2: 양식 타입 (영문, Enum 또는 식별자로 사용)
* value3: 양식 폼 컴포넌트 경로
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    ('AT0', '결재 양식', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
    ('AT1', '휴가 신청서', '휴가 신청서', 'VACATION', 'LeaveTemplate', 1, 1, 1, NOW(), NOW(), false),
    ('AT2', '출장 계획서', '출장 계획서', 'BUSINESS_TRIP', 'BusinessTripTemplate', 2, 1, 1, NOW(), NOW(), false);

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at,
 is_disabled)
VALUES ('VT0', '휴가 종류', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),
       ('VT1', '연차', 'ANNUAL', '연차', NULL, 1, 1, 1, NOW(), NOW(), false),
       ('VT2', '오전반차', 'MORNING_HALF', '오전 반차', NULL, 2, 1, 1, NOW(), NOW(), false),
       ('VT3', '오후반차', 'AFTERNOON_HALF', '오후 반차', NULL, 3, 1, 1, NOW(), NOW(), false);




/*
* =============================================
* Menu & Routes (메뉴 및 라우트)
* code: MN + 번호 (Menu) / RO + 번호 (Route)
* code_description: 메뉴/라우트 설명
* value1: JSON. (id, title, type, icon, url, componentPath, admin, target 등)
* value2: 상위 메뉴 Code (부모-자식 관계)
* =============================================
*/

INSERT INTO tbl_common_code
(code, code_description, value1, value2, value3, sort_order, creator_id, updater_id, created_at, updated_at, is_disabled)
VALUES
    /* -------------------------------------------
     * 메뉴 (새 루트)
     * ------------------------------------------- */
    ('MN0', '메뉴', NULL, NULL, NULL, 0, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 메뉴 루트 (pages.js의 root)
     * ------------------------------------------- */
    ('MN1', '메뉴 루트', '{
      "id": "pages",
      "title": "메뉴",
      "type": "group"
    }', null, NULL, 1, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 1. 코드관리 (Item)
     * ------------------------------------------- */
    ('MN2', '코드관리', '{
      "id": "code",
      "title": "코드관리",
      "type": "item",
      "icon": "IconCode",
      "url": "/code",
      "admin": true,
      "componentPath": "features/code/pages/CodePage"
    }', 'MN1', NULL, 1, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 2. 인사관리 (Item)
     * ------------------------------------------- */
    ('MN3', '인사관리', '{
      "id": "organization",
      "title": "인사관리",
      "type": "item",
      "icon": "IconBadge",
      "url": "/organization",
      "admin": true,
      "componentPath": "features/organization/pages/OrganizationPage"
    }', 'MN1', NULL, 2, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 3. 근태관리 (Item)
     * ------------------------------------------- */
    ('MN4', '근태관리', '{
      "id": "attendance",
      "title": "근태관리",
      "type": "item",
      "icon": "CoPresentIcon",
      "url": "/attendance",
      "componentPath": "features/attendance/pages/AttendancePage"
    }', 'MN1', NULL, 3, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 4. 전자결재 (Collapse)
     * ------------------------------------------- */
    ('MN5', '전자결재', '{
      "id": "approval",
      "title": "전자결재",
      "type": "collapse",
      "icon": "IconClipboardCheck"
    }', 'MN1', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('MN6', '결재 작성', '{
      "id": "insert",
      "title": "결재 작성",
      "type": "item",
      "url": "/approval/form",
      "target": false,
      "componentPath": "features/approval/pages/AddApprovalPage"
    }', 'MN5', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('MN7', '결재 대기 목록', '{
      "id": "approval-list-pending",
      "title": "결재 대기 목록",
      "type": "item",
      "url": "/approval/list/pending",
      "target": false
    }', 'MN5', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('MN8', '결재 기안 목록', '{
      "id": "approval-list-draft",
      "title": "결재 기안 목록",
      "type": "item",
      "url": "/approval/list/draft",
      "target": false
    }', 'MN5', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('MN9', '결재 완료 목록', '{
      "id": "approval-list-completed",
      "title": "결재 완료 목록",
      "type": "item",
      "url": "/approval/list/completed",
      "target": false
    }', 'MN5', NULL, 4, 1, 1, NOW(), NOW(), false),
    ('MN10', '결재 참조 목록', '{
      "id": "approval-list-reference",
      "title": "결재 참조 목록",
      "type": "item",
      "url": "/approval/list/reference",
      "target": false
    }', 'MN5', NULL, 5, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 5. 캘린더 (Item)
     * ------------------------------------------- */
    ('MN11', '캘린더', '{
      "id": "calendar",
      "title": "캘린더",
      "type": "item",
      "icon": "IconCalendar",
      "url": "/schedule",
      "componentPath": "features/schedule/pages/SchedulePage"
    }', 'MN1', NULL, 5, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 6. 메일함 (Collapse)
     * ------------------------------------------- */
    ('MN12', '메일함', '{
      "id": "mail",
      "title": "메일함",
      "type": "collapse",
      "icon": "IconMail"
    }', 'MN1', NULL, 6, 1, 1, NOW(), NOW(), false),
    ('MN13', '받은메일함', '{
      "id": "mail-inbox",
      "title": "받은메일함",
      "type": "item",
      "url": "/mail/list/INBOX",
      "target": false
    }', 'MN12', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('MN14', '보낸메일함', '{
      "id": "mail-sent",
      "title": "보낸메일함",
      "type": "item",
      "url": "/mail/list/SENT",
      "target": false
    }', 'MN12', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('MN15', '개인보관함', '{
      "id": "mail-my",
      "title": "개인보관함",
      "type": "item",
      "url": "/mail/list/MYBOX",
      "target": false
    }', 'MN12', NULL, 3, 1, 1, NOW(), NOW(), false),
    ('MN16', '휴지통', '{
      "id": "mail-trash",
      "title": "휴지통",
      "type": "item",
      "url": "/mail/list/TRASH",
      "target": false
    }', 'MN12', NULL, 4, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 7. 업무일지 (Collapse)
     * (MainRoutes.jsx에 정보가 없어 componentPath는 제외)
     * ------------------------------------------- */
    ('MN19', '업무일지', '{
      "id": "worklog",
      "title": "업무일지",
      "type": "collapse",
      "icon": "IconClipboardList"
    }', 'MN1', NULL, 7, 1, 1, NOW(), NOW(), false),
    ('MN20', '전체업무일지', '{
      "id": "worklog-all",
      "title": "전체업무일지",
      "type": "item",
      "url": "/worklog/list/all",
      "target": false
    }', 'MN19', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('MN21', '부서업무일지', '{
      "id": "worklog-department",
      "title": "부서업무일지",
      "type": "item",
      "url": "/worklog/list/department",
      "target": false
    }', 'MN19', NULL, 2, 1, 1, NOW(), NOW(), false),
    ('MN22', '나의업무일지', '{
      "id": "worklog-personal",
      "title": "나의업무일지",
      "type": "item",
      "url": "/worklog/list/personal",
      "target": false
    }', 'MN19', NULL, 3, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 8. 게시판 (Collapse)
     * (MainRoutes.jsx에 정보가 없어 componentPath는 제외)
     * ------------------------------------------- */
    ('MN23', '게시판', '{
      "id": "gaesipan",
      "title": "게시판",
      "type": "collapse",
      "icon": "IconBug"
    }', 'MN1', NULL, 8, 1, 1, NOW(), NOW(), false),
    ('MN24', '공지사항', '{
      "id": "isNotification",
      "title": "공지사항",
      "type": "item",
      "url": "/pages/error",
      "target": true
    }', 'MN23', NULL, 1, 1, 1, NOW(), NOW(), false),
    ('MN25', '자유게시판', '{
      "id": "freeBoard",
      "title": "자유게시판",
      "type": "item",
      "url": "/pages/500",
      "target": true
    }', 'MN23', NULL, 2, 1, 1, NOW(), NOW(), false),

    /* -------------------------------------------
     * 메뉴에 없는 라우트 (RO - Route)
     * ------------------------------------------- */
    ('RO0', '라우트', null, NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('RO1', '마이페이지', '{
      "id": "mypage",
      "title": "마이페이지",
      "type": "route",
      "url": "/mypage",
      "componentPath": "features/mypage/pages/MyInfoPage"
    }', NULL, NULL, 1, 1, 1, NOW(), NOW(), false),
    ('RO2', '결재 상세', '{
      "id": "approval-detail",
      "title": "결재 상세",
      "type": "route",
      "url": "/approval/detail/:docId",
      "componentPath": "features/approval/pages/ApprovalDetailPage"
    }', NULL, NULL, 2, 1, 1, NOW(), NOW(), false),
    ('RO3', '결재 목록', '{
      "id": "approval-list",
      "title": "결재 목록",
      "type": "route",
      "url": "/approval/list/:status",
      "componentPath": "features/approval/pages/ApprovalListPage"
    }', NULL, NULL, 3, 1, 1, NOW(), NOW(), false),
    ('RO4', '메일 목록', '{
      "id": "mail-list",
      "title": "메일 목록",
      "type": "route",
      "url": "/mail/list/:type",
      "componentPath": "features/mail/pages/MailListPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO6', '메일 신규작성/재작성', '{
      "id": "mail-rewrite",
      "title": "메일 신규작성/재작성",
      "type": "route",
      "url": "/mail/write/:mailId?",
      "componentPath": "features/mail/pages/MailWritePage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO7', '메일 상세', '{
      "id": "mail-detail",
      "title": "메일 상세",
      "type": "route",
      "url": "/mail/detail/:mailId",
      "componentPath": "features/mail/pages/MailDetailPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO8', '업무일지 목록', '{
      "id": "worklog-list",
      "title": "업무일지 목록",
      "type": "route",
      "url": "/worklog/list/:type",
      "componentPath": "features/worklog/pages/WorkLogListPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO9', '업무일지 작성/수정', '{
      "id": "worklog-write",
      "title": "업무일지 작성",
      "type": "route",
      "url": "/worklog/write/:worklogId?",
      "componentPath": "features/worklog/pages/WorkLogWritePage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO10', '업무일지 상세', '{
      "id": "worklog-detail",
      "title": "업무일지 상세",
      "type": "route",
      "url": "/worklog/detail/:worklogId",
      "componentPath": "features/worklog/pages/WorkLogDetailPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO11', '게시글 작성', '{
      "id": "post-write",
      "title": "게시글 작성",
      "type": "route",
      "url": "/post/write",
      "componentPath": "features/post/pages/PostWritePage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO12', '게시글 목록', '{
      "id": "post-list",
      "title": "게시글 목록",
      "type": "route",
      "url": "/post/list/:category",
      "componentPath": "features/post/pages/PostListPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false),
    ('RO13', '게시글 상세', '{
      "id": "post-detail",
      "title": "게시글 상세",
      "type": "route",
      "url": "/post/detail/:postId",
      "componentPath": "features/post/pages/PostDetailPage"
    }', NULL, NULL, 4, 1, 1, NOW(), NOW(), false);



# 이구문이 반드시 최하단에 있어야함
SET FOREIGN_KEY_CHECKS = 1;