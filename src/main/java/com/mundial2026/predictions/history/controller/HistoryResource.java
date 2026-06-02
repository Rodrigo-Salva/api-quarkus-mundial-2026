package com.mundial2026.predictions.history.controller;

import com.mundial2026.predictions.history.entity.ActivityRecord;
import com.mundial2026.predictions.history.service.HistoryService;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/history")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class HistoryResource {

    @Inject
    HistoryService historyService;

    @Inject
    SecurityUtils securityUtils;

    @GET
    @Path("/me")
    public Response myHistory(@QueryParam("limit") @DefaultValue("50") int limit) {
        Long userId = securityUtils.getCurrentUserId();
        List<ActivityRecord> history = historyService.getUserHistory(userId, limit);
        return Response.ok(ApiResponse.ok(history)).build();
    }
}
