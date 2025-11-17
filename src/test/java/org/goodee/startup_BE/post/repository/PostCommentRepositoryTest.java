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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// EmployeeRepositoryTest와 동일한 H2 DB 설정 강제
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
// @DataJpaTest가 Post, Employee, CommonCode 등 모든 엔티티를 찾도록 스캔 경로 지정
@EntityScan(basePackages = "org.goodee.startup_BE")
class PostCommentRepositoryTest {

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    // 테스트용 공통 데이터
    private Employee testUser;
    private Post testPost;
    private CommonCode postCategoryFree;
    private final String TEST_PASSWORD = "testPassword123!";

    @BeforeEach
    void setUp() {
        // FK 제약조건을 위해 자식 테이블부터 삭제
        postCommentRepository.deleteAll();
        postRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // --- given: 1. 공통 코드 데이터 생성 ---
        CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null, false);
        CommonCode roleUser = CommonCode.createCommonCode("ROLE_USER", "사용자", "USER", null, null, 1L, null, false);
        CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null, false);
        CommonCode posJunior = CommonCode.createCommonCode("POS_JUNIOR", "사원", "JUNIOR", null, null, 1L, null, false);
        postCategoryFree = CommonCode.createCommonCode("POST_FREE", "자유게시판", "FREE", null, null, 1L, null, false);

        commonCodeRepository.saveAll(List.of(statusActive, roleUser, deptDev, posJunior, postCategoryFree));

        // --- given: 2. 직원(댓글 작성자) 데이터 생성 ---
        testUser = Employee.createEmployee(
                "testuser", "테스트유저", "testuser@test.com", "010-1234-5678",
                LocalDate.now(), statusActive, roleUser, deptDev, posJunior,
                null
        );
        testUser.updateInitPassword(TEST_PASSWORD, null);
        employeeRepository.save(testUser);

        // --- given: 3. 게시글 데이터 생성 ---
        testPost = Post.create(postCategoryFree, testUser, "테스트 게시글", "내용입니다.", false, false);
        postRepository.save(testPost);
    }

    /**
     * 테스트용 댓글 엔티티를 생성하는 헬퍼 메서드
     * (PostComment.java의 팩토리 메서드 사용)
     */
    private PostComment createPersistableComment(Post post, Employee author, String content) {
        return PostComment.createPostComment(post, author, content);
    }

    @Test
    @DisplayName("C: 댓글 생성(save) 테스트")
    void saveCommentTest() {
        // given
        PostComment newComment = createPersistableComment(testPost, testUser, "새로운 댓글입니다.");

        // when
        PostComment savedComment = postCommentRepository.save(newComment);

        // then
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getCommentId()).isNotNull();
        assertThat(savedComment.getContent()).isEqualTo("새로운 댓글입니다.");
        assertThat(savedComment.getPost()).isEqualTo(testPost);
        assertThat(savedComment.getEmployee()).isEqualTo(testUser);
        assertThat(savedComment.getIsDeleted()).isFalse();
        assertThat(savedComment.getCreatedAt()).isNotNull();
        assertThat(savedComment.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("R: 댓글 ID로 조회(findById) 테스트 - 성공")
    void findByIdSuccessTest() {
        // given
        PostComment savedComment = postCommentRepository.save(
                createPersistableComment(testPost, testUser, "찾아주세요.")
        );

        // when
        Optional<PostComment> foundComment = postCommentRepository.findById(savedComment.getCommentId());

        // then
        assertThat(foundComment).isPresent();
        assertThat(foundComment.get().getCommentId()).isEqualTo(savedComment.getCommentId());
        assertThat(foundComment.get().getContent()).isEqualTo("찾아주세요.");
    }

    @Test
    @DisplayName("R: 댓글 ID로 조회(findById) 테스트 - 실패 (존재하지 않는 ID)")
    void findByIdFailureTest() {
        // given
        Long nonExistentId = 9999L;

        // when
        Optional<PostComment> foundComment = postCommentRepository.findById(nonExistentId);

        // then
        assertThat(foundComment).isNotPresent();
    }

    @Test
    @DisplayName("U: 댓글 수정(update) 테스트 (엔티티 메서드 + 변경 감지)")
    void updateCommentTest() throws InterruptedException {
        // given
        PostComment savedComment = postCommentRepository.save(
                createPersistableComment(testPost, testUser, "수정 전 내용")
        );
        LocalDateTime createdAt = savedComment.getCreatedAt();

        // @PreUpdate 시간을 명확히 구분하기 위해 잠시 대기
        Thread.sleep(10);

        // when
        // 1. 영속성 컨텍스트에서 엔티티 조회
        PostComment commentToUpdate = postCommentRepository.findById(savedComment.getCommentId()).get();

        // 2. 엔티티의 update 메서드 호출 (Dirty Checking 대상)
        commentToUpdate.update("수정된 내용입니다.");

        // 3. 변경 감지(Dirty Checking)를 테스트하기 위해 flush() 호출
        postCommentRepository.flush();

        // 4. 검증을 위해 다시 조회 (혹은 1번 엔티티 재사용)
        PostComment updatedComment = postCommentRepository.findById(savedComment.getCommentId()).get();

        // then
        assertThat(updatedComment.getContent()).isEqualTo("수정된 내용입니다.");
        assertThat(updatedComment.getUpdatedAt()).isAfter(createdAt); // @PreUpdate 동작 확인
    }

    @Test
    @DisplayName("D: 댓글 삭제(delete) 테스트 (엔티티 Soft Delete)")
    void softDeleteCommentTest() throws InterruptedException {
        // given
        PostComment savedComment = postCommentRepository.save(
                createPersistableComment(testPost, testUser, "삭제될 댓글")
        );
        LocalDateTime createdAt = savedComment.getCreatedAt();
        Thread.sleep(10);

        // when
        PostComment commentToDelete = postCommentRepository.findById(savedComment.getCommentId()).get();
        commentToDelete.delete(); // isDeleted = true, updatedAt 갱신
        postCommentRepository.flush();

        // then
        PostComment deletedComment = postCommentRepository.findById(savedComment.getCommentId()).get();
        assertThat(deletedComment.getIsDeleted()).isTrue();
        assertThat(deletedComment.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    @DisplayName("D: 댓글 삭제(deleteById) 테스트 (Hard Delete)")
    void hardDeleteCommentTest() {
        // given
        PostComment savedComment = postCommentRepository.save(
                createPersistableComment(testPost, testUser, "완전히 삭제될 댓글")
        );
        Long commentId = savedComment.getCommentId();
        assertThat(postCommentRepository.existsById(commentId)).isTrue();

        // when
        postCommentRepository.deleteById(commentId);
        postCommentRepository.flush(); // 삭제 쿼리 즉시 실행

        // then
        assertThat(postCommentRepository.existsById(commentId)).isFalse();
    }

    // --- Custom Repository Method Tests ---

    @Test
    @DisplayName("Custom: findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc (List) 테스트")
    void findByPostIdAndNotDeletedTest() throws InterruptedException {
        // given
        // 다른 게시글 생성
        Post otherPost = Post.create(postCategoryFree, testUser, "다른 게시글", "내용", false, false);
        postRepository.save(otherPost);

        // 테스트 게시글(testPost)의 댓글
        PostComment c1 = createPersistableComment(testPost, testUser, "댓글 1");
        postCommentRepository.save(c1);
        Thread.sleep(5); // 순서 보장

        PostComment c2_deleted = createPersistableComment(testPost, testUser, "댓글 2 (삭제됨)");
        postCommentRepository.save(c2_deleted);
        c2_deleted.delete(); // soft delete
        postCommentRepository.flush();
        Thread.sleep(5);

        PostComment c3 = createPersistableComment(testPost, testUser, "댓글 3");
        postCommentRepository.save(c3);

        // 다른 게시글(otherPost)의 댓글
        PostComment c4_other = createPersistableComment(otherPost, testUser, "다른 게시글의 댓글");
        postCommentRepository.save(c4_other);

        // when
        List<PostComment> comments = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost.getPostId());

        // then
        assertThat(comments).hasSize(2); // 삭제된 댓글(c2)과 다른 게시글 댓글(c4)은 제외
        assertThat(comments).containsExactly(c1, c3); // createdAt 오름차순 정렬 확인
        assertThat(comments).doesNotContain(c2_deleted, c4_other);
    }

    @Test
    @DisplayName("Custom: findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc (Page) 테스트")
    void findByPostIdAndNotDeletedPageableTest() throws InterruptedException {
        // given
        PostComment c1 = postCommentRepository.save(createPersistableComment(testPost, testUser, "댓글 1"));
        Thread.sleep(5);
        PostComment c2 = postCommentRepository.save(createPersistableComment(testPost, testUser, "댓글 2"));
        Thread.sleep(5);
        PostComment c3 = postCommentRepository.save(createPersistableComment(testPost, testUser, "댓글 3"));

        // Pageable: 0페이지, 사이즈 2
        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<PostComment> commentPage = postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(testPost.getPostId(), pageable);

        // then
        assertThat(commentPage.getTotalElements()).isEqualTo(3); // 총 댓글 수
        assertThat(commentPage.getTotalPages()).isEqualTo(2); // 총 페이지 (3개 / 2 = 1.5 -> 2)
        assertThat(commentPage.getContent()).hasSize(2); // 현재 페이지의 아이템 수
        assertThat(commentPage.getContent()).containsExactly(c1, c2); // 0페이지의 아이템 (c1, c2)
    }

    // --- Exception (Constraints) Tests ---

    @Test
    @DisplayName("Exception: 필수 FK(Post) null 저장 시 예외 발생")
    void saveNullPostTest() {
        // given
        // PostComment.java의 @Builder를 사용하여 의도적으로 Post를 null로 설정
        PostComment incompleteComment = PostComment.builder()
                .employee(testUser) // Employee는 설정
                .content("Post가 없는 댓글")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        // @JoinColumn(name = "post_id", nullable = false) 위반
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 FK(Employee) null 저장 시 예외 발생")
    void saveNullEmployeeTest() {
        // given
        PostComment incompleteComment = PostComment.builder()
                .post(testPost) // Post는 설정
                .content("Employee가 없는 댓글")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        // @JoinColumn(name = "employee_id", nullable = false) 위반
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(content) null 저장 시 예외 발생")
    void saveNullContentTest() {
        // given
        PostComment incompleteComment = PostComment.builder()
                .post(testPost)
                .employee(testUser)
                // content (nullable = false) 누락
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        // @Column(name = "content", nullable = false) 위반
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(isDeleted) null 저장 시 예외 발생")
    void saveNullIsDeletedTest() {
        // given
        PostComment incompleteComment = PostComment.builder()
                .post(testPost)
                .employee(testUser)
                .content("isDeleted가 null")
                // isDeleted (nullable = false) 누락
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        // @Column(name = "is_deleted", nullable = false) 위반
        assertThatThrownBy(() -> postCommentRepository.saveAndFlush(incompleteComment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}