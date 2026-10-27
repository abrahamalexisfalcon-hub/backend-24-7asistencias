package com.asistencias247.api.dto;

import com.asistencias247.api.entity.EstadoOrden;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreHojaServicioResponseDto {
    private String mensaje;
    private Long hojaServicioId;
    private Long ordenId;
    private EstadoOrden estado;
    private String fechaLlegada;
    private String fechaFinalizacion;
    private String firmaDigitalUrl;
    private String firmaAfiliadoUrl;
    private String firmaClienteUrl;
    private String fotoInicialUrl;
    private String fotoFinalUrl;
    private List<String> fotosEvidencia;
    private String evaluacionAveria;
    private String detalleSolucion;
    private String sugerencias;
    private Integer calificacion;
    private List<MaterialConsumidoDto> materiales;
}
