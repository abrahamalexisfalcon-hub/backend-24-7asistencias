package com.asistencias247.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "afiliados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Afiliado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    @Column(length = 15)
    private String dni; // Opcional: puede completarse durante la atención en campo

    @Column(nullable = false, length = 30)
    private String numeroCuenta;

    @Column(length = 15)
    private String telefono; // Opcional y solo visible por administración

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Column(length = 100)
    private String distrito;

    @OneToMany(mappedBy = "afiliado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AfiliadoDireccion> direcciones = new ArrayList<>();

    @Builder.Default
    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    public String getNombre() {
        return this.nombreCompleto;
    }

    public void setNombre(String nombre) {
        this.nombreCompleto = nombre;
    }

    public Boolean getActivo() {
        return this.activo != null ? this.activo : true;
    }
}
