package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// EmployeeRepositoryTest와 동일한 H2 테스트 환경 설정
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// Post, Employee, CommonCode 엔티티를 스캔
@EntityScan(basePackages = "org.goodee.startup_BE")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // --- 테스트용 공통 데이터 ---
    private Employee testUser1, testUser2;
    private CommonCode categoryNotice, categoryFree, categoryQa;
    private CommonCode statusActive, roleUser, deptDev, deptHr, posJunior;
    private final String TEST_PASSWORD = "testPassword123!";

    // searchPost 테스트용 데이터
    private Post post1, post2, post3, post4_deleted;

    @BeforeEach
    void setUp() {
        // 의존성 순서에 맞게 삭제 (Post -> Employee -> CommonCode)
        postRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 공통 코드 데이터 생성 (Employee용) ---
        statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null, false);
        deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        deptHr = CommonCode.createCommonCode("DEPT_HR", "인사팀", "HR", null, null, 2L, null, false);
        posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);

        // --- given: 공통 코드 데이터 생성 (Post용) ---
        categoryNotice = CommonCode.createCommonCode("CAT_NOTICE", "공지사항", "NOTICE", null, null, 1L, null, false);
        categoryFree = CommonCode.createCommonCode("CAT_FREE", "자유게시판", "FREE", null, null, 2L, null, false);
        categoryQa = CommonCode.createCommonCode("CAT_QA", "Q&A", "QA", null, null, 3L, null, false);

        commonCodeRepository.saveAll(List.of(
                statusActive, roleUser, deptDev, deptHr, posJunior,
                categoryNotice, categoryFree, categoryQa
        ));

        // --- given: 직원(Employee) 데이터 생성 ---
        testUser1 = createPersistableEmployee("user1", "테스트유저1", "user1@test.com", deptDev);
        testUser2 = createPersistableEmployee("user2", "테스트유저2", "user2@test.com", deptHr);
        employeeRepository.saveAll(List.of(testUser1, testUser2));

        // --- given: searchPost 테스트용 게시글 데이터 생성 ---
        post1 = createPersistablePost("공지-제목A-유저1", "검색용 내용입니다", testUser1, categoryNotice, true);
        post2 = createPersistablePost("자유-제목B-유저1", "검색용 내용입니다", testUser1, categoryFree, false);
        post3 = createPersistablePost("공지-제목C-유저2", "특별한 검색어", testUser2, categoryNotice, false);
        post4_deleted = createPersistablePost("삭제된글-유저1", "삭제된 내용", testUser1, categoryNotice, false);
        post4_deleted.delete(); // soft delete

        postRepository.saveAll(List.of(post1, post2, post3, post4_deleted));
    }

    /**
     * 테스트용 직원 엔티티 생성 헬퍼 (EmployeeRepositoryTest 형식 준수)
     */
    private Employee createPersistableEmployee(String username, String name, String email, CommonCode dept) {
        Employee employee = Employee.createEmployee(
                username, name, email, "010-1234-5678",
                LocalDate.now(), statusActive, roleUser, dept, posJunior,
                null // 테스트상 creator는 null로 통일
        );
        employee.updateInitPassword(TEST_PASSWORD, null);
        return employee;
    }

    /**
     * 테스트용 게시글 엔티티 생성 헬퍼
     */
    private Post createPersistablePost(String title, String content, Employee author, CommonCode category, Boolean isNotification) {
        return Post.create(
                category,
                author,
                title,
                content,
                isNotification,
                false // alert
        );
    }

    // --- CRUD Tests ---

    @Test
    @DisplayName("C: 게시글 생성(save) 테스트")
    void savePostTest() {
        // given
        Post newPost = createPersistablePost("새 게시글", "내용입니다", testUser1, categoryQa, false);

        // when
        Post savedPost = postRepository.save(newPost);

        // then
        assertThat(savedPost).isNotNull();
        assertThat(savedPost.getPostId()).isNotNull();
        assertThat(savedPost.getTitle()).isEqualTo("새 게시글");
        assertThat(savedPost.getEmployee()).isEqualTo(testUser1);
        // Post.create()가 employee.getName()을 저장하는지 확인
        assertThat(savedPost.getEmployeeName()).isEqualTo(testUser1.getName());
        assertThat(savedPost.getCommonCode()).isEqualTo(categoryQa);
        assertThat(savedPost.getIsDeleted()).isFalse();
        assertThat(savedPost.getIsNotification()).isFalse();
        assertThat(savedPost.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("R: 게시글 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given (setUp에서 post1이 저장됨)

        // when
        Optional<Post> foundPost = postRepository.findById(post1.getPostId());

        // then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getPostId()).isEqualTo(post1.getPostId());
        assertThat(foundPost.get().getTitle()).isEqualTo(post1.getTitle());
    }

    @Test
    @DisplayName("R: 게시글 ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<Post> foundPost = postRepository.findById(nonExistentId);

        // then
        assertThat(foundPost).isNotPresent();
    }

    @Test
    @DisplayName("U: 게시글 수정(update) 테스트")
    void updatePostTest() throws InterruptedException {
        // given
        Post savedPost = postRepository.save(
                createPersistablePost("수정 전 제목", "수정 전 내용", testUser1, categoryFree, false)
        );
        LocalDateTime createdAt = savedPost.getCreatedAt();

        // @PreUpdate 시간을 명확히 구분하기 위해 잠시 대기
        Thread.sleep(10);

        // when
        // 영속성 컨텍스트에서 엔티티를 가져옴
        Post postToUpdate = postRepository.findById(savedPost.getPostId()).get();
        String newTitle = "수정된 제목";
        String newContent = "수정된 내용";
        boolean newNotification = true;

        // 엔티티의 update 메서드 호출 (JPA 변경 감지)
        postToUpdate.update(newTitle, newContent, newNotification);

        // flush로 DB에 즉시 반영
        postRepository.flush();

        // when
        // 검증을 위해 DB에서 다시 조회
        Post updatedPost = postRepository.findById(savedPost.getPostId()).get();

        // then
        assertThat(updatedPost.getTitle()).isEqualTo(newTitle);
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
        assertThat(updatedPost.getIsNotification()).isEqualTo(newNotification);
        assertThat(updatedPost.getUpdatedAt()).isAfter(createdAt); // @PreUpdate 동작 확인
    }

    @Test
    @DisplayName("D: 게시글 논리적 삭제(soft delete) 테스트")
    void softDeletePostTest() {
        // given (setUp에서 post1이 저장됨)
        assertThat(post1.getIsDeleted()).isFalse();

        // when
        // 영속성 컨텍스트에서 가져와서 엔티티 메서드 호출
        Post postToDelete = postRepository.findById(post1.getPostId()).get();
        postToDelete.delete(); // isDeleted = true로 변경
        postRepository.flush();

        // then
        Post deletedPost = postRepository.findById(post1.getPostId()).get();
        assertThat(deletedPost.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("D: 게시글 물리적 삭제(deleteById) 테스트")
    void hardDeletePostTest() {
        // given
        Post savedPost = postRepository.save(
                createPersistablePost("삭제될 글", "내용", testUser1, categoryFree, false)
        );
        Long postId = savedPost.getPostId();
        assertThat(postRepository.existsById(postId)).isTrue();

        // when
        postRepository.deleteById(postId);
        postRepository.flush();

        // then
        assertThat(postRepository.existsById(postId)).isFalse();
    }

    // --- Custom Repository Method (searchPost) Tests ---

    @Test
    @DisplayName("Custom: searchPost - 파라미터 없음 (삭제 제외 전체 조회)")
    void searchPost_FindAllActive() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // categoryId, title, content, employeeName 모두 null
        Page<Post> result = postRepository.searchPost(null, null, null, null, pageable);

        // then
        // setUp에서 생성한 4개 중 삭제된(post4_deleted) 1개를 제외한 3개
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).containsExactlyInAnyOrder(post1, post2, post3);
    }

    @Test
    @DisplayName("Custom: searchPost - CategoryId로 검색")
    void searchPost_ByCategoryId() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // 공지사항(categoryNotice)으로 검색
        Page<Post> result = postRepository.searchPost(categoryNotice.getCommonCodeId(), null, null, null, pageable);

        // then
        // post1(공지), post3(공지) 2개
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactlyInAnyOrder(post1, post3);
    }

    @Test
    @DisplayName("Custom: searchPost - Title로 검색")
    void searchPost_ByTitle() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // "제목A" (post1)
        Page<Post> result = postRepository.searchPost(null, "제목A", null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).contains(post1);
    }

    @Test
    @DisplayName("Custom: searchPost - Content로 검색")
    void searchPost_ByContent() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // "특별한" (post3)
        Page<Post> result = postRepository.searchPost(null, null, "특별한", null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).contains(post3);
    }

    @Test
    @DisplayName("Custom: searchPost - EmployeeName으로 검색")
    void searchPost_ByEmployeeName() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String user1Name = testUser1.getName(); // "테스트유저1"

        // when
        Page<Post> result = postRepository.searchPost(null, null, null, user1Name, pageable);

        // then
        // post1(유저1), post2(유저1) 2개
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactlyInAnyOrder(post1, post2);
    }

    @Test
    @DisplayName("Custom: searchPost - 복합 조건 검색 (Category + Name)")
    void searchPost_ByCombined() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        String user2Name = testUser2.getName(); // "테스트유저2"

        // when
        // 공지사항(categoryNotice) + 작성자(user2Name)
        Page<Post> result = postRepository.searchPost(categoryNotice.getCommonCodeId(), null, null, user2Name, pageable);

        // then
        // post3(공지, 유저2) 1개
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).contains(post3);
    }

    @Test
    @DisplayName("Custom: searchPost - 검색 결과 없음")
    void searchPost_NoResult() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // 자유게시판(categoryFree) + 제목("제목A") -> 일치하는 항목 없음
        Page<Post> result = postRepository.searchPost(categoryFree.getCommonCodeId(), "제목A", null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Custom: searchPost - 삭제된 게시글(isDeleted=true)은 검색 제외")
    void searchPost_IgnoresDeleted() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        // "삭제된글" (post4_deleted) 검색
        Page<Post> result = postRepository.searchPost(null, "삭제된글", null, null, pageable);

        // then
        // @Query에서 p.isDeleted = false 조건으로 인해 검색되지 않아야 함
        assertThat(result.getTotalElements()).isEqualTo(0);
    }


    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(commonCode) null 저장 시 예외 발생")
    void saveNullCategoryTest() {
        // given
        // Post.create()는 NPE를 유발하므로, builder로 직접 생성하여 DB 제약조건 테스트
        Post post = Post.builder()
                .commonCode(null) // nullable=false 위반
                .employee(testUser1)
                .employeeName(testUser1.getName())
                .title("제목")
                .content("내용")
                .isNotification(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .alert(false)
                .build();

        // when & then
        // saveAndFlush()로 즉시 DB에 쿼리를 전송하여 제약조건 위반 확인
        assertThatThrownBy(() -> postRepository.saveAndFlush(post))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


}