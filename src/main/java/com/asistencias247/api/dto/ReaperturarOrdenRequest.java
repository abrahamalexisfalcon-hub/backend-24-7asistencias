package com.asistencias247.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReaperturarOrdenRequest {

    @NotNull(message = "La nueva fecha y hora pactada es obligatoria")
    @FutureOrPresent(message = "La nueva fecha y hora pactada no puede estar en el pasado")
    private LocalDateTime nuevaFechaPactada;

    private LocalDateTime fechaPactada;

    @NotNull(message = "El técnico asignado es obligatorio")
    private Long tecnicoId;

    @Size(max = 500, message = "La observación no puede exceder los 500 caracteres")
    private String observacion;

    public void setNuevaFechaPactada(LocalDateTime nuevaFechaPactada) {
        if (nuevaFechaPactada != null && nuevaFechaPactada.getSecond() == 0 && nuevaFechaPactada.getNano() == 0) {
            this.nuevaFechaPactada = nuevaFechaPactada.withSecond(59);
        } else {
            this.nuevaFechaPactada = nuevaFechaPactada;
        }
    }

    public void setFechaPactada(LocalDateTime fechaPactada) {
        if (fechaPactada != null && fechaPactada.getSecond() == 0 && fechaPactada.getNano() == 0) {
            this.fechaPactada = fechaPactada.withSecond(59);
        } else {
            this.fechaPactada = fechaPactada;
        }
        if (this.nuevaFechaPactada == null) {
            this.nuevaFechaPactada = this.fechaPactada;
        }
    }

    public LocalDateTime getFechaPactadaEfectiva() {
        return nuevaFechaPactada != null ? nuevaFechaPactada : fechaPactada;
    }
}
