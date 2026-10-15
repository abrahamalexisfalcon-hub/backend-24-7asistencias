package com.asistencias247.api.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialDetalleAdminDto {

    private Long id;
    private Long materialId;
    private Long materialCatalogoId;
    private String nombreCatalogo;
    private Boolean esPersonalizado;
    private String descripcion;
    private Integer cantidad;
    private String origen; // "CATALOGO" o "OTRO"
}
