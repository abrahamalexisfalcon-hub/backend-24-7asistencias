package com.asistencias247.api.service;

import com.asistencias247.api.dto.CrearMaterialCatalogoRequest;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.Material;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialCatalogoService {

    private final MaterialRepository materialRepository;
    private final EspecialidadRepository especialidadRepository;

    @Transactional
    public Material crearMaterial(CrearMaterialCatalogoRequest request) {
        String nombre = request.getNombre().trim();
        Especialidad esp = especialidadRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay especialidades disponibles para asociar el material"));

        if (materialRepository.existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrue(nombre, esp.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un material activo con el nombre: " + nombre);
        }

        Material nuevo = Material.builder()
                .nombre(nombre)
                .unidad("Unidad")
                .especialidad(esp)
                .activo(true)
                .build();

        return materialRepository.save(nuevo);
    }

    @Transactional(readOnly = true)
    public List<Material> listarTodos() {
        return materialRepository.findAll(Sort.by("nombre"));
    }

    @Transactional
    public Material toggleActivo(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado con ID: " + id));

        material.setActivo(!Boolean.TRUE.equals(material.getActivo()));
        return materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public Material obtenerPorId(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material no encontrado con ID: " + id));
    }
}
