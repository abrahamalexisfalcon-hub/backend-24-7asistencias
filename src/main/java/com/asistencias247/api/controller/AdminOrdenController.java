package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.service.OrdenServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ordenes")
@RequiredArgsConstructor
public class AdminOrdenController {

    private final OrdenServicioService ordenServicioService;

    @PostMapping
    public ResponseEntity<OrdenDetalleAdminDto> crearOrden(@Valid @RequestBody CrearOrdenRequest request) {
        OrdenDetalleAdminDto nuevaOrden = ordenServicioService.crearOrden(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaOrden);
    }

    @GetMapping
    public ResponseEntity<List<OrdenDetalleAdminDto>> listarOrdenes(
            @RequestParam(required = false) EstadoOrden estado) {
        List<OrdenDetalleAdminDto> ordenes = ordenServicioService.listarOrdenes(estado);
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenDetalleAdminDto> obtenerDetalleOrden(@PathVariable Long id) {
        OrdenDetalleAdminDto detalle = ordenServicioService.obtenerDetalle(id);
        return ResponseEntity.ok(detalle);
    }

    @RequestMapping(value = "/{id}/cancelar", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrdenDetalleAdminDto> cancelarOrden(
            @PathVariable Long id,
            @RequestBody(required = false) CancelarOrdenRequest request) {
        OrdenDetalleAdminDto cancelada = ordenServicioService.cancelarOrden(id, request);
        return ResponseEntity.ok(cancelada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarOrden(@PathVariable Long id) {
        ordenServicioService.eliminarOrden(id);
        return ResponseEntity.noContent().build();
    }
}
