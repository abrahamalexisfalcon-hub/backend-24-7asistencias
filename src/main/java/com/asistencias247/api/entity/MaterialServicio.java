package com.asistencias247.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "materiales_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_servicio_id", nullable = false)
    private HojaServicio hojaServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @Column(name = "es_personalizado", nullable = false)
    @Builder.Default
    private Boolean esPersonalizado = false;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;
}
