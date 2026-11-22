package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarAfiliadoRequest;
import com.asistencias247.api.dto.CrearAfiliadoRequest;
import com.asistencias247.api.dto.DireccionAfiliadoDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.AfiliadoDireccion;
import com.asistencias247.api.repository.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
            sincronizarDirecciones(existente, request.getDirecciones(), request.getDireccion(), request.getDistrito(), request.getReferencia());
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
                .direccion("")
                .distrito("Villa El Salvador")
                .referencia("")
                .numeroCuenta(numeroCuenta)
                .activo(true)
                .build();

        sincronizarDirecciones(afiliado, request.getDirecciones(), request.getDireccion(), request.getDistrito(), request.getReferencia());

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
        sincronizarDirecciones(afiliado, request.getDirecciones(), request.getDireccion(), request.getDistrito(), request.getReferencia());

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

    private void sincronizarDirecciones(
            Afiliado afiliado,
            List<DireccionAfiliadoDto> dtos,
            String dirFallback,
            String distFallback,
            String refFallback) {
        if (afiliado.getDirecciones() == null) {
            afiliado.setDirecciones(new ArrayList<>());
        }

        List<DireccionAfiliadoDto> validas = dtos == null ? List.of() : dtos.stream()
                .filter(d -> d != null && d.getDireccion() != null && !d.getDireccion().isBlank())
                .toList();

        if (!validas.isEmpty()) {
            int principalIdx = 0;
            for (int i = 0; i < validas.size(); i++) {
                if (Boolean.TRUE.equals(validas.get(i).getEsPrincipal())) {
                    principalIdx = i;
                    break;
                }
            }

            afiliado.getDirecciones().clear();
            for (int i = 0; i < validas.size(); i++) {
                DireccionAfiliadoDto dto = validas.get(i);
                boolean esPrincipal = (i == principalIdx);
                int idx = i + 1;

                String etiqueta = (dto.getEtiqueta() != null && !dto.getEtiqueta().isBlank())
                        ? dto.getEtiqueta().trim()
                        : (esPrincipal ? "Principal" : "Dirección " + idx);
                String dirLimpia = dto.getDireccion().trim();
                String distLimpio = (dto.getDistrito() != null && !dto.getDistrito().isBlank())
                        ? dto.getDistrito().trim()
                        : "Villa El Salvador";
                String refLimpia = dto.getReferencia() != null ? dto.getReferencia().trim() : "";

                AfiliadoDireccion dir = AfiliadoDireccion.builder()
                        .afiliado(afiliado)
                        .etiqueta(etiqueta)
                        .direccion(dirLimpia)
                        .distrito(distLimpio)
                        .referencia(refLimpia)
                        .esPrincipal(esPrincipal)
                        .build();

                afiliado.getDirecciones().add(dir);

                if (esPrincipal) {
                    afiliado.setDireccion(dirLimpia);
                    afiliado.setDistrito(distLimpio);
                    afiliado.setReferencia(refLimpia);
                }
            }
            return;
        }

        String dirLimpia = dirFallback != null ? dirFallback.trim() : "";
        if (dirLimpia.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección es obligatoria");
        }
        String distLimpio = (distFallback != null && !distFallback.isBlank())
                ? distFallback.trim()
                : ((afiliado.getDistrito() != null && !afiliado.getDistrito().isBlank())
                    ? afiliado.getDistrito().trim()
                    : "Villa El Salvador");
        String refLimpia = refFallback != null ? refFallback.trim() : "";

        afiliado.setDireccion(dirLimpia);
        afiliado.setDistrito(distLimpio);
        afiliado.setReferencia(refLimpia);

        if (afiliado.getDirecciones().isEmpty()) {
            afiliado.getDirecciones().add(AfiliadoDireccion.builder()
                    .afiliado(afiliado)
                    .etiqueta("Principal")
                    .direccion(dirLimpia)
                    .distrito(distLimpio)
                    .referencia(refLimpia)
                    .esPrincipal(true)
                    .build());
        } else {
            AfiliadoDireccion principal = afiliado.getDirecciones().stream()
                    .filter(d -> Boolean.TRUE.equals(d.getEsPrincipal()))
                    .findFirst()
                    .orElse(afiliado.getDirecciones().get(0));

            for (AfiliadoDireccion d : afiliado.getDirecciones()) {
                d.setEsPrincipal(d == principal);
            }

            principal.setAfiliado(afiliado);
            if (principal.getEtiqueta() == null || principal.getEtiqueta().isBlank()) {
                principal.setEtiqueta("Principal");
            }
            principal.setDireccion(dirLimpia);
            principal.setDistrito(distLimpio);
            principal.setReferencia(refLimpia);
        }
    }

    /**
     * Normaliza datos opcionales para no romper restricciones NOT NULL heredadas
     * de la base de datos cuando el DNI o el teléfono aún no se conocen.
     */
    private String normalizarOpcional(String valor) {
        return valor != null ? valor.trim() : "";
    }
}
