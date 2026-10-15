package com.asistencias247.api.dto;

import lombok.Builder;

@Builder
public record AuthResponseDto(
        String token,
        Long usuarioId,
        String nombreCompleto,
        String correo,
        String rol,
        Boolean disponible
) {}
