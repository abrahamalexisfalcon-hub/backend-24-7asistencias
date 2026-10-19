package com.asistencias247.api.service;

import com.asistencias247.api.entity.OrdenServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OneSignalPushService {

    private static final Logger log = LoggerFactory.getLogger(OneSignalPushService.class);

    private static final String DEFAULT_KEY_B64 =
            "b3NfdjJfYXBwX2dpcnhld2ZxZnZkcTVrYW10cmw3dnh5ZHlsdWh0cHAyZW5pZXZtdnNleXl4cG1wYXdqemgyYXZxeWR4N3B4aXFsMzRkNnBhbHU1dTdzbWhlcWdzZHJqNWthb2VpeTdybjQ3d2djY3E=";

    @Value("${onesignal.app-id:${ONESIGNAL_APP_ID:32237258-b02d-470e-a80c-9c57fadf03c2}}")
    private String appId;

    @Value("${onesignal.rest.api.key:${onesignal.rest-api-key:${ONESIGNAL_REST_API_KEY:}}}")
    private String restApiKey;

    private final RestClient restClient = RestClient.create();

    private String resolverRestApiKey() {
        if (restApiKey != null && !restApiKey.isBlank()) {
            return restApiKey.trim();
        }
        String envKey = System.getenv("ONESIGNAL_REST_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        try {
            return new String(Base64.getDecoder().decode(DEFAULT_KEY_B64), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    @Async("mediaUploadExecutor")
    public void enviarNotificacionAsignacion(OrdenServicio orden) {
        if (orden == null) {
            log.error("[OneSignal-ERROR] No se pudo enviar el push: La entidad OrdenServicio es nula.");
            return;
        }
        Long tecnicoId = (orden.getTecnico() != null) ? orden.getTecnico().getId() : null;
        String distrito = (orden.getAfiliado() != null && orden.getAfiliado().getDistrito() != null)
                ? orden.getAfiliado().getDistrito()
                : "Lima";
        String direccion = (orden.getAfiliado() != null && orden.getAfiliado().getDireccion() != null && !orden.getAfiliado().getDireccion().isBlank())
                ? orden.getAfiliado().getDireccion()
                : distrito;

        enviarNotificacionAsignacion(orden.getId(), tecnicoId, orden.getCodigoOrden(), orden.getTipoServicio(), direccion);
    }

    @Async("mediaUploadExecutor")
    public void enviarNotificacionAsignacion(Long tecnicoId, String codigoOrden, String tipoServicio, String direccion) {
        enviarNotificacionAsignacion(null, tecnicoId, codigoOrden, tipoServicio, direccion);
    }

    @Async("mediaUploadExecutor")
    public void enviarNotificacionAsignacion(Long ordenId, Long tecnicoId, String codigoOrden, String tipoServicio, String direccion) {
        log.info("[OneSignal-TRACE] Intentando enviar push para Orden ID: {} (#{}) al Técnico ID: {}",
                ordenId != null ? ordenId : "N/A", codigoOrden, tecnicoId);

        if (tecnicoId == null) {
            log.warn("[OneSignal-ERROR] Se intentó enviar push sin ID de técnico para la orden {}.", codigoOrden);
            return;
        }

        String effectiveApiKey = resolverRestApiKey();
        if (effectiveApiKey == null || effectiveApiKey.isBlank()) {
            log.error("[OneSignal-ERROR] No se pudo enviar el push: ONESIGNAL_REST_API_KEY no está configurada.");
            return;
        }
        if (appId == null || appId.isBlank()) {
            log.error("[OneSignal-ERROR] No se pudo enviar el push: ONESIGNAL_APP_ID no está configurada.");
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

        String authHeader = (effectiveApiKey.startsWith("Key ") || effectiveApiKey.startsWith("Basic "))
                ? effectiveApiKey
                : "Key " + effectiveApiKey;

        try {
            var response = restClient.post()
                .uri("https://onesignal.com/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toEntity(String.class);

            log.info("[OneSignal-RESPONSE] Status code: {}", response.getStatusCode());
            log.info("[OneSignal-RESPONSE] Body: {}", response.getBody());
        } catch (RestClientResponseException ex) {
            log.error("[OneSignal-RESPONSE] Status code: {}", ex.getStatusCode());
            log.error("[OneSignal-RESPONSE] Body: {}", ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[OneSignal-ERROR] Error inesperado al enviar notificación push: {}", ex.getMessage(), ex);
        }
    }
}
