package com.asistencias247.api.controller;

import com.asistencias247.api.dto.ActualizarCuentaRequest;
import com.asistencias247.api.dto.CrearCuentaRequest;
import com.asistencias247.api.dto.CuentaDto;
import com.asistencias247.api.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/cuentas", "/api/admin/cuentas"})
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @GetMapping
    public ResponseEntity<List<CuentaDto>> listarCuentas(
            @RequestParam(name = "todas", required = false, defaultValue = "false") boolean todas) {
        return ResponseEntity.ok(todas ? cuentaService.listarTodas() : cuentaService.listarActivas());
    }

    @PostMapping
    public ResponseEntity<CuentaDto> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.crearCuenta(request));
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<CuentaDto> actualizarCuenta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCuentaRequest request) {
        return ResponseEntity.ok(cuentaService.actualizarCuenta(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CuentaDto> toggleCuenta(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.toggleEstadoCuenta(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarCuenta(@PathVariable Long id) {
        cuentaService.eliminarCuenta(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta deshabilitada correctamente"));
    }
}
