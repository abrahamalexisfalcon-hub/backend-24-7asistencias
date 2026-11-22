package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarTecnicoRequest;
import com.asistencias247.api.dto.CrearTecnicoRequest;
import com.asistencias247.api.dto.TecnicoResumenDto;
import com.asistencias247.api.entity.Especialidad;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.repository.EspecialidadRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import com.asistencias247.api.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TecnicoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EspecialidadRepository especialidadRepository;

    @Mock
    private OrdenServicioRepository ordenServicioRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionTemplate transactionTemplate;

    private TecnicoService tecnicoService;

    @BeforeEach
    void setUp() {
        tecnicoService = new TecnicoService(
                usuarioRepository,
                especialidadRepository,
                ordenServicioRepository,
                storageService,
                passwordEncoder,
                transactionTemplate
        );
    }

    @Test
    @DisplayName("crearTecnico debe resolver múltiples especialidades por IDs y guardarlas en relación y texto")
    void crearTecnico_conEspecialidadIds_debeGuardarMultiplesEspecialidades() {
        Especialidad gasfiteria = Especialidad.builder().id(1L).nombre("Gasfitería").activo(true).build();
        Especialidad electricidad = Especialidad.builder().id(2L).nombre("Electricidad").activo(true).build();

        CrearTecnicoRequest request = CrearTecnicoRequest.builder()
                .nombreCompleto("Luis Campos")
                .correo("luis@asistencias247.com")
                .telefono("987654321")
                .password("1234")
                .especialidadIds(List.of(1L, 2L))
                .build();

        when(usuarioRepository.existsByCorreo("luis@asistencias247.com")).thenReturn(false);
        when(usuarioRepository.existsByTelefono("987654321")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("hashed");
        when(especialidadRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(electricidad, gasfiteria));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        TecnicoResumenDto dto = tecnicoService.crearTecnico(request);

        assertEquals(10L, dto.getId());
        assertEquals("Gasfitería, Electricidad", dto.getEspecialidad());
        assertEquals(List.of(1L, 2L), dto.getEspecialidadIds());
        assertEquals(List.of("Gasfitería", "Electricidad"), dto.getEspecialidades());
    }

    @Test
    @DisplayName("crearTecnico debe buscar o crear especialidades cuando se envían por lista de nombres")
    void crearTecnico_conListaEspecialidades_debeBuscarOCrear() {
        Especialidad gasfiteria = Especialidad.builder().id(1L).nombre("Gasfitería").activo(true).build();

        CrearTecnicoRequest request = CrearTecnicoRequest.builder()
                .nombreCompleto("Pedro Ramos")
                .correo("pedro@asistencias247.com")
                .telefono("912345678")
                .password("5678")
                .especialidades(List.of("Gasfitería, Cerrajería"))
                .build();

        when(usuarioRepository.existsByCorreo("pedro@asistencias247.com")).thenReturn(false);
        when(usuarioRepository.existsByTelefono("912345678")).thenReturn(false);
        when(passwordEncoder.encode("5678")).thenReturn("hashed");
        when(especialidadRepository.findByNombreIgnoreCase("Gasfitería")).thenReturn(Optional.of(gasfiteria));
        when(especialidadRepository.findByNombreIgnoreCase("Cerrajería")).thenReturn(Optional.empty());
        when(especialidadRepository.save(any(Especialidad.class))).thenAnswer(inv -> {
            Especialidad nueva = inv.getArgument(0);
            nueva.setId(3L);
            return nueva;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });

        TecnicoResumenDto dto = tecnicoService.crearTecnico(request);

        assertEquals("Gasfitería, Cerrajería", dto.getEspecialidad());
        assertEquals(List.of(1L, 3L), dto.getEspecialidadIds());
        assertEquals(List.of("Gasfitería", "Cerrajería"), dto.getEspecialidades());
        verify(especialidadRepository, times(1)).save(any(Especialidad.class));
    }

    @Test
    @DisplayName("crearTecnico debe lanzar IllegalArgumentException si no se envía ninguna especialidad válida")
    void crearTecnico_sinEspecialidad_debeLanzarIllegalArgumentException() {
        CrearTecnicoRequest request = CrearTecnicoRequest.builder()
                .nombreCompleto("Ana Torres")
                .correo("ana@asistencias247.com")
                .telefono("911222333")
                .password("4321")
                .especialidad("   ")
                .build();

        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepository.existsByTelefono(anyString())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tecnicoService.crearTecnico(request));

        assertEquals("Debe seleccionar al menos una especialidad para el técnico.", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarTecnico debe reemplazar las especialidades cuando se envían nuevas especialidades")
    void actualizarTecnico_conNuevasEspecialidades_debeActualizarRelacionYTexto() {
        Especialidad antigua = Especialidad.builder().id(1L).nombre("Gasfitería").activo(true).build();
        Especialidad nueva = Especialidad.builder().id(4L).nombre("Vidriería").activo(true).build();

        Usuario existente = Usuario.builder()
                .id(20L)
                .nombreCompleto("Mario Vega")
                .correo("mario@asistencias247.com")
                .telefono("900111222")
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad("Gasfitería")
                .especialidades(new LinkedHashSet<>(Set.of(antigua)))
                .build();

        ActualizarTecnicoRequest request = ActualizarTecnicoRequest.builder()
                .especialidadIds(List.of(4L))
                .build();

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(especialidadRepository.findAllById(List.of(4L))).thenReturn(List.of(nueva));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        TecnicoResumenDto dto = tecnicoService.actualizarTecnico(20L, request);

        assertEquals("Vidriería", dto.getEspecialidad());
        assertEquals(List.of(4L), dto.getEspecialidadIds());
        assertEquals(List.of("Vidriería"), dto.getEspecialidades());
    }

    @Test
    @DisplayName("listarTecnicos debe soportar retrocompatibilidad con especialidad en texto y respaldo General")
    void listarTecnicos_debeMapearTextoLegadoYGeneral() {
        Usuario legado = Usuario.builder()
                .id(30L)
                .nombreCompleto("Técnico Legado")
                .correo("legado@asistencias247.com")
                .telefono("955444333")
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad("Gasfitería, Electricidad")
                .especialidades(new LinkedHashSet<>())
                .build();

        Usuario sinEspecialidad = Usuario.builder()
                .id(31L)
                .nombreCompleto("Técnico Sin Especialidad")
                .correo("general@asistencias247.com")
                .telefono("955444334")
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad(null)
                .especialidades(new LinkedHashSet<>())
                .build();

        when(usuarioRepository.findByRolOrderByNombreCompletoAsc(RolUsuario.TECNICO))
                .thenReturn(List.of(legado, sinEspecialidad));

        List<TecnicoResumenDto> lista = tecnicoService.listarTecnicos(false);

        assertEquals(2, lista.size());
        assertEquals("Gasfitería, Electricidad", lista.get(0).getEspecialidad());
        assertEquals(List.of("Gasfitería", "Electricidad"), lista.get(0).getEspecialidades());
        assertTrue(lista.get(0).getEspecialidadIds().isEmpty());

        assertEquals("General", lista.get(1).getEspecialidad());
        assertEquals(List.of("General"), lista.get(1).getEspecialidades());
        assertTrue(lista.get(1).getEspecialidadIds().isEmpty());
    }

    @Test
    @DisplayName("crearTecnico y actualizarTecnico deben exigir celular de exactamente 9 dígitos numéricos")
    void crearYActualizarTecnico_celularInvalido_debeLanzarBadRequest() {
        CrearTecnicoRequest crearReq = CrearTecnicoRequest.builder()
                .nombreCompleto("Pedro Cell")
                .correo("pedro.cell@asistencias247.com")
                .telefono("9876543210")
                .password("1234")
                .especialidad("Gasfitería")
                .build();

        ResponseStatusException crearEx = assertThrows(ResponseStatusException.class,
                () -> tecnicoService.crearTecnico(crearReq));
        assertEquals(HttpStatus.BAD_REQUEST, crearEx.getStatusCode());
        assertEquals("El número de celular debe contener exactamente 9 dígitos numéricos.", crearEx.getReason());

        Usuario existente = Usuario.builder()
                .id(40L)
                .nombreCompleto("Mario Vega")
                .correo("mario@asistencias247.com")
                .telefono("900111222")
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .build();

        when(usuarioRepository.findById(40L)).thenReturn(Optional.of(existente));

        ActualizarTecnicoRequest actualizarReq = ActualizarTecnicoRequest.builder()
                .telefono("12345")
                .build();

        ResponseStatusException updEx = assertThrows(ResponseStatusException.class,
                () -> tecnicoService.actualizarTecnico(40L, actualizarReq));
        assertEquals(HttpStatus.BAD_REQUEST, updEx.getStatusCode());
        assertEquals("El número de celular debe contener exactamente 9 dígitos numéricos.", updEx.getReason());
    }

    @Test
    @DisplayName("actualizarEstadoTecnico sin body o con activo=null debe invertir el estado del técnico")
    void actualizarEstadoTecnico_sinActivo_debeHacerToggle() {
        Usuario tecnico = Usuario.builder()
                .id(50L)
                .nombreCompleto("Luis Estado")
                .correo("luis.estado@asistencias247.com")
                .telefono("988777666")
                .rol(RolUsuario.TECNICO)
                .activo(true)
                .disponible(true)
                .especialidad("Gasfitería")
                .build();

        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(tecnico));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        TecnicoResumenDto desactivado = tecnicoService.actualizarEstadoTecnico(50L, null);
        assertFalse(desactivado.getActivo());
        assertFalse(desactivado.getDisponible());

        TecnicoResumenDto reactivado = tecnicoService.actualizarEstadoTecnico(50L, new com.asistencias247.api.dto.ActualizarEstadoTecnicoRequest(null));
        assertTrue(reactivado.getActivo());
    }
}
