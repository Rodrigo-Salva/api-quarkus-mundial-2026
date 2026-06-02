package com.mundial2026.predictions.auth.controller;

import com.mundial2026.predictions.auth.dto.AuthResponse;
import com.mundial2026.predictions.auth.dto.LoginRequest;
import com.mundial2026.predictions.auth.dto.RegisterRequest;
import com.mundial2026.predictions.auth.entity.User;
import com.mundial2026.predictions.auth.service.AuthService;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Registro, login y gestión de sesión. El token JWT obtenido debe enviarse en el header `Authorization: Bearer <token>`.")
@SecurityScheme(
        securitySchemeName = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingresa el accessToken obtenido en /api/auth/login"
)
public class AuthResource {

    @Inject AuthService authService;
    @Inject SecurityUtils securityUtils;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Registrar usuario",
               description = "Crea una cuenta nueva. El `country` debe ser un código ISO-3166 de 2-3 letras (ej: PER, ARG, BRA).")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = RegisterRequest.class),
            examples = @ExampleObject(name = "Ejemplo",
                    value = """
                    {
                      "email": "rodrigo@example.com",
                      "username": "rodrigo26",
                      "password": "MiPassword123",
                      "country": "PER"
                    }
                    """)))
    @APIResponses({
        @APIResponse(responseCode = "201", description = "Usuario registrado. Retorna accessToken y refreshToken."),
        @APIResponse(responseCode = "400", description = "Email ya registrado, username ya en uso, o validación fallida."),
        @APIResponse(responseCode = "422", description = "Datos con formato incorrecto.")
    })
    public Response register(@Valid RegisterRequest req) {
        AuthResponse auth = authService.register(req);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(auth, "User registered successfully"))
                .build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Iniciar sesión",
               description = "Retorna un `accessToken` (válido 15 min) y un `refreshToken` (válido 7 días). Guarda ambos en el front.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(name = "Ejemplo",
                    value = "{ \"email\": \"rodrigo@example.com\", \"password\": \"MiPassword123\" }")))
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Login exitoso. Usar `data.accessToken` en el header Authorization."),
        @APIResponse(responseCode = "400", description = "Credenciales inválidas.")
    })
    public Response login(@Valid LoginRequest req) {
        return Response.ok(ApiResponse.ok(authService.login(req))).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Renovar access token",
               description = "Envía el `refreshToken` en el body (texto plano) para obtener un nuevo `accessToken` sin tener que hacer login de nuevo.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Nuevo accessToken generado."),
        @APIResponse(responseCode = "400", description = "Refresh token inválido o expirado.")
    })
    public Response refresh(String refreshToken) {
        return Response.ok(ApiResponse.ok(authService.refresh(refreshToken))).build();
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Cerrar sesión",
               description = "Invalida la sesión actual. Desde el front, elimina el token almacenado.")
    @APIResponse(responseCode = "200", description = "Sesión cerrada.")
    public Response logout() {
        return Response.ok(ApiResponse.ok(null, "Logged out successfully")).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Obtener usuario autenticado",
               description = "Retorna los datos del usuario dueño del token JWT actual.")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Datos del usuario: id, email, username, country, role."),
        @APIResponse(responseCode = "401", description = "Token ausente o inválido.")
    })
    public Response me() {
        Long userId = securityUtils.getCurrentUserId();
        User user = authService.findById(userId);
        var userInfo = new AuthResponse.UserInfo(user.id, user.email, user.username, user.country, user.role);
        return Response.ok(ApiResponse.ok(userInfo)).build();
    }
}
