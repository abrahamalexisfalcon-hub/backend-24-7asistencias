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
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications (VAPID)", description = "Suscripción y desuscripción de notificaciones nativas en segundo plano")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    @Operation(summary = "Obtener clave pública VAPID para suscripción push en el frontend")
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> obtenerClavePublica() {
        return ResponseEntity.ok(Map.of("publicKey", pushNotificationService.getVapidPublicKey()));
    }

    @Operation(summary = "Suscribir dispositivo del técnico a notificaciones push VAPID")
    @PostMapping("/suscribir")
    public ResponseEntity<Map<String, Object>> suscribir(
            @Valid @RequestBody SuscripcionPushRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        pushNotificationService.suscribir(request, userEmail);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Dispositivo suscrito exitosamente a notificaciones Web Push."
        ));
    }

    @Operation(summary = "Desuscribir dispositivo de notificaciones push VAPID")
    @PostMapping("/desuscribir")
    public ResponseEntity<Map<String, Object>> desuscribir(
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication
    ) {
        String endpoint = body != null && body.containsKey("endpoint") ? String.valueOf(body.get("endpoint")) : null;
        Long usuarioId = null;
        if (body != null && body.containsKey("usuarioId") && body.get("usuarioId") != null) {
            try {
                usuarioId = Long.valueOf(String.valueOf(body.get("usuarioId")));
            } catch (Exception ignored) {}
        }
        String userEmail = authentication != null ? authentication.getName() : null;

        pushNotificationService.desuscribir(endpoint, usuarioId, userEmail);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Suscripción push eliminada correctamente."
        ));
    }
}
