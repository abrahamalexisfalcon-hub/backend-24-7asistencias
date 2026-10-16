package com.asistencias247.api.service;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.mapper.OrdenServicioMapper;
import com.asistencias247.api.repository.AfiliadoRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenServicioService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenServicioMapper ordenServicioMapper;
    private final com.asistencias247.api.service.notification.PushNotificationService pushNotificationService;

    @Transactional
    public OrdenDetalleAdminDto crearOrden(CrearOrdenRequest request) {
        Afiliado afiliado = afiliadoRepository.findById(request.getAfiliadoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Afiliado no encontrado con ID: " + request.getAfiliadoId()));

        Usuario tecnico = null;
        if (request.getTecnicoId() != null) {
            tecnico = usuarioRepository.findById(request.getTecnicoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado con ID: " + request.getTecnicoId()));
        }

        LocalDateTime fechaAsignacion = LocalDateTime.now();
        String codigoOrden = generarCorrelativoAnual(fechaAsignacion.getYear());

        OrdenServicio orden = OrdenServicio.builder()
                .codigoOrden(codigoOrden)
                .tipoServicio(request.getTipoServicio())
                .estado(EstadoOrden.PENDIENTE)
                .afiliado(afiliado)
                .tecnico(tecnico)
                .fechaAsignacion(fechaAsignacion)
                .build();

        OrdenServicio guardada = ordenServicioRepository.save(orden);

        if (tecnico != null) {
            try {
                pushNotificationService.enviarNotificacionANativo(
                        tecnico.getId(),
                        "¡Nueva Orden Asignada! #" + guardada.getCodigoOrden(),
                        "Tienes un nuevo servicio de " + guardada.getTipoServicio() + " asignado.",
                        "/tecnico/ordenes"
                );
            } catch (Exception ignored) {
            }
        }

        return ordenServicioMapper.toDetalleAdmin(guardada);
    }

    @Transactional(readOnly = true)
    public List<OrdenDetalleAdminDto> listarOrdenes(EstadoOrden estado) {
        List<OrdenServicio> ordenes = (estado != null)
                ? ordenServicioRepository.findByEstadoOrderByFechaAsignacionDesc(estado)
                : ordenServicioRepository.findAllByOrderByFechaAsignacionDesc();

        return ordenes.stream()
                .map(ordenServicioMapper::toDetalleAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenDetalleAdminDto obtenerDetalle(Long id) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        return ordenServicioMapper.toDetalleAdmin(orden);
    }

    @Transactional
    public OrdenDetalleAdminDto cancelarOrden(Long id, CancelarOrdenRequest request) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        if (orden.getEstado() == EstadoOrden.FINALIZADA) {
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

    @Transactional
    public void eliminarOrden(Long id) {
        ordenServicioRepository.deleteById(id);
    }
}
