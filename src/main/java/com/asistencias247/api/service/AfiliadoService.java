package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarAfiliadoRequest;
import com.asistencias247.api.dto.CrearAfiliadoRequest;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.repository.AfiliadoRepository;
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
public class AfiliadoService {

    private final AfiliadoRepository afiliadoRepository;

    @Transactional
    public Afiliado crearAfiliado(CrearAfiliadoRequest request) {
        String dni = normalizarOpcional(request.getDni());

        // Validar duplicado únicamente cuando el DNI fue proporcionado
        if (!dni.isEmpty() && afiliadoRepository.existsByDniAndActivoTrue(dni)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un afiliado activo con el DNI: " + dni);
        }

        // Si ya existía inactivo con ese DNI, lo reactivamos y actualizamos sus datos
        Afiliado existente = dni.isEmpty() ? null : afiliadoRepository.findByDni(dni).orElse(null);
        if (existente != null && !Boolean.TRUE.equals(existente.getActivo())) {
            existente.setActivo(true);
            existente.setNombreCompleto(request.getNombreCompleto().trim());
            existente.setTelefono(normalizarOpcional(request.getTelefono()));
            existente.setDireccion(request.getDireccion().trim());
            if (request.getDistrito() != null) existente.setDistrito(request.getDistrito().trim());
            if (request.getReferencia() != null) existente.setReferencia(request.getReferencia().trim());
            Afiliado reactivado = afiliadoRepository.save(existente);
            log.info("Afiliado DNI {} reactivado exitosamente con ID {}", dni, reactivado.getId());
            return reactivado;
        }

        String numeroCuenta = (request.getNumeroCuenta() != null && !request.getNumeroCuenta().isBlank())
                ? request.getNumeroCuenta().trim()
                : (!dni.isEmpty()
                    ? "CTA-" + dni
                    : "CTA-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Afiliado afiliado = Afiliado.builder()
                .nombreCompleto(request.getNombreCompleto().trim())
                .dni(dni)
                .telefono(normalizarOpcional(request.getTelefono()))
                .direccion(request.getDireccion().trim())
                .distrito(request.getDistrito() != null ? request.getDistrito().trim() : "Villa El Salvador")
                .referencia(request.getReferencia() != null ? request.getReferencia().trim() : "")
                .numeroCuenta(numeroCuenta)
                .activo(true)
                .build();

        Afiliado guardado = afiliadoRepository.save(afiliado);
        log.info("Nuevo afiliado registrado con ID {} y DNI {}", guardado.getId(), dni.isEmpty() ? "(pendiente)" : dni);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Afiliado> listarAfiliados(String query) {
        if (query != null && !query.trim().isEmpty()) {
            return afiliadoRepository.buscarActivos(query.trim());
        }
        return afiliadoRepository.findByActivoTrueOrderByNombreCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Afiliado obtenerPorId(Long id) {
        return afiliadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Afiliado no encontrado con ID: " + id));
    }

    @Transactional
    public Afiliado actualizarAfiliado(Long id, ActualizarAfiliadoRequest request) {
        Afiliado afiliado = obtenerPorId(id);

        // Si se envió un DNI y es diferente, validar que no esté duplicado en otro afiliado activo
        if (request.getDni() != null && !request.getDni().isBlank()) {
            String nuevoDni = request.getDni().trim();
            if (!nuevoDni.equalsIgnoreCase(afiliado.getDni())) {
                afiliadoRepository.findByDniAndActivoTrue(nuevoDni).ifPresent(otro -> {
                    if (!otro.getId().equals(id)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro afiliado activo con el DNI: " + nuevoDni);
                    }
                });
                afiliado.setDni(nuevoDni);
            }
        }

        afiliado.setNombreCompleto(request.getNombreCompleto().trim());
        afiliado.setTelefono(normalizarOpcional(request.getTelefono()));
        afiliado.setDireccion(request.getDireccion().trim());

        if (request.getDistrito() != null && !request.getDistrito().isBlank()) {
            afiliado.setDistrito(request.getDistrito().trim());
        }
        afiliado.setReferencia(request.getReferencia() != null ? request.getReferencia().trim() : "");

        Afiliado actualizado = afiliadoRepository.save(afiliado);
        log.info("Afiliado ID {} actualizado correctamente", actualizado.getId());
        return actualizado;
    }

    @Transactional
    public void eliminarAfiliado(Long id) {
        Afiliado afiliado = obtenerPorId(id);
        afiliado.setActivo(false);
        afiliadoRepository.save(afiliado);
        log.info("Afiliado ID {} ('{}') dado de baja lógica.", id, afiliado.getNombreCompleto());
    }

    /**
     * Normaliza datos opcionales para no romper restricciones NOT NULL heredadas
     * de la base de datos cuando el DNI o el teléfono aún no se conocen.
     */
    private String normalizarOpcional(String valor) {
        return valor != null ? valor.trim() : "";
    }
}
