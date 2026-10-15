package com.asistencias247.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table(name = "usuarios")
@Getter
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 

public class Usuario {
    
@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Builder.Default
    private Boolean activo = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean disponible = true;

    @Column(name = "firma_url", length = 500)
    private String firmaUrl;

    @Column(length = 100)
    private String especialidad;
}

