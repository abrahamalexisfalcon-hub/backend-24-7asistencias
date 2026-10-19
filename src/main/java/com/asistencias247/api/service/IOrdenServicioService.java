package com.asistencias247.api.service;

import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IOrdenServicioService {

    OrdenDetalleAdminDto crearOrden(CrearOrdenRequest request);

    List<OrdenDetalleAdminDto> listarOrdenes(EstadoOrden estado);

    Page<OrdenDetalleAdminDto> listarOrdenesPaginadas(EstadoOrden estado, Pageable pageable);

    OrdenDetalleAdminDto obtenerDetalle(Long id);

    OrdenDetalleAdminDto cancelarOrden(Long id, CancelarOrdenRequest request);

    OrdenServicio marcarAsistidoNoRealizado(Long id, String motivo);

    OrdenDetalleAdminDto actualizarObservacionCancelacion(Long id, CancelarOrdenRequest request);

    String generarCorrelativoAnual(int anio);

    void eliminarOrden(Long id);
}
