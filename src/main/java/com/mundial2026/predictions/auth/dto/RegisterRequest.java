package com.mundial2026.predictions.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato del correo electrónico no es válido")
        String email,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50,
                message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                message = "El nombre de usuario solo puede contener letras, números y guiones bajos")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).+$",
                message = "La contraseña debe contener al menos una mayúscula y un número")
        String password,

        @NotBlank(message = "El país es obligatorio")
        @Size(min = 2, max = 3,
                message = "El país debe ser un código ISO de 2 o 3 letras (ej: PER, ARG)")
        @Pattern(regexp = "^[A-Z]{2,3}$",
                message = "El código de país debe estar en mayúsculas (ej: PER, ARG, BRA)")
        String country
) {}
