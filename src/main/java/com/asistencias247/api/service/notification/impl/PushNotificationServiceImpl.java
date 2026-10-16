package com.asistencias247.api.service.notification.impl;

import com.asistencias247.api.dto.OrdenDetalleAdminDto;
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
import org.springframework.scheduling.annotation.Async;
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

    @Value("${vapid.public-key:${vapid.public.key:BCpAAMs1UM1ZKxugrSSB2_LhAeL3SbgFnCPF2M1RLdfPqXEKg3p7pk-I5WvIefOoEnsRCUCQB5chIO1r08j0iEA}}")
    private String vapidPublicKey;

    @Value("${vapid.private-key:${vapid.private.key:cITZrEVsVXbqNOT3RrRsWRxYCix9X4r5Nuf-jKlLBRU}}")
    private String vapidPrivateKey;

    @Value("${vapid.subject:mailto:contacto@asistencias247.com}")
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
            log.info("Servicio Web Push (VAPID) inicializado correctamente para {}", vapidSubject);
        } catch (Exception e) {
            log.error("Error al inicializar PushService VAPID: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Override
    @Transactional
    public void suscribir(SuscripcionPushRequest request, String userEmail) {
        if (request == null || request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El endpoint push es requerido.");
        }

        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(request.getUsuarioId()).orElse(null);
        }
        if (usuario == null && userEmail != null && !userEmail.isBlank()) {
            usuario = usuarioRepository.findByCorreo(userEmail).orElse(null);
        }
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado para vincular la suscripción.");
        }

        String p256dh = request.getKeys() != null ? request.getKeys().getP256dh() : "";
        String auth = request.getKeys() != null ? request.getKeys().getAuth() : "";

        PushSubscription sub = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElse(PushSubscription.builder()
                        .endpoint(request.getEndpoint())
                        .build());

        sub.setUsuario(usuario);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);

        pushSubscriptionRepository.save(sub);
        log.info("Suscripción Push VAPID guardada para usuario: {} ({})", usuario.getNombreCompleto(), usuario.getCorreo());
    }

    @Override
    @Transactional
    public void desuscribir(String endpoint, Long usuarioId, String userEmail) {
        if (endpoint != null && !endpoint.isBlank()) {
            pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(pushSubscriptionRepository::delete);
            log.info("Suscripción Push eliminada por endpoint: {}", endpoint);
            return;
        }

        Usuario usuario = null;
        if (usuarioId != null) {
            usuario = usuarioRepository.findById(usuarioId).orElse(null);
        }
        if (usuario == null && userEmail != null && !userEmail.isBlank()) {
            usuario = usuarioRepository.findByCorreo(userEmail).orElse(null);
        }

        if (usuario != null) {
            List<PushSubscription> subs = pushSubscriptionRepository.findByUsuarioId(usuario.getId());
            pushSubscriptionRepository.deleteAll(subs);
            log.info("Todas las suscripciones Push eliminadas para usuario: {}", usuario.getCorreo());
        }
    }

    @Async
    @Override
    public void enviarNotificacionNuevaOrden(Long tecnicoId, Object ordenDto) {
        if (tecnicoId == null || pushService == null) return;

        List<PushSubscription> suscripciones = pushSubscriptionRepository.findByUsuarioId(tecnicoId);
        if (suscripciones.isEmpty()) {
            log.debug("El técnico ID {} no posee suscripciones push activas.", tecnicoId);
            return;
        }

        String codigo = "ORD-000";
        String servicio = "Servicio Técnico";
        String distrito = "Lima";

        if (ordenDto instanceof OrdenDetalleAdminDto dto) {
            if (dto.getCodigoOrden() != null) codigo = dto.getCodigoOrden();
            if (dto.getTipoServicio() != null) servicio = dto.getTipoServicio();
            if (dto.getDistrito() != null) distrito = dto.getDistrito();
            else if (dto.getDistritoAfiliado() != null) distrito = dto.getDistritoAfiliado();
        }

        String body = String.format("#%s: %s - %s", codigo, servicio, distrito);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("title", "¡Nueva Orden Asignada!");
        payloadMap.put("body", body);
        payloadMap.put("url", "/tecnico");
        payloadMap.put("tag", "orden-nueva");

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.error("Error al serializar payload Push: {}", e.getMessage());
            return;
        }

        log.info("Enviando Web Push a {} suscripciones para técnico ID {}", suscripciones.size(), tecnicoId);

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
                    log.info("Notificación Web Push enviada con éxito (Status {}) a técnico ID {}", statusCode, tecnicoId);
                } else if (statusCode == 404 || statusCode == 410) {
                    log.warn("Endpoint caducado (Status {}). Eliminando suscripción {}", statusCode, sub.getId());
                    pushSubscriptionRepository.deleteById(sub.getId());
                } else {
                    log.warn("Respuesta inesperada Web Push (Status {}) a técnico ID {}", statusCode, tecnicoId);
                }
            } catch (Exception ex) {
                log.error("Fallo al entregar Web Push a endpoint {}: {}", sub.getEndpoint(), ex.getMessage());
            }
        }
    }
}
