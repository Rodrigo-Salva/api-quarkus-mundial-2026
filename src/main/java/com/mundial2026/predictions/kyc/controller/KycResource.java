package com.mundial2026.predictions.kyc.controller;

import com.mundial2026.predictions.kyc.dto.KycSubmitRequest;
import com.mundial2026.predictions.kyc.dto.LimitRequest;
import com.mundial2026.predictions.kyc.dto.SelfExclusionRequest;
import com.mundial2026.predictions.kyc.service.KycService;
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

@Path("/api/kyc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "KYC", description = "Verificación de identidad y juego responsable. Flujo: submit → esperar aprobación ADMIN → aprobado. Sin KYC aprobado no puedes retirar fondos.")
public class KycResource {

    @Inject KycService    kycService;
    @Inject SecurityUtils securityUtils;

    @POST
    @Path("/submit")
    @Operation(summary = "Enviar documentos KYC",
               description = "Inicia el proceso de verificación. El `documentType` puede ser: DNI, PASSPORT, CE (carnet de extranjería).")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = "{ \"documentType\": \"DNI\", \"documentNumber\": \"12345678\" }")))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "KYC enviado con estado PENDING. Esperar aprobación de un ADMIN."),
        @APIResponse(responseCode = "400", description = "Ya enviaste tu KYC anteriormente.")
    })
    public Response submit(@Valid KycSubmitRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(kycService.submit(userId, req), "KYC submitted successfully"))
                .build();
    }

    @GET
    @Path("/status")
    @Operation(summary = "Estado de verificación KYC",
               description = "Consulta si tu KYC está PENDING, APPROVED o REJECTED.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Estado KYC: `{ status, documentType, verifiedAt }`."),
        @APIResponse(responseCode = "404", description = "Aún no enviaste tu KYC.")
    })
    public Response status() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(kycService.getStatus(userId))).build();
    }

    @POST
    @Path("/limits")
    @Operation(summary = "Configurar límite de juego",
               description = """
               Establece límites de depósito para juego responsable.
               Tipos de límite disponibles:
               - `DAILY_DEPOSIT` — máximo a depositar por día
               - `WEEKLY_DEPOSIT` — máximo a depositar por semana
               - `MONTHLY_DEPOSIT` — máximo a depositar por mes
               """)
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = "{ \"limitType\": \"DAILY_DEPOSIT\", \"amount\": 200.00 }")))
    @APIResponse(responseCode = "200", description = "Límite configurado.")
    public Response setLimit(@Valid LimitRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        kycService.setLimit(userId, req);
        return Response.ok(ApiResponse.ok(null, "Limit set successfully")).build();
    }

    @GET
    @Path("/limits")
    @Operation(summary = "Ver mis límites de juego")
    @APIResponse(responseCode = "200", description = "Lista de límites configurados.")
    public Response getLimits() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(kycService.getLimits(userId))).build();
    }

    @POST
    @Path("/self-exclude")
    @Operation(summary = "Auto-exclusión",
               description = "Bloquea tu propia cuenta para apostar. Si `endDate` es null, la exclusión es indefinida. Una vez activa no puedes colocar apuestas.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = {
                @ExampleObject(name = "Temporal",
                        value = "{ \"endDate\": \"2026-12-31T00:00:00\", \"reason\": \"Necesito un descanso\" }"),
                @ExampleObject(name = "Indefinida",
                        value = "{ \"endDate\": null, \"reason\": \"Exclusion permanente\" }")
            }))
    @APIResponse(responseCode = "200", description = "Auto-exclusión aplicada.")
    public Response selfExclude(SelfExclusionRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        kycService.selfExclude(userId, req);
        return Response.ok(ApiResponse.ok(null, "Self-exclusion applied")).build();
    }

    @PUT
    @Path("/approve/{userId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Aprobar KYC (ADMIN)",
               description = "Aprueba el KYC de un usuario. Solo disponible para administradores.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "KYC aprobado."),
        @APIResponse(responseCode = "404", description = "Usuario no tiene KYC enviado.")
    })
    public Response approve(
            @Parameter(description = "ID del usuario a aprobar", required = true)
            @PathParam("userId") Long userId) {
        return Response.ok(ApiResponse.ok(kycService.approve(userId), "KYC approved")).build();
    }
}
