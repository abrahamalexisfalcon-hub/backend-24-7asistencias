package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearMaterialCatalogoRequest {

    @NotBlank(message = "El nombre del material es obligatorio")
    private String nombre;
}
