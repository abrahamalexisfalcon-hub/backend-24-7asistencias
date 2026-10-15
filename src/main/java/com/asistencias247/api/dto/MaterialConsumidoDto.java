package com.asistencias247.api.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialConsumidoDto {

    private Long materialId;
    private String descripcion;

    @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
    private Integer cantidad;
}
