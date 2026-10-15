package com.asistencias247.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hojas_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HojaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenServicio ordenServicio;

    @Column(name = "evaluacion_averia", columnDefinition = "TEXT")
    private String evaluacionAveria;

    @Column(name = "detalle_solucion", columnDefinition = "TEXT")
    private String detalleSolucion;

    @Column(name = "sugerencias", columnDefinition = "TEXT")
    private String sugerencias;

    @Min(1)
    @Max(5)
    @Column(name = "calificacion")
    private Integer calificacion;

    @Column(name = "firma_digital_url", columnDefinition = "TEXT")
    private String firmaDigitalUrl;

    @Column(name = "foto_inicial_url", columnDefinition = "TEXT")
    private String fotoInicialUrl;

    @Column(name = "foto_final_url", columnDefinition = "TEXT")
    private String fotoFinalUrl;

    @Column(name = "pdf_acta_url", columnDefinition = "TEXT")
    private String pdfActaUrl;

    @OneToMany(mappedBy = "hojaServicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MaterialServicio> materiales = new ArrayList<>();
}

