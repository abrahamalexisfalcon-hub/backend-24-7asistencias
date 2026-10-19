package com.asistencias247.api.repository;

import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Long> {

    @Override
    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    Optional<OrdenServicio> findById(Long id);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    Optional<OrdenServicio> findByCodigoOrden(String codigoOrden);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findByTecnicoIdOrderByFechaAsignacionDesc(Long tecnicoId);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    Page<OrdenServicio> findByTecnicoIdOrderByFechaAsignacionDesc(Long tecnicoId, Pageable pageable);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findByTecnicoIdAndEstado(Long tecnicoId, EstadoOrden estado);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findByTecnicoIdAndEstadoOrderByFechaAsignacionDesc(Long tecnicoId, EstadoOrden estado);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findByTecnicoIdAndEstadoNotOrderByFechaAsignacionDesc(Long tecnicoId, EstadoOrden estado);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findAllByOrderByFechaAsignacionDesc();

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    Page<OrdenServicio> findAllByOrderByFechaAsignacionDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    List<OrdenServicio> findByEstadoOrderByFechaAsignacionDesc(EstadoOrden estado);

    @EntityGraph(attributePaths = {"afiliado", "tecnico", "hojaServicio"})
    Page<OrdenServicio> findByEstadoOrderByFechaAsignacionDesc(EstadoOrden estado, Pageable pageable);

    @Query("SELECT COUNT(o) FROM OrdenServicio o WHERE EXTRACT(YEAR FROM o.fechaAsignacion) = :anio")
    long countByAnioAsignacion(@Param("anio") int anio);

    @Modifying
    @Query("UPDATE OrdenServicio o SET o.tipoServicio = :nuevoNombre WHERE LOWER(o.tipoServicio) = LOWER(:nombreAnterior)")
    int actualizarTipoServicioMasivo(@Param("nombreAnterior") String nombreAnterior, @Param("nuevoNombre") String nuevoNombre);
}
