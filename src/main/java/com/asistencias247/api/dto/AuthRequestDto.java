package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record AuthRequestDto(
        @NotBlank(message = "El correo o número de celular es obligatorio")
        @JsonAlias({"identificador", "telefono", "celular", "username"})
        String correo,

        @NotBlank(message = "La contraseña o PIN es obligatorio")
        @JsonAlias({"pin", "clave"})
        String password
) {}
