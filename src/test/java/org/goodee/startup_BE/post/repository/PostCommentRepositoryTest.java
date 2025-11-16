package org.goodee.startup_BE.post.repository;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostComment;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@EntityScan(basePackages = "org.goodee.startup_BE")
class PostCommentRepositoryTest {

    // final 필드와 생성자 주입 유지 (Field injection 경고 제거)
    private final PostCommentRepository postCommentRepository;
    private final EmployeeRepository employeeRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PostRepository postRepository;

    // 테스트 전체에서 공유되어야 했던 필드들을 ID로 대체하거나 제거
    private Long testEmployeeId;
    private Long testPost1Id;
    private Long testPost2Id;

    private final String TEST_PASSWORD = "testPassword123!";

    // 생성자 주입
    @Autowired
    public PostCommentRepositoryTest(
            PostCommentRepository postCommentRepository,
            EmployeeRepository employeeRepository,
            CommonCodeRepository commonCodeRepository,
            PostRepository postRepository) {
        this.postCommentRepository = postCommentRepository;
        this.employeeRepository = employeeRepository;
        this.commonCodeRepository = commonCodeRepository;
        this.postRepository = postRepository;
    }

    // ID를 통해 Employee 객체를 조회하는 헬퍼 메서드
    private Employee getTestEmployee() {
        return employeeRepository.findById(testEmployeeId)
                .orElseThrow(() -> new NoSuchElementException("테스트 Employee 조회 실패"));
    }

    // ID를 통해 Post 객체를 조회하는 헬퍼 메서드
    private Post getTestPost1() {
        return postRepository.findById(testPost1Id)
                .orElseThrow(() -> new NoSuchElementException("테스트 Post1 조회 실패"));
    }

    private Post getTestPost2() {
        return postRepository.findById(testPost2Id)
                .orElseThrow(() -> new NoSuchElementException("테스트 Post2 조회 실패"));
    }


    @BeforeEach
    void setUp() {
        // DB 초기화
        postCommentRepository.deleteAll();
        postRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // CommonCode 필드를 지역 변수로 선언
        final CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        final CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 2L, null);
        final CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        final CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null);
        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior));

        // --- given: 테스트용 Employee 생성 ---
        Employee testEmployee = Employee.createEmployee(
                "commenter", "테스트", "commenter@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior,
                null
        );
        testEmployee.updateInitPassword(TEST_PASSWORD, null);
        testEmployee = employeeRepository.save(testEmployee);

        // 필드 대신 ID를 저장하여 경고 제거
        this.testEmployeeId = testEmployee.getEmployeeId();

        // --- given: 테스트용 Post 생성 ---
        Post testPost1 = Post.builder()
                .postId(null)
                .title("Test Post 1")
                .content("Content 1")
                .isDeleted(false)
                .employee(testEmployee)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Post testPost2 = Post.builder()
                .postId(null)
                .title("Test Post 2")
                .content("Content 2")
                .isDeleted(false)
                .employee(testEmployee)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testPost1 = postRepository.save(testPost1);
        testPost2 = postRepository.save(testPost2);

        // 필드 대신 ID를 저장하여 경고 제거
        this.testPost1Id = testPost1.getPostId();
        this.testPost2Id = testPost2.getPostId();
    }

    /**
     * PostComment.java의 createPostComment 팩토리 메서드를 사용하여 엔티티를 생성하고 저장
     */
    private PostComment createAndSaveComment(final Post post, final String content) {
        final PostComment comment = PostComment.createPostComment(
                post,
                getTestEmployee(), // 헬퍼 메서드 사용
                content
        );
        return postCommentRepository.save(comment);
    }

    // --------------------------------------------------------------------------------
    // 1. CRUD 테스트 (JpaRepository 기본 메서드)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("C: 댓글 저장(save) 테스트")
    void savePostCommentTest() {
        // given
        final Post testPost1 = getTestPost1(); // 헬퍼 메서드 사용
        final String content = "새로운 댓글입니다.";
        final PostComment newComment = PostComment.createPostComment(testPost1, getTestEmployee(), content);

        // when
        final PostComment savedComment = postCommentRepository.save(newComment);

        // then
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getCommentId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo(content);
        assertThat(savedComment.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("R: 댓글 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        final PostComment savedComment = createAndSaveComment(getTestPost1(), "조회할 댓글");

        // when
        final Optional<PostComment> foundComment = postCommentRepository.findById(savedComment.getCommentId());

        // then
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getContent()).isEqualTo("조회할 댓글");
    }

    @Test
    @DisplayName("R: 댓글 ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        final Long nonExistentId = 9999L;

        // when
        final Optional<PostComment> foundComment = postCommentRepository.findById(nonExistentId);

        // then
        assertThat(foundComment).isNotPresent();
    }

    @Test
    @DisplayName("U: 댓글 수정(update) 테스트")
    void updatePostCommentTest() {
        // given
        final PostComment savedComment = createAndSaveComment(getTestPost1(), "수정 전 내용");
        final LocalDateTime originalUpdatedAt = savedComment.getUpdatedAt();
        final String updatedContent = "수정된 내용입니다.";

        // when
        final PostComment commentToUpdate = postCommentRepository.findById(savedComment.getCommentId())
                .orElseThrow(() -> new NoSuchElementException("테스트 데이터 조회 실패"));

        commentToUpdate.update(updatedContent);
        postCommentRepository.flush();

        // 검증을 위해 다시 조회
        final PostComment updatedComment = postCommentRepository.findById(savedComment.getCommentId())
                .orElseThrow(() -> new NoSuchElementException("수정 후 데이터 조회 실패"));

        // then
        assertThat(updatedComment.getContent()).isEqualTo(updatedContent);
        assertThat(updatedComment.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("D: 댓글 삭제(delete) 테스트 - isDeleted = true 로 변경 (Soft Delete)")
    void softDeletePostCommentTest() {
        // given
        final PostComment savedComment = createAndSaveComment(getTestPost1(), "삭제할 댓글");
        final Long commentId = savedComment.getCommentId();

        // when
        final PostComment commentToDelete = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("삭제할 데이터 조회 실패"));

        commentToDelete.delete();
        postCommentRepository.flush();

        // then
        final PostComment deletedComment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("삭제된 데이터 조회 실패"));
        assertThat(deletedComment.getIsDeleted()).isTrue();
    }

    // --------------------------------------------------------------------------------
    // 2. Custom Repository Method 테스트 (PostCommentRepository.java)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("Custom: findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc (List) 테스트")
    void findActiveCommentsByPostIdListTest() throws InterruptedException {
        // given
        createAndSaveComment(getTestPost1(), "첫 번째 댓글");
        Thread.sleep(10);
        final PostComment deletedComment = createAndSaveComment(getTestPost1(), "삭제된 댓글");
        deletedComment.delete();
        postCommentRepository.save(deletedComment);
        Thread.sleep(10);
        createAndSaveComment(getTestPost1(), "세 번째 댓글");
        createAndSaveComment(getTestPost2(), "다른 게시글 댓글");

        // when
        final List<PostComment> activeComments = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost1Id);

        // then
        assertThat(activeComments).hasSize(2);
        assertThat(activeComments)
                .extracting(PostComment::getContent)
                .containsExactly("첫 번째 댓글", "세 번째 댓글");
    }

    @Test
    @DisplayName("Custom: findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc (Page) 테스트")
    void findActiveCommentsByPostIdPageTest() throws InterruptedException {
        // given
        createAndSaveComment(getTestPost1(), "1번 댓글");
        Thread.sleep(10);
        createAndSaveComment(getTestPost1(), "2번 댓글");
        Thread.sleep(10);
        createAndSaveComment(getTestPost1(), "3번 댓글");

        final Pageable pageable = PageRequest.of(0, 2);

        // when
        final Page<PostComment> commentPage = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost1Id, pageable);

        // then
        assertThat(commentPage.getTotalElements()).isEqualTo(3);
        assertThat(commentPage.getContent())
                .extracting(PostComment::getContent)
                .containsExactly("1번 댓글", "2번 댓글");

        // 다음 페이지 검증
        final Pageable secondPageable = PageRequest.of(1, 2);
        final Page<PostComment> secondPage = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost1Id, secondPageable);
        assertThat(secondPage.getContent())
                .extracting(PostComment::getContent)
                .containsExactly("3번 댓글");
    }

    @Test
    @DisplayName("Custom: findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc - 댓글 없는 경우")
    void findActiveCommentsEmptyTest() {
        // given (Post 1에 댓글 없음)

        // when
        final List<PostComment> activeComments = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost1Id);

        // then
        assertThat(activeComments).isEmpty();
    }


    // --------------------------------------------------------------------------------
    // 3. Exception (Constraints) 테스트
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("Exception: 필수 필드(content) null 저장 시 DataIntegrityViolationException 발생")
    void saveNullContentTest() {
        // given
        final PostComment incompleteComment = PostComment.builder()
                .post(getTestPost1())
                .employee(getTestEmployee())
                .content(null)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(post) null 저장 시 DataIntegrityViolationException 발생")
    void saveNullPostFKTest() {
        // given
        final PostComment incompleteComment = PostComment.builder()
                .post(null)
                .employee(getTestEmployee())
                .content("Valid content")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(employee) null 저장 시 DataIntegrityViolationException 발생")
    void saveNullEmployeeFKTest() {
        // given
        final PostComment incompleteComment = PostComment.builder()
                .post(getTestPost1())
                .employee(null)
                .content("Valid content")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}