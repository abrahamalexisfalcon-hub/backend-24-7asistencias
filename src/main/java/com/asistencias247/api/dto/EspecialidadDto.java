package com.asistencias247.api.dto;

import lombok.Builder;

@Builder
public record EspecialidadDto(
        Long id,
        String nombre,
        Boolean activo
) {}
