package com.asistencias247.api.config;

import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.AfiliadoDireccion;
import com.asistencias247.api.entity.Cuenta;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.AfiliadoRepository;
import com.asistencias247.api.repository.CuentaRepository;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final AfiliadoRepository afiliadoRepository;
    private final OrdenServicioRepository ordenServicioRepository;
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
                    if (admin.getPassword() == null || admin.getPassword().isBlank()) {
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

        // Migración automática: normalizar DNIs > 8 dígitos y crear dirección Principal para afiliados existentes
        try {
            List<Afiliado> afiliados = afiliadoRepository.findAll();
            int afiliadosMigrados = 0;
            int dnisNormalizados = 0;
            for (Afiliado a : afiliados) {
                boolean modificado = false;
                if (a.getDni() != null && !a.getDni().isBlank()) {
                    String soloDigitos = a.getDni().replaceAll("\\D", "");
                    if (soloDigitos.length() > 8) {
                        a.setDni(soloDigitos.substring(0, 8));
                        modificado = true;
                        dnisNormalizados++;
                    }
                }
                if ((a.getDirecciones() == null || a.getDirecciones().isEmpty())
                        && a.getDireccion() != null && !a.getDireccion().isBlank()) {
                    if (a.getDirecciones() == null) {
                        a.setDirecciones(new ArrayList<>());
                    }
                    AfiliadoDireccion dirPrincipal = AfiliadoDireccion.builder()
                            .afiliado(a)
                            .etiqueta("Principal")
                            .direccion(a.getDireccion().trim())
                            .distrito(a.getDistrito())
                            .referencia(a.getReferencia())
                            .esPrincipal(true)
                            .build();
                    a.getDirecciones().add(dirPrincipal);
                    modificado = true;
                    afiliadosMigrados++;
                }
                if (modificado) {
                    afiliadoRepository.save(a);
                }
            }
            if (dnisNormalizados > 0) {
                log.info("Normalización automática de DNI (>8 dígitos) completada para {} afiliado(s).", dnisNormalizados);
            }
            if (afiliadosMigrados > 0) {
                log.info("Migración automática de direcciones completada para {} afiliado(s).", afiliadosMigrados);
            }
        } catch (Exception e) {
            log.warn("No se pudo completar la migración automática de afiliados: {}", e.getMessage());
        }

        // Migración automática: snapshot de dirección en órdenes existentes sin direccion_servicio
        try {
            List<OrdenServicio> ordenes = ordenServicioRepository.findAll();
            int ordenesMigradas = 0;
            for (OrdenServicio o : ordenes) {
                if (o.getDireccionServicio() == null && o.getAfiliado() != null) {
                    o.setDireccionServicio(o.getAfiliado().getDireccion());
                    o.setDistritoServicio(o.getAfiliado().getDistrito());
                    o.setReferenciaServicio(o.getAfiliado().getReferencia());
                    ordenServicioRepository.save(o);
                    ordenesMigradas++;
                }
            }
            if (ordenesMigradas > 0) {
                log.info("Migración automática de snapshot de dirección completada para {} orden(es).", ordenesMigradas);
            }
        } catch (Exception e) {
            log.warn("No se pudo completar la migración automática de direcciones en órdenes de servicio: {}", e.getMessage());
        }

        log.info("Inicialización completada. No se generan datos simulados ni de prueba (modo base de datos en limpio).");
    }
}
