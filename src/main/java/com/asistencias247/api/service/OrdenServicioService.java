package com.asistencias247.api.service;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.Cuenta;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.mapper.OrdenServicioMapper;
import com.asistencias247.api.repository.AfiliadoRepository;
import com.asistencias247.api.repository.CuentaRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdenServicioService implements IOrdenServicioService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private final OrdenServicioRepository ordenServicioRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final OrdenServicioMapper ordenServicioMapper;
    private final OneSignalPushService oneSignalPushService;

    @Override
    @Transactional
    public OrdenDetalleAdminDto crearOrden(CrearOrdenRequest request) {
        String numeroCasoLimpio = request.getNumeroCaso() != null ? request.getNumeroCaso().trim() : "";
        String numeroOtLimpio = request.getNumeroOt() != null ? request.getNumeroOt().trim() : "";

        if (ordenServicioRepository.existsByNumeroCasoIgnoreCase(numeroCasoLimpio)) {
            throw new DataIntegrityViolationException("El Número de Caso '" + numeroCasoLimpio + "' ya se encuentra registrado en otra orden.");
        }

        if (ordenServicioRepository.existsByNumeroOtIgnoreCase(numeroOtLimpio)) {
            throw new DataIntegrityViolationException("El Número de OT '" + numeroOtLimpio + "' ya se encuentra registrado en otra orden.");
        }

        Cuenta cuenta = cuentaRepository.findById(request.getCuentaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta / Aseguradora no encontrada con ID: " + request.getCuentaId()));

        Afiliado afiliado = afiliadoRepository.findById(request.getAfiliadoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Afiliado no encontrado con ID: " + request.getAfiliadoId()));

        Usuario tecnico = null;
        if (request.getTecnicoId() != null) {
            tecnico = usuarioRepository.findById(request.getTecnicoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado con ID: " + request.getTecnicoId()));
        }

        LocalDateTime fechaAsignacion = LocalDateTime.now(ZONA_PERU);
        String codigoOrden = generarCorrelativoAnual(fechaAsignacion.getYear());

        OrdenServicio orden = OrdenServicio.builder()
                .codigoOrden(codigoOrden)
                .numeroCaso(numeroCasoLimpio)
                .numeroOt(numeroOtLimpio)
                .fechaPactada(request.getFechaPactada())
                .cuenta(cuenta)
                .tipoServicio(request.getTipoServicio())
                .estado(EstadoOrden.PENDIENTE)
                .afiliado(afiliado)
                .tecnico(tecnico)
                .fechaAsignacion(fechaAsignacion)
                .build();

        OrdenServicio guardada = ordenServicioRepository.save(orden);
        OrdenDetalleAdminDto ordenDto = ordenServicioMapper.toDetalleAdmin(guardada);

        notificarAsignacionTecnico(guardada);

        return ordenDto;
    }

    private void notificarAsignacionTecnico(OrdenServicio orden) {
        if (orden.getTecnico() == null) {
            log.warn("[OneSignal-TRACE] Orden ID: {} (#{}) creada sin técnico asignado. Se omite envío de push.",
                    orden.getId(), orden.getCodigoOrden());
            return;
        }
        try {
            String distrito = (orden.getAfiliado() != null && orden.getAfiliado().getDistrito() != null)
                    ? orden.getAfiliado().getDistrito()
                    : "Lima";
            String direccion = (orden.getAfiliado() != null && orden.getAfiliado().getDireccion() != null && !orden.getAfiliado().getDireccion().isBlank())
                    ? orden.getAfiliado().getDireccion()
                    : distrito;
            oneSignalPushService.enviarNotificacionAsignacion(
                    orden.getId(),
                    orden.getTecnico().getId(),
                    orden.getCodigoOrden(),
                    orden.getTipoServicio(),
                    direccion
            );
        } catch (Exception ex) {
            log.error("[OneSignal-ERROR] Error al disparar notificación de orden {}: {}", orden.getCodigoOrden(), ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenDetalleAdminDto> listarOrdenes(EstadoOrden estado) {
        List<OrdenServicio> ordenes = (estado != null)
                ? ordenServicioRepository.findByEstadoOrderByFechaAsignacionDesc(estado)
                : ordenServicioRepository.findAllByOrderByFechaAsignacionDesc();

        return ordenes.stream()
                .map(ordenServicioMapper::toDetalleAdmin)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenDetalleAdminDto> listarOrdenesPaginadas(EstadoOrden estado, Pageable pageable) {
        Page<OrdenServicio> pagina = (estado != null)
                ? ordenServicioRepository.findByEstadoOrderByFechaAsignacionDesc(estado, pageable)
                : ordenServicioRepository.findAllByOrderByFechaAsignacionDesc(pageable);

        return pagina.map(ordenServicioMapper::toDetalleAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenDetalleAdminDto obtenerDetalle(Long id) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        return ordenServicioMapper.toDetalleAdmin(orden);
    }

    @Override
    @Transactional
    public OrdenDetalleAdminDto cancelarOrden(Long id, CancelarOrdenRequest request) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        if (!orden.getEstado().esCancelable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede anular una orden con Hoja de Servicio finalizada y firmada");
        }

        String motivo = (request != null) ? request.getMotivoEfectivo() : "";
        if (motivo.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de la anulación debe tener al menos 5 caracteres");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setMotivoCancelacion(motivo);
        orden.setFechaCancelacion(LocalDateTime.now());

        OrdenServicio guardada = ordenServicioRepository.save(orden);
        return ordenServicioMapper.toDetalleAdmin(guardada);
    }

    @Override
    @Transactional
    public OrdenServicio marcarAsistidoNoRealizado(Long id, String motivo) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        validarPropiedadOrdenSiEsTecnico(orden);

        if (!orden.getEstado().esCancelable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede anular una orden ya finalizada");
        }

        String motivoLimpio = motivo != null ? motivo.trim() : "";
        if (motivoLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        orden.setMotivoCancelacion(motivoLimpio);
        orden.setFechaCancelacion(LocalDateTime.now());

        return ordenServicioRepository.save(orden);
    }

    @Override
    @Transactional
    public OrdenDetalleAdminDto actualizarObservacionCancelacion(Long id, CancelarOrdenRequest request) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        String observacion = (request != null) ? request.getMotivoEfectivo() : "";
        if (observacion.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La observación no puede estar vacía");
        }

        orden.setMotivoCancelacion(observacion);
        if (orden.getFechaCancelacion() == null) {
            orden.setFechaCancelacion(LocalDateTime.now());
        }

        OrdenServicio guardada = ordenServicioRepository.save(orden);
        return ordenServicioMapper.toDetalleAdmin(guardada);
    }

    private void validarPropiedadOrdenSiEsTecnico(OrdenServicio orden) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        String correoAutenticado = auth.getName();
        if (correoAutenticado == null || correoAutenticado.isBlank()) {
            return;
        }
        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado).orElse(null);
        if (usuario != null && usuario.getRol() == RolUsuario.TECNICO) {
            if (orden.getTecnico() != null && !orden.getTecnico().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar una orden asignada a otro técnico");
            }
        }
    }

    @Override
    public synchronized String generarCorrelativoAnual(int anio) {
        long totalEnAnio = ordenServicioRepository.countByAnioAsignacion(anio);
        long siguiente = totalEnAnio + 1;
        String codigoGenerado = String.format("ORD-%d-%03d", anio, siguiente);

        while (ordenServicioRepository.findByCodigoOrden(codigoGenerado).isPresent()) {
            siguiente++;
            codigoGenerado = String.format("ORD-%d-%03d", anio, siguiente);
        }

        return codigoGenerado;
    }

    @Override
    @Transactional
    public void eliminarOrden(Long id) {
        ordenServicioRepository.deleteById(id);
    }
}
