package com.naon.grid.modules.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import com.naon.grid.service.EmailService;
import com.naon.grid.utils.RsaUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private final GridOrganizationRepository organizationRepository;
    private final GridUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionResolver regionResolver;
    private final EmailService emailService;
    private final ReferralService referralService;
    private final EntitlementService entitlementService;
    private final EntitlementRepository entitlementRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(InstitutionRegisterDTO dto, HttpServletRequest request) {
        // 检查邮箱是否已被注册（包括已审核通过的机构用户）
        if (userRepository.findByEmail(dto.getAdminEmail()).isPresent()) {
            throw new BadRequestException("该邮箱已被注册");
        }

        // 检查是否有 PENDING 或 APPROVED 的申请记录
        organizationRepository.findByContactEmail(dto.getAdminEmail())
                .filter(org -> !"REJECTED".equals(org.getAuditStatus()))
                .ifPresent(org -> {
                    throw new BadRequestException("该邮箱已提交过申请");
                });

        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        // 加密密码（解密 RSA + BCrypt 加密）
        String encryptedPassword;
        try {
            String decryptedPassword = RsaUtils.decryptByPrivateKey(
                    RsaProperties.privateKey, dto.getAdminPassword());
            encryptedPassword = passwordEncoder.encode(decryptedPassword);
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }

        // 创建机构申请记录（PENDING）
        GridOrganization org = new GridOrganization();
        org.setName(dto.getName());
        org.setNameEn(dto.getNameEn());
        org.setOrgType(dto.getOrgType());
        org.setContactName(dto.getContactName());
        org.setContactPhone(dto.getContactPhone());
        org.setContactEmail(dto.getAdminEmail());  // adminEmail 作为联系邮箱
        org.setReferredBy(dto.getReferredBy());
        org.setAdminPassword(encryptedPassword);
        org.setRegion(region);
        // 若申请成为代理机构，设置 orgRole 为 AGENT
        if (Boolean.TRUE.equals(dto.getApplyAsAgent())) {
            org.setOrgRole("AGENT");
        }
        org.setAuditStatus("PENDING");
        organizationRepository.save(org);
    }

    @Override
    public Map<String, Object> queryApplication(ApplicationQueryDTO dto) {
        GridOrganization org = organizationRepository
                .findByContactEmail(dto.getEmail())
                .orElse(null);

        if (org == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "NOT_FOUND");
            result.put("message", "未找到申请记录");
            return result;
        }

        // 如果密码已清空（如审批通过后），跳过密码验证
        if (org.getAdminPassword() != null) {
            String decryptedPassword;
            try {
                decryptedPassword = RsaUtils.decryptByPrivateKey(
                        RsaProperties.privateKey, dto.getPassword());
            } catch (Exception e) {
                throw new BadRequestException("密码解密失败");
            }

            if (!passwordEncoder.matches(decryptedPassword, org.getAdminPassword())) {
                throw new BadRequestException("邮箱或密码不正确");
            }
        }

        Map<String, Object> result = new HashMap<>();

        switch (org.getAuditStatus()) {
            case "PENDING":
                result.put("status", "PENDING");
                result.put("message", "您的申请正在审核中，请耐心等待");
                break;
            case "APPROVED":
                result.put("status", "APPROVED");
                result.put("message", "审核已通过，请登录");
                break;
            case "REJECTED":
                Map<String, Object> data = new HashMap<>();
                data.put("name", org.getName());
                data.put("nameEn", org.getNameEn());
                data.put("orgType", org.getOrgType());
                data.put("contactName", org.getContactName());
                data.put("contactPhone", org.getContactPhone());
                data.put("rejectReason", org.getRejectReason());
                result.put("status", "REJECTED");
                result.put("message", "审核未通过");
                result.put("data", data);
                break;
            default:
                result.put("status", "ERROR");
                result.put("message", "状态异常");
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(InstitutionRegisterDTO dto, HttpServletRequest request) {
        GridOrganization org = organizationRepository
                .findByContactEmail(dto.getAdminEmail())
                .orElseThrow(() -> new BadRequestException("未找到申请记录"));

        if (!"REJECTED".equals(org.getAuditStatus())) {
            throw new BadRequestException("当前状态不允许重新提交");
        }

        // 验证当前密码
        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(
                    RsaProperties.privateKey, dto.getAdminPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }
        if (!passwordEncoder.matches(decryptedPassword, org.getAdminPassword())) {
            throw new BadRequestException("邮箱或密码不正确");
        }

        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        // 更新字段
        org.setName(dto.getName());
        org.setNameEn(dto.getNameEn());
        org.setOrgType(dto.getOrgType());
        org.setContactName(dto.getContactName());
        org.setContactPhone(dto.getContactPhone());
        org.setReferredBy(dto.getReferredBy());
        org.setRegion(region);
        if (Boolean.TRUE.equals(dto.getApplyAsAgent())) {
            org.setOrgRole("AGENT");
        } else {
            org.setOrgRole("INSTITUTION");
        }
        org.setAuditStatus("PENDING");
        org.setRejectReason(null);  // 清除驳回原因
        organizationRepository.save(org);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer orgId, String planProductCode) {
        GridOrganization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));

        if (!"PENDING".equals(org.getAuditStatus())) {
            throw new BadRequestException("当前状态不允许审核通过");
        }

        // 设置机构套餐限制
        if ("INST_STARTER".equals(planProductCode)) {
            org.setMaxMembers(30);
            org.setMaxAdmins(1);
        } else if ("INST_BASIC".equals(planProductCode)) {
            org.setMaxMembers(100);
            org.setMaxAdmins(2);
        } else if ("INST_PRO".equals(planProductCode)) {
            org.setMaxMembers(500);
            org.setMaxAdmins(5);
        } else {
            org.setMaxMembers(30);
            org.setMaxAdmins(1);
        }

        org.setCurrentMembers(1);
        // 若为代理机构，设置初始佣金比例（管理员后续可调整）
        if ("AGENT".equals(org.getOrgRole())) {
            org.setCommissionRate(BigDecimal.ZERO);
        }
        org.setAuditStatus("APPROVED");
        organizationRepository.save(org);

        // 创建管理员用户
        String encryptedPassword = org.getAdminPassword();
        if (encryptedPassword == null) {
            throw new BadRequestException("管理员密码缺失，请联系技术支持");
        }

        GridUser admin = new GridUser();
        admin.setEmail(org.getContactEmail());
        admin.setPassword(encryptedPassword);
        admin.setNickname(org.getContactName());
        admin.setGender(0);
        admin.setUserType("INSTITUTION");
        admin.setOrgId(org.getId());
        admin.setOrgRole("ADMIN");
        admin.setRegisterAuditStatus("APPROVED");
        admin.setRegion(org.getRegion());
        admin.setStatus(1);

        // 生成机构邀请码
        String referralCode;
        do {
            referralCode = "UR" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(referralCode));
        admin.setReferralCode(referralCode);
        userRepository.save(admin);

        // Grant 30-day trial for institution admin
        List<String> trialEntitlementCodes = Arrays.asList(
                "VOCAB_ACCESS", "GRAMMAR_ACCESS", "CHARACTER_ACCESS",
                "CONFUSING_WORDS_ACCESS", "CULTURE_ACCESS", "TOPIC_ACCESS");
        List<Integer> allEntitlementIds = trialEntitlementCodes.stream()
                .map(code -> entitlementRepository.findByCode(code)
                        .orElseThrow(() -> new RuntimeException("Entitlement not found: " + code)))
                .map(e -> e.getId())
                .collect(Collectors.toList());
        entitlementService.grantEntitlements(
                admin.getId(), allEntitlementIds,
                "TRIAL", null, 30, org.getRegion());

        // 处理邀请码溯源（如果申请时填了推荐码）
        if (org.getReferredBy() != null && !org.getReferredBy().isEmpty()) {
            referralService.processReferral(org.getReferredBy(), admin.getId());
        }

        // 清空 admin_password
        org.setAdminPassword(null);
        organizationRepository.save(org);

        // 发送审核通过邮件
        sendApprovalEmail(org, admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer orgId, String reason) {
        GridOrganization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));

        if (!"PENDING".equals(org.getAuditStatus())) {
            throw new BadRequestException("当前状态不允许驳回");
        }

        org.setAuditStatus("REJECTED");
        org.setRejectReason(reason);
        organizationRepository.save(org);

        log.info("Organization rejected: orgId={}, reason={}", orgId, reason);

        // 发送驳回邮件
        sendRejectionEmail(org, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Integer orgId, String orgRole) {
        GridOrganization org = findById(orgId);
        if (!"INSTITUTION".equals(orgRole) && !"AGENT".equals(orgRole)) {
            throw new BadRequestException("无效的机构角色，仅支持 INSTITUTION 或 AGENT");
        }
        org.setOrgRole(orgRole);
        organizationRepository.save(org);
    }

    @Override
    public GridOrganization findById(Integer orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
    }

    private void sendApprovalEmail(GridOrganization org, GridUser admin) {
        String subject = "您的机构【" + org.getName() + "】入驻审核已通过";
        String content = "<p>您好，</p>"
                + "<p>您的机构【" + org.getName() + "】已通过审核，现已正式入驻有路中文平台。</p>"
                + "<p>您可以点击以下链接使用邮箱【" + admin.getEmail() + "】登录平台：<br/>"
                + "<a href=\"https://yourroad.com/login\">https://yourroad.com/login</a></p>"
                + "<p>请及时设置机构信息并开始管理您的成员。</p>";
        try {
            emailService.sendHtmlEmail(admin.getEmail(), subject, content);
        } catch (Exception e) {
            log.error("Failed to send approval email to: {}", admin.getEmail(), e);
        }
    }

    private void sendRejectionEmail(GridOrganization org, String reason) {
        String subject = "您的机构【" + org.getName() + "】入驻审核未通过";
        String content = "<p>您好，</p>"
                + "<p>您的机构【" + org.getName() + "】审核未通过，原因如下：</p>"
                + "<p><strong>" + reason + "</strong></p>"
                + "<p>您可根据驳回原因修改信息后<a href=\"https://yourroad.com/institution/apply\">重新提交申请</a>。</p>";
        try {
            emailService.sendHtmlEmail(org.getContactEmail(), subject, content);
        } catch (Exception e) {
            log.error("Failed to send rejection email to: {}", org.getContactEmail(), e);
        }
    }
}
