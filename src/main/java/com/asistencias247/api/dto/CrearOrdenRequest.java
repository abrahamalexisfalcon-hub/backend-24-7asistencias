package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearOrdenRequest {

    @NotNull(message = "El ID del afiliado es obligatorio")
    private Long afiliadoId;

    private Long tecnicoId;

    @NotBlank(message = "El tipo de servicio es obligatorio")
    private String tipoServicio;
}
