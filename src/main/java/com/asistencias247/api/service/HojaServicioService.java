package com.asistencias247.api.service;

import com.asistencias247.api.config.CloudinaryService;
import com.asistencias247.api.dto.CierreHojaServicioRequest;
import com.asistencias247.api.dto.MaterialConsumidoDto;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.HojaServicio;
import com.asistencias247.api.entity.Material;
import com.asistencias247.api.entity.MaterialServicio;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.repository.HojaServicioRepository;
import com.asistencias247.api.repository.MaterialRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;


@Slf4j
@Service
@RequiredArgsConstructor
public class HojaServicioService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final HojaServicioRepository hojaServicioRepository;
    private final MaterialRepository materialRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public HojaServicio cerrarHojaServicio(Long ordenId, CierreHojaServicioRequest request) {
        OrdenServicio orden = ordenServicioRepository.findById(ordenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + ordenId));

        HojaServicio hoja = orden.getHojaServicio();
        if (hoja == null) {
            hoja = HojaServicio.builder()
                    .ordenServicio(orden)
                    .materiales(new ArrayList<>())
                    .build();
        }

        if (request != null) {
            if (request.getEvaluacionAveria() != null) {
                hoja.setEvaluacionAveria(request.getEvaluacionAveria());
            }
            if (request.getDetalleSolucion() != null) {
                hoja.setDetalleSolucion(request.getDetalleSolucion());
            }
            if (request.getSugerencias() != null) {
                hoja.setSugerencias(request.getSugerencias());
            }
            if (request.getCalificacion() != null) {
                hoja.setCalificacion(request.getCalificacion());
            }
            if (request.getFirmaDigitalUrl() != null && !request.getFirmaDigitalUrl().isBlank()) {
                String urlFirma = procesarImagenOUrl(request.getFirmaDigitalUrl(), "firmas");
                hoja.setFirmaDigitalUrl(urlFirma);
                log.info("Firma del afiliado procesada y asignada para orden {}: {}", ordenId, urlFirma);
            }
            if (request.getFirmaTecnicoUrl() != null && !request.getFirmaTecnicoUrl().isBlank()) {
                String urlFirmaTec = procesarImagenOUrl(request.getFirmaTecnicoUrl(), "firmas-tecnicos");
                if (orden.getTecnico() != null && (orden.getTecnico().getFirmaUrl() == null || orden.getTecnico().getFirmaUrl().isBlank())) {
                    orden.getTecnico().setFirmaUrl(urlFirmaTec);
                    log.info("Firma del técnico procesada y persistida en perfil para orden {}: {}", ordenId, urlFirmaTec);
                }
            }
            if (request.getFotoInicialUrl() != null && !request.getFotoInicialUrl().isBlank()) {
                hoja.setFotoInicialUrl(procesarImagenOUrl(request.getFotoInicialUrl(), "evidencias"));
            }
            if (request.getFotoFinalUrl() != null && !request.getFotoFinalUrl().isBlank()) {
                hoja.setFotoFinalUrl(procesarImagenOUrl(request.getFotoFinalUrl(), "evidencias"));
            }

            if (hoja.getMateriales() == null) {
                hoja.setMateriales(new ArrayList<>());
            } else {
                hoja.getMateriales().clear();
            }

            if (request.getMateriales() != null) {
                for (MaterialConsumidoDto item : request.getMateriales()) {
                    MaterialServicio materialServicio = MaterialServicio.builder()
                            .hojaServicio(hoja)
                            .cantidad(item.getCantidad() != null ? item.getCantidad() : 1)
                            .build();

                    if (item.getMaterialId() != null) {
                        Material material = materialRepository.findById(item.getMaterialId()).orElse(null);
                        if (material != null) {
                            materialServicio.setMaterial(material);
                            materialServicio.setEsPersonalizado(false);
                            materialServicio.setDescripcion(
                                    item.getDescripcion() != null && !item.getDescripcion().isBlank()
                                            ? item.getDescripcion()
                                            : material.getNombre()
                            );
                        } else {
                            materialServicio.setMaterial(null);
                            materialServicio.setEsPersonalizado(true);
                            materialServicio.setDescripcion(item.getDescripcion() != null && !item.getDescripcion().isBlank() ? item.getDescripcion() : "Material");
                        }
                    } else {
                        materialServicio.setMaterial(null);
                        materialServicio.setEsPersonalizado(true);
                        materialServicio.setDescripcion(item.getDescripcion() != null && !item.getDescripcion().isBlank() ? item.getDescripcion() : "Material personalizado");
                    }

                    hoja.getMateriales().add(materialServicio);
                }
            }
        }

        HojaServicio hojaGuardada = hojaServicioRepository.saveAndFlush(hoja);

        orden.setHojaServicio(hojaGuardada);
        orden.setEstado(EstadoOrden.FINALIZADA);
        orden.setFechaFinalizacion(LocalDateTime.now());
        orden.setFechaFin(LocalDateTime.now());
        ordenServicioRepository.saveAndFlush(orden);

        return hojaGuardada;
    }

    /**
     * Si el dato es una URL HTTPS/HTTP, lo devuelve tal cual.
     * Si es un string Base64 (data:image/...), lo sube a Cloudinary y devuelve la URL segura.
     */
    private String procesarImagenOUrl(String dato, String subcarpeta) {
        if (dato == null || dato.isBlank()) return null;
        if (dato.startsWith("http://") || dato.startsWith("https://")) {
            return dato;
        }
        try {
            return cloudinaryService.subirBase64(dato, subcarpeta);
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary en subcarpeta {}: {}", subcarpeta, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar la evidencia o firma.");
        }
    }
}
