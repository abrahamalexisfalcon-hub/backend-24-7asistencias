package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelarOrdenRequest {

    @JsonAlias({"motivo", "motivoAnulacion", "observacion", "observacionCancelacion"})
    private String motivoCancelacion;

    /**
     * Retorna el motivo u observación de la anulación limpio.
     */
    public String getMotivoEfectivo() {
        if (motivoCancelacion != null && !motivoCancelacion.isBlank()) {
            return motivoCancelacion.trim();
        }
        return "";
    }
}
