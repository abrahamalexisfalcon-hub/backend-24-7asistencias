package com.asistencias247.api.dto;

public record ActualizarFirmaRequest(
        String firma,
        String firmaBase64,
        String firmaUrl
) {
    public String getFirmaEfectiva() {
        if (firmaBase64 != null && !firmaBase64.isBlank()) {
            return firmaBase64;
        }
        if (firma != null && !firma.isBlank()) {
            return firma;
        }
        return firmaUrl;
    }
}
