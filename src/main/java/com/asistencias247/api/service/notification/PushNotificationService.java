package com.asistencias247.api.service.notification;

import com.asistencias247.api.dto.SuscripcionPushRequest;

public interface PushNotificationService {

    String getVapidPublicKey();

    void suscribir(SuscripcionPushRequest request, String userEmail);

    default void registrarSuscripcion(SuscripcionPushRequest request, String userEmail) {
        suscribir(request, userEmail);
    }

    void desuscribir(String endpoint, Long usuarioId, String userEmail);

    void enviarNotificacionNuevaOrden(Long tecnicoId, Object ordenDto);
}
