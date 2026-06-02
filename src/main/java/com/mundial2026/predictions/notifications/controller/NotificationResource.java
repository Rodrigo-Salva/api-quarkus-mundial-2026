package com.mundial2026.predictions.notifications.controller;

import com.mundial2026.predictions.notifications.entity.NotificationRecord;
import com.mundial2026.predictions.notifications.repository.NotificationRepository;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class NotificationResource {

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    SecurityUtils securityUtils;

    @GET
    public Response myNotifications(@QueryParam("limit") @DefaultValue("20") int limit) {
        Long userId = securityUtils.getCurrentUserId();
        List<NotificationRecord> notifications = notificationRepository.findByUserId(
                String.valueOf(userId), limit);
        return Response.ok(ApiResponse.ok(notifications)).build();
    }
}
