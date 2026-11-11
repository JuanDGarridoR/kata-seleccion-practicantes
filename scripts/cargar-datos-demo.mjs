// Carga practicantes de ejemplo usando la API real (no toca la BD directamente).
// Uso:  node scripts/cargar-datos-demo.mjs [http://localhost:8080]
// Requiere Node 18+ (fetch y FormData nativos) y la aplicación en ejecución.

const BASE = process.argv[2] ?? 'http://localhost:8080';
const CREDENCIALES = 'Basic ' + Buffer.from('analista:Analista2026*').toString('base64');

/**
 * Genera un PDF de una página con las líneas de texto dadas, para que la app pueda
 * extraer el texto y detectar habilidades. Sin líneas, el PDF no tiene texto
 * (simula una hoja de vida escaneada).
 */
function crearPdf(lineas) {
  const escapar = (t) => t.replace(/[\\()]/g, (c) => '\\' + c);
  const contenido = lineas.length
    ? 'BT /F1 12 Tf 16 TL 60 740 Td ' + lineas.map((l) => `(${escapar(l)}) Tj T*`).join(' ') + ' ET'
    : '';
  const objetos = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>',
    `<< /Length ${Buffer.byteLength(contenido, 'latin1')} >>\nstream\n${contenido}\nendstream`,
  ];
  // La tabla xref guarda la posición en bytes de cada objeto; los visores de PDF la usan para leerlo.
  let pdf = '%PDF-1.4\n';
  const posiciones = objetos.map((objeto, i) => {
    const posicion = Buffer.byteLength(pdf, 'latin1');
    pdf += `${i + 1} 0 obj\n${objeto}\nendobj\n`;
    return posicion;
  });
  const inicioXref = Buffer.byteLength(pdf, 'latin1');
  pdf += `xref\n0 ${objetos.length + 1}\n0000000000 65535 f \n`;
  pdf += posiciones.map((p) => `${String(p).padStart(10, '0')} 00000 n \n`).join('');
  pdf += `trailer\n<< /Size ${objetos.length + 1} /Root 1 0 R >>\nstartxref\n${inicioXref}\n%%EOF\n`;
  // latin1 = WinAnsi para tildes y ñ con la fuente Helvetica estándar.
  return Buffer.from(pdf, 'latin1');
}

// [nombre, correo, carrera, semestre, estado, observación, líneas de la hoja de vida]
const PRACTICANTES = [
  ['Ana María Pérez', 'ana.perez@correo.edu.co', 'Ingeniería de Sistemas', 8, 'VIABLE', 'Excelente manejo de Java y SQL.',
    ['Proyectos académicos con Java y Spring Boot.', 'Bases de datos: SQL y PostgreSQL.', 'Control de versiones con Git.', 'Inglés B2.']],
  ['Carlos Andrés Rojas', 'carlos.rojas@correo.edu.co', 'Economía', 6, 'NO_VIABLE', 'Aún no cumple el semestre mínimo del área.',
    ['Análisis de datos en Excel.', 'Conocimientos básicos de Python.']],
  ['Laura Gómez Díaz', 'laura.gomez@correo.edu.co', 'Ingeniería de Sistemas', 9, 'VIABLE', null,
    ['Desarrollo web con Angular, TypeScript y JavaScript.', 'Backend con Node.js y MongoDB.', 'Docker y Git.', 'Trabajo con Scrum.', 'Inglés C1.']],
  ['Diego Martínez', 'diego.martinez@correo.edu.co', 'Derecho', 7, null, null,
    ['Investigación jurídica y redacción.', 'Manejo de Excel.', 'English: intermediate.']],
  ['Valentina Torres', 'valentina.torres@correo.edu.co', 'Finanzas', 5, null, null,
    ['Modelos financieros en Excel.', 'Tableros en Power BI.', 'SQL básico.']],
  ['Santiago Herrera', 'santiago.herrera@correo.edu.co', 'Ingeniería Industrial', 8, 'VIABLE', 'Buen perfil analítico.',
    ['Optimización de procesos con Python.', 'Power BI, Excel y SQL.', 'Metodologías ágiles: Scrum y Kanban.']],
  ['Mariana López', 'mariana.lopez@correo.edu.co', 'Administración de Empresas', 7, null, null,
    ['Gestión de proyectos con Jira y Kanban.', 'Excel avanzado.', 'Inglés y francés.']],
  ['Juan Pablo Castro', 'juan.castro@correo.edu.co', 'Estadística', 9, 'VIABLE', null,
    ['Modelos estadísticos con Python.', 'SQL, MySQL y Power BI.', 'Git.']],
  // Sin texto: simula una hoja de vida escaneada para mostrar el aviso en el detalle.
  ['Camila Ruiz', 'camila.ruiz@correo.edu.co', 'Psicología', 6, 'NO_VIABLE', null, []],
  ['Andrés Felipe Mora', 'andres.mora@correo.edu.co', 'Ingeniería de Sistemas', 7, null, null,
    ['Desarrollo con C# y .NET.', 'Algo de C++ en la universidad.', 'Azure y Docker.', 'SQL Server.']],
];

async function registrar([nombreCompleto, correo, carrera, semestre, , , lineas]) {
  const formulario = new FormData();
  formulario.append('nombreCompleto', nombreCompleto);
  formulario.append('correo', correo);
  formulario.append('carrera', carrera);
  formulario.append('semestre', String(semestre));
  const pdf = crearPdf([`Hoja de vida - ${nombreCompleto}`, carrera, '', ...lineas].filter(() => lineas.length));
  formulario.append('hojaVida', new Blob([pdf], { type: 'application/pdf' }), `HV ${nombreCompleto}.pdf`);

  const respuesta = await fetch(`${BASE}/api/practicantes`, { method: 'POST', body: formulario });
  const cuerpo = await respuesta.json();
  if (!respuesta.ok) throw new Error(`${correo}: ${respuesta.status} ${cuerpo.mensaje}`);
  return cuerpo;
}

async function decidir(id, estado, observacion) {
  const respuesta = await fetch(`${BASE}/api/practicantes/${id}/estado`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: CREDENCIALES },
    body: JSON.stringify({ estado, observacion }),
  });
  if (!respuesta.ok) throw new Error(`Estado de ${id}: ${respuesta.status}`);
}

for (const practicante of PRACTICANTES) {
  try {
    const creado = await registrar(practicante);
    const [, , , , estado, observacion] = practicante;
    if (estado) await decidir(creado.id, estado, observacion);
    const habilidades = creado.textoExtraible ? creado.habilidadesDetectadas.join(', ') || 'ninguna' : 'sin texto';
    console.log(`✔ ${practicante[0]}${estado ? ` → ${estado}` : ''}  [${habilidades}]`);
  } catch (error) {
    console.log(`✘ ${error.message}`);
  }
}
