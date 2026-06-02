package com.mundial2026.predictions.leagues.controller;

import com.mundial2026.predictions.leagues.dto.LeagueRequest;
import com.mundial2026.predictions.leagues.dto.LeagueResponse;
import com.mundial2026.predictions.leagues.service.InviteService;
import com.mundial2026.predictions.leagues.service.LeagueService;
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

@Path("/api/leagues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Leagues", description = "Salas/grupos privados. Crea una sala, comparte el código de 6 caracteres con tus amigos y compitan entre sí.")
public class LeagueResource {

    @Inject LeagueService leagueService;
    @Inject InviteService inviteService;
    @Inject SecurityUtils securityUtils;

    @POST
    @Operation(summary = "Crear sala privada",
               description = "Crea una nueva sala. El sistema genera automáticamente un código único de 6 caracteres (ej: `ABC123`) para invitar participantes.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = "{ \"name\": \"Los Amigos de la Oficina\" }")))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Sala creada. El `code` en la respuesta es el código de invitación."),
        @APIResponse(responseCode = "400", description = "Nombre inválido (mínimo 3 caracteres).")
    })
    public Response create(@Valid LeagueRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        LeagueResponse league = leagueService.create(userId, req);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(league, "League created"))
                .build();
    }

    @POST
    @Path("/{code}/join")
    @Operation(summary = "Unirse a sala con código",
               description = "Únete a una sala usando el código de 6 caracteres que te compartieron. El código NO distingue mayúsculas/minúsculas.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Te uniste exitosamente a la sala."),
        @APIResponse(responseCode = "400", description = "Ya eres miembro de esta sala."),
        @APIResponse(responseCode = "404", description = "Código de sala no encontrado.")
    })
    public Response join(
            @Parameter(description = "Código de invitación de 6 caracteres", required = true, example = "ABC123")
            @PathParam("code") String code) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(leagueService.join(code, userId), "Joined league successfully")).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Detalle de sala",
               description = "Retorna los datos de la sala incluyendo la lista de miembros con sus fechas de ingreso.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Datos de la sala."),
        @APIResponse(responseCode = "404", description = "Sala no encontrada.")
    })
    public Response getById(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("id") Long id) {
        return Response.ok(ApiResponse.ok(leagueService.findById(id))).build();
    }

    @GET
    @Path("/{id}/ranking")
    @Operation(summary = "Ranking interno de sala",
               description = "Tabla de posiciones de los miembros. Solo cuentan los puntos de predicciones hechas a partir de la fecha en que cada usuario se unió a la sala.")
    @APIResponse(responseCode = "200", description = "Ranking real de la sala: posición, username, país, puntos y fecha de ingreso.")
    public Response ranking(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("id") Long id,
            @Parameter(description = "Límite de resultados", example = "50")
            @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(ApiResponse.ok(leagueService.getLeagueRanking(id, limit))).build();
    }

    // ── Invitación ────────────────────────────────────────────────

    @GET
    @Path("/{id}/invite/link")
    @Operation(summary = "Obtener link de invitación",
               description = "Devuelve la URL única de invitación para compartir por WhatsApp, redes sociales, etc. Formato: `http://tuapp.com/join/{CODE}`")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Link de invitación."),
        @APIResponse(responseCode = "404", description = "Sala no encontrada.")
    })
    public Response inviteLink(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("id") Long id) {
        String link = inviteService.getInviteLink(id);
        return Response.ok(ApiResponse.ok(link)).build();
    }

    @GET
    @Path("/{id}/invite/qr")
    @Produces("image/png")
    @Operation(summary = "Obtener QR de invitación",
               description = "Devuelve una imagen PNG con el QR de invitación. Úsalo directamente en el front: `<img src='/api/leagues/{id}/invite/qr' />`")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Imagen PNG del QR."),
        @APIResponse(responseCode = "404", description = "Sala no encontrada.")
    })
    public Response inviteQr(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("id") Long id,
            @Parameter(description = "Tamaño del QR en píxeles (200-600)", example = "300")
            @QueryParam("size") @DefaultValue("300") int size) {
        if (size < 100) size = 100;
        if (size > 600) size = 600;
        byte[] qr = inviteService.generateQrCode(id, size);
        return Response.ok(qr).type("image/png").build();
    }
}
