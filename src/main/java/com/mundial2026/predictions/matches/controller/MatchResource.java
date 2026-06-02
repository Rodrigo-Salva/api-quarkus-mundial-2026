package com.mundial2026.predictions.matches.controller;

import com.mundial2026.predictions.matches.dto.MatchRequest;
import com.mundial2026.predictions.matches.dto.MatchResponse;
import com.mundial2026.predictions.matches.dto.MatchResultRequest;
import com.mundial2026.predictions.matches.service.MatchService;
import com.mundial2026.predictions.shared.response.ApiResponse;
import jakarta.annotation.security.PermitAll;
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

import java.util.List;

@Path("/api/matches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Matches", description = "Partidos del Mundial 2026. Los endpoints de consulta son públicos. Los de creación/actualización requieren rol ADMIN.")
public class MatchResource {

    @Inject MatchService matchService;

    @GET
    @PermitAll
    @Operation(summary = "Listar todos los partidos",
               description = "Retorna todos los partidos con su estado: SCHEDULED (programado), LIVE (en vivo), FINISHED (terminado).")
    @APIResponse(responseCode = "200", description = "Lista de partidos.")
    public Response getAll() {
        List<MatchResponse> matches = matchService.findAll();
        return Response.ok(ApiResponse.ok(matches)).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Obtener partido por ID")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Datos del partido."),
        @APIResponse(responseCode = "404", description = "Partido no encontrado.")
    })
    public Response getById(
            @Parameter(description = "ID del partido", required = true, example = "1")
            @PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(matchService.findById(id))).build();
    }

    @GET
    @Path("/{id}/live")
    @PermitAll
    @Operation(summary = "Estado en vivo del partido",
               description = "Retorna el estado actual del partido. Para datos en tiempo real usa el WebSocket `/ws/match/{matchId}`.")
    @APIResponse(responseCode = "200", description = "Estado actual del partido.")
    public Response getLive(
            @Parameter(description = "ID del partido", required = true)
            @PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(matchService.findById(id))).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Crear partido (ADMIN)",
               description = "Solo usuarios con rol ADMIN pueden crear partidos.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = """
                    {
                      "homeTeam": "Argentina",
                      "awayTeam": "Brasil",
                      "matchDate": "2026-06-14T18:00:00"
                    }
                    """)))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Partido creado."),
        @APIResponse(responseCode = "403", description = "Se requiere rol ADMIN.")
    })
    public Response create(@Valid MatchRequest req) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(matchService.create(req), "Match created"))
                .build();
    }

    @PUT
    @Path("/{id}/result")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Registrar resultado (ADMIN)",
               description = "Registra el marcador final y cambia el estado del partido. El sistema liquida automáticamente todas las predicciones.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = """
                    {
                      "homeScore": 2,
                      "awayScore": 1,
                      "status": "FINISHED"
                    }
                    """)))
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Resultado registrado y predicciones liquidadas."),
        @APIResponse(responseCode = "404", description = "Partido no encontrado.")
    })
    public Response updateResult(
            @Parameter(description = "ID del partido") @PathParam("id") Long id,
            @Valid MatchResultRequest body) {
        return Response.ok(ApiResponse.ok(
                matchService.updateResult(id, body.homeScore(), body.awayScore(), body.status())))
                .build();
    }
}
