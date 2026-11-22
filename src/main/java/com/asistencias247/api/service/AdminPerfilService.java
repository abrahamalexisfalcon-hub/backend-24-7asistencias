package com.asistencias247.api.service;

import com.asistencias247.api.dto.CambiarPasswordAdminRequest;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void cambiarPassword(String correoAutenticado, CambiarPasswordAdminRequest request) {
        String correo = (correoAutenticado != null && !correoAutenticado.isBlank())
                ? correoAutenticado.trim()
                : "admin@asistencias247.com";

        Usuario admin = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario administrador no encontrado."));

        if (request == null || request.getPasswordActual() == null
                || !passwordEncoder.matches(request.getPasswordActual(), admin.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña actual ingresada es incorrecta.");
        }

        String nueva = request.getNuevaPassword() != null ? request.getNuevaPassword().trim() : "";
        if (nueva.length() < 6 || nueva.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La nueva contraseña debe tener entre 6 y 100 caracteres.");
        }

        if (request.getConfirmarPassword() != null
                && !request.getConfirmarPassword().isBlank()
                && !nueva.equals(request.getConfirmarPassword().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La confirmación de la nueva contraseña no coincide.");
        }

        if (passwordEncoder.matches(nueva, admin.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la contraseña actual.");
        }

        admin.setPassword(passwordEncoder.encode(nueva));
        usuarioRepository.save(admin);
    }
}
