package com.asistencias247.api.service;

import com.asistencias247.api.service.storage.StorageService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class HojaServicioService {

    private final OrdenServicioRepository ordenServicioRepository;
    private final HojaServicioRepository hojaServicioRepository;
    private final MaterialRepository materialRepository;
    private final StorageService storageService;
    private final Executor mediaUploadExecutor;
    private final TransactionTemplate transactionTemplate;

    public HojaServicioService(
            OrdenServicioRepository ordenServicioRepository,
            HojaServicioRepository hojaServicioRepository,
            MaterialRepository materialRepository,
            StorageService storageService,
            @Qualifier("mediaUploadExecutor") Executor mediaUploadExecutor,
            TransactionTemplate transactionTemplate) {
        this.ordenServicioRepository = ordenServicioRepository;
        this.hojaServicioRepository = hojaServicioRepository;
        this.materialRepository = materialRepository;
        this.storageService = storageService;
        this.mediaUploadExecutor = mediaUploadExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Cierra la hoja de servicio procesando multimedia concurrentemente fuera de la transacción
     * para no agotar el pool de conexiones a la base de datos (NeonDB Pooler).
     */
    public HojaServicio cerrarHojaServicio(Long ordenId, CierreHojaServicioRequest request) {
        // 1. Verificación preliminar rápida
        if (!ordenServicioRepository.existsById(ordenId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden no encontrada con ID: " + ordenId);
        }

        // 2. Subida concurrente a Cloudinary fuera de cualquier transacción activa
        CompletableFuture<String> firmaDigitalFuture = CompletableFuture.supplyAsync(
                () -> request != null ? procesarImagenOUrl(request.getFirmaDigitalUrl(), "firmas") : null,
                mediaUploadExecutor
        );

        CompletableFuture<String> firmaTecnicoFuture = CompletableFuture.supplyAsync(
                () -> request != null ? procesarImagenOUrl(request.getFirmaTecnicoUrl(), "firmas-tecnicos") : null,
                mediaUploadExecutor
        );

        CompletableFuture<String> fotoInicialFuture = CompletableFuture.supplyAsync(
                () -> request != null ? procesarImagenOUrl(request.getFotoInicialUrl(), "evidencias") : null,
                mediaUploadExecutor
        );

        CompletableFuture<String> fotoFinalFuture = CompletableFuture.supplyAsync(
                () -> request != null ? procesarImagenOUrl(request.getFotoFinalUrl(), "evidencias") : null,
                mediaUploadExecutor
        );

        String urlFirma;
        String urlFirmaTec;
        String urlFotoInicial;
        String urlFotoFinal;

        try {
            CompletableFuture.allOf(firmaDigitalFuture, firmaTecnicoFuture, fotoInicialFuture, fotoFinalFuture).join();
            urlFirma = firmaDigitalFuture.join();
            urlFirmaTec = firmaTecnicoFuture.join();
            urlFotoInicial = fotoInicialFuture.join();
            urlFotoFinal = fotoFinalFuture.join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
            if (cause instanceof ResponseStatusException rse) {
                throw rse;
            }
            log.error("Error al procesar archivos multimedia en Cloudinary: {}", cause.getMessage(), cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo procesar la evidencia o firma.");
        }

        // 3. Persistencia atómica de corta duración en base de datos
        return transactionTemplate.execute(status -> {
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
                if (urlFirma != null) {
                    hoja.setFirmaDigitalUrl(urlFirma);
                    log.info("Firma del afiliado procesada y asignada para orden {}: {}", ordenId, urlFirma);
                }
                if (urlFirmaTec != null) {
                    if (orden.getTecnico() != null && (orden.getTecnico().getFirmaUrl() == null || orden.getTecnico().getFirmaUrl().isBlank())) {
                        orden.getTecnico().setFirmaUrl(urlFirmaTec);
                        log.info("Firma del técnico procesada y persistida en perfil para orden {}: {}", ordenId, urlFirmaTec);
                    }
                }
                if (urlFotoInicial != null) {
                    hoja.setFotoInicialUrl(urlFotoInicial);
                }
                if (urlFotoFinal != null) {
                    hoja.setFotoFinalUrl(urlFotoFinal);
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
        });
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
            return storageService.subirBase64(dato, subcarpeta);
        } catch (Exception e) {
            log.error("Error al subir imagen a almacenamiento en subcarpeta {}: {}", subcarpeta, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar la evidencia o firma.");
        }
    }
}
