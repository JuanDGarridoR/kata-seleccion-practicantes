export type EstadoPracticante = 'PENDIENTE' | 'VIABLE' | 'NO_VIABLE';

export interface Practicante {
  id: number;
  nombreCompleto: string;
  correo: string;
  carrera: string;
  semestre: number;
  nombreHojaVida: string;
  estado: EstadoPracticante;
  observacion: string | null;
  fechaRegistro: string;
  fechaDecision: string | null;
  /** Nombres visibles del catálogo encontrados en la hoja de vida. */
  habilidadesDetectadas: string[];
  /** false si el PDF no tenía texto legible (por ejemplo, un documento escaneado). */
  textoExtraible: boolean;
}

export interface CategoriaHabilidades {
  categoria: string;
  habilidades: string[];
}

export interface RegistroPracticante {
  nombreCompleto: string;
  correo: string;
  carrera: string;
  semestre: number;
}

export interface Resumen {
  total: number;
  pendientes: number;
  viables: number;
  noViables: number;
  tasaViabilidad: number | null;
  promedioDiasDecision: number | null;
  porCarrera: Record<string, number>;
  porSemestre: Record<string, number>;
  registrosPorFecha: Record<string, number>;
}

/** Formato único de error que devuelve el GlobalExceptionHandler del backend. */
export interface ErrorApi {
  status: number;
  error: string;
  mensaje: string;
  campos?: Record<string, string> | null;
  fecha: string;
}

type Severidad = 'success' | 'danger' | 'warn';

/**
 * Colores de estado reservados: solo se usan para el estado del candidato
 * y siempre acompañados de texto e ícono, nunca el color solo.
 */
export const ESTADOS: Record<EstadoPracticante, { texto: string; severidad: Severidad; icono: string }> = {
  PENDIENTE: { texto: 'Pendiente', severidad: 'warn', icono: 'pi pi-clock' },
  VIABLE: { texto: 'Viable', severidad: 'success', icono: 'pi pi-check-circle' },
  NO_VIABLE: { texto: 'No viable', severidad: 'danger', icono: 'pi pi-times-circle' },
};

export const CARRERAS_SUGERIDAS = [
  'Administración de Empresas',
  'Contaduría Pública',
  'Derecho',
  'Diseño',
  'Economía',
  'Estadística',
  'Finanzas',
  'Ingeniería Civil',
  'Ingeniería de Sistemas',
  'Ingeniería Electrónica',
  'Ingeniería Industrial',
  'Matemáticas',
  'Mercadeo',
  'Psicología',
];
