package com.asistencias247.api.repository;

import com.asistencias247.api.entity.MaterialServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialServicioRepository extends JpaRepository<MaterialServicio, Long> {

    List<MaterialServicio> findByHojaServicioId(Long hojaServicioId);
}

