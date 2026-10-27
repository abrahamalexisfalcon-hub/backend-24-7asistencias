package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarCuentaRequest;
import com.asistencias247.api.dto.CrearCuentaRequest;
import com.asistencias247.api.dto.CuentaDto;
import com.asistencias247.api.entity.Cuenta;
import com.asistencias247.api.repository.CuentaRepository;
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
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    @Transactional(readOnly = true)
    public List<CuentaDto> listarActivas() {
        return cuentaRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CuentaDto> listarTodas() {
        return cuentaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(this::mapearADto)
                .toList();
    }

    @Transactional
    public CuentaDto crearCuenta(CrearCuentaRequest request) {
        String nombreNormalizado = request.getNombre().trim();

        if (cuentaRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            Cuenta existente = cuentaRepository.findByNombreIgnoreCase(nombreNormalizado).orElse(null);
            if (existente != null && !Boolean.TRUE.equals(existente.getActivo())) {
                existente.setActivo(true);
                Cuenta reactivada = cuentaRepository.save(existente);
                log.info("Cuenta reactivada: {}", reactivada.getNombre());
                return mapearADto(reactivada);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una cuenta activa con el nombre: " + nombreNormalizado);
        }

        Cuenta nueva = Cuenta.builder()
                .nombre(nombreNormalizado)
                .activo(true)
                .build();

        Cuenta guardada = cuentaRepository.save(nueva);
        log.info("Nueva cuenta creada: {}", guardada.getNombre());
        return mapearADto(guardada);
    }

    @Transactional
    public CuentaDto actualizarCuenta(Long id, ActualizarCuentaRequest request) {
        String nuevoNombre = request.getNombre().trim();

        Cuenta cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id));

        cuentaRepository.findByNombreIgnoreCase(nuevoNombre).ifPresent(existente -> {
            if (!existente.getId().equals(id) && Boolean.TRUE.equals(existente.getActivo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otra cuenta activa con el nombre: " + nuevoNombre);
            }
        });

        cuenta.setNombre(nuevoNombre);
        if (request.getActivo() != null) {
            cuenta.setActivo(request.getActivo());
        }

        Cuenta actualizada = cuentaRepository.save(cuenta);
        log.info("Cuenta ID {} actualizada a '{}'", id, actualizada.getNombre());
        return mapearADto(actualizada);
    }

    @Transactional
    public CuentaDto toggleEstadoCuenta(Long id) {
        Cuenta cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id));
        cuenta.setActivo(!Boolean.TRUE.equals(cuenta.getActivo()));
        Cuenta guardada = cuentaRepository.save(cuenta);
        log.info("Estado de cuenta ID {} cambiado a activo={}", id, guardada.getActivo());
        return mapearADto(guardada);
    }

    @Transactional
    public void eliminarCuenta(Long id) {
        Cuenta cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada con ID: " + id));
        cuenta.setActivo(false);
        cuentaRepository.save(cuenta);
        log.info("Cuenta dada de baja lógicamente: {}", cuenta.getNombre());
    }

    private CuentaDto mapearADto(Cuenta cuenta) {
        return CuentaDto.builder()
                .id(cuenta.getId())
                .nombre(cuenta.getNombre())
                .activo(cuenta.getActivo())
                .createdAt(cuenta.getCreatedAt())
                .updatedAt(cuenta.getUpdatedAt())
                .build();
    }
}
