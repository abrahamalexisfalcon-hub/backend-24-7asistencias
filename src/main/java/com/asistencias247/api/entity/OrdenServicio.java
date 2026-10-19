package com.asistencias247.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ordenes_servicio",
        indexes = {
                @Index(name = "idx_ordenes_estado", columnList = "estado"),
                @Index(name = "idx_ordenes_tecnico_id", columnList = "tecnico_id"),
                @Index(name = "idx_ordenes_codigo", columnList = "codigo_orden"),
                @Index(name = "idx_ordenes_fecha_creacion", columnList = "fecha_creacion")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_orden", nullable = false, unique = true, length = 30)
    private String codigoOrden;

    @Column(name = "tipo_servicio", nullable = false, length = 100)
    private String tipoServicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "afiliado_id", nullable = false)
    private Afiliado afiliado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_llegada")
    private LocalDateTime fechaLlegada;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @OneToOne(mappedBy = "ordenServicio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private HojaServicio hojaServicio;

    public LocalDateTime getFechaFin() {
        return this.fechaFinalizacion;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFinalizacion = fechaFin;
    }

    public LocalDateTime getFechaCreacion() {
        return this.fechaCreacion != null ? this.fechaCreacion : this.fechaAsignacion;
    }
}
