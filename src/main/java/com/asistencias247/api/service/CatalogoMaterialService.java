package com.asistencias247.api.service;

import com.asistencias247.api.dto.MaterialCatalogoDto;
import com.asistencias247.api.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoMaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public List<MaterialCatalogoDto> obtenerMaterialesActivos() {
        return materialRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(m -> MaterialCatalogoDto.builder()
                        .id(m.getId())
                        .nombre(m.getNombre())
                        .build())
                .toList();
    }
}
