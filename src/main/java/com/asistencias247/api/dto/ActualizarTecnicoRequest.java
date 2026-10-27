package com.asistencias247.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarTecnicoRequest {

    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String especialidad;
    private String password;
}
