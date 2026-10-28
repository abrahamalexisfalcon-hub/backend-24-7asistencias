package com.asistencias247.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreHojaServicioRequest {

    private String evaluacionAveria;
    private String detalleSolucion;
    private String sugerencias;

    /**
     * DNI del afiliado capturado opcionalmente por el técnico durante la atención.
     * Si viene vacío, se conserva el valor previamente registrado.
     */
    @JsonAlias({"dni", "dniCliente", "documentoAfiliado"})
    private String dniAfiliado;

    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    @JsonAlias({"calificacionAfiliado", "calificacion"})
    private Integer calificacion;

    @JsonAlias({"firmaAfiliadoUrl", "firmaDigitalUrl", "firmaUrl", "firmaClienteUrl", "firma"})
    private String firmaDigitalUrl;

    @JsonAlias({"tecnicoFirmaUrl", "firmaTecnicoUrl", "firmaTecnico", "tecnicoFirma"})
    private String firmaTecnicoUrl;

    @JsonAlias({"fotoAntesUrl", "fotoInicialUrl"})
    private String fotoInicialUrl;

    @JsonAlias({"fotoDespuesUrl", "fotoFinalUrl"})
    private String fotoFinalUrl;

    @Valid
    @Builder.Default
    private List<MaterialConsumidoDto> materiales = new ArrayList<>();
}
