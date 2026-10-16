package com.asistencias247.api.service.notification.impl;

import com.asistencias247.api.dto.SuscripcionPushRequest;
import com.asistencias247.api.entity.PushSubscription;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.PushSubscriptionRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import com.asistencias247.api.service.notification.PushNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Value("${vapid.public-key:BCpAAMs1UM1ZKxugrSSB2_LhAeL3SbgFnCPF2M1RLdfPqXEKg3p7pk-I5WvIefOoEnsRCUCQB5chIO1r08j0iEA}")
    private String vapidPublicKey;

    @Value("${vapid.private-key:cITZrEVsVXbqNOT3RrRsWRxYCix9X4r5Nuf-jKlLBRU}")
    private String vapidPrivateKey;

    @Value("${vapid.subject:mailto:soporte@asistencias247.com}")
    private String vapidSubject;

    private PushService pushService;

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            pushService = new PushService();
            pushService.setPublicKey(vapidPublicKey.trim());
            pushService.setPrivateKey(vapidPrivateKey.trim());
            pushService.setSubject(vapidSubject.trim());
            log.info("Servicio Web Push inicializado correctamente con VAPID Subject: {}", vapidSubject);
        } catch (Exception e) {
            log.error("Error inicializando Web Push PushService: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Override
    @Transactional
    public void registrarSuscripcion(SuscripcionPushRequest request, String userEmail) {
        if (request == null || request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El endpoint es obligatorio.");
        }

        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(request.getUsuarioId()).orElse(null);
        }
        if (usuario == null && userEmail != null && !userEmail.isBlank()) {
            usuario = usuarioRepository.findByCorreo(userEmail).orElse(null);
        }
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado para asociar la suscripción.");
        }

        String p256dh = request.getKeys() != null ? request.getKeys().getP256dh() : "";
        String auth = request.getKeys() != null ? request.getKeys().getAuth() : "";

        // Si ya existe el endpoint, actualizar el usuario y claves
        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElse(PushSubscription.builder()
                        .endpoint(request.getEndpoint())
                        .build());

        sub.setUsuario(usuario);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);

        pushSubscriptionRepository.save(sub);
        log.info("Suscripción Push registrada exitosamente para el usuario: {} ({})", usuario.getNombreCompleto(), usuario.getCorreo());
    }

    @Override
    public void enviarNotificacionANativo(Long usuarioId, String titulo, String mensaje, String url) {
        if (usuarioId == null) return;

        List<PushSubscription> suscripciones = pushSubscriptionRepository.findByUsuarioId(usuarioId);
        if (suscripciones.isEmpty()) {
            log.debug("El usuario ID {} no tiene suscripciones Push registradas.", usuarioId);
            return;
        }

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("title", titulo != null ? titulo : "24/7 Asistencias");
        payloadMap.put("body", mensaje != null ? mensaje : "Tienes una nueva notificación.");
        payloadMap.put("url", url != null ? url : "/tecnico/ordenes");
        payloadMap.put("tag", "orden-" + System.currentTimeMillis());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("Error serializando payload para push: {}", e.getMessage());
            return;
        }

        for (PushSubscription sub : suscripciones) {
            try {
                Subscription webPushSub = new Subscription(
                        sub.getEndpoint(),
                        new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );

                Notification notification = new Notification(webPushSub, payloadJson);
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 201 || statusCode == 200) {
                    log.info("Notificación Web Push enviada con éxito a usuario ID: {} (Status: {})", usuarioId, statusCode);
                } else if (statusCode == 404 || statusCode == 410) {
                    log.warn("Suscripción expirada o inválida (Status: {}). Eliminando endpoint: {}", statusCode, sub.getEndpoint());
                    pushSubscriptionRepository.deleteById(sub.getId());
                } else {
                    log.warn("Respuesta inesperada al enviar Web Push a usuario ID {}: {}", usuarioId, statusCode);
                }
            } catch (Exception ex) {
                log.error("Excepción enviando notificación Web Push a endpoint {}: {}", sub.getEndpoint(), ex.getMessage());
            }
        }
    }
}
