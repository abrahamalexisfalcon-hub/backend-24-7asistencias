package com.asistencias247.api.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarCuentaRequest {

    @Size(max = 120, message = "El nombre de la cuenta no puede exceder los 120 caracteres")
    private String nombre;

    private Boolean activo;
}
