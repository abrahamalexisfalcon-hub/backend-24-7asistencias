package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AsistidoNoRealizadoDTO(
        @NotBlank(message = "El motivo es obligatorio")
        String motivo
) {
    @JsonCreator
    public static AsistidoNoRealizadoDTO fromJson(
            @JsonProperty("motivo") String motivo,
            @JsonProperty("motivoCancelacion") String motivoCancelacion,
            @JsonProperty("motivoAnulacion") String motivoAnulacion,
            @JsonProperty("observacion") String observacion
    ) {
        String valor = (motivo != null && !motivo.isBlank()) ? motivo
                : (motivoCancelacion != null && !motivoCancelacion.isBlank()) ? motivoCancelacion
                : (motivoAnulacion != null && !motivoAnulacion.isBlank()) ? motivoAnulacion
                : observacion;
        return new AsistidoNoRealizadoDTO(valor);
    }
}
