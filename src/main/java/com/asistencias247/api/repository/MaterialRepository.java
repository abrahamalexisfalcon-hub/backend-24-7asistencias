package com.asistencias247.api.repository;

import com.asistencias247.api.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByActivoTrueOrderByNombreAsc();

    List<Material> findByEspecialidadIdAndActivoTrueOrderByNombreAsc(Long especialidadId);

    List<Material> findByEspecialidadIdOrderByNombreAsc(Long especialidadId);

    @Query("SELECT m FROM Material m WHERE m.activo = true AND LOWER(m.especialidad.nombre) = LOWER(:especialidadNombre) ORDER BY m.nombre ASC")
    List<Material> findByEspecialidadNombreAndActivoTrueOrderByNombreAsc(@Param("especialidadNombre") String especialidadNombre);

    boolean existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrue(String nombre, Long especialidadId);

    boolean existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrueAndIdNot(String nombre, Long especialidadId, Long id);
}
