package org.goodee.startup_BE.employee.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.dto.AuthenticationResponseDTO;
import org.goodee.startup_BE.employee.dto.EmployeeRequestDTO;
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
    public AuthenticationResponseDTO signup(Authentication authentication, EmployeeRequestDTO request) {
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

        // JWT 토큰 생성 (AccessToken, RefreshToken) RefreshToken은 추후 추가 예정
        String accessToken = jwtService.generateToken(null, employee);

        // 인증 응답 DTO 반환
        return AuthenticationResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .username(employee.getUsername())
                .name(employee.getName())
                .build();
    }

    // 로그인
    @Override
    public AuthenticationResponseDTO login(EmployeeRequestDTO request, String ipAddress, String userAgent) {
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

        // JWT 토큰 생성 (AccessToken, RefreshToken) RefreshToken은 추후 추가 예정
        String accessToken = jwtService.generateToken(null, employee);

        // 인증 응답 DTO 반환
        return AuthenticationResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .username(employee.getUsername())
                .name(employee.getName())
                .build();
    }

}
