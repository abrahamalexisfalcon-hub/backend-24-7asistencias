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
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public ResponseEntity<List<EspecialidadDto>> listarEspecialidades(
            @RequestParam(name = "todas", required = false, defaultValue = "false") boolean todas) {
        return ResponseEntity.ok(todas ? especialidadService.listarTodas() : especialidadService.listarActivas());
    }

    @PostMapping
    public ResponseEntity<EspecialidadDto> crearEspecialidad(@Valid @RequestBody CrearEspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crearEspecialidad(request));
    }

    @RequestMapping(value = "/{id}/toggle", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<EspecialidadDto> toggleEspecialidad(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadService.toggleEstadoEspecialidad(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<java.util.Map<String, String>> eliminarEspecialidad(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Especialidad eliminada correctamente"));
    }
}
