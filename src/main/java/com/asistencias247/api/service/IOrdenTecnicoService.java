package com.asistencias247.api.service;

import com.asistencias247.api.dto.OrdenResumenTecnicoDto;

import java.util.List;

public interface IOrdenTecnicoService {

    List<OrdenResumenTecnicoDto> obtenerOrdenesPorTecnico(Long tecnicoId);

    List<OrdenResumenTecnicoDto> obtenerOrdenesPorCorreo(String correo);

    List<OrdenResumenTecnicoDto> obtenerAsignadas(Long tecnicoId);

    List<OrdenResumenTecnicoDto> obtenerAsignadasPorCorreo(String correo);

    List<OrdenResumenTecnicoDto> obtenerHistorial(Long tecnicoId);

    List<OrdenResumenTecnicoDto> obtenerHistorialPorCorreo(String correo);

    List<OrdenResumenTecnicoDto> obtenerTodasLasOrdenes();

    OrdenResumenTecnicoDto registrarLlegada(Long ordenId);

    OrdenResumenTecnicoDto registrarAsistidoNoRealizado(Long ordenId, String motivo);

    OrdenResumenTecnicoDto obtenerDetalleOrden(Long ordenId);
}
