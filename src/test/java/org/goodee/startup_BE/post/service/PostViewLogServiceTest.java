package org.goodee.startup_BE.post.service;

import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.goodee.startup_BE.post.dto.PostViewLogRequestDTO;
import org.goodee.startup_BE.post.dto.PostViewLogResponseDTO;
import org.goodee.startup_BE.post.entity.Post;
import org.goodee.startup_BE.post.entity.PostViewLog;
import org.goodee.startup_BE.post.repository.PostRepository;
import org.goodee.startup_BE.post.repository.PostViewLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"ConstantValue", "ClassCanBeRecord"})
@ExtendWith(MockitoExtension.class)
class PostViewLogServiceTest {

    private static final String TEST_PASSWORD = "testpassword123!";
    private static final Long MOCK_POST_ID = 1L;
    private static final Long MOCK_EMPLOYEE_ID = 10L;

    @InjectMocks
    private PostViewLogServiceImpl postViewLogService;

    @Mock
    private PostRepository postrepository;
    @Mock
    private PostViewLogRepository postViewLogRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    // ========================= Helper 객체 생성 =========================

    private record TestEntities(CommonCode statusActive, CommonCode roleUser, CommonCode dept,
                                CommonCode pos, CommonCode postCat, Employee emp, Post post) { }

    private TestEntities createEntities() {
        CommonCode status = CommonCode.createCommonCode("S1", "ACTIVE", "Y", null, null, 1L, null, false);
        CommonCode role = CommonCode.createCommonCode("R1", "USER", "USER", null, null, 1L, null, false);
        CommonCode dept = CommonCode.createCommonCode("D1", "DEV", "DEV", null, null, 1L, null,false);
        CommonCode pos = CommonCode.createCommonCode("P1", "JUNIOR", "JR", null, null, 1L, null,false);
        CommonCode postCat = CommonCode.createCommonCode("PC1", "GENERAL", "GEN", null, null, 1L, null,false);

        Employee emp = Employee.createEmployee("test", "테스터", "test@test.com", "010-1111-2222",
                LocalDate.now(), status, role, dept, pos, null);
        setValue(emp, "employeeId", MOCK_EMPLOYEE_ID);

        Post post = Post.create(postCat, emp, "제목1", "내용1", true, false);
        setValue(post, "postId", MOCK_POST_ID);

        return new TestEntities(status, role, dept, pos, postCat, emp, post);
    }

    private void setValue(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {}
    }

    // ========================= TEST CASE =========================

    @Test
    @DisplayName("S1 - 최초 조회 기록 저장 성공")
    void createPostViewLog_firstView_success() {
        var ent = createEntities();
        var req = new PostViewLogRequestDTO(MOCK_POST_ID);

        when(postrepository.findById(MOCK_POST_ID)).thenReturn(Optional.of(ent.post()));
        when(employeeRepository.findById(MOCK_EMPLOYEE_ID)).thenReturn(Optional.of(ent.emp()));
        when(postViewLogRepository.existsByPost_PostIdAndEmployee_EmployeeId(MOCK_POST_ID, MOCK_EMPLOYEE_ID)).thenReturn(false);
        when(postViewLogRepository.save(any(PostViewLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostViewLogResponseDTO result = postViewLogService.createPostViewLog(req, MOCK_EMPLOYEE_ID);

        assertThat(result).isNotNull();
        assertThat(result.getPostId()).isEqualTo(MOCK_POST_ID);

        verify(postViewLogRepository, times(1)).save(any(PostViewLog.class));
    }

    @Test
    @DisplayName("S2 - 중복 조회 시 저장되지 않으며 null 반환")
    void createPostViewLog_duplicateView_returnNull() {
        var ent = createEntities();
        var req = new PostViewLogRequestDTO(MOCK_POST_ID);

        when(postrepository.findById(MOCK_POST_ID)).thenReturn(Optional.of(ent.post()));
        when(employeeRepository.findById(MOCK_EMPLOYEE_ID)).thenReturn(Optional.of(ent.emp()));
        when(postViewLogRepository.existsByPost_PostIdAndEmployee_EmployeeId(MOCK_POST_ID, MOCK_EMPLOYEE_ID)).thenReturn(true);

        PostViewLogResponseDTO result = postViewLogService.createPostViewLog(req, MOCK_EMPLOYEE_ID);

        assertThat(result).isNull();
        verify(postViewLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("E1 - 게시글 없음 -> 예외 발생")
    void createPostViewLog_noPost_exception() {
        var req = new PostViewLogRequestDTO(MOCK_POST_ID);

        when(postrepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postViewLogService.createPostViewLog(req, MOCK_EMPLOYEE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다.");

        verify(employeeRepository, never()).findById(anyLong());
        verify(postViewLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("E2 - 직원 없음 -> 예외 발생")
    void createPostViewLog_noEmployee_exception() {
        var ent = createEntities();
        var req = new PostViewLogRequestDTO(MOCK_POST_ID);

        when(postrepository.findById(MOCK_POST_ID)).thenReturn(Optional.of(ent.post()));
        when(employeeRepository.findById(MOCK_EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postViewLogService.createPostViewLog(req, MOCK_EMPLOYEE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("직원을 찾을 수 없습니다.");

        verify(postViewLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("S3 - 조회수 반환 성공")
    void getViewCount_success() {
        when(postViewLogRepository.countByPost_PostId(MOCK_POST_ID)).thenReturn(7L);

        long result = postViewLogService.getViewCount(MOCK_POST_ID);

        assertThat(result).isEqualTo(7L);
        verify(postViewLogRepository, times(1)).countByPost_PostId(MOCK_POST_ID);
    }
}
