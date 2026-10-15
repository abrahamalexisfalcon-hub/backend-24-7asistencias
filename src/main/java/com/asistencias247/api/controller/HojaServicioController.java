package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CierreHojaServicioRequest;
import com.asistencias247.api.dto.CierreHojaServicioResponseDto;
import com.asistencias247.api.dto.MaterialConsumidoDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.service.HojaServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/tecnico/ordenes")
@RequiredArgsConstructor
public class HojaServicioController {

    private final HojaServicioService hojaServicioService;

    @PostMapping("/{ordenId}/cerrar")
    public ResponseEntity<CierreHojaServicioResponseDto> cerrarHojaServicio(
            @PathVariable Long ordenId,
            @Valid @RequestBody CierreHojaServicioRequest request) {

        HojaServicio hoja = hojaServicioService.cerrarHojaServicio(ordenId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(construirRespuesta(ordenId, hoja, "Hoja de servicio registrada y orden finalizada con éxito"));
    }

    @PatchMapping("/{ordenId}/finalizar")
    public ResponseEntity<CierreHojaServicioResponseDto> finalizarOrden(
            @PathVariable Long ordenId,
            @RequestBody(required = false) CierreHojaServicioRequest request) {

        CierreHojaServicioRequest req = request != null ? request : new CierreHojaServicioRequest();
        HojaServicio hoja = hojaServicioService.cerrarHojaServicio(ordenId, req);
        return ResponseEntity.ok(construirRespuesta(ordenId, hoja, "Orden finalizada con éxito"));
    }

    private CierreHojaServicioResponseDto construirRespuesta(Long ordenId, HojaServicio hoja, String mensaje) {
        List<MaterialConsumidoDto> mats = hoja.getMateriales() != null ? hoja.getMateriales().stream()
                .map(m -> MaterialConsumidoDto.builder()
                        .materialId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                        .descripcion(m.getDescripcion())
                        .cantidad(m.getCantidad())
                        .build())
                .toList() : Collections.emptyList();

        List<String> fotos = new java.util.ArrayList<>();
        if (hoja.getFotoInicialUrl() != null && !hoja.getFotoInicialUrl().isBlank()) fotos.add(hoja.getFotoInicialUrl());
        if (hoja.getFotoFinalUrl() != null && !hoja.getFotoFinalUrl().isBlank()) fotos.add(hoja.getFotoFinalUrl());

        return CierreHojaServicioResponseDto.builder()
                .mensaje(mensaje)
                .hojaServicioId(hoja.getId())
                .ordenId(ordenId)
                .estado(EstadoOrden.FINALIZADA)
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
}
