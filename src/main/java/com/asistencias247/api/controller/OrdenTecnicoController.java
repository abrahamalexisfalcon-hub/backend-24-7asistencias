package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.OrdenResumenTecnicoDto;
import com.asistencias247.api.service.IOrdenTecnicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tecnico/ordenes")
@RequiredArgsConstructor
public class OrdenTecnicoController {

    private final IOrdenTecnicoService ordenTecnicoService;

    @GetMapping
    public ResponseEntity<List<OrdenResumenTecnicoDto>> obtenerOrdenes(
            Authentication authentication,
            @RequestParam(required = false) Long tecnicoId) {
        if (tecnicoId != null) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerOrdenesPorTecnico(tecnicoId));
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(ordenTecnicoService.obtenerOrdenesPorCorreo(authentication.getName()));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/asignadas")
    public ResponseEntity<List<OrdenResumenTecnicoDto>> obtenerOrdenesAsignadas(
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

    @GetMapping("/{tecnicoId}")
    public ResponseEntity<List<OrdenResumenTecnicoDto>> obtenerOrdenesPorTecnico(
            Authentication authentication,
            @PathVariable Long tecnicoId) {
        List<OrdenResumenTecnicoDto> ordenes = ordenTecnicoService.obtenerOrdenesPorTecnico(tecnicoId);
        if (ordenes.isEmpty() && authentication != null && authentication.isAuthenticated()) {
            ordenes = ordenTecnicoService.obtenerOrdenesPorCorreo(authentication.getName());
        }
        return ResponseEntity.ok(ordenes);
    }

    @PatchMapping("/{ordenId}/llegada")
    public ResponseEntity<OrdenResumenTecnicoDto> registrarLlegada(@PathVariable Long ordenId) {
        OrdenResumenTecnicoDto ordenActualizada = ordenTecnicoService.registrarLlegada(ordenId);
        return ResponseEntity.ok(ordenActualizada);
    }

    @RequestMapping(value = {"/{ordenId}/asistido-no-realizado", "/{ordenId}/cancelar"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<OrdenResumenTecnicoDto> registrarAsistidoNoRealizado(
            @PathVariable Long ordenId,
            @Valid @RequestBody(required = false) CancelarOrdenRequest request) {
        String motivo = request != null ? request.getMotivoEfectivo() : "";
        OrdenResumenTecnicoDto ordenCancelada = ordenTecnicoService.registrarAsistidoNoRealizado(ordenId, motivo);
        return ResponseEntity.ok(ordenCancelada);
    }

    @GetMapping("/detalle/{ordenId}")
    public ResponseEntity<OrdenResumenTecnicoDto> obtenerDetalleOrden(@PathVariable Long ordenId) {
        OrdenResumenTecnicoDto detalle = ordenTecnicoService.obtenerDetalleOrden(ordenId);
        return ResponseEntity.ok(detalle);
    }
}
