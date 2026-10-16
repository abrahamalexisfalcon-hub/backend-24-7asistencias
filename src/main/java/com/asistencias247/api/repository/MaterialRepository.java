package com.asistencias247.api.repository;

import com.asistencias247.api.entity.Material;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Override
    @EntityGraph(attributePaths = {"especialidad"})
    Optional<Material> findById(Long id);

    @EntityGraph(attributePaths = {"especialidad"})
    List<Material> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = {"especialidad"})
    List<Material> findByEspecialidadIdAndActivoTrueOrderByNombreAsc(Long especialidadId);

    @EntityGraph(attributePaths = {"especialidad"})
    List<Material> findByEspecialidadIdOrderByNombreAsc(Long especialidadId);

    @EntityGraph(attributePaths = {"especialidad"})
    @Query("SELECT m FROM Material m WHERE m.activo = true AND LOWER(m.especialidad.nombre) = LOWER(:especialidadNombre) ORDER BY m.nombre ASC")
    List<Material> findByEspecialidadNombreAndActivoTrueOrderByNombreAsc(@Param("especialidadNombre") String especialidadNombre);

    boolean existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrue(String nombre, Long especialidadId);

    boolean existsByNombreIgnoreCaseAndEspecialidadIdAndActivoTrueAndIdNot(String nombre, Long especialidadId, Long id);
}
