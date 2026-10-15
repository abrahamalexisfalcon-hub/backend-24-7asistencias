package com.asistencias247.api.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaServicioDetalleAdminDto {

    private Long id;
    private String evaluacionAveria;
    private String detalleSolucion;
    private String sugerencias;
    private Integer calificacion;
    private String firmaDigitalUrl;
    private String firmaTecnico;
    private String firmaTecnicoUrl;
    private String fotoInicialUrl;
    private String fotoFinalUrl;
    private String pdfActaUrl;

    @Builder.Default
    private List<String> fotosEvidencia = new ArrayList<>();

    @Builder.Default
    private List<MaterialDetalleAdminDto> materiales = new ArrayList<>();
}
