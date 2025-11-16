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
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest(
        // Repository와 Entity 스캔 범위를 명시적으로 설정
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
        }
)
@EntityScan(basePackages = "org.goodee.startup_BE")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommonCodeRepository commonCodeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 테스트 환경에서 필수적으로 사용되는 불변 필드만 유지하여 경고를 제거합니다.
    private Employee creator;
    private Long noticeCategoryId;
    private Long freeCategoryId;

    private static final String TEST_PASSWORD = "testPassword123!"; // 상수(static final)로 변환

    /**
     * @BeforeEach: 테스트 실행 전 환경 설정 및 데이터 초기화
     * CommonCode 객체들은 지역 변수로 선언하여 경고를 제거합니다.
     */
    @BeforeEach
    void setUp() {
        // H2 DB 초기화
        postRepository.deleteAll();
        employeeRepository.deleteAll();
        commonCodeRepository.deleteAll();

        // 1. Employee 및 Post 생성에 필요한 공통 코드 객체 (지역 변수로 선언)
        final CommonCode statusActive = CommonCode.createCommonCode("STATUS_ACTIVE", "재직", "ACTIVE", null, null, 1L, null);
        final CommonCode roleAdmin = CommonCode.createCommonCode("ROLE_ADMIN", "관리자", "ADMIN", null, null, 1L, null);
        final CommonCode deptDev = CommonCode.createCommonCode("DEPT_DEV", "개발팀", "DEV", null, null, 1L, null);
        final CommonCode posSenior = CommonCode.createCommonCode("POS_SENIOR", "대리", "SENIOR", null, null, 2L, null);

        final CommonCode postCategoryNotice = CommonCode.createCommonCode("PC1", "게시판 카테고리", "NOTICE", "공지사항", null, 1L, null);
        final CommonCode postCategoryFree = CommonCode.createCommonCode("PC2", "게시판 카테고리", "FREE", "자유게시판", null, 2L, null);

        // CommonCode 저장 및 ID 저장 (ID는 @Test 메서드에서 필요하므로 필드로 저장)
        commonCodeRepository.saveAll(List.of(
                statusActive, roleAdmin, deptDev, posSenior
        ));
        noticeCategoryId = commonCodeRepository.save(postCategoryNotice).getCommonCodeId();
        freeCategoryId = commonCodeRepository.save(postCategoryFree).getCommonCodeId();

        // 2. Creator Employee 생성
        creator = Employee.createEmployee(
                "admin", "관리자", "admin@test.com", "010-0000-0000",
                LocalDate.now(), statusActive, roleAdmin, deptDev, posSenior,
                null
        );
        creator.updateInitPassword(TEST_PASSWORD, null);
        employeeRepository.save(creator);
    }

    /**
     * DB에서 CommonCode를 조회하여 유효한 Post 엔티티를 생성하는 헬퍼 메서드
     * CommonCode 객체 대신 ID를 받아 내부에서 조회하여 CommonCode 필드 사용 경고를 제거합니다.
     */
    private Post createValidPost(Long categoryId, String title, String content) {
        final CommonCode category = commonCodeRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Test setup error: CommonCode not found"));

        return Post.create(
                category,
                creator, // 필드 유지
                title,
                content,
                true, // isNotification
                false // alert
        );
    }

    // -------------------------------------------------------------------------------- //
    // ------------------------------------ C. Create & R. Read ----------------------- //
    // -------------------------------------------------------------------------------- //

    @Test
    @DisplayName("C, R: Post 생성 후 ID로 조회 테스트")
    void saveAndFindByIdTest() {
        // given
        Post newPost = createValidPost(noticeCategoryId, "첫 게시글", "내용입니다.");

        // when
        Post savedPost = postRepository.save(newPost);
        Optional<Post> foundPost = postRepository.findById(savedPost.getPostId());

        // then
        assertThat(foundPost).isPresent();
        assertThat(foundPost.get().getTitle()).isEqualTo("첫 게시글");
        assertThat(foundPost.get().getEmployee().getEmployeeId()).isEqualTo(creator.getEmployeeId());
    }

    @Test
    @DisplayName("R: 전체 Post 조회(findAll) 테스트")
    void findAllPostsTest() {
        // given
        postRepository.save(createValidPost(noticeCategoryId, "공지1", "내용1"));
        postRepository.save(createValidPost(freeCategoryId, "자유2", "내용2"));
        postRepository.save(createValidPost(noticeCategoryId, "공지3", "내용3"));

        // when
        List<Post> posts = postRepository.findAll();

        // then
        assertThat(posts).hasSize(3);
        assertThat(posts).extracting(Post::getTitle).containsExactlyInAnyOrder("공지1", "자유2", "공지3");
    }

    // -------------------------------------------------------------------------------- //
    // ------------------------------------ U. Update --------------------------------- //
    // -------------------------------------------------------------------------------- //

    @Test
    @DisplayName("U: Post 정보 수정(update) 테스트")
    void updatePostTest() throws InterruptedException {
        // given
        Post savedPost = postRepository.save(createValidPost(noticeCategoryId, "수정 전 제목", "수정 전 내용"));
        LocalDateTime originalUpdateTime = savedPost.getUpdatedAt();

        // when
        // 시간을 약간 지연시켜 updated_at 값이 변경됨을 보장
        TimeUnit.MILLISECONDS.sleep(100);

        // Post 엔티티의 update 메서드 사용
        String newTitle = "수정 후 제목";
        String newContent = "수정 후 내용";
        savedPost.update(newTitle, newContent, false);

        Post updatedPost = postRepository.saveAndFlush(savedPost);

        // then
        assertThat(updatedPost.getTitle()).isEqualTo(newTitle);
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
        // UpdatedAt이 변경되었는지 확인
        assertThat(updatedPost.getUpdatedAt()).isAfter(originalUpdateTime);
    }

    // -------------------------------------------------------------------------------- //
    // ------------------------------------ D. Delete --------------------------------- //
    // -------------------------------------------------------------------------------- //

    @Test
    @DisplayName("D: Post 논리적 삭제(delete) 테스트 - isDeleted 변경 확인")
    void softDeletePostTest() {
        // given
        Post postToDelete = postRepository.save(createValidPost(freeCategoryId, "삭제 대상", "내용"));
        Long postId = postToDelete.getPostId();
        assertThat(postToDelete.getIsDeleted()).isFalse();

        // when
        postToDelete.delete(); // Post 엔티티의 delete() 메서드 사용
        postRepository.saveAndFlush(postToDelete); // DB에 변경 사항 반영

        // then
        Optional<Post> deletedPost = postRepository.findById(postId);
        assertThat(deletedPost).isPresent();
        assertThat(deletedPost.get().getIsDeleted()).isTrue(); // 논리적 삭제 확인
    }


    // -------------------------------------------------------------------------------- //
    // --------------------------------- Custom Query: searchPost --------------------- //
    // -------------------------------------------------------------------------------- //

    @Test
    @DisplayName("Custom: searchPost - 제목으로 검색 테스트 (삭제된 글 제외)")
    void searchPost_ByTitle_Test() {
        // given
        postRepository.save(createValidPost(noticeCategoryId, "중요한 공지사항", "내용1"));
        postRepository.save(createValidPost(freeCategoryId, "자유로운 의견", "내용2"));
        Post deletedPost = createValidPost(noticeCategoryId, "삭제된 공지사항", "내용4");
        postRepository.save(deletedPost);
        deletedPost.delete(); // 삭제
        postRepository.saveAndFlush(deletedPost); // DB에 삭제 상태 반영

        Pageable pageable = PageRequest.of(0, 10, Sort.by("postId").ascending());

        // when
        Page<Post> result = postRepository.searchPost(
                null, "공지사항", null, null, pageable // 제목 검색
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1); // '중요한 공지사항' 1건만 조회
        assertThat(result.getContent()).extracting(Post::getTitle)
                .containsExactly("중요한 공지사항");
    }


    @Test
    @DisplayName("Custom: searchPost - 검색 조건이 모두 null일 경우 전체 비삭제 게시글 조회 테스트")
    void searchPost_NoCriteria_Test() {
        // given
        postRepository.save(createValidPost(noticeCategoryId, "공지1", "내용1"));
        postRepository.save(createValidPost(freeCategoryId, "자유2", "내용2"));

        // 삭제된 게시글
        Post deletedPost = createValidPost(noticeCategoryId, "삭제글", "내용3");
        postRepository.save(deletedPost);
        deletedPost.delete();
        postRepository.saveAndFlush(deletedPost); // DB에 삭제 상태 반영

        Pageable pageable = PageRequest.of(0, 10);

        // when
        // 모든 검색 조건(categoryId, title, content, employeeName)이 null인 경우
        Page<Post> result = postRepository.searchPost(
                null, null, null, null, pageable
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(2); // 삭제되지 않은 2개만 조회되어야 함
        assertThat(result.getContent()).extracting(Post::getTitle)
                .containsExactlyInAnyOrder("공지1", "자유2");
    }

    @Test
    @DisplayName("Custom: searchPost - 카테고리 ID와 작성자 이름으로 검색 테스트")
    void searchPost_ByCategoryAndEmployeeName_Test() {
        // given
        postRepository.save(createValidPost(noticeCategoryId, "공지1", "작성1"));
        postRepository.save(createValidPost(freeCategoryId, "자유2", "작성2"));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        // 공지 카테고리(noticeCategoryId)와 작성자 이름("관리자")으로 검색
        Page<Post> result = postRepository.searchPost(
                noticeCategoryId, null, null, creator.getName(), pageable
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("공지1");
        assertThat(result.getContent().get(0).getEmployeeName()).isEqualTo(creator.getName());
    }


    // -------------------------------------------------------------------------------- //
    // --------------------------------- Exception Test ------------------------------- //
    // -------------------------------------------------------------------------------- //

    @Test
    @DisplayName("Exception: 필수 FK(Employee) null 저장 시 DataIntegrityViolationException 발생")
    void saveNullEmployeeTest() {
        // given
        final CommonCode postCategoryNotice = commonCodeRepository.findById(noticeCategoryId)
                .orElseThrow(() -> new RuntimeException("Test setup error"));

        // Employee 필드를 null로 설정 (nullable = false 위반)
        Post incompletePost = Post.builder()
                .commonCode(postCategoryNotice)
                .employee(null) // 필수 FK 누락
                .employeeName(creator.getName())
                .title("제목")
                .content("내용")
                .isNotification(false)
                .alert(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postRepository.saveAndFlush(incompletePost))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Exception: 필수 필드(title) null 저장 시 DataIntegrityViolationException 발생")
    void saveNullTitleTest() {
        // given
        final CommonCode postCategoryNotice = commonCodeRepository.findById(noticeCategoryId)
                .orElseThrow(() -> new RuntimeException("Test setup error"));

        // Title 필드를 null로 설정하여 @Column(nullable = false) 제약 조건 위반
        Post incompletePost = Post.builder()
                .commonCode(postCategoryNotice)
                .employee(creator)
                .employeeName(creator.getName())
                .title(null) // 필수 필드 누락
                .content("내용")
                .isNotification(false)
                .alert(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when & then
        assertThatThrownBy(() -> postRepository.saveAndFlush(incompletePost))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}