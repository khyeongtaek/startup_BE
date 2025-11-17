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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceImplTest {

    @InjectMocks
    private PostCommentServiceImpl postCommentService;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    // 테스트에서 공통으로 사용할 Mock 객체
    private Post mockPost;
    private Employee mockEmployee;
    private PostComment mockComment;

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화
        mockPost = mock(Post.class);
        mockEmployee = mock(Employee.class);
        mockComment = mock(PostComment.class);

        // PostCommentResponseDTO.toDTO() 메서드 실행 시
        // NPE(NullPointerException) 방지를 위한 기본 Mocking
        // lenient() : 해당 Mocking이 모든 테스트에서 사용되지 않더라도 경고/오류를 발생시키지 않음
        lenient().when(mockComment.getPost()).thenReturn(mockPost);
        lenient().when(mockComment.getEmployee()).thenReturn(mockEmployee);

        // toDTO 내부의 .getPostId(), .getEmployeeId(), .getName() 호출 Mocking
        lenient().when(mockPost.getPostId()).thenReturn(1L);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(1L);
        lenient().when(mockEmployee.getName()).thenReturn("테스트작성자");

        // toDTO 및 기타 로직에서 사용될 기본값 Mocking
        lenient().when(mockComment.getCommentId()).thenReturn(1L);
        lenient().when(mockComment.getContent()).thenReturn("테스트 댓글 내용");
        lenient().when(mockComment.getIsDeleted()).thenReturn(false);
        lenient().when(mockComment.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(mockComment.getUpdatedAt()).thenReturn(LocalDateTime.now());
    }

    @Nested
    @DisplayName("댓글 등록 (createComment)")
    class CreateComment {

        @Test
        @DisplayName("성공")
        void createComment_Success() {
            // given
            PostCommentRequestDTO requestDTO = new PostCommentRequestDTO();
            requestDTO.setPostId(1L);
            requestDTO.setEmployeeId(1L);
            requestDTO.setContent("새 댓글 내용");

            // PostComment.createPostComment는 static 메서드라 Mocking하지 않음.
            // Repositories Mocking
            given(postRepository.findById(1L)).willReturn(Optional.of(mockPost));
            given(employeeRepository.findById(1L)).willReturn(Optional.of(mockEmployee));
            // save(any())가 mockComment를 반환하도록 설정
            given(postCommentRepository.save(any(PostComment.class))).willReturn(mockComment);

            // when
            PostCommentResponseDTO response = postCommentService.createComment(requestDTO);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCommentId()).isEqualTo(1L);
            assertThat(response.getEmployeename()).isEqualTo("테스트작성자");

            // verify
            verify(postRepository).findById(1L);
            verify(employeeRepository).findById(1L);
            verify(postCommentRepository).save(any(PostComment.class));
        }

        @Test
        @DisplayName("실패 - 게시글 없음")
        void createComment_Fail_PostNotFound() {
            // given
            PostCommentRequestDTO requestDTO = new PostCommentRequestDTO();
            requestDTO.setPostId(99L); // 존재하지 않는 게시글 ID

            // 게시글 조회 실패 Mocking
            given(postRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentService.createComment(requestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("게시글을 찾을 수 없습니다.");

            // 이후 로직(employeeRepository, postCommentRepository)이 호출되지 않았는지 검증
            verify(employeeRepository, never()).findById(anyLong());
            verify(postCommentRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 작성자(직원) 없음")
        void createComment_Fail_EmployeeNotFound() {
            // given
            PostCommentRequestDTO requestDTO = new PostCommentRequestDTO();
            requestDTO.setPostId(1L);
            requestDTO.setEmployeeId(99L); // 존재하지 않는 직원 ID

            // 게시글 조회 성공 Mocking
            given(postRepository.findById(1L)).willReturn(Optional.of(mockPost));
            // 직원 조회 실패 Mocking
            given(employeeRepository.findById(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentService.createComment(requestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("작성자를 찾을 수 없습니다.");

            // save 로직이 호출되지 않았는지 검증
            verify(postCommentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("댓글 수정 (updateComment)")
    class UpdateComment {

        private PostCommentRequestDTO requestDTO;
        private final Long commentId = 1L;
        private final Long validEmployeeId = 1L;

        @BeforeEach
        void updateSetup() {
            requestDTO = new PostCommentRequestDTO();
            requestDTO.setContent("수정된 댓글 내용");
        }

        @Test
        @DisplayName("성공")
        void updateComment_Success() {
            // given
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));
            // 삭제되지 않은 댓글
            given(mockComment.getIsDeleted()).willReturn(false);
            // 권한 확인 성공 (setUp에서 employeeId가 1L로 설정됨)

            // when
            PostCommentResponseDTO response = postCommentService.updateComment(commentId, validEmployeeId, requestDTO);

            // then
            // 엔티티의 update 메서드가 "수정된 댓글 내용"으로 호출되었는지 검증
            verify(mockComment).update("수정된 댓글 내용");
            // DTO 변환이 정상적으로 되었는지 검증 (toDTO는 setUp에서 Mocking됨)
            assertThat(response).isNotNull();
            assertThat(response.getCommentId()).isEqualTo(commentId);
        }

        @Test
        @DisplayName("실패 - 수정할 댓글 없음")
        void updateComment_Fail_CommentNotFound() {
            // given
            // 댓글 조회 실패
            given(postCommentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentService.updateComment(commentId, validEmployeeId, requestDTO))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("수정할 댓글을 찾을 수 없습니다.");

            // 엔티티 update 메서드가 호출되지 않았는지 검증
            verify(mockComment, never()).update(anyString());
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 댓글")
        void updateComment_Fail_AlreadyDeleted() {
            // given
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));
            // 이미 삭제된 상태(true) 반환
            given(mockComment.getIsDeleted()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> postCommentService.updateComment(commentId, validEmployeeId, requestDTO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("삭제된 댓글은 수정할 수 없습니다.");

            verify(mockComment, never()).update(anyString());
        }

        @Test
        @DisplayName("실패 - 수정 권한 없음")
        void updateComment_Fail_Unauthorized() {
            // given
            Long invalidEmployeeId = 2L; // 요청자 ID (권한 없음)
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));
            // 삭제되지 않음
            given(mockComment.getIsDeleted()).willReturn(false);
            // 댓글 작성자 ID는 1L (setUp에서 설정됨), 요청자 ID는 2L
            // mockComment.getEmployee().getEmployeeId()는 setUp에서 1L로 설정됨

            // when & then
            assertThatThrownBy(() -> postCommentService.updateComment(commentId, invalidEmployeeId, requestDTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("댓글 수정 권한이 없습니다.");

            verify(mockComment, never()).update(anyString());
        }
    }

    @Nested
    @DisplayName("댓글 삭제 (deleteComment)")
    class DeleteComment {

        private final Long commentId = 1L;

        @Test
        @DisplayName("성공")
        void deleteComment_Success() {
            // given
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));
            // 삭제되지 않음
            given(mockComment.getIsDeleted()).willReturn(false);

            // when
            boolean result = postCommentService.deleteComment(commentId);

            // then
            assertThat(result).isTrue();
            // 엔티티의 delete 메서드가 호출되었는지 검증
            verify(mockComment).delete();
        }

        @Test
        @DisplayName("실패 - 삭제할 댓글 없음")
        void deleteComment_Fail_CommentNotFound() {
            // given
            // 댓글 조회 실패
            given(postCommentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentService.deleteComment(commentId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("댓글을 찾을 수 없습니다.");

            verify(mockComment, never()).delete();
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 댓글")
        void deleteComment_Fail_AlreadyDeleted() {
            // given
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));
            // 이미 삭제된 상태(true) 반환
            given(mockComment.getIsDeleted()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> postCommentService.deleteComment(commentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 삭제된 댓글입니다.");

            verify(mockComment, never()).delete();
        }
    }

    @Nested
    @DisplayName("특정 게시글의 댓글 조회 (getCommentsByPostId)")
    class GetCommentsByPostId {

        private final Long postId = 1L;
        private Pageable pageable;

        @BeforeEach
        void searchSetup() {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("성공 - 댓글 목록 있음")
        void getCommentsByPostId_Success_CommentsFound() {
            // given
            // 1. 게시글 존재 유무 확인
            given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

            // 2. 댓글 목록 조회
            List<PostComment> commentList = List.of(mockComment);
            Page<PostComment> commentPage = new PageImpl<>(commentList, pageable, 1);
            given(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable))
                    .willReturn(commentPage);

            // when
            Page<PostCommentResponseDTO> resultPage = postCommentService.getCommentsByPostId(postId, pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent()).hasSize(1);
            assertThat(resultPage.getContent().get(0).getCommentId()).isEqualTo(1L);
            assertThat(resultPage.getContent().get(0).getEmployeename()).isEqualTo("테스트작성자");
        }

        @Test
        @DisplayName("성공 - 댓글 목록 없음 (빈 페이지)")
        void getCommentsByPostId_Success_NoCommentsFound() {
            // given
            // 1. 게시글 존재 유무 확인
            given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));

            // 2. 빈 댓글 목록 조회
            Page<PostComment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            given(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<PostCommentResponseDTO> resultPage = postCommentService.getCommentsByPostId(postId, pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isZero();
            assertThat(resultPage.getContent()).isEmpty();
        }

        @Test
        @DisplayName("실패 - 게시글 없음")
        void getCommentsByPostId_Fail_PostNotFound() {
            // given
            // 1. 게시글 존재 유무 확인 (실패)
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentService.getCommentsByPostId(postId, pageable))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("게시글을 찾을 수 없습니다.");

            // 댓글 조회 로직이 호출되지 않았는지 검증
            verify(postCommentRepository, never()).findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(anyLong(), any(Pageable.class));
        }
    }
}