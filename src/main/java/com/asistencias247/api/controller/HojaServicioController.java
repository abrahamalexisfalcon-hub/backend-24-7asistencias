package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CierreHojaServicioRequest;
import com.asistencias247.api.dto.CierreHojaServicioResponseDto;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.mapper.HojaServicioMapper;
import com.asistencias247.api.service.HojaServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tecnico/ordenes")
@RequiredArgsConstructor
public class HojaServicioController {

    private final HojaServicioService hojaServicioService;
    private final HojaServicioMapper hojaServicioMapper;

    @PostMapping("/{ordenId}/cerrar")
    public ResponseEntity<CierreHojaServicioResponseDto> cerrarHojaServicio(
            @PathVariable Long ordenId,
            @Valid @RequestBody CierreHojaServicioRequest request) {

        HojaServicio hoja = hojaServicioService.cerrarHojaServicio(ordenId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hojaServicioMapper.toCierreResponse(ordenId, hoja, "Hoja de servicio registrada y orden finalizada con éxito"));
    }

    @PatchMapping("/{ordenId}/finalizar")
    public ResponseEntity<CierreHojaServicioResponseDto> finalizarOrden(
            @PathVariable Long ordenId,
            @RequestBody(required = false) CierreHojaServicioRequest request) {

        CierreHojaServicioRequest req = request != null ? request : new CierreHojaServicioRequest();
        HojaServicio hoja = hojaServicioService.cerrarHojaServicio(ordenId, req);
        return ResponseEntity.ok(hojaServicioMapper.toCierreResponse(ordenId, hoja, "Orden finalizada con éxito"));
    }
}
