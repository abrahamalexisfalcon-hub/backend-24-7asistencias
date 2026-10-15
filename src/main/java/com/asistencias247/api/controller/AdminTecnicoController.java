package com.asistencias247.api.controller;

import com.asistencias247.api.dto.ActualizarEstadoTecnicoRequest;
import com.asistencias247.api.dto.ActualizarTecnicoRequest;
import com.asistencias247.api.dto.CrearTecnicoRequest;
import com.asistencias247.api.dto.TecnicoResumenDto;
import com.asistencias247.api.service.TecnicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tecnicos")
@RequiredArgsConstructor
public class AdminTecnicoController {

    private final TecnicoService tecnicoService;

    @GetMapping
    public ResponseEntity<List<TecnicoResumenDto>> listarTecnicos(
            @RequestParam(required = false) Boolean soloActivos) {
        List<TecnicoResumenDto> tecnicos = tecnicoService.listarTecnicos(soloActivos);
        return ResponseEntity.ok(tecnicos);
    }

    @PostMapping
    public ResponseEntity<TecnicoResumenDto> crearTecnico(@Valid @RequestBody CrearTecnicoRequest request) {
        TecnicoResumenDto nuevo = tecnicoService.crearTecnico(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TecnicoResumenDto> actualizarEstadoTecnico(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoTecnicoRequest request) {
        TecnicoResumenDto actualizado = tecnicoService.actualizarEstadoTecnico(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TecnicoResumenDto> actualizarTecnico(
            @PathVariable Long id,
            @RequestBody ActualizarTecnicoRequest request) {
        TecnicoResumenDto actualizado = tecnicoService.actualizarTecnico(id, request);
        return ResponseEntity.ok(actualizado);
    }
}
