package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearCuentaRequest {

    @NotBlank(message = "El nombre de la cuenta es obligatorio")
    @Size(max = 120, message = "El nombre de la cuenta no puede exceder los 120 caracteres")
    private String nombre;
}
