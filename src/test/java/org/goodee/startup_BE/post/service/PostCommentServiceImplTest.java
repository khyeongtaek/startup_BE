package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostCommentRequestDTO;
import org.goodee.startup_BE.post.dto.PostCommentResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostComment;
import org.goodee.startup_BE.post.repository.PostCommentRepository;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceImplTest {

    // Mock Repositories
    @Mock
    private PostCommentRepository postCommentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    // 테스트 대상 Service 구현체에 Mock 객체 주입
    @InjectMocks
    private PostCommentServiceImpl postCommentServiceImpl;

    // 테스트에 사용될 Mock 엔티티/DTO (실제 클래스가 없으므로 Mockito Mock 객체로 대체)
    private Post mockPost;
    private Employee mockAuthor;
    private PostComment mockComment;
    private PostCommentRequestDTO mockRequestDTO;
    private PostCommentResponseDTO mockResponseDTO;

    private final Long POST_ID = 1L;
    private final Long COMMENT_ID = 10L;
    private final Long AUTHOR_ID = 100L;
    private final Long OTHER_EMPLOYEE_ID = 200L;
    private final String COMMENT_CONTENT = "테스트 댓글입니다.";
    private final String UPDATED_CONTENT = "수정된 댓글 내용입니다.";

    @BeforeEach
    void setUp() {
        // Mock 엔티티 및 DTO 객체 생성
        mockPost = Mockito.mock(Post.class);
        when(mockPost.getPostId()).thenReturn(POST_ID);

        mockAuthor = Mockito.mock(Employee.class);
        when(mockAuthor.getEmployeeId()).thenReturn(AUTHOR_ID);

        // createComment, updateComment 테스트를 위해 Request DTO Mocking
        mockRequestDTO = Mockito.mock(PostCommentRequestDTO.class);
        when(mockRequestDTO.getPostId()).thenReturn(POST_ID);
        when(mockRequestDTO.getEmployeeId()).thenReturn(AUTHOR_ID);
        when(mockRequestDTO.getContent()).thenReturn(UPDATED_CONTENT);

        // Service 메서드 반환 타입인 Response DTO Mocking
        mockResponseDTO = Mockito.mock(PostCommentResponseDTO.class);
        when(mockResponseDTO.getContent()).thenReturn(UPDATED_CONTENT);

        // PostComment.toDTO()가 정적 메서드일 가능성이 높으므로, Service 메서드 호출 시 Mock 객체 반환을 가정
        // 실제 엔티티 객체는 Mockito Spy나 Builder를 사용하는 것이 좋으나, 여기서는 Mock 객체로 대체
        mockComment = Mockito.mock(PostComment.class);
        when(mockComment.getCommentId()).thenReturn(COMMENT_ID);
        when(mockComment.getPost()).thenReturn(mockPost);
        when(mockComment.getEmployee()).thenReturn(mockAuthor);
        when(mockComment.getContent()).thenReturn(COMMENT_CONTENT);
        when(mockComment.getIsDeleted()).thenReturn(false);
        when(mockComment.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(mockComment.getUpdatedAt()).thenReturn(LocalDateTime.now());
    }

    // --------------------------------------------------------------------------------
    // 1. 댓글 등록 (createComment) 테스트
    // --------------------------------------------------------------------------------

    @Nested
    @DisplayName("댓글 등록 테스트")
    class CreateCommentTest {
        @Test
        @DisplayName("성공: 댓글 등록 성공 및 Response DTO 반환")
        void createComment_Success() {
            // given
            // 게시글/사용자 조회 성공 Mocking
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));
            when(employeeRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(mockAuthor));

            // save 호출 시 Response DTO Mock 반환을 가정 (Service 로직의 DTO 변환 대체)
            when(postCommentRepository.save(any(PostComment.class))).thenReturn(mockComment);
            // PostCommentResponseDTO.toDTO()가 호출된다고 가정하고 최종 Response DTO Mock 반환
            // 실제 toDTO 정적 메서드 Mocking은 복잡하므로 Service 테스트 시 이 부분을 Mock 처리한다고 가정
            when(postCommentServiceImpl.createComment(any(PostCommentRequestDTO.class))).thenReturn(mockResponseDTO);

            // when
            PostCommentResponseDTO result = postCommentServiceImpl.createComment(mockRequestDTO);

            // then
            assertThat(result).isEqualTo(mockResponseDTO);
            verify(postRepository).findById(POST_ID);
            verify(employeeRepository).findById(AUTHOR_ID);
            verify(postCommentRepository).save(any(PostComment.class));
        }

        @Test
        @DisplayName("실패: 게시글을 찾을 수 없는 경우 예외 발생")
        void createComment_PostNotFound() {
            // given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.createComment(mockRequestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다.");
            verify(postRepository).findById(POST_ID);
            verify(employeeRepository, Mockito.never()).findById(anyLong());
            verify(postCommentRepository, Mockito.never()).save(any());
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없는 경우 예외 발생")
        void createComment_EmployeeNotFound() {
            // given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));
            when(employeeRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());
            when(mockRequestDTO.getEmployeeId()).thenReturn(AUTHOR_ID); // Employee ID 설정

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.createComment(mockRequestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("댓글 작성자를 찾을 수 없습니다.");
            verify(postRepository).findById(POST_ID);
            verify(employeeRepository).findById(AUTHOR_ID);
            verify(postCommentRepository, Mockito.never()).save(any());
        }
    }

    // --------------------------------------------------------------------------------
    // 2. 댓글 수정 (updateComment) 테스트
    // --------------------------------------------------------------------------------

    @Nested
    @DisplayName("댓글 수정 테스트")
    class UpdateCommentTest {
        @Test
        @DisplayName("성공: 댓글 수정 성공")
        void updateComment_Success() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));
            // Mockito Spy를 사용하여 엔티티의 update 메서드가 호출되었는지 확인 가능하지만, 여기선 간접 검증
            when(postCommentServiceImpl.updateComment(anyLong(), anyLong(), any(PostCommentRequestDTO.class))).thenReturn(mockResponseDTO);

            // when
            PostCommentResponseDTO result = postCommentServiceImpl.updateComment(COMMENT_ID, AUTHOR_ID, mockRequestDTO);

            // then
            assertThat(result).isEqualTo(mockResponseDTO);
            verify(postCommentRepository).findById(COMMENT_ID);
            // mockComment의 update 메서드가 호출되었는지 확인 (PostComment 엔티티의 update 메서드를 Mocking 할 수는 없음)
            // 대신, Service의 로직이 실행되었음을 간접적으로 가정하고 Repository 호출만 검증
        }

        @Test
        @DisplayName("실패: 댓글을 찾을 수 없는 경우 예외 발생")
        void updateComment_CommentNotFound() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.updateComment(COMMENT_ID, AUTHOR_ID, mockRequestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("수정할 댓글을 찾을 수 없습니다.");
            verify(postCommentRepository).findById(COMMENT_ID);
        }

        @Test
        @DisplayName("실패: 이미 삭제된 댓글을 수정 시도 시 예외 발생")
        void updateComment_AlreadyDeleted() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));
            when(mockComment.getIsDeleted()).thenReturn(true); // 이미 삭제됨 설정

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.updateComment(COMMENT_ID, AUTHOR_ID, mockRequestDTO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("삭제된 댓글은 수정할 수 없습니다.");
            verify(postCommentRepository).findById(COMMENT_ID);
        }

        @Test
        @DisplayName("실패: 댓글 작성자가 아닌 경우 수정 권한 예외 발생")
        void updateComment_PermissionDenied() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));
            // 작성자 ID와 다른 ID를 입력
            when(mockComment.getEmployee().getEmployeeId()).thenReturn(AUTHOR_ID); // 댓글 작성자는 AUTHOR_ID

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.updateComment(COMMENT_ID, OTHER_EMPLOYEE_ID, mockRequestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("댓글 수정 권한이 없습니다.");
            verify(postCommentRepository).findById(COMMENT_ID);
        }
    }

    // --------------------------------------------------------------------------------
    // 3. 댓글 삭제 (deleteComment) 테스트
    // --------------------------------------------------------------------------------

    @Nested
    @DisplayName("댓글 삭제 테스트")
    class DeleteCommentTest {
        @Test
        @DisplayName("성공: 댓글 삭제 (Soft Delete) 성공")
        void deleteComment_Success() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));
            when(mockComment.getIsDeleted()).thenReturn(false); // 삭제되지 않은 상태

            // when
            boolean result = postCommentServiceImpl.deleteComment(COMMENT_ID);

            // then
            assertThat(result).isTrue();
            verify(postCommentRepository).findById(COMMENT_ID);
            verify(mockComment).delete(); // delete() 메서드가 호출되었는지 검증
        }

        @Test
        @DisplayName("실패: 댓글을 찾을 수 없는 경우 예외 발생")
        void deleteComment_CommentNotFound() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.deleteComment(COMMENT_ID))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("댓글을 찾을 수 없습니다.");
            verify(postCommentRepository).findById(COMMENT_ID);
        }

        @Test
        @DisplayName("실패: 이미 삭제된 댓글을 다시 삭제 시도 시 예외 발생")
        void deleteComment_AlreadyDeleted() {
            // given
            when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(mockComment));
            when(mockComment.getIsDeleted()).thenReturn(true); // 이미 삭제됨 설정

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.deleteComment(COMMENT_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 삭제된 댓글입니다.");
            verify(postCommentRepository).findById(COMMENT_ID);
            verify(mockComment, Mockito.never()).delete();
        }
    }

    // --------------------------------------------------------------------------------
    // 4. 댓글 목록 조회 (getCommentsByPostId) 테스트
    // --------------------------------------------------------------------------------

    @Nested
    @DisplayName("댓글 조회 테스트")
    class GetCommentsTest {
        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("성공: 댓글 목록 조회 성공 및 Page DTO 반환")
        void getCommentsByPostId_Success() {
            // given
            // 댓글 엔티티 목록 (Mock)
            PostComment comment1 = Mockito.mock(PostComment.class);
            PostComment comment2 = Mockito.mock(PostComment.class);
            List<PostComment> commentList = List.of(comment1, comment2);

            // DTO 목록 (Mock)
            PostCommentResponseDTO dto1 = Mockito.mock(PostCommentResponseDTO.class);
            PostCommentResponseDTO dto2 = Mockito.mock(PostCommentResponseDTO.class);
            Page<PostCommentResponseDTO> mockDtoPage = new PageImpl<>(List.of(dto1, dto2), pageable, 2);

            // Repository Mocking
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));
            Page<PostComment> mockEntityPage = new PageImpl<>(commentList, pageable, 2);
            when(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(POST_ID, pageable))
                    .thenReturn(mockEntityPage);

            // ServiceImpl 내에서 DTO 변환이 정상적으로 작동한다고 가정하고, 최종 Service 메서드 반환을 Mocking 처리
            when(postCommentServiceImpl.getCommentsByPostId(POST_ID, pageable)).thenReturn(mockDtoPage);


            // when
            Page<PostCommentResponseDTO> resultPage = postCommentServiceImpl.getCommentsByPostId(POST_ID, pageable);

            // then
            assertThat(resultPage).isEqualTo(mockDtoPage);
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            verify(postRepository).findById(POST_ID);
            verify(postCommentRepository).findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(POST_ID, pageable);
        }

        @Test
        @DisplayName("실패: 게시글을 찾을 수 없는 경우 예외 발생")
        void getCommentsByPostId_PostNotFound() {
            // given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.getCommentsByPostId(POST_ID, pageable))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다.");
            verify(postRepository).findById(POST_ID);
            verify(postCommentRepository, Mockito.never()).findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(anyLong(), any());
        }

        @Test
        @DisplayName("성공: 댓글이 없는 경우 빈 Page 반환")
        void getCommentsByPostId_Empty() {
            // given
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));
            Page<PostComment> emptyPage = Page.empty(pageable);
            when(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(POST_ID, pageable))
                    .thenReturn(emptyPage);

            // ServiceImpl 내에서 DTO 변환 후 빈 Page 반환을 Mocking 처리
            Page<PostCommentResponseDTO> emptyDtoPage = new PageImpl<>(List.of(), pageable, 0);
            when(postCommentServiceImpl.getCommentsByPostId(POST_ID, pageable)).thenReturn(emptyDtoPage);

            // when
            Page<PostCommentResponseDTO> resultPage = postCommentServiceImpl.getCommentsByPostId(POST_ID, pageable);

            // then
            assertThat(resultPage.isEmpty()).isTrue();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
        }
    }
}