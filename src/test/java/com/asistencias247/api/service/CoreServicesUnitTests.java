package com.asistencias247.api.service;

import com.asistencias247.api.dto.AuthRequestDto;
import com.asistencias247.api.dto.AuthResponseDto;
import com.asistencias247.api.dto.CancelarOrdenRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.Cuenta;
import com.asistencias247.api.entity.EstadoOrden;
import com.asistencias247.api.entity.OrdenServicio;
import com.asistencias247.api.entity.RolUsuario;
import com.asistencias247.api.entity.Usuario;
import com.asistencias247.api.mapper.OrdenServicioMapper;
import com.asistencias247.api.repository.AfiliadoRepository;
import com.asistencias247.api.repository.CuentaRepository;
import com.asistencias247.api.repository.OrdenServicioRepository;
import com.asistencias247.api.repository.UsuarioRepository;
import com.asistencias247.api.security.JwtService;
import com.asistencias247.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoreServicesUnitTests {

    @Mock
    private OrdenServicioRepository ordenServicioRepository;

    @Mock
    private AfiliadoRepository afiliadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    private OrdenServicioMapper ordenServicioMapper;
    private OneSignalPushService oneSignalPushService;
    private AtomicInteger pushCounter;
    private OrdenServicioService ordenServicioService;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 36000000L);

        ordenServicioMapper = new OrdenServicioMapper();
        pushCounter = new AtomicInteger(0);
        oneSignalPushService = new OneSignalPushService() {
            @Override
            public void enviarNotificacionAsignacion(Long ordenId, Long tecnicoId, String codigoOrden, String tipoServicio, String direccion) {
                pushCounter.incrementAndGet();
            }
        };

        ordenServicioService = new OrdenServicioService(
                ordenServicioRepository,
                afiliadoRepository,
                usuarioRepository,
                cuentaRepository,
                ordenServicioMapper,
                oneSignalPushService
        );

        authService = new AuthServiceImpl(authenticationManager, usuarioRepository, jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── 1. PRUEBAS DE JWTSERVICE & AUTHSERVICE ──────────────────────────────

    @Test
    @DisplayName("JwtService debe generar un token válido y extraer claims correctamente")
    void jwtService_debeGenerarYValidarTokenConClaims() {
        UserDetails userDetails = new User(
                "tecnico@asistencias247.com",
                "hashed_pass",
                List.of(new SimpleGrantedAuthority("ROLE_TECNICO"))
        );

        String token = jwtService.generateToken(userDetails, 15L, "TECNICO");

        assertNotNull(token);
        assertEquals("tecnico@asistencias247.com", jwtService.extractUsername(token));
        assertEquals(15L, jwtService.extractUsuarioId(token));
        assertEquals("TECNICO", jwtService.extractRol(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("AuthServiceImpl debe autenticar credenciales y retornar AuthResponseDto con JWT")
    void authService_debeAutenticarYRetornarDto() {
        String correo = "admin@asistencias247.com";
        UserDetails principal = new User(correo, "encoded", List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));
        Authentication authResult = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombreCompleto("Admin Principal")
                .correo(correo)
                .rol(RolUsuario.ADMINISTRADOR)
                .disponible(true)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(usuarioRepository.findByCorreo(correo)).thenReturn(Optional.of(usuario));

        AuthResponseDto response = authService.autenticar(new AuthRequestDto(correo, "secret123"));

        assertNotNull(response);
        assertNotNull(response.token());
        assertEquals(1L, response.usuarioId());
        assertEquals("ADMINISTRADOR", response.rol());
        assertTrue(response.disponible());
    }

    // ─── 2. PRUEBAS DE ORDENSERVICIOSERVICE (FLUJO & RBAC) ───────────────────

    @Test
    @DisplayName("crearOrden debe guardar orden PENDIENTE y disparar notificación OneSignal al técnico")
    void crearOrden_debeGuardarYDispararPush() {
        Afiliado afiliado = Afiliado.builder()
                .id(10L)
                .nombreCompleto("Juan Pérez")
                .direccion("Av. Javier Prado 123")
                .distrito("San Isidro")
                .build();

        Usuario tecnico = Usuario.builder()
                .id(7L)
                .nombreCompleto("Carlos Técnico")
                .rol(RolUsuario.TECNICO)
                .build();

        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .nombre("Rímac Seguros")
                .activo(true)
                .build();

        CrearOrdenRequest req = new CrearOrdenRequest();
        req.setAfiliadoId(10L);
        req.setTecnicoId(7L);
        req.setCuentaId(1L);
        req.setNumeroCaso("CASO-2026-001");
        req.setNumeroOt("OT-2026-001");
        req.setFechaPactada(LocalDateTime.now().plusHours(2));
        req.setTipoServicio("GASFITERIA");

        when(afiliadoRepository.findById(10L)).thenReturn(Optional.of(afiliado));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(tecnico));
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(ordenServicioRepository.existsByNumeroCaso(anyString())).thenReturn(false);
        when(ordenServicioRepository.existsByNumeroOt(anyString())).thenReturn(false);
        when(ordenServicioRepository.countByAnioAsignacion(anyInt())).thenReturn(4L);
        when(ordenServicioRepository.findByCodigoOrden(anyString())).thenReturn(Optional.empty());
        when(ordenServicioRepository.save(any(OrdenServicio.class))).thenAnswer(inv -> {
            OrdenServicio o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        OrdenDetalleAdminDto resultado = ordenServicioService.crearOrden(req);

        assertNotNull(resultado);
        assertEquals(99L, resultado.getId());
        assertEquals(EstadoOrden.PENDIENTE, resultado.getEstado());
        assertEquals(1, pushCounter.get(), "Debe dispararse exactamente 1 notificación push al técnico asignado");
    }

    @Test
    @DisplayName("marcarAsistidoNoRealizado debe transicionar de EN_DOMICILIO a CANCELADA con motivo")
    void marcarAsistidoNoRealizado_debeCancelarConMotivo() {
        OrdenServicio orden = OrdenServicio.builder()
                .id(50L)
                .codigoOrden("ORD-2026-050")
                .estado(EstadoOrden.EN_DOMICILIO)
                .build();

        when(ordenServicioRepository.findById(50L)).thenReturn(Optional.of(orden));
        when(ordenServicioRepository.save(any(OrdenServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenServicio actualizada = ordenServicioService.marcarAsistidoNoRealizado(50L, "Cliente ausente en domicilio");

        assertEquals(EstadoOrden.CANCELADA, actualizada.getEstado());
        assertEquals("Cliente ausente en domicilio", actualizada.getMotivoCancelacion());
        assertNotNull(actualizada.getFechaCancelacion());
    }

    @Test
    @DisplayName("marcarAsistidoNoRealizado debe rechazar anulación si la orden ya está FINALIZADA")
    void marcarAsistidoNoRealizado_debeRechazarSiFinalizada() {
        OrdenServicio orden = OrdenServicio.builder()
                .id(51L)
                .estado(EstadoOrden.FINALIZADA)
                .build();

        when(ordenServicioRepository.findById(51L)).thenReturn(Optional.of(orden));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ordenServicioService.marcarAsistidoNoRealizado(51L, "Intento inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("marcarAsistidoNoRealizado debe lanzar 403 FORBIDDEN si otro técnico intenta anular orden ajena")
    void marcarAsistidoNoRealizado_debeValidarPropiedadDeTecnico() {
        Usuario tecnicoPropietario = Usuario.builder().id(10L).rol(RolUsuario.TECNICO).build();
        Usuario tecnicoIntruso = Usuario.builder().id(20L).correo("intruso@test.com").rol(RolUsuario.TECNICO).build();

        OrdenServicio orden = OrdenServicio.builder()
                .id(52L)
                .estado(EstadoOrden.PENDIENTE)
                .tecnico(tecnicoPropietario)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("intruso@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_TECNICO")))
        );

        when(ordenServicioRepository.findById(52L)).thenReturn(Optional.of(orden));
        when(usuarioRepository.findByCorreo("intruso@test.com")).thenReturn(Optional.of(tecnicoIntruso));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ordenServicioService.marcarAsistidoNoRealizado(52L, "Motivo intruso"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(ordenServicioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarObservacionCancelacion debe actualizar observación administrativa correctamente")
    void actualizarObservacionCancelacion_debeActualizarTexto() {
        OrdenServicio orden = OrdenServicio.builder()
                .id(60L)
                .estado(EstadoOrden.CANCELADA)
                .motivoCancelacion("Motivo inicial")
                .build();

        CancelarOrdenRequest req = new CancelarOrdenRequest();
        req.setMotivoCancelacion("Observación auditada por administración");

        when(ordenServicioRepository.findById(60L)).thenReturn(Optional.of(orden));
        when(ordenServicioRepository.save(any(OrdenServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenDetalleAdminDto dto = ordenServicioService.actualizarObservacionCancelacion(60L, req);

        assertEquals("Observación auditada por administración", orden.getMotivoCancelacion());
        assertEquals("Observación auditada por administración", dto.getMotivoCancelacion());
    }

    // ─── 3. PRUEBAS DEFENSIVAS DE ONESIGNALZUSHSERVICE ───────────────────────

    @Test
    @DisplayName("OneSignalPushService no debe lanzar excepción si faltan variables de entorno o técnico es nulo")
    void oneSignalPushService_debeSerResilienteAnteConfiguracionVaciaONula() {
        OneSignalPushService service = new OneSignalPushService();
        ReflectionTestUtils.setField(service, "appId", "");
        ReflectionTestUtils.setField(service, "restApiKey", "");

        assertDoesNotThrow(() -> service.enviarNotificacionAsignacion((OrdenServicio) null));
        assertDoesNotThrow(() -> service.enviarNotificacionAsignacion(1L, null, "ORD-2026-001", "ELECTRICIDAD", "Av. Test"));
        assertDoesNotThrow(() -> service.enviarNotificacionAsignacion(1L, 5L, "ORD-2026-001", "ELECTRICIDAD", "Av. Test"));
    }
}
