package com.asistencias247.api.service;

import com.asistencias247.api.service.storage.StorageService;
import com.asistencias247.api.dto.ActualizarEstadoTecnicoRequest;
import com.asistencias247.api.dto.ActualizarTecnicoRequest;
import com.asistencias247.api.dto.CrearTecnicoRequest;
import com.asistencias247.api.dto.PerfilTecnicoDto;
import com.asistencias247.api.dto.TecnicoResumenDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.exception.RecursoNoEncontradoException;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TecnicoService {

    private final UsuarioRepository usuarioRepository;
    private final OrdenServicioRepository ordenServicioRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

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
        String telefonoLimpio = request.getTelefono().replaceAll("\\D", "");
        String pinLimpio = request.getPassword().trim();

        if (telefonoLimpio.length() < 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de teléfono debe tener al menos 9 dígitos numéricos.");
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

        Usuario tecnico = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto().trim())
                .correo(correoLimpio)
                .telefono(telefonoLimpio)
                .password(passwordEncoder.encode(pinLimpio))
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad(request.getEspecialidad().trim())
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

        boolean nuevoEstado = Boolean.TRUE.equals(request.getActivo());
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
            String telLimpio = request.getTelefono().replaceAll("\\D", "");
            if (telLimpio.length() < 9) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de teléfono debe tener al menos 9 dígitos numéricos.");
            }
            if (usuarioRepository.existsByTelefonoAndIdNot(telLimpio, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro usuario registrado con el número de celular: " + telLimpio);
            }
            tecnico.setTelefono(telLimpio);
        }
        if (request.getEspecialidad() != null && !request.getEspecialidad().isBlank()) {
            tecnico.setEspecialidad(request.getEspecialidad().trim());
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

    private TecnicoResumenDto mapearATecnicoResumen(Usuario u) {
        return TecnicoResumenDto.builder()
                .id(u.getId())
                .nombreCompleto(u.getNombreCompleto())
                .correo(u.getCorreo())
                .telefono(u.getTelefono())
                .especialidad(u.getEspecialidad() != null ? u.getEspecialidad() : "General")
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
