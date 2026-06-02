package com.mundial2026.predictions.kyc.service;

import com.mundial2026.predictions.kyc.dto.KycStatusResponse;
import com.mundial2026.predictions.kyc.dto.KycSubmitRequest;
import com.mundial2026.predictions.kyc.dto.LimitRequest;
import com.mundial2026.predictions.kyc.dto.SelfExclusionRequest;
import com.mundial2026.predictions.kyc.entity.GamblingLimit;
import com.mundial2026.predictions.kyc.entity.KycVerification;
import com.mundial2026.predictions.kyc.entity.SelfExclusion;
import com.mundial2026.predictions.kyc.repository.ExclusionRepository;
import com.mundial2026.predictions.kyc.repository.KycRepository;
import com.mundial2026.predictions.kyc.repository.LimitRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class KycService {

    @Inject
    KycRepository kycRepository;

    @Inject
    LimitRepository limitRepository;

    @Inject
    ExclusionRepository exclusionRepository;

    @Transactional
    public KycStatusResponse submit(Long userId, KycSubmitRequest req) {
        if (kycRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("Ya enviaste tus documentos de verificación anteriormente");
        }
        KycVerification kyc = new KycVerification();
        kyc.userId = userId;
        kyc.documentType = req.documentType();
        kyc.documentNumber = req.documentNumber();
        kyc.status = "PENDING";
        kycRepository.persist(kyc);
        return toResponse(kyc);
    }

    @Transactional
    public KycStatusResponse approve(Long userId) {
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("KYC not found for user: " + userId));
        kyc.status = "APPROVED";
        kyc.verifiedAt = LocalDateTime.now();
        return toResponse(kyc);
    }

    public KycStatusResponse getStatus(Long userId) {
        KycVerification kyc = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("KYC not submitted"));
        return toResponse(kyc);
    }

    public boolean isVerified(Long userId) {
        return kycRepository.findByUserId(userId)
                .map(k -> "APPROVED".equals(k.status))
                .orElse(false);
    }

    public boolean isExcluded(Long userId) {
        List<SelfExclusion> exclusions = exclusionRepository.findActiveByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        return exclusions.stream().anyMatch(e ->
                Boolean.TRUE.equals(e.active) && (e.endDate == null || e.endDate.isAfter(now))
        );
    }

    @Transactional
    public void setLimit(Long userId, LimitRequest req) {
        GamblingLimit limit = limitRepository.findByUserIdAndType(userId, req.limitType())
                .orElseGet(() -> {
                    GamblingLimit l = new GamblingLimit();
                    l.userId = userId;
                    l.limitType = req.limitType();
                    return l;
                });
        limit.amount = req.amount();
        limitRepository.persist(limit);
    }

    public List<GamblingLimit> getLimits(Long userId) {
        return limitRepository.findByUserId(userId);
    }

    public void checkDepositLimit(Long userId, BigDecimal amount) {
        limitRepository.findByUserIdAndType(userId, "DAILY_DEPOSIT").ifPresent(limit -> {
            if (amount.compareTo(limit.amount) > 0) {
                throw new IllegalStateException(
                        "El depósito supera tu límite diario de " + limit.amount + " " + limit.currency + ". Modifica tu límite en /api/kyc/limits");
            }
        });
    }

    @Transactional
    public void selfExclude(Long userId, SelfExclusionRequest req) {
        SelfExclusion exclusion = new SelfExclusion();
        exclusion.userId = userId;
        exclusion.endDate = req.endDate();
        exclusion.reason = req.reason();
        exclusion.active = true;
        exclusionRepository.persist(exclusion);
    }

    private KycStatusResponse toResponse(KycVerification kyc) {
        return new KycStatusResponse(kyc.status, kyc.documentType, kyc.verifiedAt, kyc.createdAt);
    }
}
