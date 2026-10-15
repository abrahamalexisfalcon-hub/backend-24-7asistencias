package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarEstadoTecnicoRequest {

    @NotNull(message = "El estado activo es obligatorio")
    @JsonAlias({"estado", "estaActivo"})
    private Boolean activo;
}
