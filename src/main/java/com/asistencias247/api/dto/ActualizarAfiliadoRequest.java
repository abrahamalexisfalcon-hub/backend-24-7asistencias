package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarAfiliadoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @JsonAlias({"nombre", "nombreCompleto"})
    private String nombreCompleto;

    @JsonAlias({"dni"})
    private String dni;

    @JsonAlias({"telefono", "celular"})
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    private String distrito;

    private String referencia;
}
