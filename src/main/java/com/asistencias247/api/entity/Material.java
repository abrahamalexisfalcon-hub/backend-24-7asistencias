package com.asistencias247.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "materiales")
@org.hibernate.annotations.BatchSize(size = 50)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String unidad = "Unidad";

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;
}
