package com.asistencias247.api.controller;

import com.asistencias247.api.dto.MaterialDto;
import com.asistencias247.api.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiales")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<List<MaterialDto>> listarMateriales(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) String especialidadNombre
    ) {
        return ResponseEntity.ok(materialService.listarActivos(especialidadId, especialidadNombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.obtenerPorId(id));
    }
}
