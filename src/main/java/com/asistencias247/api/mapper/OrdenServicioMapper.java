package com.asistencias247.api.mapper;

import com.asistencias247.api.dto.HojaServicioDetalleAdminDto;
import com.asistencias247.api.dto.MaterialDetalleAdminDto;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.entity.MaterialServicio;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.Usuario;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrdenServicioMapper {

    public OrdenDetalleAdminDto toDetalleAdmin(OrdenServicio orden) {
        if (orden == null) {
            return null;
        }

        OrdenDetalleAdminDto.OrdenDetalleAdminDtoBuilder builder = OrdenDetalleAdminDto.builder()
                .id(orden.getId())
                .codigoOrden(orden.getCodigoOrden())
                .numeroCaso(orden.getNumeroCaso())
                .numeroOt(orden.getNumeroOt())
                .fechaPactada(orden.getFechaPactada())
                .tipoServicio(orden.getTipoServicio())
                .estado(orden.getEstado())
                .fechaAsignacion(orden.getFechaAsignacion())
                .fechaLlegada(orden.getFechaLlegada())
                .fechaFinalizacion(orden.getFechaFinalizacion())
                .motivoCancelacion(orden.getMotivoCancelacion())
                .fechaCancelacion(orden.getFechaCancelacion());

        if (orden.getCuenta() != null) {
            com.asistencias247.api.entity.Cuenta cta = orden.getCuenta();
            builder.cuentaId(cta.getId())
                    .cuentaNombre(cta.getNombre())
                    .cuenta(com.asistencias247.api.dto.CuentaDto.builder()
                            .id(cta.getId())
                            .nombre(cta.getNombre())
                            .activo(cta.getActivo())
                            .createdAt(cta.getCreatedAt())
                            .updatedAt(cta.getUpdatedAt())
                            .build());
        }

        if (orden.getAfiliado() != null) {
            Afiliado af = orden.getAfiliado();
            builder.afiliadoId(af.getId())
                    .nombreAfiliado(af.getNombreCompleto())
                    .dniAfiliado(af.getDni())
                    .numeroCuentaAfiliado(af.getNumeroCuenta())
                    .telefonoAfiliado(af.getTelefono())
                    .direccionAfiliado(af.getDireccion())
                    .distritoAfiliado(af.getDistrito())
                    .referenciaAfiliado(af.getReferencia())
                    .direccion(af.getDireccion())
                    .distrito(af.getDistrito())
                    .cliente(toClienteInfoAdminDto(af));
        }

        String tecFirmaUrl = null;
        if (orden.getTecnico() != null) {
            Usuario tec = orden.getTecnico();
            tecFirmaUrl = tec.getFirmaUrl();
            builder.tecnicoId(tec.getId())
                    .nombreTecnico(tec.getNombreCompleto())
                    .tecnicoNombre(tec.getNombreCompleto())
                    .correoTecnico(tec.getCorreo())
                    .telefonoTecnico(tec.getTelefono())
                    .tecnicoFirmaUrl(tecFirmaUrl)
                    .firmaTecnico(tecFirmaUrl)
                    .firmaTecnicoUrl(tecFirmaUrl)
                    .tecnico(toTecnicoInfoAdminDto(tec));
        }

        java.time.LocalDateTime fechaBase = orden.getFechaCreacion() != null 
                ? orden.getFechaCreacion() 
                : orden.getFechaAsignacion();
        if (fechaBase != null) {
            builder.fechaCreacion(fechaBase.toString());
            builder.fechaProgramada(orden.getFechaPactada() != null ? orden.getFechaPactada().toString() : fechaBase.toString());
        } else if (orden.getFechaPactada() != null) {
            builder.fechaProgramada(orden.getFechaPactada().toString());
        }

        if (orden.getHojaServicio() != null) {
            HojaServicio hoja = orden.getHojaServicio();
            List<MaterialDetalleAdminDto> materialesDto = new ArrayList<>();

            if (hoja.getMateriales() != null) {
                materialesDto = hoja.getMateriales().stream()
                        .map(this::toMaterialDetalleAdminDto)
                        .toList();
            }

            List<String> fotosEvidencia = new ArrayList<>();
            if (hoja.getFotoInicialUrl() != null && !hoja.getFotoInicialUrl().isBlank()) {
                fotosEvidencia.add(hoja.getFotoInicialUrl());
            }
            if (hoja.getFotoFinalUrl() != null && !hoja.getFotoFinalUrl().isBlank()) {
                fotosEvidencia.add(hoja.getFotoFinalUrl());
            }

            HojaServicioDetalleAdminDto hojaDto = HojaServicioDetalleAdminDto.builder()
                    .id(hoja.getId())
                    .evaluacionAveria(hoja.getEvaluacionAveria())
                    .detalleSolucion(hoja.getDetalleSolucion())
                    .sugerencias(hoja.getSugerencias())
                    .calificacion(hoja.getCalificacion())
                    .firmaDigitalUrl(hoja.getFirmaDigitalUrl())
                    .firmaTecnico(tecFirmaUrl)
                    .firmaTecnicoUrl(tecFirmaUrl)
                    .fotoInicialUrl(hoja.getFotoInicialUrl())
                    .fotoFinalUrl(hoja.getFotoFinalUrl())
                    .fotosEvidencia(fotosEvidencia)
                    .pdfActaUrl(hoja.getPdfActaUrl())
                    .materiales(materialesDto)
                    .build();

            builder.hojaServicio(hojaDto);
            builder.fotosEvidencia(fotosEvidencia);
            builder.fotoInicialUrl(hoja.getFotoInicialUrl());
            builder.fotoFinalUrl(hoja.getFotoFinalUrl());
        }

        return builder.build();
    }

    public OrdenDetalleAdminDto.ClienteInfoAdminDto toClienteInfoAdminDto(Afiliado af) {
        if (af == null) return null;
        return OrdenDetalleAdminDto.ClienteInfoAdminDto.builder()
                .id(af.getId())
                .nombre(af.getNombreCompleto())
                .direccion(af.getDireccion())
                .referencia(af.getReferencia())
                .distrito(af.getDistrito())
                .telefono(af.getTelefono())
                .build();
    }

    public OrdenDetalleAdminDto.TecnicoInfoAdminDto toTecnicoInfoAdminDto(Usuario tec) {
        if (tec == null) return null;
        return OrdenDetalleAdminDto.TecnicoInfoAdminDto.builder()
                .id(tec.getId())
                .nombre(tec.getNombreCompleto())
                .email(tec.getCorreo())
                .telefono(tec.getTelefono())
                .firmaUrl(tec.getFirmaUrl())
                .firmaDigitalUrl(tec.getFirmaUrl())
                .firmaDigital(tec.getFirmaUrl())
                .firmaTecnico(tec.getFirmaUrl())
                .build();
    }

    public MaterialDetalleAdminDto toMaterialDetalleAdminDto(MaterialServicio m) {
        if (m == null) return null;
        return MaterialDetalleAdminDto.builder()
                .id(m.getId())
                .materialId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                .materialCatalogoId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                .nombreCatalogo(m.getMaterial() != null ? m.getMaterial().getNombre() : null)
                .esPersonalizado(m.getEsPersonalizado())
                .descripcion(m.getDescripcion())
                .cantidad(m.getCantidad())
                .origen(Boolean.TRUE.equals(m.getEsPersonalizado()) ? "OTRO" : "CATALOGO")
                .build();
    }
}
