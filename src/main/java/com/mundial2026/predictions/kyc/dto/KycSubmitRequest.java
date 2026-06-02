package com.mundial2026.predictions.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KycSubmitRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Pattern(regexp = "^(DNI|PASSPORT|CE)$",
                message = "El tipo de documento debe ser DNI, PASSPORT o CE")
        String documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(min = 6, max = 20,
                message = "El número de documento debe tener entre 6 y 20 caracteres")
        @Pattern(regexp = "^[A-Z0-9]+$",
                message = "El número de documento solo puede contener letras mayúsculas y números")
        String documentNumber
) {}
