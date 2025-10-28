package org.goodee.startup_BE.common.controller;

import lombok.RequiredArgsConstructor;
import org.goodee.startup_BE.common.dto.CommonCodeResponseDTO;
import org.goodee.startup_BE.common.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    /*
     * 전체 트리 조회 API
     * URL 예 : "/api/organization/tree
     * - 최상위 부서(DP1-스타트업)기준으로 하위 부서를 트리구조로 조회한다.
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CommonCodeResponseDTO>> getOrganizationTree() {
        // organizationService를 호출하고, 조직도 트리 조회.
        List<CommonCodeResponseDTO> organizationTree = organizationService.getOrganizationTree();
        return ResponseEntity.ok(organizationTree);
    }

    /*
     * 특정 부서 조회
     * URL 예 : "/api/organization/DP1"
     * - 경로변수(@PathVariable)로 부서코드{code}에 해당하는 부서 정보를 조회한다.
     */
    @GetMapping("/{code}")
    public ResponseEntity<CommonCodeResponseDTO> getDepartmentByCode(@PathVariable String code) {
        // organizationService를 호출하고, 특정 부서 조회
        CommonCodeResponseDTO departmentByCode = organizationService.getDepartmentByCode(code);
        return ResponseEntity.ok(departmentByCode);
    }

}
