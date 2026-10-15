package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CrearMaterialCatalogoRequest;
import com.asistencias247.api.entity.Material;
import com.asistencias247.api.service.MaterialCatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/catalogo")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private final MaterialCatalogoService materialCatalogoService;

    @PostMapping("/materiales")
    public ResponseEntity<Material> agregarMaterial(@Valid @RequestBody CrearMaterialCatalogoRequest request) {
        Material nuevoMaterial = materialCatalogoService.crearMaterial(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoMaterial);
    }

    @PatchMapping("/materiales/{id}/toggle")
    public ResponseEntity<Material> toggleEstadoMaterial(@PathVariable Long id) {
        Material materialActualizado = materialCatalogoService.toggleActivo(id);
        return ResponseEntity.ok(materialActualizado);
    }

    @GetMapping("/materiales")
    public ResponseEntity<List<Material>> listarTodos() {
        List<Material> materiales = materialCatalogoService.listarTodos();
        return ResponseEntity.ok(materiales);
    }
}
