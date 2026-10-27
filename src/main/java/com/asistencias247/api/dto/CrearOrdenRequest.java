package com.asistencias247.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

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

    @NotNull(message = "La cuenta o aseguradora es obligatoria")
    private Long cuentaId;

    @NotBlank(message = "El número de caso es obligatorio")
    @Size(max = 50, message = "El número de caso no puede exceder los 50 caracteres")
    private String numeroCaso;

    @NotBlank(message = "El número de OT es obligatorio")
    @Size(max = 50, message = "El número de OT no puede exceder los 50 caracteres")
    private String numeroOt;

    @NotNull(message = "La fecha y hora pactada es obligatoria")
    @FutureOrPresent(message = "La fecha y hora pactada no puede estar en el pasado")
    private LocalDateTime fechaPactada;

    public void setFechaPactada(LocalDateTime fechaPactada) {
        if (fechaPactada != null && fechaPactada.getSecond() == 0 && fechaPactada.getNano() == 0) {
            this.fechaPactada = fechaPactada.withSecond(59);
        } else {
            this.fechaPactada = fechaPactada;
        }
    }
}
