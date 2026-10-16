package com.asistencias247.api.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class NotificacionSseService {

    // Mapeo concurrente de técnicoId -> SseEmitter
    private final Map<Long, SseEmitter> suscripciones = new ConcurrentHashMap<>();

    public SseEmitter suscribir(Long tecnicoId) {
        // Timeout de 30 minutos (el cliente se reconecta automáticamente en caso de expirar)
        SseEmitter emitter = new SseEmitter(1800000L);
        suscripciones.put(tecnicoId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completado para técnico ID: {}", tecnicoId);
            suscripciones.remove(tecnicoId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE timeout para técnico ID: {}", tecnicoId);
            suscripciones.remove(tecnicoId);
        });
        emitter.onError(e -> {
            log.debug("SSE error para técnico ID {}: {}", tecnicoId, e.getMessage());
            suscripciones.remove(tecnicoId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Conexión en tiempo real establecida"));
            log.info("Canal SSE en tiempo real activado para técnico ID: {}", tecnicoId);
        } catch (IOException e) {
            log.warn("No se pudo enviar evento INIT SSE al técnico {}: {}", tecnicoId, e.getMessage());
            suscripciones.remove(tecnicoId);
        }

        return emitter;
    }

    public void notificarNuevaOrden(Long tecnicoId, Object payloadOrden) {
        if (tecnicoId == null) return;
        SseEmitter emitter = suscripciones.get(tecnicoId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("NUEVA_ORDEN")
                        .data(payloadOrden));
                log.info("Evento SSE 'NUEVA_ORDEN' emitido instantáneamente a técnico ID: {}", tecnicoId);
            } catch (IOException e) {
                log.warn("Fallo al emitir evento SSE a técnico {}. Eliminando emisor.", tecnicoId);
                suscripciones.remove(tecnicoId);
            }
        } else {
            log.debug("No hay emisor SSE activo para técnico ID: {}", tecnicoId);
        }
    }
}
