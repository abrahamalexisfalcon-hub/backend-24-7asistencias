package com.asistencias247.api.dto;

import lombok.Builder;

@Builder
public record PerfilTecnicoDto(
        Long id,
        String nombreCompleto,
        String correo,
        String telefono,
        String rol,
        Boolean disponible,
        Long ordenesCompletadasHoy,
        Double calificacionPromedio,
        String firmaUrl,
        String firmaDigitalUrl
) {}

