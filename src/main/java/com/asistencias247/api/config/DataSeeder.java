package com.asistencias247.api.config;

import com.asistencias247.api.entity.Cuenta;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.CuentaRepository;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final EspecialidadRepository especialidadRepository;
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

        if (cuentaRepository.count() == 0) {
            List<String> cuentasBase = List.of(
                    "Rímac Seguros",
                    "Pacífico Seguros",
                    "Mapfre",
                    "La Positiva",
                    "Interseguro"
            );
            for (String nombre : cuentasBase) {
                cuentaRepository.save(Cuenta.builder()
                        .nombre(nombre)
                        .activo(true)
                        .build());
            }
            log.info("Cuentas aseguradoras iniciales registradas: {}", cuentasBase);
        }

        // Migración automática: vincular especialidades de texto legado hacia tabla intermedia usuario_especialidades
        try {
            List<Usuario> tecnicos = usuarioRepository.findByRolOrderByNombreCompletoAsc(RolUsuario.TECNICO);
            int migrados = 0;
            for (Usuario u : tecnicos) {
                if ((u.getEspecialidades() == null || u.getEspecialidades().isEmpty())
                        && u.getEspecialidad() != null && !u.getEspecialidad().isBlank()) {
                    Set<Especialidad> especialidadesResueltas = new LinkedHashSet<>();
                    for (String parte : u.getEspecialidad().split(",")) {
                        String nombre = parte.trim();
                        if (!nombre.isEmpty()) {
                            especialidadRepository.findByNombreIgnoreCase(nombre)
                                    .ifPresent(especialidadesResueltas::add);
                        }
                    }
                    if (!especialidadesResueltas.isEmpty()) {
                        if (u.getEspecialidades() == null) {
                            u.setEspecialidades(especialidadesResueltas);
                        } else {
                            u.getEspecialidades().addAll(especialidadesResueltas);
                        }
                        usuarioRepository.save(u);
                        migrados++;
                    }
                }
            }
            if (migrados > 0) {
                log.info("Migración automática de especialidades completada para {} técnico(s).", migrados);
            }
        } catch (Exception e) {
            log.warn("No se pudo completar la migración automática de especialidades de técnicos: {}", e.getMessage());
        }

        log.info("Inicialización completada. No se generan datos simulados ni de prueba (modo base de datos en limpio).");
    }
}
