package com.mundial2026.predictions.tournaments.controller;

import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import com.mundial2026.predictions.tournaments.dto.TournamentRequest;
import com.mundial2026.predictions.tournaments.service.TournamentService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/tournaments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tournaments", description = "Super Salas — torneos entre ligas")
public class TournamentResource {

    @Inject TournamentService tournamentService;
    @Inject SecurityUtils     securityUtils;

    @GET
    @PermitAll
    public Response getAll() {
        return Response.ok(ApiResponse.ok(tournamentService.findAll())).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getById(@PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(tournamentService.findById(id))).build();
    }

    @GET
    @Path("/{id}/ranking")
    @PermitAll
    public Response getRanking(@PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(tournamentService.getRanking(id))).build();
    }

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    public Response create(@Valid TournamentRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.status(201).entity(ApiResponse.ok(tournamentService.create(req, userId))).build();
    }

    // Body: { "leagueId": 5 }
    @POST
    @Path("/join/{code}")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    public Response join(@PathParam("code") String code, Map<String, Long> body) {
        Long leagueId = body.get("leagueId");
        if (leagueId == null) return Response.status(400).entity(ApiResponse.error("leagueId requerido")).build();
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(tournamentService.joinWithLeague(code, leagueId, userId))).build();
    }

    @DELETE
    @Path("/{id}/leagues/{leagueId}")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    public Response removeLeague(@PathParam("id") Long id, @PathParam("leagueId") Long leagueId) {
        Long userId = securityUtils.getCurrentUserId();
        tournamentService.removeLeague(id, leagueId, userId);
        return Response.ok(ApiResponse.ok(null, "Sala expulsada del torneo")).build();
    }

    // Body: { "status": "ACTIVE" }
    @PUT
    @Path("/{id}/status")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    public Response updateStatus(@PathParam("id") Long id, Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null) return Response.status(400).entity(ApiResponse.error("status requerido")).build();
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(tournamentService.updateStatus(id, newStatus, userId))).build();
    }
}
