package com.naon.grid.modules.system.service;

import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;
import javax.servlet.http.HttpServletRequest;

public interface OrganizationService {
    /**
     * 机构自助注册
     */
    TokenDTO register(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 审核机构（后台管理员操作）
     */
    void approve(Integer orgId, String planProductCode);

    /**
     * 驳回机构
     */
    void reject(Integer orgId, String reason);

    /**
     * 根据ID查询
     */
    GridOrganization findById(Integer orgId);
}
