package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.dto.ReaperturarOrdenRequest;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.service.IOrdenServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ordenes")
@RequiredArgsConstructor
public class AdminOrdenController {

    private final IOrdenServicioService ordenServicioService;

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

    @GetMapping("/paginadas")
    public ResponseEntity<Page<OrdenDetalleAdminDto>> listarOrdenesPaginadas(
            @RequestParam(required = false) EstadoOrden estado,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrdenDetalleAdminDto> pagina = ordenServicioService.listarOrdenesPaginadas(estado, pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenDetalleAdminDto> obtenerDetalleOrden(@PathVariable Long id) {
        OrdenDetalleAdminDto detalle = ordenServicioService.obtenerDetalle(id);
        return ResponseEntity.ok(detalle);
    }

    @RequestMapping(value = "/{id}/cancelar", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<OrdenDetalleAdminDto> cancelarOrden(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelarOrdenRequest request) {
        OrdenDetalleAdminDto cancelada = ordenServicioService.cancelarOrden(id, request);
        return ResponseEntity.ok(cancelada);
    }

    @RequestMapping(value = "/{id}/observacion-cancelacion", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<OrdenDetalleAdminDto> actualizarObservacionCancelacion(
            @PathVariable Long id,
            @Valid @RequestBody CancelarOrdenRequest request) {
        OrdenDetalleAdminDto actualizada = ordenServicioService.actualizarObservacionCancelacion(id, request);
        return ResponseEntity.ok(actualizada);
    }

    @RequestMapping(value = "/{id}/reaperturar", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrdenDetalleAdminDto> reaperturarOrden(
            @PathVariable Long id,
            @Valid @RequestBody ReaperturarOrdenRequest request) {
        OrdenDetalleAdminDto reaperturada = ordenServicioService.reaperturarOrden(id, request);
        return ResponseEntity.ok(reaperturada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarOrden(@PathVariable Long id) {
        ordenServicioService.eliminarOrden(id);
        return ResponseEntity.noContent().build();
    }
}
