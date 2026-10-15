package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearEspecialidadRequest {

    @NotBlank(message = "El nombre de la especialidad es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre de la especialidad debe tener entre 2 y 100 caracteres")
    private String nombre;
}
