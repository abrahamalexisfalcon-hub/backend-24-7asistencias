package com.asistencias247.api.service;

import com.asistencias247.api.service.storage.StorageService;
import com.asistencias247.api.dto.ActualizarEstadoTecnicoRequest;
import com.asistencias247.api.dto.ActualizarTecnicoRequest;
import com.asistencias247.api.dto.CrearTecnicoRequest;
import com.asistencias247.api.dto.PerfilTecnicoDto;
import com.asistencias247.api.dto.TecnicoResumenDto;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.exception.RecursoNoEncontradoException;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TecnicoService {

    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final OrdenServicioRepository ordenServicioRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    private record EspecialidadesResueltas(Set<Especialidad> entidades, List<String> nombres) {}

    @Transactional(readOnly = true)
    public List<TecnicoResumenDto> listarTecnicos(Boolean soloActivos) {
        List<Usuario> usuarios = Boolean.TRUE.equals(soloActivos)
                ? usuarioRepository.findByRolAndActivoTrueOrderByNombreCompletoAsc(RolUsuario.TECNICO)
                : usuarioRepository.findByRolOrderByNombreCompletoAsc(RolUsuario.TECNICO);

        return usuarios.stream()
                .map(this::mapearATecnicoResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TecnicoResumenDto> listarTecnicosActivos() {
        return listarTecnicos(true);
    }

    @Transactional
    public TecnicoResumenDto crearTecnico(CrearTecnicoRequest request) {
        String correoLimpio = request.getCorreo().trim().toLowerCase();
        String telefonoLimpio = request.getTelefono() != null
                ? request.getTelefono().trim().replaceAll("\\s+", "")
                : "";
        String pinLimpio = request.getPassword() != null ? request.getPassword().trim() : "";

        if (!telefonoLimpio.matches("^\\d{9}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de celular debe contener exactamente 9 dígitos numéricos.");
        }

        if (!pinLimpio.matches("^\\d{4}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El PIN de acceso debe tener exactamente 4 dígitos numéricos.");
        }

        if (usuarioRepository.existsByCorreo(correoLimpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un usuario registrado con el correo: " + correoLimpio);
        }

        if (usuarioRepository.existsByTelefono(telefonoLimpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un usuario registrado con el número de celular: " + telefonoLimpio);
        }

        EspecialidadesResueltas resueltas = resolverEspecialidades(
                request.getEspecialidadIds(),
                request.getEspecialidades(),
                request.getEspecialidad()
        );
        if (resueltas.entidades().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una especialidad para el técnico.");
        }

        String nombresUnidos = String.join(", ", resueltas.nombres());

        Usuario tecnico = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto().trim())
                .correo(correoLimpio)
                .telefono(telefonoLimpio)
                .password(passwordEncoder.encode(pinLimpio))
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad(nombresUnidos)
                .especialidades(resueltas.entidades())
                .build();

        Usuario guardado = usuarioRepository.save(tecnico);
        log.info("Nuevo técnico registrado por administración: id={}, correo={}, telefono={}", guardado.getId(), guardado.getCorreo(), guardado.getTelefono());
        return mapearATecnicoResumen(guardado);
    }

    @Transactional
    public TecnicoResumenDto actualizarEstadoTecnico(Long id, ActualizarEstadoTecnicoRequest request) {
        Usuario tecnico = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado con ID: " + id));

        if (tecnico.getRol() != RolUsuario.TECNICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario especificado no tiene rol de técnico.");
        }

        boolean nuevoEstado = (request == null || request.getActivo() == null)
                ? !Boolean.TRUE.equals(tecnico.getActivo())
                : Boolean.TRUE.equals(request.getActivo());
        tecnico.setActivo(nuevoEstado);
        if (!nuevoEstado) {
            tecnico.setDisponible(false);
        }

        Usuario guardado = usuarioRepository.save(tecnico);
        log.info("Estado de técnico id={} actualizado a activo={}", id, nuevoEstado);
        return mapearATecnicoResumen(guardado);
    }

    @Transactional
    public TecnicoResumenDto actualizarTecnico(Long id, ActualizarTecnicoRequest request) {
        Usuario tecnico = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado con ID: " + id));

        if (tecnico.getRol() != RolUsuario.TECNICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario especificado no tiene rol de técnico.");
        }

        if (request.getNombreCompleto() != null && !request.getNombreCompleto().isBlank()) {
            tecnico.setNombreCompleto(request.getNombreCompleto().trim());
        }
        if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
            String correoLimpio = request.getCorreo().trim().toLowerCase();
            if (!correoLimpio.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato del correo electrónico no es válido.");
            }
            if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correoLimpio, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro usuario registrado con el correo: " + correoLimpio);
            }
            tecnico.setCorreo(correoLimpio);
        }
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            String telLimpio = request.getTelefono().trim().replaceAll("\\s+", "");
            if (!telLimpio.matches("^\\d{9}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de celular debe contener exactamente 9 dígitos numéricos.");
            }
            if (usuarioRepository.existsByTelefonoAndIdNot(telLimpio, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro usuario registrado con el número de celular: " + telLimpio);
            }
            tecnico.setTelefono(telLimpio);
        }

        boolean tieneEspecialidades = (request.getEspecialidadIds() != null && !request.getEspecialidadIds().isEmpty())
                || (request.getEspecialidades() != null && !request.getEspecialidades().isEmpty())
                || (request.getEspecialidad() != null && !request.getEspecialidad().isBlank());
        if (tieneEspecialidades) {
            EspecialidadesResueltas resueltas = resolverEspecialidades(
                    request.getEspecialidadIds(),
                    request.getEspecialidades(),
                    request.getEspecialidad()
            );
            if (resueltas.entidades().isEmpty()) {
                throw new IllegalArgumentException("Debe seleccionar al menos una especialidad válida para el técnico.");
            }
            tecnico.setEspecialidades(resueltas.entidades());
            tecnico.setEspecialidad(String.join(", ", resueltas.nombres()));
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            String pinLimpio = request.getPassword().trim();
            if (!pinLimpio.matches("^\\d{4}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El PIN de acceso debe tener exactamente 4 dígitos numéricos.");
            }
            tecnico.setPassword(passwordEncoder.encode(pinLimpio));
        }

        Usuario guardado = usuarioRepository.save(tecnico);
        log.info("Datos de técnico id={} actualizados por administración", id);
        return mapearATecnicoResumen(guardado);
    }

    private EspecialidadesResueltas resolverEspecialidades(
            List<Long> especialidadIds,
            List<String> especialidadesNombres,
            String especialidadSimple) {

        Set<Especialidad> resultado = new LinkedHashSet<>();

        if (especialidadIds != null && !especialidadIds.isEmpty()) {
            List<Long> idsValidos = especialidadIds.stream()
                    .filter(Objects::nonNull)
                    .toList();
            if (!idsValidos.isEmpty()) {
                List<Especialidad> encontradas = especialidadRepository.findAllById(idsValidos);
                for (Long idBuscado : idsValidos) {
                    encontradas.stream()
                            .filter(e -> idBuscado.equals(e.getId()))
                            .findFirst()
                            .ifPresent(e -> agregarEspecialidadSinDuplicados(resultado, e));
                }
                for (Especialidad esp : encontradas) {
                    if (esp.getId() == null) {
                        agregarEspecialidadSinDuplicados(resultado, esp);
                    }
                }
            }
        }

        if (especialidadesNombres != null && !especialidadesNombres.isEmpty()) {
            for (String item : especialidadesNombres) {
                if (item == null) {
                    continue;
                }
                for (String parte : item.split(",")) {
                    String nombreLimpio = parte.trim();
                    if (!nombreLimpio.isEmpty()) {
                        Especialidad esp = buscarOCrearEspecialidad(nombreLimpio);
                        agregarEspecialidadSinDuplicados(resultado, esp);
                    }
                }
            }
        }

        if (resultado.isEmpty() && especialidadSimple != null && !especialidadSimple.isBlank()) {
            for (String parte : especialidadSimple.split(",")) {
                String nombreLimpio = parte.trim();
                if (!nombreLimpio.isEmpty()) {
                    Especialidad esp = buscarOCrearEspecialidad(nombreLimpio);
                    agregarEspecialidadSinDuplicados(resultado, esp);
                }
            }
        }

        List<String> nombres = resultado.stream()
                .map(Especialidad::getNombre)
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .toList();

        return new EspecialidadesResueltas(resultado, nombres);
    }

    private Especialidad buscarOCrearEspecialidad(String nombre) {
        return especialidadRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> {
                    Especialidad nueva = Especialidad.builder()
                            .nombre(nombre)
                            .activo(true)
                            .build();
                    Especialidad guardada = especialidadRepository.save(nueva);
                    return guardada != null ? guardada : nueva;
                });
    }

    private void agregarEspecialidadSinDuplicados(Set<Especialidad> set, Especialidad candidata) {
        if (candidata == null) {
            return;
        }
        boolean yaExiste = set.stream().anyMatch(e ->
                (e.getId() != null && candidata.getId() != null && e.getId().equals(candidata.getId()))
                        || (e.getNombre() != null && candidata.getNombre() != null
                        && e.getNombre().trim().equalsIgnoreCase(candidata.getNombre().trim()))
        );
        if (!yaExiste) {
            set.add(candidata);
        }
    }

    private TecnicoResumenDto mapearATecnicoResumen(Usuario u) {
        List<Long> especialidadIds;
        List<String> especialidades;
        String especialidad;

        if (u.getEspecialidades() != null && !u.getEspecialidades().isEmpty()) {
            especialidadIds = u.getEspecialidades().stream()
                    .map(Especialidad::getId)
                    .filter(Objects::nonNull)
                    .toList();
            especialidades = u.getEspecialidades().stream()
                    .map(Especialidad::getNombre)
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .toList();
            if (!especialidades.isEmpty()) {
                especialidad = String.join(", ", especialidades);
            } else if (u.getEspecialidad() != null && !u.getEspecialidad().isBlank()) {
                especialidades = Arrays.stream(u.getEspecialidad().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                especialidad = especialidades.isEmpty() ? "General" : u.getEspecialidad();
                if (especialidades.isEmpty()) {
                    especialidades = List.of("General");
                }
            } else {
                especialidad = "General";
                especialidades = List.of("General");
            }
        } else if (u.getEspecialidad() != null && !u.getEspecialidad().isBlank()) {
            especialidades = Arrays.stream(u.getEspecialidad().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            especialidadIds = List.of();
            if (especialidades.isEmpty()) {
                especialidad = "General";
                especialidades = List.of("General");
            } else {
                especialidad = u.getEspecialidad();
            }
        } else {
            especialidad = "General";
            especialidades = List.of("General");
            especialidadIds = List.of();
        }

        return TecnicoResumenDto.builder()
                .id(u.getId())
                .nombreCompleto(u.getNombreCompleto())
                .correo(u.getCorreo())
                .telefono(u.getTelefono())
                .especialidad(especialidad)
                .especialidadIds(especialidadIds)
                .especialidades(especialidades)
                .activo(Boolean.TRUE.equals(u.getActivo()))
                .disponible(Boolean.TRUE.equals(u.getDisponible()))
                .build();
    }

    @Transactional(readOnly = true)
    public PerfilTecnicoDto obtenerPerfilPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correo));

        long ordenesCompletadas = ordenServicioRepository
                .findByTecnicoIdAndEstado(usuario.getId(), EstadoOrden.FINALIZADA).size();

        return PerfilTecnicoDto.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol().name())
                .disponible(Boolean.TRUE.equals(usuario.getDisponible()))
                .ordenesCompletadasHoy(ordenesCompletadas)
                .calificacionPromedio(5.0)
                .firmaUrl(usuario.getFirmaUrl())
                .firmaDigitalUrl(usuario.getFirmaUrl())
                .build();
    }

    @Transactional
    public PerfilTecnicoDto actualizarDisponibilidad(String correo, Boolean disponible) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correo));

        usuario.setDisponible(Boolean.TRUE.equals(disponible));
        usuarioRepository.save(usuario);

        return obtenerPerfilPorCorreo(correo);
    }

    public PerfilTecnicoDto actualizarFirma(String correo, String firmaBase64) {
        if (firmaBase64 == null || firmaBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La firma no puede estar vacía.");
        }

        // Subida externa a almacenamiento fuera de la transacción de base de datos
        String url;
        try {
            url = (firmaBase64.startsWith("http://") || firmaBase64.startsWith("https://"))
                    ? firmaBase64
                    : storageService.subirBase64(firmaBase64, "firmas-tecnicos");
        } catch (Exception e) {
            log.error("Error al subir firma de técnico a almacenamiento: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo procesar la firma digital.");
        }

        // Transacción rápida de BD para persistir la URL y retornar el perfil
        return transactionTemplate.execute(status -> {
            Usuario usuario = usuarioRepository.findByCorreo(correo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correo));

            usuario.setFirmaUrl(url);
            usuarioRepository.save(usuario);

            return obtenerPerfilPorCorreo(correo);
        });
    }
}
