# MEMORY.md — Estado del Proyecto

## Estado Actual
- Conexión a PostgreSQL (Neon.tech Serverless) verificada y operativa.
- Entidades base creadas y verificadas: `Usuario`, `Afiliado`, `EstadoOrden`, `RolUsuario`.
- Entidades de cierre creadas y sincronizadas con Hibernate en Neon: `MaterialCatalogo`, `MaterialServicio`, `HojaServicio`, `OrdenServicio`.
- Tablas creadas exitosamente en Neon (`afiliados`, `catalogo_materiales`, `hojas_servicio`, `materiales_servicio`, `ordenes_servicio`, `usuarios`) con sus llaves foráneas y restricciones únicas.
- DTOs implementados en `com.asistencias247.api.dto`:
  * `OrdenResumenTecnicoDto` (id, codigoOrden, tipoServicio, estado, nombreAfiliado, direccion, referencia, distrito; sin teléfono).
  * `MaterialConsumidoDto` (materialId, descripcion, cantidad).
  * `CierreHojaServicioRequest` (evaluacionAveria, detalleSolucion, sugerencias, calificacion, URLs, materiales).
  * `MaterialCatalogoDto` (id, nombre).
- Controladores REST implementados en `com.asistencias247.api.controller`:
  * Autenticación (`/api/auth`): `POST /login`.
  * Módulo Técnico (`/api/tecnico`):
    - `GET /perfil` (o `/me`): Perfil operativo del técnico autenticado (incluye estado `disponible`, órdenes completadas hoy y calificación).
    - `PATCH /disponibilidad`: Actualiza el estado booleano `disponible` del técnico autenticado.
    - `GET /ordenes/{tecnicoId}`: Listado de órdenes asignadas al técnico (sin teléfono).
    - `PATCH /ordenes/{ordenId}/llegada`: Actualiza estado a `EN_DOMICILIO` y marca `fechaLlegada`.
    - `POST /ordenes/{ordenId}/cerrar`: Registra `HojaServicio`, asocia materiales (catálogo u "Otro" imprevisto), marca orden como `FINALIZADA` y registra `fechaFinalizacion`.
  * Catálogo Público / Técnico (`/api/catalogo`):
    - `GET /materiales`: Lista materiales activos ordenados por nombre para autocompletado.
  * Módulo de Administración (`/api/admin` - Protegido por rol `ADMINISTRADOR`):
    - Endpoints de órdenes, afiliados, técnicos y catálogo de materiales.
- Capa de Seguridad (Spring Security 6 + JJWT 0.12.5 stateless):
  * BCrypt, CORS global habilitado para desarrollo (`localhost:5173`, etc.) y orígenes móviles.
- Manejo Global de Excepciones (`com.asistencias247.api.exception`):
  * `GlobalExceptionHandler` con `ErrorResponseDto`.
- Poblado Inicial de Datos (`DataSeeder` en `com.asistencias247.api.config`):
  * Usuarios: `admin@asistencias247.com` (ADMINISTRADOR) y `tecnico@asistencias247.com` (TECNICO).
  * Afiliado y orden de prueba (`ORD-2026-001`, estado PENDIENTE).

---

## Interfaz Móvil del Técnico (Fiel a Figma `Login_merged.pdf`)
Implementación completa y pixel-perfect del prototipo móvil en [`frontend/src/pages/tecnico/`](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/pages/tecnico) con Tailwind CSS, React 19, TypeScript y PWA:

1. **Configuración Visual Base:**
   * **Colores:** Primario cian/celeste (`#0ea5e9` / `#0284c7`), Carbón corporativo (`#111827`), Fondos (`#f8fafc`), Tarjetas redondeadas (`rounded-2xl`) con sombras suaves.
   * **Layout Móvil:** Contenedor `max-w-md mx-auto` con barra superior de estado (10:15, señal, wifi, batería) y barra inferior fija (`BottomNav`).
   * **Bottom Navigation Bar Persistente:** Pestañas fijas 'Órdenes', 'Historial', 'Mi Perfil' con fondo `#111827` e icono cian activo.

2. **Pantallas Implementadas:**
   * **1. Login (`/login`):**
     - Imagen oficial importada directamente desde `src/assets/Logo_negro.png` con tamaño controlado y `object-contain`.
     - Inputs redondeados con íconos para 'Correo o celular' y 'PIN'.
     - Botón primario 'Iniciar Sesión' conectado a `POST /api/auth/login`.
     - Enlace 'Olvidé mi contraseña' y accesos rápidos de prueba.
   * **2. Listado de Órdenes (`/tecnico/ordenes`):**
     - Buscador superior interactivo: "Buscar órden, cliente...".
     - Pestañas/Filtros horizontales: "Todas", "En proceso", "Pendientes", "Finalizadas".
     - Cards de órdenes con código correlativo (`#ORD-2026-001`), badges de estado según Figma, nombre del servicio, dirección con ícono de ubicación y carita sonriente, fecha/hora y técnico asignado.
     - Navegación al tocar la tarjeta hacia el detalle de la orden.
   * **3. Detalle de Orden - En Camino (`/tecnico/ordenes/:id`):**
     - Barra de retroceso `<` con código de orden.
     - Stepper de 4 pasos (Paso 1: Llegada activo).
     - Badge "En camino al domicilio", nombre del servicio y fecha/hora.
     - Sección "DATOS DEL AFILIADO": Nombre, DNI, Código de cuenta (**sin número de teléfono por estricta regla de privacidad**).
     - Sección "DIRECCIÓN DE ATENCIÓN": Dirección completa, distrito y referencia.
     - Botón "Abrir ruta en Waze" (abre enlace Waze de navegación).
     - Botón primario "Confirmar Llegada al Domicilio" conectado a `PATCH /api/tecnico/ordenes/{id}/llegada`.
   * **4. Formulario de Atención Técnica (`/tecnico/ordenes/:id/atencion`):**
     - Stepper (Paso 2: Atención activa, Paso 1 completado con check).
     - Badge "En atención técnica" con hora de llegada registrada (10:48 AM).
     - Textareas para 'EVALUACIÓN', 'DETALLE DE SOLUCIÓN', 'SUGERENCIAS'.
     - Selector dinámico de 'MATERIALES EMPLEADOS' consumiendo `GET /api/catalogo/materiales`, con checkbox para materiales personalizados "Otro" y selector de cantidad (sin mostrar precios).
     - Textarea complementaria para notas de repuestos o materiales.
     - **Draft Recovery en tiempo real:** Persistencia automática en `localStorage` (`borrador_atencion_${ordenId}`) con carga inicial reactiva, auto-guardado en cada cambio, preservación ante recargas o navegación atrás (`<`) y limpieza segura únicamente al finalizar con éxito la orden.
     - Botón primario "Completar Servicio e Ir a Firma".
   * **5. Validación y Cierre (`/tecnico/ordenes/:id/cierre`):**
     - Stepper (Paso 3: Cierre activo, Pasos 1 y 2 con checks).
     - Badge "Validación y Cierre" con hora de fin (11:25 AM) y duración calculada (37 min).
     - Sección "EVIDENCIA DEL SERVICIO": Cajas de subida y previsualización para "Foto inicial" y "Foto final".
     - Sección "EVALUACIÓN DEL SERVICIO": Calificación interactiva de 1 a 5 estrellas para "Presentación y puntualidad" y "Trabajo y solución brindada".
     - Sección "CONFORMIDAD DEL AFILIADO": "Declaro recibir el servicio a entera satisfacción" y lienzo de firma táctil con `react-signature-canvas` y botón "Borrar".
     - Botón primario "Finalizar Orden" conectado a `POST /api/tecnico/ordenes/{id}/cerrar`.
   * **6. Pantalla de Éxito (`/tecnico/ordenes/:id/exito`):**
     - Stepper de 4 pasos (los 4 completados en cian con checks blancos).
     - Círculo verde con check animado.
     - Título "¡Servicio Finalizado con Éxito!" y descripción "La hoja de servicio fue generada y enviada al afiliado".
     - Tarjeta resumen con Código de orden, Afiliado, Duración (37 min) y Calificación en 5 estrellas.
     - Botón primario "Volver al Listado de Órdenes".
     - Botón secundario "Ver Acta en PDF" con modal interactivo y opción de descarga/impresión.
   * **Pestañas Adicionales del BottomNav:**
     - `/tecnico/historial`: Listado de asistencias técnicas finalizadas.
     - `/tecnico/perfil`: Datos del técnico, credencial 24/7 y botón de logout.

3. **Componentes Clave Creados:**
   * [WazeIcon.tsx](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/components/tecnico/WazeIcon.tsx): Ícono vectorial oficial de Waze utilizado en tarjetas de órdenes y botón de navegación a destino.
   * [BottomNav.tsx](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/components/tecnico/BottomNav.tsx): Barra inferior móvil persistente fija.
   * [TecnicoLayout.tsx](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/components/tecnico/TecnicoLayout.tsx): Layout móvil shell `max-w-md mx-auto` con barra de estado.
   * [OrderStepper.tsx](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/components/tecnico/OrderStepper.tsx): Stepper de 4 etapas con íconos de mapa, llave, firma y check.
   * [SignaturePad.tsx](file:///c:/Users/USER/Desktop/asistencias247/frontend/src/components/tecnico/SignaturePad.tsx): Firma táctil fluida con borrado y exportación a base64.

---

## Validación de Compilación
- Ejecución de `npm run build` en `frontend/`:
  * `tsc -b`: Cero errores de tipos y sintaxis (100% limpio).
  * `vite build`: Build de producción generado en ~5.2s.
  * PWA Service Worker y precache de 12 recursos generados correctamente.