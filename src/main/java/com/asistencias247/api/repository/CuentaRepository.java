package com.asistencias247.api.repository;

import com.asistencias247.api.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    List<Cuenta> findByActivoTrueOrderByNombreAsc();

    List<Cuenta> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Cuenta> findByNombreIgnoreCase(String nombre);
}
