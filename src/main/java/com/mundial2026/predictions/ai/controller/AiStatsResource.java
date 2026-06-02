package com.mundial2026.predictions.ai.controller;

import com.mundial2026.predictions.ai.service.AiStatsService;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/ai")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "AI Stats", description = "Estadísticas e insights generados por IA. El proveedor se configura en `application.properties` con `ai.provider` (gemini | chatgpt | claude | grok). Cada respuesta indica qué proveedor la generó.")
public class AiStatsResource {

    @Inject AiStatsService aiStatsService;
    @Inject SecurityUtils  securityUtils;

    @GET
    @Path("/my-stats")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Análisis personal con IA",
               description = "La IA analiza tu historial de predicciones y genera: un análisis de tu rendimiento, una predicción de tu tendencia futura, y un tip personalizado para mejorar tu puntaje.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Análisis generado: `{ provider, analysis, prediction, tip }`."),
        @APIResponse(responseCode = "401", description = "Token requerido.")
    })
    public Response myStats() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(aiStatsService.analyzeUser(userId))).build();
    }

    @GET
    @Path("/tournament")
    @PermitAll
    @Operation(summary = "Análisis del torneo",
               description = "La IA analiza el Top 10 del ranking global y genera observaciones sobre las tendencias del torneo y quién tiene más chances de ganar.")
    @APIResponse(responseCode = "200", description = "Análisis del torneo: `{ provider, analysis, prediction, tip }`.")
    public Response tournament() {
        return Response.ok(ApiResponse.ok(aiStatsService.analyzeTournament())).build();
    }

    @GET
    @Path("/suggest")
    @PermitAll
    @Operation(summary = "Sugerencia de predicción para un partido",
               description = "La IA sugiere el marcador más probable para un partido y qué mercado conviene apostar.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Sugerencia: `{ provider, analysis, prediction, tip }`. `prediction` contiene el marcador sugerido."),
        @APIResponse(responseCode = "400", description = "Parámetros `home` y `away` son requeridos.")
    })
    public Response suggest(
            @Parameter(description = "Equipo local", required = true, example = "Argentina")
            @QueryParam("home") String home,
            @Parameter(description = "Equipo visitante", required = true, example = "Brasil")
            @QueryParam("away") String away) {
        if (home == null || away == null) {
            throw new IllegalArgumentException("Query params 'home' and 'away' are required");
        }
        return Response.ok(ApiResponse.ok(aiStatsService.suggestPrediction(home, away))).build();
    }
}
