package com.asistencias247.api.repository;

import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByTelefono(String telefono);

    Optional<Usuario> findByCorreoIgnoreCaseOrTelefono(String correo, String telefono);

    boolean existsByCorreo(String correo);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    List<Usuario> findByRolAndActivoTrueOrderByNombreCompletoAsc(RolUsuario rol);

    List<Usuario> findByRolOrderByNombreCompletoAsc(RolUsuario rol);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Usuario u SET u.especialidad = :nuevoNombre WHERE LOWER(u.especialidad) = LOWER(:nombreAnterior)")
    int actualizarEspecialidadMasiva(@org.springframework.data.repository.query.Param("nombreAnterior") String nombreAnterior, @org.springframework.data.repository.query.Param("nuevoNombre") String nuevoNombre);
}
