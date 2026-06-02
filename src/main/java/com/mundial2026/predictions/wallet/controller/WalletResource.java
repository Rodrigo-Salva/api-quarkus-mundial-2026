package com.mundial2026.predictions.wallet.controller;

import com.mundial2026.predictions.wallet.dto.DepositRequest;
import com.mundial2026.predictions.wallet.dto.WithdrawRequest;
import com.mundial2026.predictions.wallet.service.WalletService;
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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/wallet")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Wallet", description = "Billetera simulada en PEN. Los depósitos y retiros son ficticios (sin pasarela real). Se requiere KYC aprobado para retirar.")
public class WalletResource {

    @Inject WalletService walletService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Ver saldo",
               description = "Retorna el saldo actual en PEN. Si no tienes wallet, se crea automáticamente con saldo 0.")
    @APIResponse(responseCode = "200", description = "Saldo actual: `{ userId, balance, currency, updatedAt }`.")
    public Response getBalance() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(walletService.getBalance(userId))).build();
    }

    @POST
    @Path("/deposit")
    @Operation(summary = "Depositar saldo (simulado)",
               description = "Añade saldo a tu wallet. Operación simulada — no involucra dinero real. Si tienes límite de depósito diario configurado en KYC, se respeta.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = "{ \"amount\": 100.00 }")))
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Saldo actualizado."),
        @APIResponse(responseCode = "422", description = "Supera el límite de depósito diario configurado.")
    })
    public Response deposit(@Valid DepositRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(
                walletService.deposit(userId, req.amount()), "Deposit successful")).build();
    }

    @POST
    @Path("/withdraw")
    @Operation(summary = "Retirar saldo (simulado)",
               description = "Retira saldo de tu wallet. Requiere KYC aprobado y saldo suficiente.")
    @RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            examples = @ExampleObject(value = "{ \"amount\": 50.00 }")))
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Retiro procesado."),
        @APIResponse(responseCode = "422", description = "Saldo insuficiente o KYC no aprobado.")
    })
    public Response withdraw(@Valid WithdrawRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(
                walletService.withdraw(userId, req.amount()), "Withdrawal successful")).build();
    }

    @GET
    @Path("/transactions")
    @Operation(summary = "Historial de movimientos",
               description = "Lista todas las transacciones: DEPOSIT, WITHDRAWAL, BET_PLACED, BET_WON, BET_REFUND. Ordenadas de más reciente a más antigua.")
    @APIResponse(responseCode = "200", description = "Lista de transacciones con balanceBefore y balanceAfter.")
    public Response transactions() {
        Long userId = securityUtils.getCurrentUserId();
        return Response.ok(ApiResponse.ok(walletService.getTransactions(userId))).build();
    }
}
