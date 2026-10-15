package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearMaterialRequest {

    @NotBlank(message = "El nombre del material es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre del material debe tener entre 2 y 150 caracteres")
    private String nombre;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 50, message = "La unidad no debe exceder 50 caracteres")
    private String unidad;

    @NotNull(message = "El ID de la especialidad es obligatorio")
    private Long especialidadId;
}
