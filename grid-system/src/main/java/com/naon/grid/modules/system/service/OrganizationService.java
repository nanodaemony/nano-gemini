package com.naon.grid.modules.system.service;

import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface OrganizationService {

    /**
     * 机构申请入驻（提交申请信息，不创建用户）
     */
    void register(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 查询申请状态（验证邮箱+密码后返回申请进度）
     */
    Map<String, Object> queryApplication(ApplicationQueryDTO dto);

    /**
     * 驳回后重新提交申请
     */
    void resubmit(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 审核通过（后台管理员操作）
     */
    void approve(Integer orgId, String planProductCode);

    /**
     * 审核驳回
     */
    void reject(Integer orgId, String reason);

    /**
     * 根据ID查询
     */
    GridOrganization findById(Integer orgId);
}
