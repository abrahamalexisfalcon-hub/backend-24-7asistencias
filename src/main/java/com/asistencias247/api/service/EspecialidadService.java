package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarEspecialidadRequest;
import com.asistencias247.api.dto.CrearEspecialidadRequest;
import com.asistencias247.api.dto.EspecialidadDto;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenServicioRepository ordenServicioRepository;

    @Transactional(readOnly = true)
    public List<EspecialidadDto> listarActivas() {
        return especialidadRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional
    public EspecialidadDto crearEspecialidad(CrearEspecialidadRequest request) {
        String nombreNormalizado = request.getNombre().trim();

        if (especialidadRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            // Si ya existía inactiva, la reactivamos
            Especialidad existente = especialidadRepository.findByNombreIgnoreCase(nombreNormalizado).orElse(null);
            if (existente != null && !Boolean.TRUE.equals(existente.getActivo())) {
                existente.setActivo(true);
                Especialidad reactivada = especialidadRepository.save(existente);
                log.info("Especialidad reactivada: {}", reactivada.getNombre());
                return mapearADto(reactivada);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una especialidad con el nombre: " + nombreNormalizado);
        }

        Especialidad nueva = Especialidad.builder()
                .nombre(nombreNormalizado)
                .activo(true)
                .build();

        Especialidad guardada = especialidadRepository.save(nueva);
        log.info("Nueva especialidad creada: {}", guardada.getNombre());
        return mapearADto(guardada);
    }

    @Transactional
    public EspecialidadDto actualizarEspecialidad(Long id, ActualizarEspecialidadRequest request) {
        String nuevoNombre = request.getNombre().trim();

        Especialidad esp = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada con ID: " + id));

        String nombreAnterior = esp.getNombre();

        // Si el nombre no cambió, responder directamente
        if (nombreAnterior.equalsIgnoreCase(nuevoNombre)) {
            esp.setNombre(nuevoNombre);
            return mapearADto(especialidadRepository.save(esp));
        }

        // Validar que no exista otra especialidad activa con el mismo nombre
        especialidadRepository.findByNombreIgnoreCase(nuevoNombre).ifPresent(existente -> {
            if (!existente.getId().equals(id) && Boolean.TRUE.equals(existente.getActivo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otra especialidad activa con el nombre: " + nuevoNombre);
            }
        });

        esp.setNombre(nuevoNombre);
        Especialidad actualizada = especialidadRepository.save(esp);

        // Actualizar en masa técnicos y órdenes de trabajo para mantener integridad referencial
        int tecnicosActualizados = usuarioRepository.actualizarEspecialidadMasiva(nombreAnterior, nuevoNombre);
        int ordenesActualizadas = ordenServicioRepository.actualizarTipoServicioMasivo(nombreAnterior, nuevoNombre);

        log.info("Especialidad ID {} actualizada de '{}' a '{}'. Técnicos actualizados: {}, Órdenes actualizadas: {}",
                id, nombreAnterior, nuevoNombre, tecnicosActualizados, ordenesActualizadas);

        return mapearADto(actualizada);
    }

    @Transactional
    public void eliminarEspecialidad(Long id) {
        Especialidad esp = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada con ID: " + id));

        esp.setActivo(false);
        especialidadRepository.save(esp);
        log.info("Especialidad ID {} ('{}') dada de baja lógica (activo = false).", id, esp.getNombre());
    }

    private EspecialidadDto mapearADto(Especialidad esp) {
        return EspecialidadDto.builder()
                .id(esp.getId())
                .nombre(esp.getNombre())
                .activo(esp.getActivo())
                .build();
    }
}
