package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommentServiceImplTest {

    @InjectMocks
    private PostCommentServiceImpl postCommentServiceImpl; // 테스트 대상

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostRepository postRepository; // Post 엔티티 조회를 가정하여 Mocking

    @Mock
    private EmployeeRepository employeeRepository; // Employee 엔티티 조회를 가정하여 Mocking

    // 테스트에서 공통으로 사용할 Mock 객체 선언
    private Post mockPost;
    private Employee mockEmployee;
    private PostComment mockComment;
    private Long postId = 10L;
    private Long employeeId = 1L;
    private Long commentId = 100L;
    private String initialContent = "초기 댓글 내용";
    private String newContent = "수정된 댓글 내용";

    @BeforeEach
    void setUp() {
        // 1. 핵심 의존 엔티티 Mock 객체 초기화
        mockPost = mock(Post.class);
        mockEmployee = mock(Employee.class);
        mockComment = mock(PostComment.class);

        // 2. Mock 객체의 ID 및 기본 필드 설정 (toDTO를 위한 lenient Mocking 포함)
        lenient().when(mockPost.getPostId()).thenReturn(postId);
        lenient().when(mockEmployee.getEmployeeId()).thenReturn(employeeId);
        lenient().when(mockEmployee.getName()).thenReturn("댓글 작성자");

        // toDTO 실행 시 필요한 Mocking (EmployeeServiceImplTest 참고)
        lenient().when(mockComment.getCommentId()).thenReturn(commentId);
        lenient().when(mockComment.getPost()).thenReturn(mockPost);
        lenient().when(mockComment.getEmployee()).thenReturn(mockEmployee);
        lenient().when(mockComment.getContent()).thenReturn(initialContent);
        lenient().when(mockComment.getIsDeleted()).thenReturn(false);
        lenient().when(mockComment.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(mockComment.getUpdatedAt()).thenReturn(LocalDateTime.now());
    }

    // --- C: 댓글 등록 (Create) ---
    @Nested
    @DisplayName("createComment (댓글 등록)")
    class CreateComment {

        private PostCommentRequestDTO requestDTO;

        @BeforeEach
        void createSetup() {
            requestDTO = new PostCommentRequestDTO(null, postId, employeeId, initialContent);
        }

        @Test
        @DisplayName("성공")
        void createComment_Success() {
            // given
            // 1. Post, Employee 조회 성공
            given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.of(mockEmployee));

            // 2. PostCommentRepository.save() 호출 시, DTO 변환을 위해 mockComment 반환 설정
            // 실제 구현체는 PostComment.createPostComment()를 호출하고 save()를 할 것이므로
            // save()의 인자가 PostComment 엔티티임을 확인하고, 반환은 mockComment로 설정
            given(postCommentRepository.save(any(PostComment.class))).willReturn(mockComment);

            // when
            PostCommentResponseDTO result = postCommentServiceImpl.createComment(requestDTO);

            // then
            // 1. save() 호출 검증
            verify(postCommentRepository).save(any(PostComment.class));
            // 2. 결과 DTO 검증
            assertThat(result).isNotNull();
            assertThat(result.getPostId()).isEqualTo(postId);
            assertThat(result.getEmployeeId()).isEqualTo(employeeId);
            assertThat(result.getContent()).isEqualTo(initialContent);
            assertThat(result.getEmployeename()).isEqualTo("댓글 작성자");
        }

        @Test
        @DisplayName("실패 - 게시글 없음")
        void createComment_Fail_PostNotFound() {
            // given
            // Post 조회 실패
            given(postRepository.findById(postId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.createComment(requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("게시글 정보를 찾을 수 없습니다.");

            // save는 호출되지 않아야 함
            verify(postCommentRepository, never()).save(any(PostComment.class));
        }

        @Test
        @DisplayName("실패 - 사원(작성자) 없음")
        void createComment_Fail_EmployeeNotFound() {
            // given
            // Post 조회 성공, Employee 조회 실패
            given(postRepository.findById(postId)).willReturn(Optional.of(mockPost));
            given(employeeRepository.findById(employeeId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.createComment(requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("사원 정보를 찾을 수 없습니다.");

            // save는 호출되지 않아야 함
            verify(postCommentRepository, never()).save(any(PostComment.class));
        }
    }

    // --- U: 댓글 수정 (Update) ---
    @Nested
    @DisplayName("updateComment (댓글 수정)")
    class UpdateComment {

        private PostCommentRequestDTO requestDTO;

        @BeforeEach
        void updateSetup() {
            requestDTO = new PostCommentRequestDTO(commentId, postId, employeeId, newContent);
        }

        @Test
        @DisplayName("성공")
        void updateComment_Success() {
            // given
            // 1. 댓글 조회 성공 및 작성자가 요청자와 동일
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));

            // 2. update() 후 변경된 내용을 반환하도록 Mocking
            // (update 메서드는 void이므로 when/thenReturn이 아닌 verify로 검증)
            when(mockComment.getContent()).thenReturn(newContent);
            when(mockComment.getUpdatedAt()).thenReturn(LocalDateTime.now().plusHours(1));

            // when
            PostCommentResponseDTO result = postCommentServiceImpl.updateComment(commentId, employeeId, requestDTO);

            // then
            // 1. Comment 엔티티의 update() 메서드가 요청된 새 내용으로 호출되었는지 검증
            verify(mockComment).update(newContent);
            // 2. save는 내부적으로 영속성 컨텍스트에 의해 일어나므로 save 호출은 검증하지 않아도 됨.
            // 3. 결과 DTO 검증
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("실패 - 댓글 없음")
        void updateComment_Fail_CommentNotFound() {
            // given
            given(postCommentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postCommentServiceImpl.updateComment(commentId, employeeId, requestDTO))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("댓글 정보를 찾을 수 없습니다.");

            // update() 메서드가 호출되지 않아야 함
            verify(mockComment, never()).update(anyString());
        }

        @Test
        @DisplayName("실패 - 권한 없음 (작성자 ID 불일치)")
        void updateComment_Fail_Unauthorized() {
            // given
            Long unauthorizedEmployeeId = 999L; // 다른 ID
            // 댓글 조회 성공, 하지만 엔티티의 작성자 ID는 employeeId(1L)임
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));

            // when & then
            // 권한 없음 예외 (ResponseStatusException 또는 BadCredentialsException 등)를 가정
            // EmployeeServiceImplTest의 BadCredentialsException 예외를 참고하여 ResponseStatusException (403 Forbidden)을 가정
            assertThatThrownBy(() -> postCommentServiceImpl.updateComment(commentId, unauthorizedEmployeeId, requestDTO))
                    .isInstanceOf(ResponseStatusException.class) // 403 Forbidden을 반환하는 예외를 가정
                    .hasMessageContaining("수정 권한이 없습니다.");

            // update() 메서드가 호출되지 않아야 함
            verify(mockComment, never()).update(anyString());
        }
    }

    // --- D: 댓글 삭제 (Delete) ---
    @Nested
    @DisplayName("deleteComment (댓글 삭제)")
    class DeleteComment {

        @Test
        @DisplayName("성공")
        void deleteComment_Success() {
            // given
            // 댓글 조회 성공
            given(postCommentRepository.findById(commentId)).willReturn(Optional.of(mockComment));

            // when
            boolean result = postCommentServiceImpl.deleteComment(commentId);

            // then
            // 1. Comment 엔티티의 delete() 메서드가 호출되었는지 검증
            verify(mockComment).delete();
            // 2. save는 내부적으로 영속성 컨텍스트에 의해 일어나므로 save 호출은 검증하지 않아도 됨.
            // 3. 결과 검증
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패 - 댓글 없음")
        void deleteComment_Fail_CommentNotFound() {
            // given
            given(postCommentRepository.findById(commentId)).willReturn(Optional.empty());

            // when & then
            // 예외가 아닌 false를 반환하도록 서비스 로직을 가정 (혹은 ResourceNotFoundException 발생을 가정할 수도 있음)
            // 여기서는 `ResourceNotFoundException`을 가정하고 테스트
            assertThatThrownBy(() -> postCommentServiceImpl.deleteComment(commentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("댓글 정보를 찾을 수 없습니다.");

            // delete() 메서드가 호출되지 않아야 함
            verify(mockComment, never()).delete();
        }
    }

    // --- R: 댓글 조회 (Read) ---
    @Nested
    @DisplayName("getCommentsByPostId (특정 게시글의 댓글 조회)")
    class GetCommentsByPostId {

        private Pageable pageable;

        @BeforeEach
        void readSetup() {
            pageable = PageRequest.of(0, 10); // 페이지 0, 사이즈 10
        }

        @Test
        @DisplayName("성공 - 댓글 목록 존재")
        void getCommentsByPostId_Success_Found() {
            // given
            // 1. PostComment.createPostComment()를 Mocking 없이 사용하기 위해 ReflectionTestUtils를 이용하여 ID를 주입
            PostComment comment1 = PostComment.createPostComment(mockPost, mockEmployee, "댓글1");
            ReflectionTestUtils.setField(comment1, "commentId", 101L);
            PostComment comment2 = PostComment.createPostComment(mockPost, mockEmployee, "댓글2");
            ReflectionTestUtils.setField(comment2, "commentId", 102L);

            List<PostComment> commentList = List.of(comment1, comment2);
            Page<PostComment> commentPage = new PageImpl<>(commentList, pageable, commentList.size());

            // 2. Repository Mocking
            given(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable))
                    .willReturn(commentPage);

            // when
            Page<PostCommentResponseDTO> resultPage = postCommentServiceImpl.getCommentsByPostId(postId, pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(2);
            assertThat(resultPage.getContent()).hasSize(2);
            assertThat(resultPage.getContent().get(0).getContent()).isEqualTo("댓글1");
            assertThat(resultPage.getContent().get(1).getContent()).isEqualTo("댓글2");
        }

        @Test
        @DisplayName("성공 - 댓글 목록 없음")
        void getCommentsByPostId_Success_Empty() {
            // given
            Page<PostComment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            // Repository Mocking (빈 페이지 반환)
            given(postCommentRepository.findByPost_PostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<PostCommentResponseDTO> resultPage = postCommentServiceImpl.getCommentsByPostId(postId, pageable);

            // then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getTotalElements()).isEqualTo(0);
            assertThat(resultPage.getContent()).isEmpty();
        }
    }
}