package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearTecnicoRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String correo;

    @NotBlank(message = "El PIN de acceso es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "El PIN de acceso debe contener exactamente 4 dígitos numéricos")
    @JsonAlias({"pin"})
    private String password;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "El teléfono debe contener solo números (9 dígitos)")
    private String telefono;

    @NotBlank(message = "La especialidad es obligatoria")
    private String especialidad;
}
