package com.asistencias247.api.service;

import com.asistencias247.api.dto.OrdenResumenTecnicoDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.Usuario;
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
public class OrdenTecnicoService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerOrdenesPorTecnico(Long tecnicoId) {
        return ordenServicioRepository.findByTecnicoIdOrderByFechaAsignacionDesc(tecnicoId)
                .stream()
                .map(this::convertirAResumenDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerOrdenesPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            return List.of();
        }
        return obtenerOrdenesPorTecnico(usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerAsignadas(Long tecnicoId) {
        return ordenServicioRepository.findByTecnicoIdAndEstadoNotOrderByFechaAsignacionDesc(tecnicoId, EstadoOrden.FINALIZADA)
                .stream()
                .map(this::convertirAResumenDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerAsignadasPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            return List.of();
        }
        return obtenerAsignadas(usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerHistorial(Long tecnicoId) {
        return ordenServicioRepository.findByTecnicoIdAndEstadoOrderByFechaAsignacionDesc(tecnicoId, EstadoOrden.FINALIZADA)
                .stream()
                .map(this::convertirAResumenDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerHistorialPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            return List.of();
        }
        return obtenerHistorial(usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenTecnicoDto> obtenerTodasLasOrdenes() {
        return ordenServicioRepository.findAllByOrderByFechaAsignacionDesc()
                .stream()
                .map(this::convertirAResumenDto)
                .toList();
    }

    @Transactional
    public OrdenResumenTecnicoDto registrarLlegada(Long ordenId) {
        OrdenServicio orden = ordenServicioRepository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + ordenId));

        orden.setEstado(EstadoOrden.EN_DOMICILIO);
        orden.setFechaLlegada(LocalDateTime.now());
        OrdenServicio ordenActualizada = ordenServicioRepository.save(orden);

        return convertirAResumenDto(ordenActualizada);
    }

    @Transactional(readOnly = true)
    public OrdenResumenTecnicoDto obtenerDetalleOrden(Long ordenId) {
        OrdenServicio orden = ordenServicioRepository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + ordenId));
        return convertirAResumenDto(orden);
    }

    private OrdenResumenTecnicoDto convertirAResumenDto(OrdenServicio orden) {
        String fechaProg = orden.getFechaAsignacion() != null
                ? "Hoy, " + orden.getFechaAsignacion().toLocalTime().toString().substring(0, 5)
                : "Hoy, 10:30 AM";
        String horaLleg = orden.getFechaLlegada() != null
                ? orden.getFechaLlegada().toLocalTime().toString().substring(0, 5) + " AM"
                : null;

        String tecNombre = orden.getTecnico() != null ? orden.getTecnico().getNombreCompleto() : null;
        String tecFirmaUrl = orden.getTecnico() != null ? orden.getTecnico().getFirmaUrl() : null;

        com.asistencias247.api.entity.HojaServicio hoja = orden.getHojaServicio();
        String firmaDigitalUrl = hoja != null ? hoja.getFirmaDigitalUrl() : null;
        String evaluacion = hoja != null ? hoja.getEvaluacionAveria() : null;
        String solucion = hoja != null ? hoja.getDetalleSolucion() : null;
        String sugerencias = hoja != null ? hoja.getSugerencias() : null;
        Integer calificacion = hoja != null ? hoja.getCalificacion() : null;
        String fotoInicialUrl = hoja != null ? hoja.getFotoInicialUrl() : null;
        String fotoFinalUrl = hoja != null ? hoja.getFotoFinalUrl() : null;
        Long hojaServicioId = hoja != null ? hoja.getId() : null;

        java.util.List<com.asistencias247.api.dto.MaterialConsumidoDto> mats = null;
        if (hoja != null && hoja.getMateriales() != null) {
            mats = hoja.getMateriales().stream()
                    .map(m -> com.asistencias247.api.dto.MaterialConsumidoDto.builder()
                            .materialId(m.getMaterial() != null ? m.getMaterial().getId() : null)
                            .descripcion(m.getDescripcion())
                            .cantidad(m.getCantidad())
                            .build())
                    .toList();
        }

        String nomAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getNombreCompleto() != null
                ? orden.getAfiliado().getNombreCompleto() : "Cliente 24/7";
        String dirAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getDireccion() != null
                ? orden.getAfiliado().getDireccion() : "Dirección no especificada";
        String refAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getReferencia() != null
                ? orden.getAfiliado().getReferencia() : "";
        String distAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getDistrito() != null
                ? orden.getAfiliado().getDistrito() : "Lima";
        String dniAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getDni() != null
                ? orden.getAfiliado().getDni() : "458921456";
        String ctaAfiliado = orden.getAfiliado() != null && orden.getAfiliado().getNumeroCuenta() != null
                ? orden.getAfiliado().getNumeroCuenta() : "CTA-8821";

        java.util.List<String> fotosEvidencia = new java.util.ArrayList<>();
        if (fotoInicialUrl != null && !fotoInicialUrl.isBlank()) fotosEvidencia.add(fotoInicialUrl);
        if (fotoFinalUrl != null && !fotoFinalUrl.isBlank()) fotosEvidencia.add(fotoFinalUrl);

        return OrdenResumenTecnicoDto.builder()
                .id(orden.getId())
                .codigoOrden(orden.getCodigoOrden() != null ? orden.getCodigoOrden() : "ORD-" + orden.getId())
                .tipoServicio(orden.getTipoServicio() != null ? orden.getTipoServicio() : "General")
                .estado(orden.getEstado())
                .nombreAfiliado(nomAfiliado)
                .direccion(dirAfiliado)
                .referencia(refAfiliado)
                .distrito(distAfiliado)
                .dniAfiliado(dniAfiliado)
                .codigoCuenta(ctaAfiliado)
                .fechaProgramada(fechaProg)
                .horaLlegada(horaLleg)
                .tecnicoNombre(tecNombre)
                .tecnicoFirmaUrl(tecFirmaUrl)
                .firmaDigitalUrl(firmaDigitalUrl)
                .firmaAfiliadoUrl(firmaDigitalUrl)
                .evaluacionAveria(evaluacion)
                .detalleSolucion(solucion)
                .sugerencias(sugerencias)
                .calificacion(calificacion)
                .fotoInicialUrl(fotoInicialUrl)
                .fotoFinalUrl(fotoFinalUrl)
                .fotosEvidencia(fotosEvidencia)
                .hojaServicioId(hojaServicioId)
                .materiales(mats)
                .build();
    }
}
