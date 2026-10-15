package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelarOrdenRequest {

    @JsonAlias({"motivo", "motivoAnulacion"})
    private String motivoCancelacion;

    /**
     * Retorna el motivo de la anulación limpio.
     */
    public String getMotivoEfectivo() {
        if (motivoCancelacion != null && !motivoCancelacion.isBlank()) {
            return motivoCancelacion.trim();
        }
        return "";
    }
}
