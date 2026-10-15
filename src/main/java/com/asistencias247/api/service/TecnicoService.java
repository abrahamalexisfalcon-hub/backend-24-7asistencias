package com.asistencias247.api.service;

import com.asistencias247.api.config.CloudinaryService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TecnicoService {

    private final UsuarioRepository usuarioRepository;
    private final OrdenServicioRepository ordenServicioRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;

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
        if (usuarioRepository.existsByCorreo(request.getCorreo().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un usuario registrado con el correo: " + request.getCorreo());
        }

        Usuario tecnico = Usuario.builder()
                .nombreCompleto(request.getNombreCompleto().trim())
                .correo(request.getCorreo().trim().toLowerCase())
                .telefono(request.getTelefono().trim())
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad(request.getEspecialidad().trim())
                .build();

        Usuario guardado = usuarioRepository.save(tecnico);
        log.info("Nuevo técnico registrado por administración: id={}, correo={}", guardado.getId(), guardado.getCorreo());
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
        if (request.getTelefono() != null && !request.getTelefono().isBlank()) {
            tecnico.setTelefono(request.getTelefono().trim());
        }
        if (request.getEspecialidad() != null && !request.getEspecialidad().isBlank()) {
            tecnico.setEspecialidad(request.getEspecialidad().trim());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            tecnico.setPassword(passwordEncoder.encode(request.getPassword().trim()));
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

    @Transactional
    public PerfilTecnicoDto actualizarFirma(String correo, String firmaBase64) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correo));

        if (firmaBase64 == null || firmaBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La firma no puede estar vacía.");
        }

        try {
            String url = firmaBase64.startsWith("http://") || firmaBase64.startsWith("https://")
                    ? firmaBase64
                    : cloudinaryService.subirBase64(firmaBase64, "firmas-tecnicos");
            usuario.setFirmaUrl(url);
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.error("Error al subir firma de técnico a Cloudinary: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo procesar la firma digital.");
        }

        return obtenerPerfilPorCorreo(correo);
    }
}
