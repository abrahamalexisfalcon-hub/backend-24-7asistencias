package com.asistencias247.api.dto;

import lombok.Builder;

@Builder
public record MaterialDto(
        Long id,
        String nombre,
        String unidad,
        Boolean activo,
        Long especialidadId,
        String especialidadNombre
) {}
