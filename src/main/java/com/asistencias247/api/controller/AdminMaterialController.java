package com.asistencias247.api.controller;

import com.asistencias247.api.dto.ActualizarMaterialRequest;
import com.asistencias247.api.dto.CrearMaterialRequest;
import com.asistencias247.api.dto.MaterialDto;
import com.asistencias247.api.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/materiales")
@RequiredArgsConstructor
public class AdminMaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialDto>> listarMateriales(
            @RequestParam(required = false) Long especialidadId
    ) {
        return ResponseEntity.ok(materialService.listarPorEspecialidad(especialidadId));
    }

    @PostMapping
    public ResponseEntity<MaterialDto> crearMaterial(@Valid @RequestBody CrearMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.crearMaterial(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialDto> actualizarMaterial(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarMaterialRequest request
    ) {
        return ResponseEntity.ok(materialService.actualizarMaterial(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarMaterial(@PathVariable Long id) {
        materialService.eliminarMaterial(id);
        return ResponseEntity.ok(Map.of("mensaje", "Material desactivado correctamente"));
    }
}
