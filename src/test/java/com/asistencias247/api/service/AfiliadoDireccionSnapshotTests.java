package com.asistencias247.api.service;

import com.asistencias247.api.dto.ActualizarAfiliadoRequest;
import com.asistencias247.api.dto.CrearAfiliadoRequest;
import com.asistencias247.api.dto.CrearOrdenRequest;
import com.asistencias247.api.dto.DireccionAfiliadoDto;
import com.asistencias247.api.dto.OrdenDetalleAdminDto;
import com.asistencias247.api.dto.OrdenResumenTecnicoDto;
import com.asistencias247.api.entity.Afiliado;
import com.asistencias247.api.entity.AfiliadoDireccion;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AfiliadoDireccionSnapshotTests {

    @Mock
    private AfiliadoRepository afiliadoRepository;

    @Mock
    private OrdenServicioRepository ordenServicioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    private AfiliadoService afiliadoService;
    private OrdenServicioMapper ordenServicioMapper;
    private OrdenServicioService ordenServicioService;
    private OrdenTecnicoService ordenTecnicoService;
    private AtomicReference<String> lastPushDireccion;

    @BeforeEach
    void setUp() {
        afiliadoService = new AfiliadoService(afiliadoRepository);
        ordenServicioMapper = new OrdenServicioMapper();
        lastPushDireccion = new AtomicReference<>();
        OneSignalPushService pushStub = new OneSignalPushService() {
            @Override
            public void enviarNotificacionAsignacion(Long ordenId, Long tecnicoId, String codigoOrden, String tipoServicio, String direccion) {
                lastPushDireccion.set(direccion);
            }
        };
        ordenServicioService = new OrdenServicioService(
                ordenServicioRepository,
                afiliadoRepository,
                usuarioRepository,
                cuentaRepository,
                ordenServicioMapper,
                pushStub
        );
        ordenTecnicoService = new OrdenTecnicoService(ordenServicioRepository, usuarioRepository);
    }

    @Test
    @DisplayName("crearAfiliado con múltiples direcciones debe garantizar única principal y sincronizar campos raíz")
    void crearAfiliado_conMultiplesDirecciones_sincronizaPrincipal() {
        CrearAfiliadoRequest req = CrearAfiliadoRequest.builder()
                .nombreCompleto("María López")
                .dni("44556677")
                .telefono("987111222")
                .direcciones(List.of(
                        DireccionAfiliadoDto.builder()
                                .etiqueta("Casa 1")
                                .direccion("Jr. Los Pinos 100")
                                .distrito("Surco")
                                .referencia("Frente al parque")
                                .esPrincipal(true)
                                .build(),
                        DireccionAfiliadoDto.builder()
                                .etiqueta("Oficina")
                                .direccion("Av. Javier Prado 500")
                                .distrito("San Isidro")
                                .referencia("Piso 4")
                                .esPrincipal(true)
                                .build()
                ))
                .build();

        when(afiliadoRepository.existsByDniAndActivoTrue("44556677")).thenReturn(false);
        when(afiliadoRepository.findAllByDniOrderByIdDesc("44556677")).thenReturn(List.of());
        when(afiliadoRepository.save(any(Afiliado.class))).thenAnswer(inv -> {
            Afiliado a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        Afiliado creado = afiliadoService.crearAfiliado(req);

        assertEquals("Jr. Los Pinos 100", creado.getDireccion());
        assertEquals("Surco", creado.getDistrito());
        assertEquals("Frente al parque", creado.getReferencia());
        assertEquals(2, creado.getDirecciones().size());
        assertTrue(creado.getDirecciones().get(0).getEsPrincipal());
        assertFalse(creado.getDirecciones().get(1).getEsPrincipal());
        assertSame(creado, creado.getDirecciones().get(0).getAfiliado());
    }

    @Test
    @DisplayName("crearAfiliado sin lista direcciones debe crear dirección Principal desde fallback")
    void crearAfiliado_sinListaDirecciones_creaPrincipalFallback() {
        CrearAfiliadoRequest req = CrearAfiliadoRequest.builder()
                .nombreCompleto("Pedro Quispe")
                .dni("11223344")
                .direccion("Av. Central 222")
                .build();

        when(afiliadoRepository.existsByDniAndActivoTrue("11223344")).thenReturn(false);
        when(afiliadoRepository.findAllByDniOrderByIdDesc("11223344")).thenReturn(List.of());
        when(afiliadoRepository.save(any(Afiliado.class))).thenAnswer(inv -> inv.getArgument(0));

        Afiliado creado = afiliadoService.crearAfiliado(req);

        assertEquals("Av. Central 222", creado.getDireccion());
        assertEquals("Villa El Salvador", creado.getDistrito());
        assertEquals(1, creado.getDirecciones().size());
        assertEquals("Principal", creado.getDirecciones().get(0).getEtiqueta());
        assertTrue(creado.getDirecciones().get(0).getEsPrincipal());
    }

    @Test
    @DisplayName("actualizarAfiliado sin enviar direcciones actualiza la principal y conserva secundarias")
    void actualizarAfiliado_sinDirecciones_actualizaPrincipalConservandoSecundarias() {
        Afiliado existente = Afiliado.builder()
                .id(5L)
                .nombreCompleto("Ana Ruiz")
                .dni("22334455")
                .numeroCuenta("CTA-22334455")
                .direccion("Dir Antigua")
                .distrito("Lima")
                .referencia("Ref Antigua")
                .direcciones(new ArrayList<>())
                .build();
        existente.getDirecciones().add(AfiliadoDireccion.builder()
                .id(10L).afiliado(existente).etiqueta("Principal")
                .direccion("Dir Antigua").distrito("Lima").referencia("Ref Antigua").esPrincipal(true).build());
        existente.getDirecciones().add(AfiliadoDireccion.builder()
                .id(11L).afiliado(existente).etiqueta("Playa")
                .direccion("Km 97 Panamericana").distrito("Asia").referencia("Condominio").esPrincipal(false).build());

        ActualizarAfiliadoRequest req = ActualizarAfiliadoRequest.builder()
                .nombreCompleto("Ana Ruiz Actualizada")
                .direccion("Av. Nueva 999")
                .distrito("Miraflores")
                .referencia("Cerca al óvalo")
                .build();

        when(afiliadoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(afiliadoRepository.save(any(Afiliado.class))).thenAnswer(inv -> inv.getArgument(0));

        Afiliado actualizado = afiliadoService.actualizarAfiliado(5L, req);

        assertEquals("Av. Nueva 999", actualizado.getDireccion());
        assertEquals("Miraflores", actualizado.getDistrito());
        assertEquals(2, actualizado.getDirecciones().size());
        assertEquals("Av. Nueva 999", actualizado.getDirecciones().get(0).getDireccion());
        assertEquals("Km 97 Panamericana", actualizado.getDirecciones().get(1).getDireccion());
    }

    @Test
    @DisplayName("crearOrden con direccionId guarda snapshot de dirección secundaria y la expone en Admin, Técnico y Push")
    void crearOrden_conDireccionId_guardaSnapshotDeSecundaria() {
        Afiliado afiliado = Afiliado.builder()
                .id(10L)
                .nombreCompleto("Carlos Vega")
                .numeroCuenta("CTA-10")
                .direccion("Av. Principal 111")
                .distrito("Surco")
                .referencia("Casa matriz")
                .direcciones(new ArrayList<>())
                .build();
        afiliado.getDirecciones().add(AfiliadoDireccion.builder()
                .id(101L).afiliado(afiliado).etiqueta("Principal")
                .direccion("Av. Principal 111").distrito("Surco").referencia("Casa matriz").esPrincipal(true).build());
        afiliado.getDirecciones().add(AfiliadoDireccion.builder()
                .id(102L).afiliado(afiliado).etiqueta("Oficina")
                .direccion("Calle Las Begonias 450").distrito("San Isidro").referencia("").esPrincipal(false).build());

        Usuario tecnico = Usuario.builder().id(7L).nombreCompleto("Tec 1").rol(RolUsuario.TECNICO).build();
        Cuenta cuenta = Cuenta.builder().id(1L).nombre("Rímac Seguros").activo(true).build();

        CrearOrdenRequest req = CrearOrdenRequest.builder()
                .afiliadoId(10L)
                .tecnicoId(7L)
                .cuentaId(1L)
                .numeroCaso("CASO-SEC-01")
                .numeroOt("OT-SEC-01")
                .fechaPactada(LocalDateTime.now().plusHours(2))
                .tipoServicio("ELECTRICIDAD")
                .direccionId(102L)
                .build();

        when(afiliadoRepository.findById(10L)).thenReturn(Optional.of(afiliado));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(tecnico));
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(ordenServicioRepository.existsByNumeroCasoIgnoreCase(anyString())).thenReturn(false);
        when(ordenServicioRepository.existsByNumeroOtIgnoreCase(anyString())).thenReturn(false);
        when(ordenServicioRepository.countByAnioAsignacion(anyInt())).thenReturn(0L);
        when(ordenServicioRepository.findByCodigoOrden(anyString())).thenReturn(Optional.empty());
        when(ordenServicioRepository.save(any(OrdenServicio.class))).thenAnswer(inv -> {
            OrdenServicio o = inv.getArgument(0);
            o.setId(200L);
            return o;
        });

        OrdenDetalleAdminDto adminDto = ordenServicioService.crearOrden(req);

        assertEquals("Calle Las Begonias 450", adminDto.getDireccion());
        assertEquals("Calle Las Begonias 450", adminDto.getDireccionAfiliado());
        assertEquals("San Isidro", adminDto.getDistrito());
        assertEquals("", adminDto.getReferenciaAfiliado());
        assertEquals("Calle Las Begonias 450", adminDto.getCliente().getDireccion());
        assertEquals("San Isidro", adminDto.getCliente().getDistrito());
        assertEquals("", adminDto.getCliente().getReferencia());
        assertEquals("Calle Las Begonias 450", lastPushDireccion.get());
        verify(afiliadoRepository, never()).save(any());

        // Verificar también aislamiento frente a cambios futuros en el afiliado y vista de Técnico
        OrdenServicio ordenConSnapshot = OrdenServicio.builder()
                .id(200L)
                .codigoOrden("ORD-2026-001")
                .estado(EstadoOrden.PENDIENTE)
                .afiliado(afiliado)
                .direccionServicio("Calle Las Begonias 450")
                .distritoServicio("San Isidro")
                .referenciaServicio("")
                .tecnico(tecnico)
                .build();

        afiliado.setDireccion("Av. Cambiada 999");
        afiliado.setDistrito("Callao");
        afiliado.setReferencia("Otra referencia");

        when(ordenServicioRepository.findById(200L)).thenReturn(Optional.of(ordenConSnapshot));
        OrdenResumenTecnicoDto tecnicoDto = ordenTecnicoService.obtenerDetalleOrden(200L);
        assertEquals("Calle Las Begonias 450", tecnicoDto.getDireccion());
        assertEquals("San Isidro", tecnicoDto.getDistrito());
        assertEquals("", tecnicoDto.getReferencia());
    }

    @Test
    @DisplayName("crearOrden con nueva direccionServicio debe guardar snapshot y registrar nueva dirección en el afiliado")
    void crearOrden_conNuevaDireccionServicio_agregaAlAfiliado() {
        Afiliado afiliado = Afiliado.builder()
                .id(11L)
                .nombreCompleto("Lucía Méndez")
                .numeroCuenta("CTA-11")
                .direccion("Av. Principal 111")
                .distrito("Surco")
                .referencia("Principal")
                .direcciones(new ArrayList<>())
                .build();
        afiliado.getDirecciones().add(AfiliadoDireccion.builder()
                .id(101L).afiliado(afiliado).etiqueta("Principal")
                .direccion("Av. Principal 111").distrito("Surco").referencia("Principal").esPrincipal(true).build());

        Cuenta cuenta = Cuenta.builder().id(1L).nombre("Mapfre").activo(true).build();

        CrearOrdenRequest req = CrearOrdenRequest.builder()
                .afiliadoId(11L)
                .cuentaId(1L)
                .numeroCaso("CASO-NEW-02")
                .numeroOt("OT-NEW-02")
                .fechaPactada(LocalDateTime.now().plusHours(3))
                .tipoServicio("GASFITERIA")
                .direccionServicio("Calle Nueva 777")
                .distritoServicio("Barranco")
                .referenciaServicio("Portón verde")
                .etiquetaDireccion("Casa de Mamá")
                .build();

        when(afiliadoRepository.findById(11L)).thenReturn(Optional.of(afiliado));
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuenta));
        when(ordenServicioRepository.existsByNumeroCasoIgnoreCase(anyString())).thenReturn(false);
        when(ordenServicioRepository.existsByNumeroOtIgnoreCase(anyString())).thenReturn(false);
        when(ordenServicioRepository.countByAnioAsignacion(anyInt())).thenReturn(1L);
        when(ordenServicioRepository.findByCodigoOrden(anyString())).thenReturn(Optional.empty());
        when(ordenServicioRepository.save(any(OrdenServicio.class))).thenAnswer(inv -> {
            OrdenServicio o = inv.getArgument(0);
            o.setId(201L);
            return o;
        });

        OrdenDetalleAdminDto dto = ordenServicioService.crearOrden(req);

        assertEquals("Calle Nueva 777", dto.getDireccion());
        assertEquals("Barranco", dto.getDistrito());
        assertEquals("Portón verde", dto.getReferenciaAfiliado());
        assertEquals(2, afiliado.getDirecciones().size());
        AfiliadoDireccion agregada = afiliado.getDirecciones().get(1);
        assertEquals("Casa de Mamá", agregada.getEtiqueta());
        assertEquals("Calle Nueva 777", agregada.getDireccion());
        assertFalse(agregada.getEsPrincipal());
        verify(afiliadoRepository, times(1)).save(afiliado);
    }

    @Test
    @DisplayName("crearAfiliado debe rechazar DNI distinto de 8 dígitos numéricos")
    void crearAfiliado_dniInvalido_debeLanzarBadRequest() {
        CrearAfiliadoRequest req = CrearAfiliadoRequest.builder()
                .nombreCompleto("Carlos Pérez")
                .dni("865616768955888")
                .telefono("987654321")
                .direccion("Av. Lima 123")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> afiliadoService.crearAfiliado(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El DNI debe contener exactamente 8 dígitos numéricos.", ex.getReason());
        verify(afiliadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearAfiliado debe rechazar celular distinto de 9 dígitos numéricos")
    void crearAfiliado_telefonoInvalido_debeLanzarBadRequest() {
        CrearAfiliadoRequest req = CrearAfiliadoRequest.builder()
                .nombreCompleto("Carlos Pérez")
                .dni("12345678")
                .telefono("98765432")
                .direccion("Av. Lima 123")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> afiliadoService.crearAfiliado(req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("El número de celular debe contener exactamente 9 dígitos numéricos.", ex.getReason());
        verify(afiliadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarAfiliado debe validar DNI de 8 dígitos, normalizar celular de 9 dígitos y tolerar duplicados históricos")
    void actualizarAfiliado_conDniYTelefonoValidos_normalizaYValidaDuplicados() {
        Afiliado existente = Afiliado.builder()
                .id(5L)
                .nombreCompleto("Ana Ruiz")
                .dni("22334455")
                .numeroCuenta("CTA-22334455")
                .direccion("Dir Antigua")
                .distrito("Lima")
                .direcciones(new ArrayList<>())
                .build();

        ActualizarAfiliadoRequest req = ActualizarAfiliadoRequest.builder()
                .nombreCompleto("Ana Ruiz")
                .dni("87654321")
                .telefono("987 654 321")
                .direccion("Dir Nueva")
                .build();

        when(afiliadoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(afiliadoRepository.findAllByDniAndActivoTrue("87654321")).thenReturn(List.of());
        when(afiliadoRepository.save(any(Afiliado.class))).thenAnswer(inv -> inv.getArgument(0));

        Afiliado actualizado = afiliadoService.actualizarAfiliado(5L, req);

        assertEquals("87654321", actualizado.getDni());
        assertEquals("987654321", actualizado.getTelefono());
    }
}
