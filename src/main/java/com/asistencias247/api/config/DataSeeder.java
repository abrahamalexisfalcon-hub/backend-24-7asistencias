package com.asistencias247.api.config;

import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verificando existencia y estado del usuario Administrador inicial...");

        // Conservar ÚNICAMENTE la inicialización o verificación del usuario Administrador inicial
        usuarioRepository.findByCorreo("admin@asistencias247.com").ifPresentOrElse(
                admin -> {
                    boolean modificado = false;
                    if (!Boolean.TRUE.equals(admin.getActivo())) {
                        admin.setActivo(true);
                        modificado = true;
                    }
                    if (admin.getRol() != RolUsuario.ADMINISTRADOR) {
                        admin.setRol(RolUsuario.ADMINISTRADOR);
                        modificado = true;
                    }
                    if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                        admin.setPassword(passwordEncoder.encode("admin123"));
                        modificado = true;
                    }
                    if (modificado) {
                        usuarioRepository.save(admin);
                        log.info("Usuario Administrador inicial verificado y actualizado con credenciales válidas.");
                    } else {
                        log.info("Usuario Administrador inicial verificado y activo.");
                    }
                },
                () -> {
                    log.info("Inicializando usuario Administrador inicial (admin@asistencias247.com)...");
                    Usuario admin = Usuario.builder()
                            .nombreCompleto("Administrador del Sistema")
                            .correo("admin@asistencias247.com")
                            .telefono("999888777")
                            .password(passwordEncoder.encode("admin123"))
                            .rol(RolUsuario.ADMINISTRADOR)
                            .activo(true)
                            .build();
                    usuarioRepository.save(admin);
                    log.info("Usuario Administrador inicial creado exitosamente.");
                }
        );

        // Verificar que el técnico base tenga firma digital registrada si existe
        usuarioRepository.findByCorreo("tecnico@asistencias247.com").ifPresent(tecnico -> {
            if (tecnico.getFirmaUrl() == null || tecnico.getFirmaUrl().isBlank()) {
                tecnico.setFirmaUrl("https://res.cloudinary.com/vuf0hnox/image/upload/v1792035047/asistencias-24-7/firmas-tecnicos/umxficsxqfcakih6vseu.png");
                usuarioRepository.save(tecnico);
                log.info("Firma digital base asociada al técnico tecnico@asistencias247.com");
            }
        });

        log.info("Inicialización completada. No se generan datos simulados ni de prueba (modo base de datos en limpio).");
    }
}
