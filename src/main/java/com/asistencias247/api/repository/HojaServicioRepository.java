package com.asistencias247.api.repository;

import com.asistencias247.api.entity.HojaServicio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HojaServicioRepository extends JpaRepository<HojaServicio, Long> {

    @EntityGraph(attributePaths = {"materiales", "materiales.material"})
    Optional<HojaServicio> findByOrdenServicioId(Long ordenServicioId);
}
