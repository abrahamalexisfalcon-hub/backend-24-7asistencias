package com.asistencias247.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuscripcionPushRequest {

    @NotBlank(message = "El endpoint push es requerido")
    private String endpoint;

    private Keys keys;

    private Long usuarioId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Keys {
        @NotBlank(message = "La clave p256dh es requerida")
        private String p256dh;

        @NotBlank(message = "La clave auth es requerida")
        private String auth;
    }
}
