package com.mundial2026.predictions.history.service;

import com.mundial2026.predictions.history.entity.ActivityRecord;
import com.mundial2026.predictions.history.repository.ActivityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class HistoryService {

    @Inject
    ActivityRepository activityRepository;

    public void record(Long userId, String action, String details) {
        activityRepository.save(ActivityRecord.of(userId, action, details));
    }

    public List<ActivityRecord> getUserHistory(Long userId, int limit) {
        return activityRepository.findByUserId(userId, limit);
    }
}
