package com.mundial2026.predictions.predictions.controller;

import com.mundial2026.predictions.predictions.dto.PredictionRequest;
import com.mundial2026.predictions.predictions.service.PredictionService;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/predictions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Predictions", description = """
        Sistema de predicciones del Mundial 2026. Reglas de puntuación:
        • 5 pts — marcador exacto (ej: predice 2-1, termina 2-1)
        • 3 pts — ganador correcto (ej: predice 3-0, termina 1-0)
        • 2 pts — diferencia de goles correcta (ej: predice 3-1, termina 2-0, ambos +2)
        • +2 pts — bonus por cada 3 partidos consecutivos acertados
        • +1 pt  — predicción registrada más de 24h antes del partido
        """)
public class PredictionResource {

    @Inject PredictionService predictionService;
    @Inject SecurityUtils securityUtils;

    @POST
    @Operation(summary = "Enviar predicción",
               description = """
               Registra tu predicción para un partido SCHEDULED.
               **Consejo**: envíala con más de 24h de anticipación para ganar +1 punto extra.
               Solo se permite una predicción por partido por usuario.
               """)
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(name = "Predicción Argentina vs Brasil",
                    value = "{ \"matchId\": 1, \"homeScore\": 2, \"awayScore\": 1 }")))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Predicción guardada. `earlyBonus` será 1 si se registró >24h antes."),
        @APIResponse(responseCode = "400", description = "Partido no disponible para predicciones o ya predijiste este partido."),
        @APIResponse(responseCode = "401", description = "Token requerido.")
    })
    public Response submit(@Valid PredictionRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(predictionService.submit(userId, req), "Prediction submitted"))
                .build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Mis predicciones",
               description = "Lista todas tus predicciones con el desglose de puntos: `points` (base) + `earlyBonus` + `streakBonus` = `totalPoints`.")
    @APIResponse(responseCode = "200", description = "Lista de predicciones del usuario autenticado.")
    public Response myPredictions() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(predictionService.findByUser(userId))).build();
    }

    @GET
    @Path("/match/{matchId}")
    @Operation(summary = "Predicciones de un partido",
               description = "Ver todas las predicciones de todos los usuarios para un partido específico.")
    @APIResponse(responseCode = "200", description = "Lista de predicciones del partido.")
    public Response byMatch(
            @Parameter(description = "ID del partido", required = true, example = "1")
            @PathParam("matchId") Long matchId) {
        return Response.ok(ApiResponse.ok(predictionService.findByMatch(matchId))).build();
    }

    @GET
    @Path("/me/history")
    @Operation(summary = "Mi historial detallado con desglose de puntos",
               description = """
                       Devuelve todas tus predicciones con el desglose completo de por qué
                       ganaste o perdiste cada punto. Cada predicción incluye:
                       - `result`: EXACT | WINNER | DIFFERENCE | MISS | PENDING
                       - `breakdown`: lista de conceptos con descripción y si los ganaste
                       Ideal para mostrar al usuario el motivo de sus puntos.
                       """)
    @APIResponse(responseCode = "200", description = "Historial detallado con breakdown de puntos.")
    public Response myHistory() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(predictionService.getDetailedHistory(userId))).build();
    }

    @GET
    @Path("/{id}/detail")
    @Operation(summary = "Desglose de puntos de una predicción",
               description = """
                       Muestra exactamente por qué ganaste o perdiste puntos en una predicción:
                       ✅ Resultado exacto (+5), ✅ Ganador correcto (+3),
                       ✅ Diferencia de goles (+2), ✅ Anticipada (+1), ✅ Racha (+2)
                       """)
    @APIResponse(responseCode = "200", description = "Desglose detallado de la predicción.")
    public Response detail(
            @Parameter(description = "ID de la predicción", required = true)
            @PathParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(predictionService.getDetail(id, userId))).build();
    }
}
