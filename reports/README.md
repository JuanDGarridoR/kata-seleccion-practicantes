# Reportería con Power BI

Power BI Desktop (gratuito) consume la API de la aplicación con el conector **Web** y autenticación **Básica**.
El dataset es una lista plana sin nombres, correos ni rutas de archivo (minimización de datos personales).

```
GET http://localhost:8080/api/reportes/dataset
[
  { "id": 5, "carrera": "Finanzas", "semestre": 5, "estado": "PENDIENTE",
    "fechaRegistro": "2026-11-10T20:38:12.1", "fechaDecision": null, "diasHastaDecision": null },
  ...
]
```

## 1. Conectar

1. Ejecuta la aplicación (ver README principal) y, si quieres datos de ejemplo:
   `node scripts/cargar-datos-demo.mjs`
2. Abre Power BI Desktop → **Obtener datos** → **Web**.
3. URL: `http://localhost:8080/api/reportes/dataset` → Aceptar.
4. En el cuadro de credenciales elige **Básica**:
   - Usuario: `analista`
   - Contraseña: `Analista2026*`
   - Nivel: `http://localhost:8080/`
5. Clic en **Transformar datos**, abre **Editor avanzado** y reemplaza el contenido con
   [`consulta-practicantes.pq`](consulta-practicantes.pq) (expande la lista, asigna tipos y agrega
   `Estado` legible, `OrdenEstado` para el embudo y `FechaRegistroDia`).
6. Renombra la consulta a `Practicantes` → **Cerrar y aplicar**.

> Si te equivocas de credenciales: Archivo → Opciones y configuración → Configuración de origen de datos →
> `http://localhost:8080/` → Editar permisos.

## 2. Medidas DAX

En la tabla `Practicantes` → **Nueva medida**:

```DAX
Total = COUNTROWS ( Practicantes )

Pendientes = CALCULATE ( [Total], Practicantes[estado] = "PENDIENTE" )

Viables = CALCULATE ( [Total], Practicantes[estado] = "VIABLE" )

Decididos = CALCULATE ( [Total], Practicantes[estado] <> "PENDIENTE" )

Tasa de viabilidad = DIVIDE ( [Viables], [Decididos] )

Tiempo promedio de decisión (días) = AVERAGE ( Practicantes[diasHastaDecision] )
```

Formato: `Tasa de viabilidad` como porcentaje; el tiempo promedio con 2 decimales.

> La tasa se calcula sobre los candidatos **decididos** (no sobre el total), igual que en la vista de
> Reportes de la app, para que los pendientes no la bajen artificialmente.

## 3. Visualizaciones sugeridas (`panel-seleccion.pbix`)

| Visual | Configuración |
|---|---|
| Tarjetas (4) | `Total`, `Pendientes`, `Tasa de viabilidad`, `Tiempo promedio de decisión (días)` |
| Embudo de estados | Categoría: `Estado` (ordenar por `OrdenEstado`) · Valores: `Total` |
| Practicantes por carrera | Barras horizontales · Eje: `carrera` · Valores: `Total` · ordenar desc. |
| Distribución por semestre | Columnas · Eje: `semestre` · Valores: `Total` |
| Registros por fecha | Líneas · Eje: `FechaRegistroDia` · Valores: `Total` |
| Segmentador | `Estado` y `carrera` |

Colores sugeridos para `Estado` (los mismos de la app): Viable `#0CA30C`, Pendiente `#FAB219`,
No viable `#D03B3B`. Para el resto, un solo azul `#2A78D6`.

Guarda el archivo como `reports/panel-seleccion.pbix`.

## 4. Actualizar en la demo

Registra un practicante nuevo en la app → en Power BI clic en **Actualizar** (Inicio) → las tarjetas y
gráficas reflejan el cambio.

## Decisiones

- **No se usa "Publicar en la web"**: haría públicos datos de candidatos.
- **Embeber Power BI en la app** de forma privada requiere licencia Pro / Embedded → queda como siguiente paso.
- La app también ofrece **Exportar CSV** (`/api/reportes/practicantes.csv`) para quien prefiera Excel.
