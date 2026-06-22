package com.naon.grid.modules.app.service.impl;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.app.service.dto.ActivateSubscriptionDTO;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final GridUserRoleRepository userRoleRepository;
    private final EntitlementEngine entitlementEngine;
    private final GridUserRepository userRepository;

    @Value("${app.subscription.trial-days:7}")
    private int trialDays;

    @Override
    public AppSubscriptionVO getMySubscription(Long userId) {
        List<GridUserRole> roles = userRoleRepository.findByUserIdAndExpireTimeAfterOrExpireTimeIsNull(userId, new Date());

        AppSubscriptionVO vo = new AppSubscriptionVO();
        vo.setLevel("NORMAL");
        vo.setExpireTime(null);
        vo.setExpiringSoon(false);

        // 查找最高级别的会员角色
        GridUserRole highest = null;
        for (GridUserRole role : roles) {
            if ("SVIP".equals(role.getRoleCode())) {
                highest = role;
                break;  // SVIP 是最高级
            }
            if ("VIP".equals(role.getRoleCode())) {
                highest = role;
                // 继续检查是否有 SVIP
            }
        }

        if (highest != null) {
            vo.setLevel(highest.getRoleCode());
            vo.setExpireTime(highest.getExpireTime());

            // 检查是否 15 天内到期
            if (highest.getExpireTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, 15);
                vo.setExpiringSoon(highest.getExpireTime().before(cal.getTime()));
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateSubscription(ActivateSubscriptionDTO dto) {
        String level = dto.getLevel();
        if (!"VIP".equals(level) && !"SVIP".equals(level)) {
            throw new BadRequestException("不支持的会员级别: " + level);
        }

        // 查询用户已有的同级别角色
        Optional<GridUserRole> existingRole = userRoleRepository
                .findByUserIdAndRoleCode(dto.getUserId(), level);

        if (existingRole.isPresent()) {
            GridUserRole role = existingRole.get();
            Date now = new Date();

            // 如果已过期，从当前时间重新计算
            if (role.getExpireTime() != null && role.getExpireTime().before(now)) {
                role.setExpireTime(addDays(now, dto.getDays()));
                role.setRoleName(level + "会员");
            } else {
                // 续期：延长 expire_time
                role.setExpireTime(addDays(role.getExpireTime() != null ? role.getExpireTime() : now, dto.getDays()));
            }
            userRoleRepository.save(role);
        } else {
            // 新购：创建新角色
            GridUserRole newRole = new GridUserRole();
            newRole.setUserId(dto.getUserId());
            newRole.setRoleCode(level);
            newRole.setRoleName(level + "会员");
            newRole.setExpireTime(addDays(new Date(), dto.getDays()));
            userRoleRepository.save(newRole);
        }

        log.info("Subscription activated: userId={}, level={}, days={}", dto.getUserId(), level, dto.getDays());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantTrial(Long userId) {
        if (trialDays <= 0) {
            return;
        }

        // Delegate to new engine
        String region = userRepository.findById(userId)
                .map(GridUser::getRegion)
                .filter(r -> r != null)
                .orElse("C");
        entitlementEngine.grant(userId, "TRIAL", null, "PLUS", trialDays, region);

        log.info("Trial granted: userId={}, days={}", userId, trialDays);
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }
}
