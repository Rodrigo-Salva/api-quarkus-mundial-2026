package com.mundial2026.predictions.rankings.controller;

import com.mundial2026.predictions.auth.entity.User;
import com.mundial2026.predictions.auth.service.AuthService;
import com.mundial2026.predictions.rankings.service.RankingService;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/rankings")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Rankings", description = "Tabla de posiciones en tiempo real. Servido desde Redis — latencia < 5ms.")
public class RankingResource {

    @Inject RankingService rankingService;
    @Inject SecurityUtils  securityUtils;
    @Inject AuthService    authService;

    @GET
    @Path("/global")
    @PermitAll
    @Operation(summary = "Ranking global",
               description = "Top N participantes del torneo ordenados por `totalPoints`. Público, sin autenticación.")
    @APIResponse(responseCode = "200", description = "Lista de posiciones: `{ position, username, points, country }`.")
    public Response global(
            @Parameter(description = "Cantidad de posiciones a retornar (máx recomendado: 100)", example = "50")
            @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(ApiResponse.ok(rankingService.getGlobalRanking(limit))).build();
    }

    @GET
    @Path("/country/{code}")
    @PermitAll
    @Operation(summary = "Ranking por país",
               description = "Top N participantes de un país específico. Usar código ISO-3166 en mayúsculas.")
    @APIResponse(responseCode = "200", description = "Ranking filtrado por país.")
    public Response byCountry(
            @Parameter(description = "Código de país ISO-3166 (ej: PER, ARG, BRA)", required = true, example = "PER")
            @PathParam("code") String code,
            @Parameter(description = "Límite de resultados", example = "50")
            @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(ApiResponse.ok(rankingService.getCountryRanking(code, limit))).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Mi posición en el ranking",
               description = "Retorna la posición actual del usuario autenticado. `position: -1` significa que aún no tiene puntos.")
    @APIResponse(responseCode = "200", description = "Posición y puntos del usuario.")
    public Response me() {
        Long userId = securityUtils.getCurrentUserId();
        User user = authService.findById(userId);
        return Response.ok(ApiResponse.ok(
                rankingService.getUserRanking(userId, user.username, user.country))).build();
    }
}
