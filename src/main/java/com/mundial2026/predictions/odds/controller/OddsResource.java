package com.mundial2026.predictions.odds.controller;

import com.mundial2026.predictions.odds.dto.AddPlayerMarketRequest;
import com.mundial2026.predictions.odds.service.OddsService;
import com.mundial2026.predictions.odds.service.PlayerMarketService;
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

@Path("/api/odds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Odds", description = "Cuotas de apuesta. Las cuotas son dinámicas: bajan automáticamente cuando se acumulan más de 1000 PEN apostados al mismo lado (ajuste del 2% por cada 1000 PEN).")
public class OddsResource {

    @Inject OddsService         oddsService;
    @Inject PlayerMarketService playerMarketService;

    @GET
    @Path("/match/{matchId}")
    @PermitAll
    @Operation(summary = "Cuotas de un partido",
               description = "Lista todas las cuotas activas del partido. El campo `totalStaked` muestra cuánto se ha apostado a ese mercado y `betCount` cuántas apuestas tiene.")
    @APIResponse(responseCode = "200", description = "Lista de cuotas con `baseOdds` (original) y `odds` (ajustada dinámicamente).")
    public Response getByMatch(
            @Parameter(description = "ID del partido", required = true, example = "1")
            @PathParam("matchId") Long matchId) {
        return Response.ok(ApiResponse.ok(oddsService.getOddsForMatch(matchId))).build();
    }

    @GET
    @Path("/match/{matchId}/market/{market}")
    @PermitAll
    @Operation(summary = "Cuota de un mercado específico",
               description = "Obtiene la cuota actual de un mercado. Útil para mostrar la cuota en tiempo real antes de apostar.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Cuota del mercado."),
        @APIResponse(responseCode = "404", description = "Mercado no disponible para este partido.")
    })
    public Response getByMarket(
            @Parameter(description = "ID del partido", required = true) @PathParam("matchId") Long matchId,
            @Parameter(description = "Nombre del mercado (ej: HOME_WIN, OVER_25, CORNERS_OVER_95)", required = true, example = "HOME_WIN")
            @PathParam("market") String market) {
        return Response.ok(ApiResponse.ok(oddsService.getOdds(matchId, market))).build();
    }

    @GET
    @Path("/match/{matchId}/players")
    @PermitAll
    @Operation(summary = "Mercados de jugador",
               description = "Cuotas para mercados de jugadores específicos: FIRST_GOAL_SCORER, ANYTIME_SCORER, FIRST_ASSIST.")
    @APIResponse(responseCode = "200", description = "Lista de mercados de jugadores del partido.")
    public Response getPlayerMarkets(
            @Parameter(description = "ID del partido", required = true)
            @PathParam("matchId") Long matchId) {
        return Response.ok(ApiResponse.ok(playerMarketService.getByMatch(matchId))).build();
    }

    @POST
    @Path("/match/{matchId}/players")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Agregar mercado de jugador (ADMIN)",
               description = "Agrega una cuota para un jugador específico en un partido.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = """
                    { "playerName": "Lionel Messi", "market": "FIRST_GOAL_SCORER", "odds": 4.50 }
                    """)))
    @APIResponse(responseCode = "201", description = "Mercado de jugador creado.")
    public Response addPlayerMarket(
            @Parameter(description = "ID del partido", required = true)
            @PathParam("matchId") Long matchId,
            @Valid AddPlayerMarketRequest req) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(playerMarketService.addPlayerMarket(matchId, req), "Player market added"))
                .build();
    }

    @GET
    @Path("/match/{matchId}/players/{playerName}/{market}")
    @PermitAll
    @Operation(summary = "Cuota de jugador específico",
               description = "Obtiene la cuota actual de un jugador en un mercado concreto.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Cuota del jugador."),
        @APIResponse(responseCode = "404", description = "No hay cuota para este jugador/mercado.")
    })
    public Response getPlayerOdds(
            @Parameter(description = "ID del partido") @PathParam("matchId") Long matchId,
            @Parameter(description = "Nombre del jugador", example = "Lionel Messi") @PathParam("playerName") String playerName,
            @Parameter(description = "Mercado del jugador", example = "FIRST_GOAL_SCORER") @PathParam("market") String market) {
        return Response.ok(ApiResponse.ok(
                playerMarketService.getPlayerOdds(matchId, playerName, market))).build();
    }
}
