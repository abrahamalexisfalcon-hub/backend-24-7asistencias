package com.asistencias247.api.controller;

import com.asistencias247.api.dto.AsistidoNoRealizadoDTO;
import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.dto.OrdenResumenTecnicoDto;
import com.asistencias247.api.dto.ReaperturarOrdenRequest;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.service.IOrdenServicioService;
import com.asistencias247.api.service.IOrdenTecnicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final IOrdenTecnicoService ordenTecnicoService;
    private final IOrdenServicioService ordenServicioService;

    @GetMapping("/paginadas")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<Page<OrdenDetalleAdminDto>> listarOrdenesPaginadas(
            @RequestParam(required = false) EstadoOrden estado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ordenServicioService.listarOrdenesPaginadas(estado, pageable));
    }

    @GetMapping("/asignadas")
    public ResponseEntity<List<OrdenResumenTecnicoDto>> obtenerAsignadas(
            Authentication authentication,
            @RequestParam(required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerAsignadas(tecnicoId));
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerAsignadasPorCorreo(authentication.getName()));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<OrdenResumenTecnicoDto>> obtenerHistorial(
            Authentication authentication,
            @RequestParam(required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerHistorial(tecnicoId));
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerHistorialPorCorreo(authentication.getName()));
        }
        return ResponseEntity.ok(List.of());
    }

    @RequestMapping(value = {"/{id}/asistido-no-realizado", "/{id}/cancelar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> marcarAsistidoNoRealizado(
            @PathVariable Long id,
            @Valid @RequestBody AsistidoNoRealizadoDTO dto) {
        OrdenServicio orden = ordenServicioService.marcarAsistidoNoRealizado(id, dto.motivo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Orden registrada como asistida no realizada exitosamente",
                "id", orden.getId(),
                "estado", orden.getEstado().name(),
                "motivoCancelacion", orden.getMotivoCancelacion(),
                "fechaCancelacion", orden.getFechaCancelacion().toString()
        ));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    @RequestMapping(value = "/{id}/observacion-cancelacion", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<OrdenDetalleAdminDto> actualizarObservacionCancelacion(
            @PathVariable Long id,
            @Valid @RequestBody CancelarOrdenRequest request) {
        OrdenDetalleAdminDto actualizada = ordenServicioService.actualizarObservacionCancelacion(id, request);
        return ResponseEntity.ok(actualizada);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRADOR')")
    @RequestMapping(value = "/{id}/reaperturar", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrdenDetalleAdminDto> reaperturarOrden(
            @PathVariable Long id,
            @Valid @RequestBody ReaperturarOrdenRequest request) {
        OrdenDetalleAdminDto reaperturada = ordenServicioService.reaperturarOrden(id, request);
        return ResponseEntity.ok(reaperturada);
    }
}
