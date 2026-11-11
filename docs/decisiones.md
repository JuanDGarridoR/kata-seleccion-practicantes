# Decisiones técnicas

Registro corto de las decisiones del proyecto, con su porqué y lo que se sacrificó.

## 1. Monolito: Spring Boot sirve la API y el build de Angular

- **Decisión:** un solo proyecto desplegable. `ng build` deja los archivos en
  `backend/src/main/resources/static` y Spring Boot los sirve junto con `/api/**`.
- **Por qué:** una aplicación, un puerto, un comando. Sin CORS en producción y fácil de ejecutar localmente.
- **Costo:** frontend y backend se despliegan juntos. En desarrollo se usa `ng serve` con proxy a `:8080`.
- `WebConfig` reenvía `/registro` y `/analista/**` a `index.html` para que recargar la página no dé 404.

## 2. SQLite como base de datos local

- **Decisión:** SQLite con Spring Data JPA (`sqlite-jdbc` + dialecto de `hibernate-community-dialects`).
- **Por qué:** el enunciado pide BD local; SQLite es un archivo (`data/practicantes.db`), sin instalar servidor.
- **Costo:** un solo escritor a la vez → pool de 1 conexión. Para producción se cambiaría a PostgreSQL
  modificando solo `application.properties` y el driver (JPA abstrae el resto).
- El correo tiene restricción `UNIQUE` en la BD además de la validación en el servicio: la primera da el
  mensaje claro (409), la segunda protege ante registros simultáneos.

## 3. HTTP Basic con Spring Security

- **Decisión:** Basic Auth sin sesión (stateless), un analista en memoria con contraseña en BCrypt.
- **Por qué:** es lo más simple de explicar y es compatible de forma nativa con el conector Web de Power BI.
- **Detalle:** Angular envía `X-Requested-With: XMLHttpRequest`; con ese header Spring Security no manda
  `WWW-Authenticate` y el navegador no muestra su ventana nativa de login. Power BI no lo envía, recibe el
  desafío Basic y pide credenciales.
- **CSRF deshabilitado:** no hay cookies de sesión que otro sitio pueda reutilizar.
- **Costo:** las credenciales viajan en cada petición (requiere HTTPS en un entorno real) y se guardan en
  `sessionStorage`. Siguiente paso: usuarios en BD + JWT.

## 4. Archivos guardados con nombre UUID

- **Decisión:** el PDF se guarda como `uploads/<uuid>.pdf`; el nombre original solo se usa para la descarga.
- **Por qué:** evita colisiones y rutas maliciosas (`../../archivo`).
- **Validación doble:** `Content-Type: application/pdf` (lo envía el cliente, se puede falsificar) **y**
  los primeros bytes `%PDF`. Límite de 5 MB → 413 con JSON.
- Si falla el guardado en BD después de escribir el archivo, se borra el archivo para no dejar huérfanos.

## 5. PrimeNG (preset Aura) con layout inspirado en Sakai

- **Decisión:** PrimeNG 20 para tabla, select, tags, diálogos, toasts y gráficas (Chart.js).
- **Por qué:** componentes accesibles y listos; el tema se define en un único archivo (`tema.ts`).
- **Colores:** verde/ámbar/rojo reservados para el **estado** del candidato y siempre con ícono + texto;
  las gráficas de magnitudes usan un solo azul.
- Las pantallas del analista se cargan bajo demanda (lazy loading): el practicante solo descarga el formulario.

## 6. Reportería: KPIs en la app + Power BI vía API

- **Decisión:** endpoint `/api/reportes/dataset` (JSON plano) consumido por Power BI Desktop, además de una
  vista de Reportes en la app y exportación CSV.
- **Por qué:** Power BI se actualiza con un clic sin exportar archivos; el dataset excluye datos personales.
- **No** se usa "Publicar en la web" (expondría candidatos). Embeber de forma privada requiere licencia.
- Los cálculos de KPIs se hacen en memoria (volumen pequeño). Con miles de registros → consultas `GROUP BY`.

## 7. Organización por funcionalidad y DTOs

- Paquetes `practicante/`, `archivo/`, `reporte/`, `auth/`, `config/` en lugar de `controllers/`, `services/`.
- DTOs (`record`) de entrada y salida: la entidad no se expone y la ruta interna del archivo nunca sale de la API.
- La regla "no se puede volver a PENDIENTE y toda decisión tiene fecha" vive en la entidad (`cambiarEstado`).
- Errores centralizados en `GlobalExceptionHandler` con un único formato (`status`, `mensaje`, `campos`).
