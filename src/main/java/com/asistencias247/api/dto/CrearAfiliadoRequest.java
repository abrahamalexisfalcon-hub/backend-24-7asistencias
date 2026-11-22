package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearAfiliadoRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @JsonAlias({"nombre", "nombreCompleto"})
    private String nombreCompleto;

    @JsonAlias({"dni"})
    private String dni;

    @JsonAlias({"telefono", "celular"})
    private String telefono;

    private String direccion;

    private String distrito;

    private String referencia;

    private String numeroCuenta;

    private List<DireccionAfiliadoDto> direcciones;

    @AssertTrue(message = "La dirección es obligatoria")
    @JsonIgnore
    public boolean isDireccionValida() {
        if (direccion != null && !direccion.isBlank()) {
            return true;
        }
        return direcciones != null && direcciones.stream()
                .anyMatch(d -> d != null && d.getDireccion() != null && !d.getDireccion().isBlank());
    }
}
