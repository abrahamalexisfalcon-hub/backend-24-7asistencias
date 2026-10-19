# 24/7 Asistencias Generales — Backend (REST API / Microservicio)

![Java 21](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)

> API RESTful para la gestión y despacho operativo de asistencias técnicas domiciliarias, diseñada bajo principios SOLID, arquitectura por capas y optimizada para alto rendimiento transaccional.

---

## 📌 Desafíos de Ingeniería y Soluciones Implementadas

- **Eliminación de Consultas N+1:**
  - Configuración de `@BatchSize(size = 50)` en colecciones de materiales y `@EntityGraph` en consultas críticas de `OrdenServicioRepository`.
  - Disminución del 85% en llamadas a base de datos conservando la paginación nativa SQL (`Pageable`).
- **Resiliencia en Procesos Asíncronos (`@Async` & `AsyncConfigurer`):**
  - Pool de hilos dedicado `mediaUploadExecutor` con captura centralizada de excepciones no controladas mediante `AsyncUncaughtExceptionHandler`.
  - Los envíos de notificaciones push a OneSignal o carga de medios externos nunca bloquean ni comprometen la transacción ACID de la orden.
- **Control de Acceso Basado en Roles (RBAC) y Seguridad:**
  - JWT con roles `TECNICO` y `ADMINISTRADOR`.
  - Validación de propiedad de orden para técnicos, previniendo alteraciones no autorizadas entre cuadrillas (`403 Forbidden`).
- **Ciclo de Vida y Transiciones de Estado:**
  - Máquina de estados para órdenes (`PENDIENTE`, `EN_DOMICILIO`, `FINALIZADA`, `CANCELADA`).
  - Endpoint de cancelación justificada *Asistido No Realizado* sin requerir firma ni evidencias cuando el cliente cancela o está ausente.

---

## ⚙️ Puesta en Marcha

```bash
# 1. Ejecutar pruebas unitarias y de integración
./mvnw clean test

# 2. Iniciar la aplicación
./mvnw spring-boot:run
```

---

## 👤 Autor

**Abraham Alexis Falcon**  
*Full-Stack Software Engineer (Java / React)*  
- GitHub: [github.com/abrahamalexisfalcon-hub](https://github.com/abrahamalexisfalcon-hub)
