package com.mundial2026.predictions.shared.exception;

import com.mundial2026.predictions.shared.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;
import java.util.stream.Collectors;

public class GlobalExceptionHandler {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @ServerExceptionMapper
    public Response handleNotFound(NotFoundException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado";
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.of("NOT_FOUND", msg))
                .build();
    }

    @ServerExceptionMapper
    public Response handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + v.getMessage();
                })
                .sorted()
                .collect(Collectors.toList());

        String summary = details.size() == 1
                ? details.get(0)
                : "Se encontraron " + details.size() + " errores de validación";

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.of("ERROR_VALIDACION", summary, details))
                .build();
    }

    @ServerExceptionMapper
    public Response handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Solicitud inválida";
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.of("SOLICITUD_INVALIDA", msg))
                .build();
    }

    @ServerExceptionMapper
    public Response handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Operación no permitida";
        return Response.status(422)
                .entity(ErrorResponse.of("REGLA_DE_NEGOCIO", msg))
                .build();
    }

    @ServerExceptionMapper
    public Response handleUnauthorized(NotAuthorizedException ex) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ErrorResponse.of("NO_AUTENTICADO",
                        "Debes iniciar sesión para realizar esta acción. Envía un token Bearer válido en el header Authorization"))
                .build();
    }

    @ServerExceptionMapper
    public Response handleForbidden(ForbiddenException ex) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(ErrorResponse.of("SIN_PERMISOS",
                        "No tienes permisos para realizar esta acción"))
                .build();
    }

    @ServerExceptionMapper
    public Response handleSecurity(SecurityException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Acceso denegado";
        return Response.status(Response.Status.FORBIDDEN)
                .entity(ErrorResponse.of("ACCESO_DENEGADO", msg))
                .build();
    }

    @ServerExceptionMapper
    public Response handleMethodNotAllowed(NotAllowedException ex) {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity(ErrorResponse.of("METODO_NO_PERMITIDO",
                        "El método HTTP utilizado no está soportado para este endpoint"))
                .build();
    }

    @ServerExceptionMapper
    public Response handleGeneral(Exception ex) {
        LOG.errorf(ex, "Error inesperado: %s", ex.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ErrorResponse.of("ERROR_INTERNO",
                        "Ocurrió un error inesperado. Por favor intenta de nuevo más tarde."))
                .build();
    }
}
