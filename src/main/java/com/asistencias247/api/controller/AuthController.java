package com.asistencias247.api.controller;

import com.asistencias247.api.dto.AuthRequestDto;
import com.asistencias247.api.dto.AuthResponseDto;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.UsuarioRepository;
import com.asistencias247.api.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByCorreo(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + request.correo()));

        String token = jwtService.generateToken(userDetails, usuario.getId(), usuario.getRol().name());

        AuthResponseDto response = AuthResponseDto.builder()
                .token(token)
                .usuarioId(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().name())
                .disponible(Boolean.TRUE.equals(usuario.getDisponible()))
                .build();

        return ResponseEntity.ok(response);
    }
}
