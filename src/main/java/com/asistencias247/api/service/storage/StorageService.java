package com.asistencias247.api.service.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String subirArchivo(MultipartFile archivo, String carpeta) throws IOException;
    String subirBase64(String base64Data, String carpeta) throws IOException;
}
