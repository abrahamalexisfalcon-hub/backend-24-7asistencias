package com.asistencias247.api.dto;

import com.asistencias247.api.entity.EstadoOrden;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenResumenTecnicoDto {

    private Long id;
    private String codigoOrden;
    private String tipoServicio;
    private EstadoOrden estado;
    private String nombreAfiliado;
    private String direccion;
    private String referencia;
    private String distrito;
    private String dniAfiliado;
    private String codigoCuenta;
    private String fechaProgramada;
    private String fechaCreacion;
    private String fechaAsignacion;
    private String horaLlegada;
    private String tecnicoNombre;
    private String tecnicoFirmaUrl;
    private String firmaDigitalUrl;
    private String firmaAfiliadoUrl;
    private String evaluacionAveria;
    private String detalleSolucion;
    private String sugerencias;
    private Integer calificacion;
    private String fotoInicialUrl;
    private String fotoFinalUrl;
    private java.util.List<String> fotosEvidencia;
    private Long hojaServicioId;
    private java.util.List<MaterialConsumidoDto> materiales;
}

