package com.asistencias247.api.controller;

import com.asistencias247.api.dto.ActualizarAfiliadoRequest;
import com.asistencias247.api.dto.CrearAfiliadoRequest;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.service.AfiliadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/api/admin/afiliados", "/api/clientes"})
@RequiredArgsConstructor
public class AdminAfiliadoController {

    private final AfiliadoService afiliadoService;

    @PostMapping
    public ResponseEntity<Afiliado> registrarAfiliado(@Valid @RequestBody CrearAfiliadoRequest request) {
        Afiliado nuevoAfiliado = afiliadoService.crearAfiliado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoAfiliado);
    }

    @GetMapping
    public ResponseEntity<List<Afiliado>> listarAfiliados(@RequestParam(required = false) String query) {
        List<Afiliado> afiliados = afiliadoService.listarAfiliados(query);
        return ResponseEntity.ok(afiliados);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Afiliado> obtenerPorId(@PathVariable Long id) {
        Afiliado afiliado = afiliadoService.obtenerPorId(id);
        return ResponseEntity.ok(afiliado);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<Afiliado> actualizarAfiliado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAfiliadoRequest request) {
        Afiliado actualizado = afiliadoService.actualizarAfiliado(id, request);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarAfiliado(@PathVariable Long id) {
        afiliadoService.eliminarAfiliado(id);
        return ResponseEntity.ok(Map.of("mensaje", "Afiliado dado de baja correctamente"));
    }
}
