package com.asistencias247.api.controller;

import com.asistencias247.api.dto.OrdenResumenTecnicoDto;
import com.asistencias247.api.service.OrdenTecnicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenTecnicoService ordenTecnicoService;

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
}
