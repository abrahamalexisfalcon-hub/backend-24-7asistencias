package com.asistencias247.api.controller;

import com.asistencias247.api.dto.SuscripcionPushRequest;
import com.asistencias247.api.service.notification.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones Push", description = "Gestión de suscripciones Web Push para técnicos y operadores")
public class NotificacionPushController {

    private final PushNotificationService pushNotificationService;

    @Operation(summary = "Obtener clave pública VAPID para suscripción push en el frontend")
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> obtenerClavePublica() {
        return ResponseEntity.ok(Map.of("publicKey", pushNotificationService.getVapidPublicKey()));
    }

    @Operation(summary = "Registrar suscripción Push del navegador del técnico")
    @PostMapping("/suscripcion-push")
    public ResponseEntity<Map<String, Object>> registrarSuscripcion(
            @Valid @RequestBody SuscripcionPushRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        pushNotificationService.registrarSuscripcion(request, userEmail);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Suscripción Push registrada exitosamente."
        ));
    }
}
