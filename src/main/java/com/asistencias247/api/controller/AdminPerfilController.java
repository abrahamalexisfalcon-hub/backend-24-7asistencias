package com.asistencias247.api.controller;

import com.asistencias247.api.dto.CambiarPasswordAdminRequest;
import com.asistencias247.api.service.AdminPerfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/perfil")
@RequiredArgsConstructor
public class AdminPerfilController {

    private final AdminPerfilService adminPerfilService;

    @RequestMapping(value = "/password", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, String>> cambiarPassword(
            Authentication authentication,
            @Valid @RequestBody CambiarPasswordAdminRequest request) {
        String correo = authentication != null ? authentication.getName() : "admin@asistencias247.com";
        adminPerfilService.cambiarPassword(correo, request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }
}
