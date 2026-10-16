# AGENTS.md — 24/7 Asistencias API
Backend REST para gestión de asistencias técnicas domiciliarias (móvil técnicos y web administración).

## Stack y Estructura
- Java 25 (LTS), Spring Boot 3.x, Spring Data JPA, Hibernate, PostgreSQL (Neon Serverless).
- Entidades en `com.asistencias247.api.entity`.
- Repositorios en `com.asistencias247.api.repository`.
- Servicios en `com.asistencias247.api.service`.
- Controladores en `com.asistencias247.api.controller`.

## Reglas de Dominio y Negocio
- **Seguridad / Privacidad:** Los técnicos NUNCA deben tener acceso al número de teléfono del afiliado. El contacto directo y envío de PDF lo gestiona administración.
- **Correlativo:** El código de orden (`codigoOrden`) se genera automáticamente en formato `ORD-YYYY-XXX` usando `EXTRACT(YEAR FROM fechaAsignacion)`.
- **Precios:** Los técnicos NO registran ni ven precios ni dinero. Solo reportan `material` (o `descripcion` si es personalizado) y `cantidad`.
- **Materiales (Opción B):** Si el material existe en el catálogo, se asocia `material_id`. Si es nuevo/imprevisto ("Otro"), `material` es null, `esPersonalizado = true` y se guarda la descripción escrita.

## Convenciones y Límites
- Usar anotaciones Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`).
- ✅ Siempre: mantener las entidades consistentes con JPA/PostgreSQL y actualizar `MEMORY.md` al terminar cada tarea.
- ⚠️ Pregunta antes: agregar dependencias al `pom.xml` o modificar la estructura de base de datos ya conectada.
- 🚫 Nunca: almacenar contraseñas en texto plano (usar BCrypt).

## Flujo de Validación y Cero Errores (Obligatorio)
Antes de finalizar cualquier tarea o responder al usuario:
1. **Compilación automática:** Ejecuta `./mvnw clean compile` (o el comando de build correspondiente) en la terminal integrada.
2. **Auto-corrección:** Si la compilación arroja errores o warnings de tipos/sintaxis, analízalos y corrígelos de inmediato de forma autónoma. No des la tarea por concluida hasta que la compilación sea 100% exitosa (BUILD SUCCESS).
3. **Persistencia:** Solo cuando el código compile limpio, actualiza el estado en `MEMORY.md`.