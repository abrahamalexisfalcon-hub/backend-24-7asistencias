package com.asistencias247.api.controller;

import com.asistencias247.api.dto.ActualizarDisponibilidadRequest;
import com.asistencias247.api.dto.ActualizarFirmaRequest;
import com.asistencias247.api.dto.PerfilTecnicoDto;
import com.asistencias247.api.service.TecnicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tecnico")
@RequiredArgsConstructor
public class TecnicoController {

    private final TecnicoService tecnicoService;

    @GetMapping({"/perfil", "/me"})
    public ResponseEntity<PerfilTecnicoDto> obtenerPerfil(Authentication authentication) {
        PerfilTecnicoDto perfil = tecnicoService.obtenerPerfilPorCorreo(authentication.getName());
        return ResponseEntity.ok(perfil);
    }

    @PatchMapping("/disponibilidad")
    public ResponseEntity<PerfilTecnicoDto> actualizarDisponibilidad(
            Authentication authentication,
            @Valid @RequestBody ActualizarDisponibilidadRequest request) {
        PerfilTecnicoDto perfilActualizado = tecnicoService.actualizarDisponibilidad(
                authentication.getName(),
                request.disponible()
        );
        return ResponseEntity.ok(perfilActualizado);
    }

    @RequestMapping(value = {"/perfil/firma", "/firma"}, method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<PerfilTecnicoDto> actualizarFirma(
            Authentication authentication,
            @RequestBody ActualizarFirmaRequest request) {
        String base64 = request != null ? request.getFirmaEfectiva() : null;
        PerfilTecnicoDto perfilActualizado = tecnicoService.actualizarFirma(
                authentication.getName(),
                base64
        );
        return ResponseEntity.ok(perfilActualizado);
    }
}
