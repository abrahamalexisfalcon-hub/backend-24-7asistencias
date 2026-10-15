package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarDisponibilidadRequest(
        @NotNull(message = "El campo disponible es obligatorio")
        Boolean disponible
) {}
