package com.asistencias247.api.service.impl;

import com.asistencias247.api.dto.AuthRequestDto;
import com.asistencias247.api.dto.AuthResponseDto;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.UsuarioRepository;
import com.asistencias247.api.security.JwtService;
import com.asistencias247.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto autenticar(AuthRequestDto request) {
        String identificador = request.correo() != null ? request.correo().trim() : "";
        String credencial = request.password() != null ? request.password().trim() : "";

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identificador, credencial)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByCorreo(userDetails.getUsername())
                .or(() -> usuarioRepository.findByTelefono(identificador))
                .or(() -> usuarioRepository.findByCorreoIgnoreCaseOrTelefono(identificador, identificador))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + identificador));

        if (usuario.getRol() == RolUsuario.TECNICO && !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new DisabledException("El usuario técnico se encuentra inactivo. Comuníquese con el administrador.");
        }

        String rolName = usuario.getRol().name();
        String token = jwtService.generateToken(userDetails, usuario.getId(), rolName);
        long expiresIn = jwtService.getExpirationForRole(rolName);

        return AuthResponseDto.builder()
                .token(token)
                .usuarioId(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .rol(rolName)
                .disponible(Boolean.TRUE.equals(usuario.getDisponible()))
                .expiresIn(expiresIn)
                .build();
    }
}
