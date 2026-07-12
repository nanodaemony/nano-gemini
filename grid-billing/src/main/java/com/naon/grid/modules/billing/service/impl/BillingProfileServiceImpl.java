package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.BillingProfile;
import com.naon.grid.modules.billing.repository.BillingProfileRepository;
import com.naon.grid.modules.billing.service.BillingProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingProfileServiceImpl implements BillingProfileService {

    private final BillingProfileRepository profileRepository;

    @Override
    @Transactional
    public BillingProfile saveOrUpdate(BillingProfile profile) {
        if (profile.getCreateTime() == null) {
            profile.setCreateTime(LocalDateTime.now());
        }
        profile.setUpdateTime(LocalDateTime.now());
        return profileRepository.save(profile);
    }

    @Override
    public Optional<BillingProfile> findByUserAndOrg(Long userId, Integer orgId) {
        return profileRepository.findByUserIdAndOrgId(userId, orgId);
    }

    @Override
    public Optional<BillingProfile> findByUser(Long userId) {
        return profileRepository.findByUserIdAndOrgIdIsNull(userId);
    }

    @Override
    public Optional<BillingProfile> findByOrg(Integer orgId) {
        return profileRepository.findByOrgId(orgId);
    }
}
