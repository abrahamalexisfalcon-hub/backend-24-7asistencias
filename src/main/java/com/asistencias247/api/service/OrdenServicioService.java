package com.asistencias247.api.service;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.HojaServicioDetalleAdminDto;
import com.asistencias247.api.dto.MaterialDetalleAdminDto;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.AfiliadoRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenServicioService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final AfiliadoRepository afiliadoRepository;
    private final UsuarioRepository usuarioRepository;

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
        return mapearADetalleAdmin(guardada);
    }

    @Transactional(readOnly = true)
    public List<OrdenDetalleAdminDto> listarOrdenes(EstadoOrden estado) {
        List<OrdenServicio> ordenes = (estado != null)
                ? ordenServicioRepository.findByEstadoOrderByFechaAsignacionDesc(estado)
                : ordenServicioRepository.findAllByOrderByFechaAsignacionDesc();

        return ordenes.stream()
                .map(this::mapearADetalleAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenDetalleAdminDto obtenerDetalle(Long id) {
        OrdenServicio orden = ordenServicioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden de servicio no encontrada con ID: " + id));

        return mapearADetalleAdmin(orden);
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
        return mapearADetalleAdmin(guardada);
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

    private OrdenDetalleAdminDto mapearADetalleAdmin(OrdenServicio orden) {
        OrdenDetalleAdminDto.OrdenDetalleAdminDtoBuilder builder = OrdenDetalleAdminDto.builder()
                .id(orden.getId())
                .codigoOrden(orden.getCodigoOrden())
                .tipoServicio(orden.getTipoServicio())
                .estado(orden.getEstado())
                .fechaAsignacion(orden.getFechaAsignacion())
                .fechaLlegada(orden.getFechaLlegada())
                .fechaFinalizacion(orden.getFechaFinalizacion())
                .motivoCancelacion(orden.getMotivoCancelacion())
                .fechaCancelacion(orden.getFechaCancelacion());

        if (orden.getAfiliado() != null) {
            Afiliado af = orden.getAfiliado();
            builder.afiliadoId(af.getId())
                    .nombreAfiliado(af.getNombreCompleto())
                    .dniAfiliado(af.getDni())
                    .numeroCuentaAfiliado(af.getNumeroCuenta())
                    .telefonoAfiliado(af.getTelefono())
                    .direccionAfiliado(af.getDireccion())
                    .distritoAfiliado(af.getDistrito())
                    .referenciaAfiliado(af.getReferencia())
                    .direccion(af.getDireccion())
                    .distrito(af.getDistrito())
                    .cliente(OrdenDetalleAdminDto.ClienteInfoAdminDto.builder()
                            .id(af.getId())
                            .nombre(af.getNombreCompleto())
                            .direccion(af.getDireccion())
                            .referencia(af.getReferencia())
                            .distrito(af.getDistrito())
                            .telefono(af.getTelefono())
                            .build());
        }

        String tecFirmaUrl = null;
        if (orden.getTecnico() != null) {
            Usuario tec = orden.getTecnico();
            tecFirmaUrl = tec.getFirmaUrl();
            builder.tecnicoId(tec.getId())
                    .nombreTecnico(tec.getNombreCompleto())
                    .tecnicoNombre(tec.getNombreCompleto())
                    .correoTecnico(tec.getCorreo())
                    .telefonoTecnico(tec.getTelefono())
                    .tecnicoFirmaUrl(tecFirmaUrl)
                    .firmaTecnico(tecFirmaUrl)
                    .firmaTecnicoUrl(tecFirmaUrl)
                    .tecnico(OrdenDetalleAdminDto.TecnicoInfoAdminDto.builder()
                            .id(tec.getId())
                            .nombre(tec.getNombreCompleto())
                            .email(tec.getCorreo())
                            .telefono(tec.getTelefono())
                            .firmaUrl(tecFirmaUrl)
                            .firmaDigitalUrl(tecFirmaUrl)
                            .firmaDigital(tecFirmaUrl)
                            .firmaTecnico(tecFirmaUrl)
                            .build());
        }

        if (orden.getFechaAsignacion() != null) {
            builder.fechaCreacion(orden.getFechaAsignacion().toString());
            builder.fechaProgramada(orden.getFechaAsignacion().toString());
        }

        if (orden.getHojaServicio() != null) {
            HojaServicio hoja = orden.getHojaServicio();
            List<MaterialDetalleAdminDto> materialesDto = new ArrayList<>();

            if (hoja.getMateriales() != null) {
                materialesDto = hoja.getMateriales().stream().map(m -> MaterialDetalleAdminDto.builder()
                        .id(m.getId())
                        .materialId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                        .materialCatalogoId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                        .nombreCatalogo(m.getMaterial() != null ? m.getMaterial().getNombre() : null)
                        .esPersonalizado(m.getEsPersonalizado())
                        .descripcion(m.getDescripcion())
                        .cantidad(m.getCantidad())
                        .origen(Boolean.TRUE.equals(m.getEsPersonalizado()) ? "OTRO" : "CATALOGO")
                        .build()
                ).toList();
            }

            List<String> fotosEvidencia = new ArrayList<>();
            if (hoja.getFotoInicialUrl() != null && !hoja.getFotoInicialUrl().isBlank()) {
                fotosEvidencia.add(hoja.getFotoInicialUrl());
            }
            if (hoja.getFotoFinalUrl() != null && !hoja.getFotoFinalUrl().isBlank()) {
                fotosEvidencia.add(hoja.getFotoFinalUrl());
            }

            HojaServicioDetalleAdminDto hojaDto = HojaServicioDetalleAdminDto.builder()
                    .id(hoja.getId())
                    .evaluacionAveria(hoja.getEvaluacionAveria())
                    .detalleSolucion(hoja.getDetalleSolucion())
                    .sugerencias(hoja.getSugerencias())
                    .calificacion(hoja.getCalificacion())
                    .firmaDigitalUrl(hoja.getFirmaDigitalUrl())
                    .firmaTecnico(tecFirmaUrl)
                    .firmaTecnicoUrl(tecFirmaUrl)
                    .fotoInicialUrl(hoja.getFotoInicialUrl())
                    .fotoFinalUrl(hoja.getFotoFinalUrl())
                    .fotosEvidencia(fotosEvidencia)
                    .pdfActaUrl(hoja.getPdfActaUrl())
                    .materiales(materialesDto)
                    .build();

            builder.hojaServicio(hojaDto);
            builder.fotosEvidencia(fotosEvidencia);
            builder.fotoInicialUrl(hoja.getFotoInicialUrl());
            builder.fotoFinalUrl(hoja.getFotoFinalUrl());
        }

        return builder.build();
    }

    @Transactional
    public void eliminarOrden(Long id) {
        ordenServicioRepository.deleteById(id);
    }
}
