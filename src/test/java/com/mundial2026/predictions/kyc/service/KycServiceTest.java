package com.mundial2026.predictions.kyc.service;

import com.mundial2026.predictions.kyc.dto.KycStatusResponse;
import com.mundial2026.predictions.kyc.dto.KycSubmitRequest;
import com.mundial2026.predictions.kyc.dto.LimitRequest;
import com.mundial2026.predictions.kyc.dto.SelfExclusionRequest;
import com.mundial2026.predictions.shared.TestUserHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class KycServiceTest {

    @Inject KycService kycService;
    @Inject TestUserHelper users;

    @Test
    void testSubmitKyc() {
        Long uid = users.createUser(1001);
        KycStatusResponse res = kycService.submit(uid, new KycSubmitRequest("DNI", "12345678"));
        assertEquals("PENDING", res.status());
        assertEquals("DNI", res.documentType());
        assertNull(res.verifiedAt());
    }

    @Test
    void testIsVerifiedFalse() {
        assertFalse(kycService.isVerified(999999L));
    }

    @Test
    void testIsVerifiedTrue() {
        Long uid = users.createUser(1002);
        kycService.submit(uid, new KycSubmitRequest("DNI", "87654321"));
        kycService.approve(uid);
        assertTrue(kycService.isVerified(uid));
    }

    @Test
    void testSetDepositLimit() {
        Long uid = users.createUser(1003);
        kycService.setLimit(uid, new LimitRequest("DAILY_DEPOSIT", new BigDecimal("500.00")));
        var limits = kycService.getLimits(uid);
        assertFalse(limits.isEmpty());
        assertEquals("DAILY_DEPOSIT", limits.get(0).limitType);
        assertEquals(new BigDecimal("500.00"), limits.get(0).amount);
    }

    @Test
    void testCheckDepositLimitOk() {
        Long uid = users.createUser(1004);
        kycService.setLimit(uid, new LimitRequest("DAILY_DEPOSIT", new BigDecimal("1000.00")));
        assertDoesNotThrow(() -> kycService.checkDepositLimit(uid, new BigDecimal("500.00")));
    }

    @Test
    void testCheckDepositLimitExceeded() {
        Long uid = users.createUser(1005);
        kycService.setLimit(uid, new LimitRequest("DAILY_DEPOSIT", new BigDecimal("100.00")));
        assertThrows(IllegalStateException.class,
                () -> kycService.checkDepositLimit(uid, new BigDecimal("200.00")));
    }

    @Test
    void testSelfExclude() {
        Long uid = users.createUser(1006);
        kycService.selfExclude(uid, new SelfExclusionRequest(
                LocalDateTime.now().plusDays(30), "Taking a break"));
        assertTrue(kycService.isExcluded(uid));
    }

    @Test
    void testIsExcludedTrue() {
        Long uid = users.createUser(1007);
        kycService.selfExclude(uid, new SelfExclusionRequest(null, "Indefinite"));
        assertTrue(kycService.isExcluded(uid));
    }

    @Test
    void testIsExcludedFalse() {
        Long uid = users.createUser(1008);
        assertFalse(kycService.isExcluded(uid));
    }
}
