package org.goodee.startup_BE.employee.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.APIResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
import org.goodee.startup_BE.employee.dto.EmployeeResponseDTO;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.entity.LoginHistory;
import org.goodee.startup_BE.employee.enums.LoginStatus;
import org.goodee.startup_BE.employee.exception.DuplicateEmailException;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginHistoryService loginHistoryService;

    // 회원가입
    @Override
    public APIResponseDTO<EmployeeResponseDTO> signup(Authentication authentication, EmployeeRequestDTO request) {
        // 이메일 중복 체크
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail() + "은(는) 이미 존재하는 이메일입니다.");
        }


        // 새로운 사용자 생성을 위한 코드 entity 생성
        CommonCode statusCode = commonCodeRepository
                .findById(request.getStatus())
                .orElseThrow(() -> new ResourceNotFoundException("status code: " + request.getStatus() + " 를 찾을 수 없습니다."));

        CommonCode roleCode = commonCodeRepository
                .findById(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("role code: " + request.getRole() + " 를 찾을 수 없습니다."));

        CommonCode departmentCode = commonCodeRepository
                .findById(request.getDepartment())
                .orElseThrow(() -> new ResourceNotFoundException("department code: " + request.getDepartment() + " 를 찾을 수 없습니다."));

        CommonCode positionCode = commonCodeRepository
                .findById(request.getPosition())
                .orElseThrow(() -> new ResourceNotFoundException("position code: " + request.getPosition() + " 를 찾을 수 없습니다."));

        Employee creater = employeeRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("인증되지 않은 사용자입니다."));


        // 새 사용자 엔티티 생성
        Employee employee = request.toEntity(statusCode, roleCode, departmentCode, positionCode, creater);

        //초기비밀번호를 사용자아이디로 등록
        employee.updateInitPassword(passwordEncoder.encode(employee.getUsername()), creater);

        // 사용자 등록
        employee = employeeRepository.save(employee);

        return APIResponseDTO.<EmployeeResponseDTO>builder()
                .message("회원가입 성공")
                .data(EmployeeResponseDTO.toDTO(employee))
                .build();

    }

    // 로그인
    @Override
    public Map<String, Object> login(EmployeeRequestDTO request, String ipAddress, String userAgent) {
        // 인증 시도 기록 남김 - 기본값(실패)
        LoginHistory loginHistory = loginHistoryService
                .recodeLoginHistory(
                        request.getUsername()
                        , ipAddress
                        , userAgent
                        , commonCodeRepository
                                .findByCodeStartsWithAndKeywordExactMatchInValues("LS", LoginStatus.FAIL.name())
                                .get(0)
                );

        // 인증 정보로 사용자 정보 조회
        Employee employee = employeeRepository.findByUsername(request.getUsername())
                .orElse(null);

        // 사용자가 없거나 || 비밀번호가 일치하지 않으면 실패 처리
        if (employee == null || !passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        //인증 성공 시 사용자id 남김.
        loginHistory
                .updateEmployee(
                        commonCodeRepository
                                .findByCodeStartsWithAndKeywordExactMatchInValues("LS", LoginStatus.SUCCESS.name())
                                .get(0)
                        , employee
                );


        // 만약 초기비밀번호 상태라면 초기비밀번호 알림을 보내줘야함.
        if (employee.getIsInitialPassword()) {
            //추후 추가
        }

        // JWT 토큰 생성 (AccessToken, RefreshToken)
        // Access Token 생성
        String accessToken = jwtService.generateToken(Map.of(), employee);
        // Refresh Token 생성
        String refreshToken = jwtService.generateRefreshToken(employee);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "employee", EmployeeResponseDTO.toDTO(employee)
        );
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {

        final String username;

        // 전달받은 토큰이 비어있는지 확인
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadCredentialsException("Refresh Token이 필요합니다.");
        }

        // 토큰에서 사용자 이름 추출
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("유효하지 않은 Refresh Token입니다.", e);
        }

        // 사용자 이름이 존재하면
        if (username != null) {
            // DB에서 사용자 정보 조회
            Employee employee = this.employeeRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));

            // Refresh Token 유효성 검증
            if (jwtService.isValidToken(refreshToken, employee)) {
                // 새로운 Access Token 생성
                String newAccessToken = jwtService.generateToken(Map.of(), employee);

                String newRefreshToken = jwtService.generateRefreshToken(employee);


                return Map.of(
                        "accessToken", newAccessToken,
                        "refreshToken", newRefreshToken,
                        "employee", EmployeeResponseDTO.toDTO(employee)
                );
            }
        }

        // 유효성 검증 실패 또는 사용자 이름이 없는 경우
        throw new BadCredentialsException("Refresh Token이 만료되었거나 유효하지 않습니다.");
    }
}