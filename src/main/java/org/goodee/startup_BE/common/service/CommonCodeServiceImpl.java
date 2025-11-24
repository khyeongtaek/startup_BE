package org.goodee.startup_BE.common.service;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.approval.enums.ApprovalTemplate;
import org.goodee.startup_BE.common.dto.CommonCodeRequestDTO;
import org.goodee.startup_BE.approval.enums.VacationType;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.entity.CommonCode;
import org.goodee.startup_BE.common.repository.CommonCodeRepository;
import org.goodee.startup_BE.employee.entity.Employee;
import org.goodee.startup_BE.employee.enums.EmployeeStatus;
import org.goodee.startup_BE.employee.enums.Position;
import org.goodee.startup_BE.employee.enums.Role;
import org.goodee.startup_BE.employee.exception.ResourceNotFoundException;
import org.goodee.startup_BE.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommonCodeServiceImpl implements CommonCodeService {

    private final CommonCodeRepository commonCodeRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public List<CommonCodeResponseDTO> getAllCodePrefixes() {
        return commonCodeRepository.findDistinctCodePrefixes();
    }

    @Override
    public List<CommonCodeResponseDTO> getCommonCodeByPrefix(String codePrefix) {
        return commonCodeRepository
                .findByCodeStartsWith(codePrefix)
                .stream()
                .map(CommonCodeResponseDTO::toDTO)
                .toList();

    }

    @Override
    public List<CommonCodeResponseDTO> getCommonCodeByPrefixWithoutRoot(String codePrefix){
        return getFilteredCommonCodes(codePrefix);
    }


    @Override
    public CommonCodeResponseDTO createCode(String username, CommonCodeRequestDTO commonCodeRequestDTO) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("생성자 정보를 찾을 수 없습니다."));

        return CommonCodeResponseDTO
                .toDTO(commonCodeRepository
                        .save(commonCodeRequestDTO.toEntity(admin)));
    }

    @Override
    public CommonCodeResponseDTO updateCode(String username, CommonCodeRequestDTO commonCodeRequestDTO) {
        Employee admin = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("생성자 정보를 찾을 수 없습니다."));

        CommonCode commonCode = commonCodeRepository.findById(commonCodeRequestDTO.getCommonCodeId())
                .orElseThrow(() -> new ResourceNotFoundException("코드 정보를 찾을 수 없습니다."));

        commonCode
                .update(
                        commonCodeRequestDTO.getCode(),
                        commonCodeRequestDTO.getCodeDescription(),
                        commonCodeRequestDTO.getValue1(),
                        commonCodeRequestDTO.getValue2(),
                        commonCodeRequestDTO.getValue3(),
                        commonCodeRequestDTO.getSortOrder(),
                        admin
                        ,commonCodeRequestDTO.getIsDisabled()
                );

        return CommonCodeResponseDTO
                .toDTO(commonCode);
    }


    /**
     * 공통 코드를 조회하되, 'Prefix + 0' (ex: "DP0")인 코드를 제외하는 헬퍼 메소드
     *
     * @param prefix 조회할 코드의 Prefix
     * @return 'Prefix + 0'이 제외된 DTO 리스트
     */
    private List<CommonCodeResponseDTO> getFilteredCommonCodes(String prefix) {
        // 제외할 코드 (e.g., "DP0", "ES0")
        String excludedCode = prefix + "0";

        return commonCodeRepository
                .findByCodeStartsWithAndIsDisabledFalse(prefix)
                .stream()
                // 제외할 코드를 필터링
                .filter(commonCode -> !commonCode.getCode().equals(excludedCode))
                .map(CommonCodeResponseDTO::toDTO)
                .toList();
    }

    @Override
    public List<CommonCodeResponseDTO> getVacationTypes() {
        return getFilteredCommonCodes(VacationType.PREFIX);
    }


}