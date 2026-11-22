# AGENTS.md — Directrices Técnicas de Backend
# Ubicación: `Backend/` | Stack: Java 21 LTS + Spring Boot 3.4.x + PostgreSQL 16 (NeonDB)

Esta capa contiene la API REST central del sistema 24/7 Asistencias, gobernando la seguridad transaccional, los contratos de datos y la orquestación de servicios externos (Cloudinary, OneSignal, WhatsApp, NeonDB).

---

## 1. Arquitectura Limpia y Estructura de Capas

```
Backend/src/main/java/com/asistencias247/api/
├── controller/     # Controladores REST livianos. Solo reciben DTOs validados, delegan al Service y retornan ResponseEntity.
├── service/        # Lógica de negocio pura, orquestación de servicios externos, reglas de validación y @Transactional.
├── repository/     # Repositorios Spring Data JPA con queries declarativas o JPQL optimizado.
├── entity/         # Entidades de dominio JPA mapeadas a PostgreSQL con Lombok y Hibernate.
├── dto/            # Data Transfer Objects inmutables con Bean Validation (@NotNull, @NotBlank, @Future, etc.).
├── config/         # Configuración de Seguridad (SecurityFilterChain, JWT, CORS), DataSeeder y Beans de Cloudinary/OneSignal.
└── exception/      # Manejo de errores transversal mediante @RestControllerAdvice y GlobalExceptionHandler.
```

### Reglas de Diseño de Código
- **Controllers Delgados:** Prohibida la lógica de negocio, cálculos o consultas directas en controladores.
- **DTOs Estrictos:** Nunca exponer entidades JPA directamente como respuesta ni recibirlas en `@RequestBody`. Usar DTOs de Request y Response fuertemente tipados.
- **Inyección por Constructor:** Inyección explícita con `private final` y `@RequiredArgsConstructor`. Prohibido `@Autowired` en atributos de campo.

---

## 2. Reglas Críticas de Negocio y Dominio

1. **Integridad de Órdenes de Servicio (`OrdenServicio`):**
   - Requiere `numeroCaso` y `numeroOt` no nulos, únicos y alfanuméricos.
   - `codigoOrden` generado automáticamente con correlativo anual (`ORD-YYYY-XXX`).
   - `cuenta_id` obligatorio (`@ManyToOne` con `Cuenta`).
   - `fechaPactada`: Validación obligatoria de fecha presente o futura (`@Future` o validación en Service).
2. **Máquina de Estados de la Orden:**
   - Transiciones permitidas: `PENDIENTE` ➔ `ASIGNADA` ➔ `EN_CAMINO` ➔ `EN_PROCESO` ➔ `ASISTIDO_TERMINADO` / `ASISTIDO_NO_REALIZADO` / `CANCELADA`.
   - `ASISTIDO_NO_REALIZADO`: Requiere justificación técnica obligatoria y registro fotográfico del motivo.
3. **Timeouts de Sesión y Autenticación Diferenciados:**
   - **Administradores:** Login con `email` + `password` (BCrypt). Token JWT con expiración estricta de **2 horas** para estaciones de trabajo.
   - **Técnicos en Campo:** Login rápido con `telefono` (9 dígitos) + `pin` (4 dígitos). Token JWT con sesión **persistente extendida** para evitar deslogueos mientras operan en ruta sin señal.
4. **Manejo Seguro de URLs y Evidencias (Cloudinary):**
   - Las URLs de evidencias fotográficas ("Antes", "Después") y firmas digitales deben ser sanitizadas y almacenadas en Cloudinary con HTTPS estricto.
5. **Privacidad del Afiliado:**
   - En las respuestas de API destinadas al rol técnico (`ROLE_TECNICO`), el teléfono y datos no operacionales del afiliado deben ser omitidos o retornar nulos.

---

## 3. Protocolo de Pruebas y Compilación Obligatoria

> [!CRITICAL]
> **Regla de Inyección en Tests Unitarios:**
> Cada vez que se agregue o modifique un repositorio/servicio inyectado en un `Service` (ej. `OrdenServicioService`), se debe **actualizar de forma inmediata** la instanciación en los tests unitarios:
> 👉 [`Backend/src/test/java/com/asistencias247/api/service/CoreServicesUnitTests.java`](file:///c:/Users/USER/Desktop/asistencias247/Backend/src/test/java/com/asistencias247/api/service/CoreServicesUnitTests.java)

### Comandos de Verificación Local (desde `Backend/`)
```powershell
# 1. Compilación sintáctica rápida:
.\mvnw.cmd test-compile

# 2. Ejecución de la suite completa de pruebas unitarias y de integración:
.\mvnw.cmd test

# 3. Empaquetado de producción (idéntico al paso de Dockerfile para Hostinger VPS):
.\mvnw.cmd clean package -Dmaven.test.skip=true
```

---

## 4. Requisitos para Despliegue en Producción (Hostinger VPS)

- **Dockerfile de Producción:** Mantener en `Backend/Dockerfile` la directiva `RUN mvn clean package -Dmaven.test.skip=true`.
- **Límites de Memoria JVM:** El entrypoint debe conservar las banderas de memoria para contenedor ligero:
  ```dockerfile
  ENTRYPOINT ["java", "-Xms180m", "-Xmx300m", "-XX:+UseSerialGC", "-jar", "app.jar"]
  ```
- **Compatibilidad con CI/CD:** El build debe pasar limpiamente para no romper el paso `docker compose up -d --build` en el workflow de GitHub Actions `Deploy to Hostinger VPS`.
