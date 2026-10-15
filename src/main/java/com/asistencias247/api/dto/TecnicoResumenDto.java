package com.asistencias247.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TecnicoResumenDto {

    private Long id;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String especialidad;
    private Boolean activo;
    private Boolean disponible;
}
