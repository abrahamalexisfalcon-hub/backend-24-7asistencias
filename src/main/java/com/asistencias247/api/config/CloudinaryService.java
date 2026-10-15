package com.asistencias247.api.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Sube fotos directas (ej: foto inicial, foto final, foto de perfil)
     */
    public String subirArchivo(MultipartFile archivo, String carpeta) throws IOException {
        if (archivo == null || archivo.isEmpty()) return null;

        Map<?, ?> uploadResult = cloudinary.uploader().upload(archivo.getBytes(), ObjectUtils.asMap(
                "folder", "asistencias-24-7/" + carpeta,
                "resource_type", "auto"
        ));
        // Devuelve la URL HTTPS directa de Cloudinary
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Sube la firma en Base64 que viene del canvas táctil del celular
     */
    public String subirBase64(String base64Data, String carpeta) throws IOException {
        if (base64Data == null || base64Data.trim().isEmpty()) return null;

        Map<?, ?> uploadResult = cloudinary.uploader().upload(base64Data, ObjectUtils.asMap(
                "folder", "asistencias-24-7/" + carpeta,
                "resource_type", "auto"
        ));
        return (String) uploadResult.get("secure_url");
    }
}