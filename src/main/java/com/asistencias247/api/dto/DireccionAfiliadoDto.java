package com.asistencias247.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionAfiliadoDto {

    private Long id;
    private String etiqueta;
    private String direccion;
    private String distrito;
    private String referencia;
    private Boolean esPrincipal;
}
