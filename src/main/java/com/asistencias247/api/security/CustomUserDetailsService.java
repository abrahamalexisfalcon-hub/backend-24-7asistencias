package com.asistencias247.api.security;

import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        String idLimpio = identificador != null ? identificador.trim() : "";
        Usuario usuario = usuarioRepository.findByCorreo(idLimpio)
                .or(() -> usuarioRepository.findByTelefono(idLimpio))
                .or(() -> usuarioRepository.findByCorreoIgnoreCaseOrTelefono(idLimpio, idLimpio))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo o celular: " + idLimpio));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new User(
                usuario.getCorreo(),
                usuario.getPassword(),
                Boolean.TRUE.equals(usuario.getActivo()),
                true,
                true,
                true,
                authorities
        );
    }
}
