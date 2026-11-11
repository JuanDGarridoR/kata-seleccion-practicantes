// Carga practicantes de ejemplo usando la API real (no toca la BD directamente).
// Uso:  node scripts/cargar-datos-demo.mjs [http://localhost:8080]
// Requiere Node 18+ (fetch y FormData nativos) y la aplicación en ejecución.

const BASE = process.argv[2] ?? 'http://localhost:8080';
const CREDENCIALES = 'Basic ' + Buffer.from('analista:Analista2026*').toString('base64');

// PDF mínimo válido (empieza por %PDF) para no depender de archivos externos.
const PDF = Buffer.from(
  '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' +
    '2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' +
    '3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
);

const PRACTICANTES = [
  ['Ana María Pérez', 'ana.perez@correo.edu.co', 'Ingeniería de Sistemas', 8, 'VIABLE', 'Excelente manejo de Java y SQL.'],
  ['Carlos Andrés Rojas', 'carlos.rojas@correo.edu.co', 'Economía', 6, 'NO_VIABLE', 'Aún no cumple el semestre mínimo del área.'],
  ['Laura Gómez Díaz', 'laura.gomez@correo.edu.co', 'Ingeniería de Sistemas', 9, 'VIABLE', null],
  ['Diego Martínez', 'diego.martinez@correo.edu.co', 'Derecho', 7, null, null],
  ['Valentina Torres', 'valentina.torres@correo.edu.co', 'Finanzas', 5, null, null],
  ['Santiago Herrera', 'santiago.herrera@correo.edu.co', 'Ingeniería Industrial', 8, 'VIABLE', 'Buen perfil analítico.'],
  ['Mariana López', 'mariana.lopez@correo.edu.co', 'Administración de Empresas', 7, null, null],
  ['Juan Pablo Castro', 'juan.castro@correo.edu.co', 'Estadística', 9, 'VIABLE', null],
  ['Camila Ruiz', 'camila.ruiz@correo.edu.co', 'Psicología', 6, 'NO_VIABLE', null],
  ['Andrés Felipe Mora', 'andres.mora@correo.edu.co', 'Ingeniería de Sistemas', 7, null, null],
];

async function registrar([nombreCompleto, correo, carrera, semestre]) {
  const formulario = new FormData();
  formulario.append('nombreCompleto', nombreCompleto);
  formulario.append('correo', correo);
  formulario.append('carrera', carrera);
  formulario.append('semestre', String(semestre));
  formulario.append('hojaVida', new Blob([PDF], { type: 'application/pdf' }), `HV ${nombreCompleto}.pdf`);

  const respuesta = await fetch(`${BASE}/api/practicantes`, { method: 'POST', body: formulario });
  const cuerpo = await respuesta.json();
  if (!respuesta.ok) throw new Error(`${correo}: ${respuesta.status} ${cuerpo.mensaje}`);
  return cuerpo.id;
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
    const id = await registrar(practicante);
    const [, , , , estado, observacion] = practicante;
    if (estado) await decidir(id, estado, observacion);
    console.log(`✔ ${practicante[0]}${estado ? ` → ${estado}` : ''}`);
  } catch (error) {
    console.log(`✘ ${error.message}`);
  }
}
