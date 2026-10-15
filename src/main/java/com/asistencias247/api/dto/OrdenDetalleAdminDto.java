package com.asistencias247.api.dto;

import com.asistencias247.api.entity.EstadoOrden;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenDetalleAdminDto {

    private Long id;
    private String codigoOrden;
    private String tipoServicio;
    private EstadoOrden estado;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaLlegada;
    private LocalDateTime fechaFinalizacion;

    // Datos del afiliado (incluye teléfono para gestión de administración)
    private Long afiliadoId;
    private String nombreAfiliado;
    private String dniAfiliado;
    private String numeroCuentaAfiliado;
    private String telefonoAfiliado;
    private String direccionAfiliado;
    private String distritoAfiliado;
    private String referenciaAfiliado;

    // Datos del técnico asignado
    private Long tecnicoId;
    private String nombreTecnico;
    private String correoTecnico;
    private String telefonoTecnico;

    // Hoja de servicio asociada
    private HojaServicioDetalleAdminDto hojaServicio;

    // Firma digital del técnico
    private String tecnicoFirmaUrl;
    private String firmaTecnico;
    private String firmaTecnicoUrl;

    // Datos de anulación / cancelación
    private String motivoCancelacion;
    private LocalDateTime fechaCancelacion;

    // Aliases para compatibilidad directa con frontend
    private String tecnicoNombre;
    private String fechaCreacion;
    private String fechaProgramada;
    private String direccion;
    private String distrito;
    private String fotoInicialUrl;
    private String fotoFinalUrl;

    @Builder.Default
    private java.util.List<String> fotosEvidencia = new java.util.ArrayList<>();

    private TecnicoInfoAdminDto tecnico;
    private ClienteInfoAdminDto cliente;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TecnicoInfoAdminDto {
        private Long id;
        private String nombre;
        private String email;
        private String telefono;
        private String firmaUrl;
        private String firmaDigitalUrl;
        private String firmaDigital;
        private String firmaTecnico;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClienteInfoAdminDto {
        private Long id;
        private String nombre;
        private String direccion;
        private String referencia;
        private String distrito;
        private String telefono;
    }
}
