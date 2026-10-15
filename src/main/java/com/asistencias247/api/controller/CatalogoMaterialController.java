package com.asistencias247.api.controller;

import com.asistencias247.api.dto.MaterialCatalogoDto;
import com.asistencias247.api.service.CatalogoMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo/materiales")
@RequiredArgsConstructor
public class CatalogoMaterialController {

    private final CatalogoMaterialService catalogoMaterialService;

    @GetMapping
    public ResponseEntity<List<MaterialCatalogoDto>> obtenerMaterialesActivos() {
        List<MaterialCatalogoDto> materiales = catalogoMaterialService.obtenerMaterialesActivos();
        return ResponseEntity.ok(materiales);
    }
}
