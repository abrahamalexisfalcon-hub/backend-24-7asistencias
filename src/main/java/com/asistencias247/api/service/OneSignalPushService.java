package com.asistencias247.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class OneSignalPushService {

    private static final Logger log = LoggerFactory.getLogger(OneSignalPushService.class);

    @Value("${onesignal.app-id:32237258-b02d-470e-a80c-9c57fadf03c2}")
    private String appId;

    @Value("${onesignal.rest-api-key:${ONESIGNAL_REST_API_KEY:}}")
    private String restApiKey;

    private final RestClient restClient = RestClient.create();

    @Async
    public void enviarNotificacionAsignacion(Long tecnicoId, String codigoOrden, String tipoServicio, String direccion) {
        if (tecnicoId == null) {
            log.warn("[OneSignal] Se intentó enviar push sin ID de técnico.");
            return;
        }

        if (restApiKey == null || restApiKey.isBlank()) {
            log.warn("[OneSignal] OneSignal REST API Key no configurada. Se omite envío de push.");
            return;
        }

        String titulo = "¡Nueva orden asignada! #" + (codigoOrden != null ? codigoOrden : "");
        String detalleDireccion = (direccion != null && !direccion.isBlank()) ? direccion : "Lima";
        String contenido = (tipoServicio != null ? tipoServicio : "Servicio") + " • " + detalleDireccion;

        // OneSignal requiere clave 'en' como idioma predeterminado/fallback obligatorio
        Map<String, Object> payload = Map.of(
            "app_id", appId.trim(),
            "include_aliases", Map.of("external_id", List.of(String.valueOf(tecnicoId))),
            "target_channel", "push",
            "headings", Map.of("en", titulo, "es", titulo),
            "contents", Map.of("en", contenido, "es", contenido),
            "url", "https://app.24-7asistencias.com/tecnico"
        );

        String rawKey = restApiKey.trim();
        String authHeader = (rawKey.startsWith("Key ") || rawKey.startsWith("Basic "))
                ? rawKey
                : "Key " + rawKey;

        log.info("[OneSignal] Enviando push a técnico ID {} para la orden {}", tecnicoId, codigoOrden);

        try {
            var response = restClient.post()
                .uri("https://onesignal.com/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(String.class);

            log.info("[OneSignal] Push enviado con éxito (Status: 200 OK) - Respuesta: {}", response.getBody());
        } catch (RestClientResponseException ex) {
            log.error("[OneSignal] Error al enviar push (Status: {}): {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[OneSignal] Error inesperado al enviar notificación push: {}", ex.getMessage(), ex);
        }
    }
}
