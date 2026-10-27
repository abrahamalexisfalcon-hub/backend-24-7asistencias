package com.asistencias247.api.mapper;

import com.asistencias247.api.dto.CierreHojaServicioResponseDto;
import com.asistencias247.api.dto.MaterialConsumidoDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.entity.MaterialServicio;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class HojaServicioMapper {

    public CierreHojaServicioResponseDto toCierreResponse(Long ordenId, HojaServicio hoja, String mensaje) {
        List<MaterialConsumidoDto> mats = hoja.getMateriales() != null ? hoja.getMateriales().stream()
                .map(this::toMaterialConsumidoDto)
                .toList() : Collections.emptyList();

        List<String> fotos = new ArrayList<>();
        if (hoja.getFotoInicialUrl() != null && !hoja.getFotoInicialUrl().isBlank()) {
            fotos.add(hoja.getFotoInicialUrl().trim());
        }
        if (hoja.getFotoFinalUrl() != null && !hoja.getFotoFinalUrl().isBlank()) {
            String finalUrl = hoja.getFotoFinalUrl().trim();
            if (!fotos.contains(finalUrl)) {
                fotos.add(finalUrl);
            }
        }

        String fechaLlegadaIso = hoja.getOrdenServicio() != null && hoja.getOrdenServicio().getFechaLlegada() != null
                ? hoja.getOrdenServicio().getFechaLlegada().toString()
                : null;
        String fechaFinIso = hoja.getOrdenServicio() != null && hoja.getOrdenServicio().getFechaFinalizacion() != null
                ? hoja.getOrdenServicio().getFechaFinalizacion().toString()
                : null;

        return CierreHojaServicioResponseDto.builder()
                .mensaje(mensaje)
                .hojaServicioId(hoja.getId())
                .ordenId(ordenId)
                .estado(EstadoOrden.FINALIZADA)
                .fechaLlegada(fechaLlegadaIso)
                .fechaFinalizacion(fechaFinIso)
                .firmaDigitalUrl(hoja.getFirmaDigitalUrl())
                .firmaAfiliadoUrl(hoja.getFirmaDigitalUrl())
                .firmaClienteUrl(hoja.getFirmaDigitalUrl())
                .fotoInicialUrl(hoja.getFotoInicialUrl())
                .fotoFinalUrl(hoja.getFotoFinalUrl())
                .fotosEvidencia(fotos)
                .evaluacionAveria(hoja.getEvaluacionAveria())
                .detalleSolucion(hoja.getDetalleSolucion())
                .sugerencias(hoja.getSugerencias())
                .calificacion(hoja.getCalificacion())
                .materiales(mats)
                .build();
    }

    public MaterialConsumidoDto toMaterialConsumidoDto(MaterialServicio m) {
        return MaterialConsumidoDto.builder()
                .materialId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                .descripcion(m.getDescripcion())
                .cantidad(m.getCantidad())
                .build();
    }
}
