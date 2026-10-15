package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CrearEspecialidadRequest;
import com.asistencias247.api.dto.EspecialidadDto;
import com.asistencias247.api.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/especialidades")
@RequiredArgsConstructor
public class AdminEspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public ResponseEntity<List<EspecialidadDto>> listarEspecialidades() {
        return ResponseEntity.ok(especialidadService.listarActivas());
    }

    @PostMapping
    public ResponseEntity<EspecialidadDto> crearEspecialidad(@Valid @RequestBody CrearEspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crearEspecialidad(request));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EspecialidadDto> actualizarEspecialidad(
            @PathVariable Long id,
            @Valid @RequestBody com.asistencias247.api.dto.ActualizarEspecialidadRequest request) {
        return ResponseEntity.ok(especialidadService.actualizarEspecialidad(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<java.util.Map<String, String>> eliminarEspecialidad(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Especialidad eliminada correctamente"));
    }
}
