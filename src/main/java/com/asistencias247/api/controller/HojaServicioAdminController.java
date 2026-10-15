package com.asistencias247.api.controller;

import com.asistencias247.api.dto.HojaServicioDetalleAdminDto;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.service.OrdenServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hojas-servicio")
@RequiredArgsConstructor
public class HojaServicioAdminController {

    private final OrdenServicioService ordenServicioService;

    @GetMapping("/orden/{ordenId}")
    public ResponseEntity<HojaServicioDetalleAdminDto> obtenerPorOrdenId(@PathVariable Long ordenId) {
        OrdenDetalleAdminDto orden = ordenServicioService.obtenerDetalle(ordenId);
        return ResponseEntity.ok(orden.getHojaServicio());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HojaServicioDetalleAdminDto> obtenerPorId(@PathVariable Long id) {
        // En nuestro modelo la orden mapea 1 a 1 con la hoja de servicio
        OrdenDetalleAdminDto orden = ordenServicioService.obtenerDetalle(id);
        return ResponseEntity.ok(orden.getHojaServicio());
    }
}
