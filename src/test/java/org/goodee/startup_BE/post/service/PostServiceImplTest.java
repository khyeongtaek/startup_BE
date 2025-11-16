package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostRequestDTO;
import org.goodee.startup_BE.post.dto.PostResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean; // updatePost를 위한 추가
import static org.mockito.ArgumentMatchers.eq; // eq 추가
import static org.mockito.ArgumentMatchers.isNull; // isNull 추가
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @InjectMocks // 테스트 대상 클래스
    private PostServiceImpl postService;

    @Mock // 의존성 Mock 객체
    private PostRepository postRepository;

    @Mock // 의존성 Mock 객체
    private CommonCodeRepository commonCodeRepository;

    @Mock // 의존성 Mock 객체
    private EmployeeRepository employeeRepository;

    // 테스트에서 공통으로 사용할 Mock 객체 및 Fixture 선언
    private Post mockPost;
    private CommonCode mockCategory;
    private Employee mockEmployee;
    private PostRequestDTO postRequest;
    private Pageable pageable;

    private final Long POST_ID = 1L;
    private final Long CATEGORY_ID = 10L;
    private final Long EMPLOYEE_ID = 100L;
    private final String EMPLOYEE_NAME = "테스트 작성자";

    @BeforeEach
    void setUp() {
        // Post Request DTO Fixture
        postRequest = new PostRequestDTO();
        postRequest.setPostId(POST_ID);
        postRequest.setCommonCodeId(CATEGORY_ID);
        postRequest.setEmployeeId(EMPLOYEE_ID);
        postRequest.setEmployeeName(EMPLOYEE_NAME);
        postRequest.setTitle("테스트 제목");
        postRequest.setContent("테스트 내용");
        postRequest.setIsNotification(false);
        postRequest.setAlert(false);

        // Pageable Fixture
        pageable = PageRequest.of(0, 10);

        // Mock 객체 초기화
        mockPost = mock(Post.class);
        mockCategory = mock(CommonCode.class);
        mockEmployee = mock(Employee.class);

        // PostResponseDTO.toDTO() 실행 시 NPE 방지를 위한 기본 Mocking
        lenient().when(mockPost.getPostId()).thenReturn(POST_ID);
        lenient().when(mockPost.getTitle()).thenReturn("테스트 제목");
        lenient().when(mockPost.getContent()).thenReturn("테스트 내용");
        lenient().when(mockPost.getEmployeeName()).thenReturn(EMPLOYEE_NAME);
        lenient().when(mockPost.getIsDeleted()).thenReturn(false);
        lenient().when(mockPost.getCommonCode()).thenReturn(mockCategory);
        lenient().when(mockPost.getEmployee()).thenReturn(mockEmployee);
        lenient().when(mockCategory.getCommonCodeId()).thenReturn(CATEGORY_ID);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    // --- C (Create) : 게시글 생성 테스트 ---
    @Nested
    @DisplayName("게시글 생성 (createPost)")
    class CreatePost {

        @Test
        @DisplayName("성공")
        void createPost_Success() {
            // given
            // CommonCode, Employee 존재 Mocking
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.of(mockCategory));
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(mockEmployee));

            // Repository.save() Mocking (저장 후 객체 반환)
            given(postRepository.save(any(Post.class))).willReturn(mockPost);

            // when
            PostResponseDTO result = postService.createPost(postRequest);

            // then
            // 1. save() 메서드가 호출되었는지 검증
            verify(postRepository).save(any(Post.class));
            // 2. 반환된 DTO의 ID가 예상과 일치하는지 검증
            assertThat(result.getPostId()).isEqualTo(POST_ID);
            // 3. 반환된 DTO의 제목이 요청 값과 일치하는지 검증 (PostResponseDTO.toDTO 내부 로직)
            assertThat(result.getTitle()).isEqualTo("테스트 제목");
        }

        @Test
        @DisplayName("실패 - CommonCode (게시판 ID) 없음")
        void createPost_Fail_CategoryNotFound() {
            // given
            // CommonCode 조회 실패 Mocking
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());
            // Employee 조회는 필요 없음 (예외가 먼저 발생)

            // when & then
            assertThatThrownBy(() -> postService.createPost(postRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 게시판 ID입니다.");

            // postRepository.save()가 호출되지 않았는지 검증
            verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("실패 - Employee (작성자) 없음")
        void createPost_Fail_EmployeeNotFound() {
            // given
            // CommonCode 조회 성공 Mocking
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.of(mockCategory));
            // Employee 조회 실패 Mocking
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.createPost(postRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 작성자입니다.");

            // postRepository.save()가 호출되지 않았는지 검증
            verify(postRepository, never()).save(any(Post.class));
        }
    }

    // --- R (Read) : 게시글 검색 테스트 ---
    @Nested
    @DisplayName("게시글 검색 (searchPost)")
    class SearchPost {

        @Test
        @DisplayName("성공 - 검색 조건으로 리스트 반환")
        void searchPost_Success_WithResults() {
            // given
            PostRequestDTO searchDto = new PostRequestDTO();
            searchDto.setTitle("테스트");
            searchDto.setCommonCodeId(CATEGORY_ID);

            // Repository가 Post Mock 객체 1개를 포함하는 Page 반환하도록 Mocking
            Page<Post> postPage = new PageImpl<>(List.of(mockPost), pageable, 1);

            // ★ 수정: given()의 인자를 실제 서비스 호출 인자와 정확히 일치하도록 수정
            given(postRepository.searchPost(
                    eq(CATEGORY_ID), // 10L (DTO에서 설정됨)
                    eq("테스트"),   // "테스트" (DTO에서 설정됨)
                    isNull(),       // content (DTO에 설정 안 됨 -> null)
                    isNull(),       // employeeName (DTO에 설정 안 됨 -> null)
                    eq(pageable)    // Pageable 객체
            )).willReturn(postPage);

            // when
            List<PostResponseDTO> resultList = postService.searchPost(searchDto, pageable);

            // then
            // 1. searchPost() 메서드가 호출되었는지 검증
            verify(postRepository).searchPost(
                    eq(CATEGORY_ID), eq("테스트"), isNull(), isNull(), eq(pageable));
            // 2. 결과 리스트 크기 및 내용 검증
            assertThat(resultList).hasSize(1);
            assertThat(resultList.get(0).getPostId()).isEqualTo(POST_ID);
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void searchPost_Success_NoResults() {
            // given
            PostRequestDTO searchDto = new PostRequestDTO(); // 빈 DTO
            Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            // ★ 수정: given()의 인자를 빈 DTO가 전달될 때의 호출 인자(모두 null)와 일치하도록 수정
            given(postRepository.searchPost(
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
            )).willReturn(emptyPage);

            // when
            List<PostResponseDTO> resultList = postService.searchPost(searchDto, pageable);

            // then
            // searchPost() 메서드가 호출되었는지 검증
            verify(postRepository).searchPost(
                    isNull(), isNull(), isNull(), isNull(), eq(pageable));

            assertThat(resultList).isNotNull();
            assertThat(resultList).isEmpty();
        }
    }

    // --- U (Update) : 게시글 수정 테스트 ---
    @Nested
    @DisplayName("게시글 수정 (updatePost)")
    class UpdatePost {

        private PostRequestDTO updateRequest;
        private final String NEW_TITLE = "새 제목";
        private final String NEW_CONTENT = "새 내용";
        private final boolean NEW_NOTIFICATION = true;

        @BeforeEach
        void updateSetup() {
            updateRequest = new PostRequestDTO();
            updateRequest.setPostId(POST_ID);
            updateRequest.setTitle(NEW_TITLE);
            updateRequest.setContent(NEW_CONTENT);
            updateRequest.setIsNotification(NEW_NOTIFICATION);

            // 업데이트 후 Post 객체의 getter가 새 값을 반환하도록 설정
            lenient().when(mockPost.getTitle()).thenReturn(NEW_TITLE);
            lenient().when(mockPost.getContent()).thenReturn(NEW_CONTENT);
            lenient().when(mockPost.getIsNotification()).thenReturn(NEW_NOTIFICATION);
        }

        @Test
        @DisplayName("성공")
        void updatePost_Success() {
            // given
            // 게시글 존재 Mocking
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));
            // isDeleted가 false인 상태 Mocking (삭제되지 않음)
            given(mockPost.getIsDeleted()).willReturn(false);

            // when
            PostResponseDTO result = postService.updatePost(updateRequest);

            // then
            // 1. Post.update() 메서드가 올바른 인자로 호출되었는지 검증
            verify(mockPost).update(NEW_TITLE, NEW_CONTENT, NEW_NOTIFICATION);
            // 2. 반환된 DTO의 값이 업데이트 요청 값과 일치하는지 검증
            assertThat(result.getTitle()).isEqualTo(NEW_TITLE);
            assertThat(result.getContent()).isEqualTo(NEW_CONTENT);
            assertThat(result.getIsNotification()).isEqualTo(NEW_NOTIFICATION);
        }

        @Test
        @DisplayName("실패 - 게시글 ID 없음")
        void updatePost_Fail_PostNotFound() {
            // given
            // 게시글 조회 실패 Mocking
            given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.updatePost(updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 게시글입니다.");

            // Post.update()가 호출되지 않았는지 검증
            verify(mockPost, never()).update(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 게시글")
        void updatePost_Fail_AlreadyDeleted() {
            // given
            // 게시글 존재 Mocking
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));
            // isDeleted가 true인 상태 Mocking (삭제됨)
            given(mockPost.getIsDeleted()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> postService.updatePost(updateRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("삭제된 게시글입니다.");

            // Post.update()가 호출되지 않았는지 검증
            verify(mockPost, never()).update(anyString(), anyString(), anyBoolean());
        }
    }

    // --- D (Delete) : 게시글 삭제 테스트 ---
    @Nested
    @DisplayName("게시글 삭제 (deletePost)")
    class DeletePost {

        @Test
        @DisplayName("성공")
        void deletePost_Success() {
            // given
            // 게시글 존재 Mocking
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));

            // when
            postService.deletePost(POST_ID);

            // then
            // 1. PostRepository.findById()가 호출되었는지 검증
            verify(postRepository).findById(POST_ID);
            // 2. Post.delete() 메서드가 호출되어 isDeleted 필드가 업데이트되었는지 검증
            verify(mockPost).delete();
            // 참고: delete()는 엔티티 내부 필드만 업데이트하고 save()를 명시적으로 호출하지 않으므로,
            // 트랜잭션 종료 시 dirty checking에 의해 DB에 반영됨
        }

        @Test
        @DisplayName("실패 - 게시글 ID 없음")
        void deletePost_Fail_PostNotFound() {
            // given
            // 게시글 조회 실패 Mocking
            given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deletePost(POST_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 게시글입니다.");

            // Post.delete()가 호출되지 않았는지 검증
            verify(mockPost, never()).delete();
        }
    }
}