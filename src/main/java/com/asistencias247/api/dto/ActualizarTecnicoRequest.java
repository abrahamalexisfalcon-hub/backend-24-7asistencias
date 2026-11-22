package com.asistencias247.api.dto;

import lombok.*;

import java.util.List;

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
    private List<Long> especialidadIds;
    private List<String> especialidades;
    private String password;
}
