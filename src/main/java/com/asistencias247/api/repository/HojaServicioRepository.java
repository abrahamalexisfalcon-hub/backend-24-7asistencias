package com.asistencias247.api.repository;

import com.asistencias247.api.entity.HojaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HojaServicioRepository extends JpaRepository<HojaServicio, Long> {

    Optional<HojaServicio> findByOrdenServicioId(Long ordenServicioId);
}
