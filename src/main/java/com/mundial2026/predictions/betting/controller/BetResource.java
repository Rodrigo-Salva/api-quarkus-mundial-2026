package com.mundial2026.predictions.betting.controller;

import com.mundial2026.predictions.betting.dto.AccumulatorBetRequest;
import com.mundial2026.predictions.betting.dto.BetRequest;
import com.mundial2026.predictions.betting.service.BetService;
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

@Path("/api/bets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Bets", description = """
        Apuestas simuladas. Mercados disponibles:
        **Resultado**: HOME_WIN, DRAW, AWAY_WIN
        **Goles**: OVER_25, UNDER_25, OVER_35, UNDER_35, BOTH_SCORE_YES, BOTH_SCORE_NO
        **Córners**: CORNERS_OVER_95, CORNERS_UNDER_95, CORNERS_OVER_115, CORNERS_UNDER_115
        **Tarjetas**: CARDS_OVER_35, CARDS_UNDER_35, HOME_RED_CARD, AWAY_RED_CARD
        **Primera mitad**: FIRST_HALF_HOME, FIRST_HALF_DRAW, FIRST_HALF_AWAY

        Requisitos: KYC aprobado + saldo suficiente en wallet.
        Las cuotas bajan dinámicamente cuando se apuesta mucho a un lado.
        """)
public class BetResource {

    @Inject BetService    betService;
    @Inject SecurityUtils securityUtils;

    @POST
    @Operation(summary = "Colocar apuesta simple",
               description = "Apuesta a un mercado de un partido. El stake mínimo es 1.00 PEN. Se descuenta del wallet inmediatamente.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = {
                @ExampleObject(name = "Resultado",
                        value = "{ \"matchId\": 1, \"market\": \"HOME_WIN\", \"stake\": 50.00 }"),
                @ExampleObject(name = "Corners",
                        value = "{ \"matchId\": 1, \"market\": \"CORNERS_OVER_95\", \"stake\": 20.00 }"),
                @ExampleObject(name = "Tarjetas",
                        value = "{ \"matchId\": 1, \"market\": \"CARDS_OVER_35\", \"stake\": 15.00 }")
            }))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Apuesta registrada. Ver `potentialPayout` para el pago máximo posible."),
        @APIResponse(responseCode = "422", description = "KYC requerido, usuario auto-excluido, o saldo insuficiente."),
        @APIResponse(responseCode = "400", description = "Partido finalizado o mercado no disponible.")
    })
    public Response placeBet(@Valid BetRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(betService.placeBet(userId, req), "Bet placed successfully"))
                .build();
    }

    @POST
    @Path("/accumulator")
    @Operation(summary = "Apuesta acumuladora",
               description = "Combina 2 o más selecciones. Las cuotas se multiplican, aumentando el pago potencial. Si una selección falla, pierdes toda la apuesta.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(name = "Doble", value = """
                    {
                      "selections": [
                        { "matchId": 1, "market": "HOME_WIN" },
                        { "matchId": 2, "market": "OVER_25" }
                      ],
                      "stake": 30.00
                    }
                    """)))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Acumuladora registrada."),
        @APIResponse(responseCode = "422", description = "Saldo insuficiente o KYC requerido."),
        @APIResponse(responseCode = "400", description = "Mínimo 2 selecciones requeridas.")
    })
    public Response placeAccumulatorBet(@Valid AccumulatorBetRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(betService.placeAccumulatorBet(userId, req), "Accumulator bet placed"))
                .build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Mis apuestas",
               description = "Lista todas tus apuestas. El campo `status` puede ser: PENDING, WON, LOST, CANCELLED.")
    @APIResponse(responseCode = "200", description = "Lista de apuestas del usuario.")
    public Response myBets() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(betService.findByUser(userId))).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Detalle de apuesta")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Datos de la apuesta."),
        @APIResponse(responseCode = "404", description = "Apuesta no encontrada.")
    })
    public Response getById(
            @Parameter(description = "ID de la apuesta", required = true)
            @PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(betService.findById(id))).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Cancelar apuesta",
               description = "Cancela una apuesta PENDING y devuelve el stake al wallet. Solo se puede cancelar si el partido aún está en estado SCHEDULED.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Apuesta cancelada y stake devuelto."),
        @APIResponse(responseCode = "422", description = "No se puede cancelar una apuesta ya liquidada o en partido en vivo.")
    })
    public Response cancelBet(
            @Parameter(description = "ID de la apuesta", required = true)
            @PathParam("id") Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(betService.cancelBet(id, userId), "Bet cancelled")).build();
    }
}
