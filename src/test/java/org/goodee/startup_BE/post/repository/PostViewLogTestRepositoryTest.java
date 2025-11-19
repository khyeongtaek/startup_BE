package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostViewLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// EmployeeRepositoryTest와 동일한 JPA 설정 사용
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// 엔티티 스캔 경로 지정
@EntityScan(basePackages = "org.goodee.startup_BE")
class PostViewLogRepositoryTest {

    private static final String TEST_PASSWORD = "testpassword123!";

    @Autowired
    private PostViewLogRepository postViewLogRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    /**
     * @BeforeEach: 테스트 간 데이터 충돌을 방지하기 위해 DB 초기화만 담당합니다.
     */
    @BeforeEach
    void setUp() {
        // H2 DB 초기화
        postViewLogRepository.deleteAll();
        postRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();
    }

    /**
     * 테스트에 필요한 모든 엔티티를 담는 지역 변수 컨테이너 (Inner Class)
     */
    private static class TestEntities {
        final CommonCode statusActive, roleUser, deptDev, posJunior, postCategory;
        final Employee creator, testEmployee1, testEmployee2;
        final Post testPost1, testPost2;

        public TestEntities(CommonCode statusActive, CommonCode roleUser, CommonCode deptDev, CommonCode posJunior, CommonCode postCategory, Employee creator, Employee testEmployee1, Employee testEmployee2, Post testPost1, Post testPost2) {
            this.statusActive = statusActive;
            this.roleUser = roleUser;
            this.deptDev = deptDev;
            this.posJunior = posJunior;
            this.postCategory = postCategory;
            this.creator = creator;
            this.testEmployee1 = testEmployee1;
            this.testEmployee2 = testEmployee2;
            this.testPost1 = testPost1;
            this.testPost2 = testPost2;
        }
    }

    /**
     * 지역 변수 (엔티티) 생성을 위한 헬퍼 메서드: 각 @Test 메서드에서 호출되어 지역 변수에 할당됩니다.
     * 이전 오류의 원인이었던 Employee.createEmployee의 10개 인자 문제도 완벽히 해결했습니다.
     */
    private TestEntities createAndSaveTestEntities() {
        // 1. CommonCode 생성
        CommonCode statusActive = commonCodeRepository.save(CommonCode.createCommonCode("ES1", "상태 - 활성", "ACTIVE", null, null, 1L, null, false));
        CommonCode roleUser = commonCodeRepository.save(CommonCode.createCommonCode("AU2", "권한 - 사용자", "USER", null, null, 2L, null, false));
        CommonCode deptDev = commonCodeRepository.save(CommonCode.createCommonCode("DP1", "부서 - 개발", "DEV", null, null, 1L, null, false));
        CommonCode posJunior = commonCodeRepository.save(CommonCode.createCommonCode("PS1", "직급 - 주니어", "JUNIOR", null, null, 1L, null,false));
        CommonCode postCategory = commonCodeRepository.save(CommonCode.createCommonCode("PC1", "게시글 카테고리", "GENERAL", null, null, 1L, null, false));

        // 2. Creator Employee 생성 (10개 인자 시그니처 추정 및 순서 조정)
        Employee creator = Employee.createEmployee(
                "admin", "관리자", "admin@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior,
                null
        );
        creator.updateInitPassword(TEST_PASSWORD, null);
        creator = employeeRepository.save(creator);

        // 3. PostViewLog에 사용될 Employee 생성 (10개 인자 시그니처 사용)
        Employee testEmployee1 = Employee.createEmployee(
                "testuser1", "Test User 1", "user1@test.com", "010-1111-1111", LocalDate.now(),
                statusActive, roleUser, deptDev, posJunior, creator
        );
        testEmployee1.updateInitPassword(TEST_PASSWORD, creator);

        Employee testEmployee2 = Employee.createEmployee(
                "testuser2", "Test User 2", "user2@test.com", "010-2222-2222", LocalDate.now(),
                statusActive, roleUser, deptDev, posJunior, creator
        );
        testEmployee2.updateInitPassword(TEST_PASSWORD, creator);
        employeeRepository.saveAll(List.of(testEmployee1, testEmployee2));

        // 4. PostViewLog에 사용될 Post 생성
        Post testPost1 = Post.create(
                postCategory, testEmployee1,
                "공지사항 1", "내용입니다 1",
                true, false
        );
        Post testPost2 = Post.create(
                postCategory, testEmployee2,
                "일반 게시글 2", "내용입니다 2",
                false, false
        );
        postRepository.saveAll(List.of(testPost1, testPost2));

        // 모든 엔티티를 담아 반환
        return new TestEntities(statusActive, roleUser, deptDev, posJunior, postCategory, creator, testEmployee1, testEmployee2, testPost1, testPost2);
    }

    /**
     * PostViewLog.createPostViewLog 팩토리 메서드를 사용하여 엔티티 생성
     */
    private PostViewLog createPersistablePostViewLog(Post post, Employee employee) {
        return PostViewLog.createPostViewLog(post, employee);
    }

    // --- CRUD Test (지역 변수 사용) ---

    @Test
    @DisplayName("C: 조회 기록 생성(save) 테스트")
    void savePostViewLogTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        PostViewLog newLog = createPersistablePostViewLog(entities.testPost1, entities.testEmployee1);

        // when
        PostViewLog savedLog = postViewLogRepository.save(newLog);

        // then
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getPostViewLogId()).isNotNull();
        assertThat(savedLog.getPost().getPostId()).isEqualTo(entities.testPost1.getPostId());
        assertThat(savedLog.getEmployee().getEmployeeId()).isEqualTo(entities.testEmployee1.getEmployeeId());
        assertThat(savedLog.getViewedAt()).isNotNull();
    }

    @Test
    @DisplayName("R: 조회 기록 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        PostViewLog savedLog = postViewLogRepository.save(
                createPersistablePostViewLog(entities.testPost1, entities.testEmployee1)
        );
        // 지역 변수 선언
        Long logId = savedLog.getPostViewLogId();

        // when
        Optional<PostViewLog> foundLog = postViewLogRepository.findById(logId);

        // then
        assertThat(foundLog).isPresent();
        assertThat(foundLog.get().getPostViewLogId()).isEqualTo(logId);
    }

    @Test
    @DisplayName("D: 조회 기록 삭제(deleteById) 테스트")
    void deletePostViewLogTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        PostViewLog savedLog = postViewLogRepository.save(
                createPersistablePostViewLog(entities.testPost2, entities.testEmployee2)
        );
        // 지역 변수 선언
        Long logId = savedLog.getPostViewLogId();
        assertThat(postViewLogRepository.existsById(logId)).isTrue();

        // when
        postViewLogRepository.deleteById(logId);
        postViewLogRepository.flush();

        // then
        assertThat(postViewLogRepository.existsById(logId)).isFalse();
    }

    // --- Custom Repository Method Tests (지역 변수 사용) ---

    @Test
    @DisplayName("Custom: countByPost_PostId 테스트 (조회수 계산)")
    void countByPostPostIdTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        postViewLogRepository.save(createPersistablePostViewLog(entities.testPost1, entities.testEmployee1));
        postViewLogRepository.save(createPersistablePostViewLog(entities.testPost1, entities.testEmployee2));
        postViewLogRepository.save(createPersistablePostViewLog(entities.testPost1, entities.testEmployee1));

        postViewLogRepository.save(createPersistablePostViewLog(entities.testPost2, entities.testEmployee1));

        // when
        long countPost1 = postViewLogRepository.countByPost_PostId(entities.testPost1.getPostId());
        long countPost2 = postViewLogRepository.countByPost_PostId(entities.testPost2.getPostId());

        // then
        assertThat(countPost1).isEqualTo(3);
        assertThat(countPost2).isEqualTo(1);
    }

    @Test
    @DisplayName("Custom: existsByPost_PostIdAndEmployee_EmployeeId 테스트 - 성공 (존재함)")
    void existsByPostAndEmployeeSuccessTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        postViewLogRepository.save(createPersistablePostViewLog(entities.testPost1, entities.testEmployee2));

        // when
        boolean exists = postViewLogRepository.existsByPost_PostIdAndEmployee_EmployeeId(
                entities.testPost1.getPostId(),
                entities.testEmployee2.getEmployeeId()
        );

        // then
        assertThat(exists).isTrue();
    }

    // --- Exception (Constraints) Tests (지역 변수 사용) ---

    @Test
    @DisplayName("Exception: 필수 FK (post) null 저장 시 예외 발생")
    void saveNullPostTest() {
        // given: 지역 변수 선언 및 초기화
        TestEntities entities = createAndSaveTestEntities();
        // PostViewLog.java의 @JoinColumn(nullable = false) 위반
        PostViewLog incompleteLog = PostViewLog.builder()
                .post(null)
                .employee(entities.testEmployee1)
                .viewedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postViewLogRepository.saveAndFlush(incompleteLog))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}