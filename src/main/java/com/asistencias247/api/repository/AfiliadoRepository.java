package com.asistencias247.api.repository;

import com.asistencias247.api.entity.Afiliado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AfiliadoRepository extends JpaRepository<Afiliado, Long> {

    Optional<Afiliado> findByDni(String dni);

    @Query("SELECT a FROM Afiliado a WHERE a.dni = :dni AND (a.activo IS NULL OR a.activo = true)")
    Optional<Afiliado> findByDniAndActivoTrue(@Param("dni") String dni);

    Optional<Afiliado> findByNumeroCuenta(String numeroCuenta);

    boolean existsByDni(String dni);

    @Query("SELECT COUNT(a) > 0 FROM Afiliado a WHERE a.dni = :dni AND (a.activo IS NULL OR a.activo = true)")
    boolean existsByDniAndActivoTrue(@Param("dni") String dni);

    List<Afiliado> findAllByOrderByNombreCompletoAsc();

    @Query("SELECT a FROM Afiliado a WHERE (a.activo IS NULL OR a.activo = true) ORDER BY a.nombreCompleto ASC")
    List<Afiliado> findByActivoTrueOrderByNombreCompletoAsc();

    @Query("SELECT a FROM Afiliado a WHERE (a.activo IS NULL OR a.activo = true) AND (" +
           "LOWER(a.nombreCompleto) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.dni) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.direccion) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(a.distrito, '')) LIKE LOWER(CONCAT('%', :query, '%'))" +
           ") ORDER BY a.nombreCompleto ASC")
    List<Afiliado> buscarActivos(@Param("query") String query);
}
