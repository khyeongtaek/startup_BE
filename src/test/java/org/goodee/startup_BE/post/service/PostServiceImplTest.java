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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postServiceImpl;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommonCodeRepository commonCodeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    // 공통 Mock 객체
    private Employee mockEmployee;
    private CommonCode mockCommonCode;
    private Post mockPost;

    // 공통 데이터
    private final Long POST_ID = 1L;
    private final Long EMPLOYEE_ID = 10L;
    private final Long CATEGORY_ID = 100L;
    private final String EMPLOYEE_NAME = "테스트 작성자";
    private final String POST_TITLE = "초기 테스트 제목";
    private final String POST_CONTENT = "초기 테스트 내용";

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화 (EmployeeServiceImplTest 참고)
        mockEmployee = mock(Employee.class);
        mockCommonCode = mock(CommonCode.class);
        mockPost = mock(Post.class);

        // PostResponseDTO.toDTO() 실행 시 NPE 방지를 위한 기본 Mocking
        lenient().when(mockPost.getEmployee()).thenReturn(mockEmployee);

        // 공통 반환 값 설정
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(EMPLOYEE_ID);
        lenient().when(mockPost.getPostId()).thenReturn(POST_ID);
        lenient().when(mockPost.getEmployeeName()).thenReturn(EMPLOYEE_NAME);
        lenient().when(mockPost.getTitle()).thenReturn(POST_TITLE);
        lenient().when(mockPost.getContent()).thenReturn(POST_CONTENT);
        lenient().when(mockPost.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(mockPost.getUpdatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(mockPost.getIsNotification()).thenReturn(false);
        lenient().when(mockPost.getAlert()).thenReturn(false);
        lenient().when(mockPost.getIsDeleted()).thenReturn(false); // 삭제되지 않은 상태 기본 설정
    }

    // --- 게시글 생성 (createPost) 테스트 그룹 ---
    @Nested
    @DisplayName("게시글 생성 (createPost)")
    class CreatePost {
        private PostRequestDTO requestDto;

        @BeforeEach
        void createSetup() {
            requestDto = new PostRequestDTO(
                    null, EMPLOYEE_ID, EMPLOYEE_NAME, POST_TITLE, POST_CONTENT,
                    false, false, CATEGORY_ID, null
            );
        }

        @Test
        @DisplayName("성공")
        void createPost_Success() {
            // given
            // CommonCodeRepository에서 CommonCode Mock 객체를 반환하도록 설정
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.of(mockCommonCode));
            // EmployeeRepository에서 Employee Mock 객체를 반환하도록 설정
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(mockEmployee));

            // PostRepository.save(Post)가 실제 Post 객체를 저장하고 반환하는 상황을 Mocking
            // Post.builder()... 에 대한 Mocking이 복잡하므로, save를 Mocking함
            // 여기서는 toDTO를 위해 필요한 정보만 담은 Post 객체를 반환하도록 설정
            Post savedPost = Post.builder()
                    .postId(POST_ID)
                    .employee(mockEmployee)
                    .employeeName(EMPLOYEE_NAME)
                    .title(POST_TITLE)
                    .content(POST_CONTENT)
                    .isNotification(false)
                    .alert(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostResponseDTO result = postServiceImpl.createPost(requestDto);

            // then
            verify(postRepository, times(1)).save(any(Post.class));
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(POST_TITLE);
            assertThat(result.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시판 ID")
        void createPost_Fail_CategoryNotFound() {
            // given
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postServiceImpl.createPost(requestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 게시판 ID입니다.");

            verify(employeeRepository, never()).findById(anyLong()); // 다음 로직 호출 안됨 검증
            verify(postRepository, never()).save(any(Post.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 작성자 ID")
        void createPost_Fail_EmployeeNotFound() {
            // given
            given(commonCodeRepository.findById(CATEGORY_ID)).willReturn(Optional.of(mockCommonCode));
            given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postServiceImpl.createPost(requestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 작성자입니다.");

            verify(postRepository, never()).save(any(Post.class));
        }
    }

    // --- 게시글 수정 (updatePost) 테스트 그룹 ---
    @Nested
    @DisplayName("게시글 수정 (updatePost)")
    class UpdatePost {
        private PostRequestDTO requestDto;
        private final String UPDATED_TITLE = "수정된 제목";
        private final String UPDATED_CONTENT = "수정된 내용";

        @BeforeEach
        void updateSetup() {
            requestDto = new PostRequestDTO(
                    POST_ID, EMPLOYEE_ID, EMPLOYEE_NAME, UPDATED_TITLE, UPDATED_CONTENT,
                    true, false, CATEGORY_ID, null
            );
        }

        @Test
        @DisplayName("성공")
        void updatePost_Success() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));
            // update 메서드 호출 후 업데이트된 값을 반환하도록 Mocking
            when(mockPost.getTitle()).thenReturn(UPDATED_TITLE);
            when(mockPost.getContent()).thenReturn(UPDATED_CONTENT);
            when(mockPost.getIsNotification()).thenReturn(true);

            // when
            PostResponseDTO result = postServiceImpl.updatePost(requestDto);

            // then
            // Post 엔티티의 update 메서드가 올바른 인자로 호출되었는지 검증
            verify(mockPost, times(1)).update(UPDATED_TITLE, UPDATED_CONTENT, true);
            // PostRepository.save는 @Transactional에 의해 flush/merge되므로, 명시적 save 호출은 검증할 필요 없음
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo(UPDATED_TITLE);
            assertThat(result.getIsNotification()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글 ID")
        void updatePost_Fail_PostNotFound() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postServiceImpl.updatePost(requestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 게시글입니다.");

            verify(mockPost, never()).update(anyString(), anyString(), anyBoolean());
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 게시글")
        void updatePost_Fail_PostIsDeleted() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));
            // 게시글이 삭제된 상태라고 Mocking
            when(mockPost.getIsDeleted()).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> postServiceImpl.updatePost(requestDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제된 게시글입니다.");

            verify(mockPost, never()).update(anyString(), anyString(), anyBoolean());
        }
    }

    // --- 게시글 삭제 (deletePost) 테스트 그룹 ---
    @Nested
    @DisplayName("게시글 삭제 (deletePost)")
    class DeletePost {

        @Test
        @DisplayName("성공 (Soft Delete)")
        void deletePost_Success_SoftDelete() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.of(mockPost));

            // when
            postServiceImpl.deletePost(POST_ID);

            // then
            // Post 엔티티의 delete 메서드가 1회 호출되었는지 검증 (Soft Delete)
            verify(mockPost, times(1)).delete();
            verify(postRepository, times(1)).findById(POST_ID);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글 ID")
        void deletePost_Fail_PostNotFound() {
            // given
            given(postRepository.findById(POST_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postServiceImpl.deletePost(POST_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 게시글입니다.");

            verify(mockPost, never()).delete();
        }
    }

    // --- 게시글 검색 (searchPost) 테스트 그룹 ---
    @Nested
    @DisplayName("게시글 검색 (searchPost)")
    class SearchPost {

        private PostRequestDTO searchDto;
        private Pageable pageable;

        @BeforeEach
        void searchSetup() {
            searchDto = new PostRequestDTO(
                    null, null, EMPLOYEE_NAME, POST_TITLE, POST_CONTENT,
                    null, null, CATEGORY_ID, null
            );
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("성공 - 검색 결과 존재")
        void searchPost_Success_ResultsFound() {
            // given
            // searchPost Repository 메서드가 Post Mock 객체 1개를 포함하는 Page를 반환하도록 Mocking
            Page<Post> mockPage = new PageImpl<>(Arrays.asList(mockPost), pageable, 1);
            given(postRepository.searchPost(
                    searchDto.getCommonCodeId(),
                    searchDto.getTitle(),
                    searchDto.getContent(),
                    searchDto.getEmployeeName(),
                    pageable
            )).willReturn(mockPage);

            // when
            List<PostResponseDTO> resultList = postServiceImpl.searchPost(searchDto, pageable);

            // then
            verify(postRepository, times(1)).searchPost(any(), any(), any(), any(), any());
            assertThat(resultList).isNotNull();
            assertThat(resultList).hasSize(1);
            assertThat(resultList.get(0).getTitle()).isEqualTo(POST_TITLE);
            assertThat(resultList.get(0).getEmployeeName()).isEqualTo(EMPLOYEE_NAME);
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void searchPost_Success_NoResults() {
            // given
            // 빈 Page를 반환하도록 Mocking
            Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(postRepository.searchPost(any(), any(), any(), any(), any())).willReturn(emptyPage);

            // when
            List<PostResponseDTO> resultList = postServiceImpl.searchPost(searchDto, pageable);

            // then
            verify(postRepository, times(1)).searchPost(any(), any(), any(), any(), any());
            assertThat(resultList).isNotNull();
            assertThat(resultList).isEmpty();
        }

        @Test
        @DisplayName("성공 - 검색 조건 없음 (전체 조회와 동일)")
        void searchPost_Success_NoSearchCriteria() {
            // given
            PostRequestDTO emptySearchDto = new PostRequestDTO(
                    null, null, null, null, null,
                    null, null, null, null
            );
            // Post Mock 객체 1개를 포함하는 Page 반환 Mocking
            Page<Post> mockPage = new PageImpl<>(Arrays.asList(mockPost), pageable, 1);
            given(postRepository.searchPost(
                    null, null, null, null, pageable
            )).willReturn(mockPage);

            // when
            List<PostResponseDTO> resultList = postServiceImpl.searchPost(emptySearchDto, pageable);

            // then
            verify(postRepository, times(1)).searchPost(null, null, null, null, pageable);
            assertThat(resultList).hasSize(1);
        }
    }
}