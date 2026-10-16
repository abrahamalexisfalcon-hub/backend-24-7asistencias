package com.asistencias247.api.service.notification;

import com.asistencias247.api.dto.SuscripcionPushRequest;

public interface PushNotificationService {

    void registrarSuscripcion(SuscripcionPushRequest request, String userEmail);

    void enviarNotificacionANativo(Long usuarioId, String titulo, String mensaje, String url);

    String getVapidPublicKey();
}
