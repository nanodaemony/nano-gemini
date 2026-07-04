package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.Entitlement;
import com.naon.grid.modules.billing.domain.UserEntitlement;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRecordRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceImplTest {

    @Mock private UserEntitlementRecordRepository recordRepository;
    @Mock private UserEntitlementRepository userEntitlementRepository;
    @Mock private EntitlementRepository entitlementRepository;

    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        entitlementService = new EntitlementServiceImpl(
                recordRepository, userEntitlementRepository, entitlementRepository);
    }

    @Test
    void grantEntitlements_newUser_createsSummary() {
        entitlementService.grantEntitlements(1L, Arrays.asList(1),
                "TRIAL", null, 7, "A");

        verify(recordRepository, times(1)).save(any());
        verify(userEntitlementRepository, times(1)).upsertUserEntitlement(
                eq(1L), eq(1), any(), eq(7), any());
    }

    @Test
    void grantEntitlements_existingUser_stacksExpiry() {
        entitlementService.grantEntitlements(1L, Arrays.asList(1),
                "PURCHASE", "ORD001", 30, "A");

        verify(recordRepository, times(1)).save(any());
        verify(userEntitlementRepository, times(1)).upsertUserEntitlement(
                eq(1L), eq(1), any(), eq(30), any());
    }

    @Test
    void hasModuleAccess_activeEntitlement_returnsTrue() {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(1);
        entitlement.setModuleCode("VOCAB");

        UserEntitlement ue = new UserEntitlement();
        ue.setExpireAt(LocalDateTime.now().plusDays(10));
        ue.setStatus("ACTIVE");

        when(entitlementRepository.findByModuleCode("VOCAB"))
                .thenReturn(Optional.of(entitlement));
        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.of(ue));

        assertTrue(entitlementService.hasModuleAccess(1L, "VOCAB"));
    }

    @Test
    void hasModuleAccess_expired_returnsFalse() {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(1);
        entitlement.setModuleCode("VOCAB");

        UserEntitlement ue = new UserEntitlement();
        ue.setExpireAt(LocalDateTime.now().minusDays(1));
        ue.setStatus("ACTIVE");

        when(entitlementRepository.findByModuleCode("VOCAB"))
                .thenReturn(Optional.of(entitlement));
        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.of(ue));

        assertFalse(entitlementService.hasModuleAccess(1L, "VOCAB"));
    }

    @Test
    void hasModuleAccess_unknownModule_returnsFalse() {
        when(entitlementRepository.findByModuleCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertFalse(entitlementService.hasModuleAccess(1L, "UNKNOWN"));
    }

    @Test
    void hasReceivedTrial_true() {
        when(recordRepository.existsByUserIdAndSourceType(1L, "TRIAL"))
                .thenReturn(true);

        assertTrue(entitlementService.hasReceivedTrial(1L));
    }

    @Test
    void hasReceivedTrial_false() {
        when(recordRepository.existsByUserIdAndSourceType(1L, "TRIAL"))
                .thenReturn(false);

        assertFalse(entitlementService.hasReceivedTrial(1L));
    }

    @Test
    void grantEntitlements_emptyList_noOp() {
        entitlementService.grantEntitlements(1L, Arrays.asList(),
                "TRIAL", null, 7, "A");

        verify(recordRepository, never()).save(any());
        verify(userEntitlementRepository, never()).save(any());
    }
}
