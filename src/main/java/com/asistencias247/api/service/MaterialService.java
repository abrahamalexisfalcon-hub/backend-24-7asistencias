package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarMaterialRequest;
import com.asistencias247.api.dto.CrearMaterialRequest;
import com.asistencias247.api.dto.MaterialDto;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.Material;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.MaterialRepository;
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
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final EspecialidadRepository especialidadRepository;

    @Transactional(readOnly = true)
    public List<MaterialDto> listarActivos(Long especialidadId, String especialidadNombre) {
        List<Material> materiales;
        if (especialidadId != null) {
            materiales = materialRepository.findByEspecialidadIdAndActivoTrueOrderByNombreAsc(especialidadId);
        } else if (especialidadNombre != null && !especialidadNombre.trim().isEmpty()) {
            materiales = materialRepository.findByEspecialidadNombreAndActivoTrueOrderByNombreAsc(especialidadNombre.trim());
        } else {
            materiales = materialRepository.findByActivoTrueOrderByNombreAsc();
        }

        return materiales.stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialDto> listarPorEspecialidad(Long especialidadId) {
        List<Material> materiales;
        if (especialidadId != null) {
            materiales = materialRepository.findByEspecialidadIdOrderByNombreAsc(especialidadId);
        } else {
            materiales = materialRepository.findByActivoTrueOrderByNombreAsc();
        }
        return materiales.stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialDto obtenerPorId(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado con ID: " + id));
        return mapearADto(material);
    }

    @Transactional
    public MaterialDto crearMaterial(CrearMaterialRequest request) {
        String nombreNormalizado = request.getNombre().trim();
        String unidadNormalizada = request.getUnidad().trim();

        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada con ID: " + request.getEspecialidadId()));

        if (materialRepository.existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrue(nombreNormalizado, especialidad.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe un material con el nombre '" + nombreNormalizado + "' para la especialidad " + especialidad.getNombre());
        }

        Material nuevo = Material.builder()
                .nombre(nombreNormalizado)
                .unidad(unidadNormalizada)
                .especialidad(especialidad)
                .activo(true)
                .build();

        Material guardado = materialRepository.save(nuevo);
        log.info("Nuevo material creado ID {}: '{}' ({}) para especialidad '{}'",
                guardado.getId(), guardado.getNombre(), guardado.getUnidad(), especialidad.getNombre());

        return mapearADto(guardado);
    }

    @Transactional
    public MaterialDto actualizarMaterial(Long id, ActualizarMaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado con ID: " + id));

        String nuevoNombre = request.getNombre().trim();
        String nuevaUnidad = request.getUnidad().trim();

        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Especialidad no encontrada con ID: " + request.getEspecialidadId()));

        if (materialRepository.existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrueAndIdNot(nuevoNombre, especialidad.getId(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe otro material con el nombre '" + nuevoNombre + "' para la especialidad " + especialidad.getNombre());
        }

        material.setNombre(nuevoNombre);
        material.setUnidad(nuevaUnidad);
        material.setEspecialidad(especialidad);
        if (request.getActivo() != null) {
            material.setActivo(request.getActivo());
        }

        Material actualizado = materialRepository.save(material);
        log.info("Material ID {} actualizado a '{}' ({}) en especialidad '{}'",
                actualizado.getId(), actualizado.getNombre(), actualizado.getUnidad(), especialidad.getNombre());

        return mapearADto(actualizado);
    }

    @Transactional
    public void eliminarMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado con ID: " + id));

        material.setActivo(false);
        materialRepository.save(material);
        log.info("Material ID {} desactivado (soft delete): {}", id, material.getNombre());
    }

    private MaterialDto mapearADto(Material m) {
        return MaterialDto.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .unidad(m.getUnidad())
                .activo(m.getActivo())
                .especialidadId(m.getEspecialidad() != null ? m.getEspecialidad().getId() : null)
                .especialidadNombre(m.getEspecialidad() != null ? m.getEspecialidad().getNombre() : null)
                .build();
    }
}
