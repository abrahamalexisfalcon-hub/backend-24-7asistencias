package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAfiliadoRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @JsonAlias({"nombre", "nombreCompleto"})
    private String nombreCompleto;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    private String distrito;

    private String referencia;

    private String numeroCuenta;
}
