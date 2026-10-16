package com.asistencias247.api.controller;

import com.asistencias247.api.service.notification.NotificacionSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones en Tiempo Real", description = "Canal Server-Sent Events (SSE) para emisión de eventos instantáneos")
public class NotificacionController {

    private final NotificacionSseService notificacionSseService;

    @Operation(summary = "Suscribirse al canal SSE en tiempo real para recibir órdenes instantáneas")
    @GetMapping(value = "/stream/{tecnicoId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir(@PathVariable Long tecnicoId) {
        return notificacionSseService.suscribir(tecnicoId);
    }
}
