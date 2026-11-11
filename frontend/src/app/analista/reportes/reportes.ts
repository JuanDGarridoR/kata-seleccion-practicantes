import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { PracticanteService } from '../../core/practicante.service';
import { Resumen } from '../../shared/models/practicante.model';
import { guardarArchivo } from '../../shared/utils/descargas';

/*
 * Colores de las gráficas:
 * - Estados: paleta de estado reservada (verde / ámbar / rojo), la misma de las etiquetas.
 * - Magnitudes (carrera, semestre, fechas): un solo tono azul; el color no codifica nada más.
 */
const COLOR_SERIE = '#2a78d6';
const COLOR_ESTADO = { viable: '#0ca30c', pendiente: '#fab219', noViable: '#d03b3b' };
const TEXTO_SECUNDARIO = '#64748b';
const REJILLA = '#eef2f6';

const fuente = { family: "'Inter', system-ui, sans-serif", size: 12 };

const ejesMagnitud = (horizontal = false) => {
  const ejeValor = {
    beginAtZero: true,
    ticks: { precision: 0, color: TEXTO_SECUNDARIO, font: fuente },
    grid: { color: REJILLA },
    border: { display: false },
  };
  const ejeCategoria = {
    ticks: { color: TEXTO_SECUNDARIO, font: fuente },
    grid: { display: false },
    border: { color: '#cbd5e1' },
  };
  return horizontal ? { x: ejeValor, y: ejeCategoria } : { x: ejeCategoria, y: ejeValor };
};

@Component({
  selector: 'app-reportes',
  imports: [DecimalPipe, ButtonModule, ChartModule, SkeletonModule],
  templateUrl: './reportes.html',
  styleUrl: './reportes.scss',
})
export class Reportes implements OnInit {
  private readonly servicio = inject(PracticanteService);
  private readonly mensajes = inject(MessageService);

  readonly resumen = signal<Resumen | null>(null);
  readonly error = signal(false);
  readonly exportando = signal(false);

  readonly datosEstados = computed(() => {
    const r = this.resumen();
    if (!r) return null;
    return {
      labels: ['Viable', 'Pendiente', 'No viable'],
      datasets: [
        {
          data: [r.viables, r.pendientes, r.noViables],
          backgroundColor: [COLOR_ESTADO.viable, COLOR_ESTADO.pendiente, COLOR_ESTADO.noViable],
          borderColor: '#ffffff',
          borderWidth: 2,
        },
      ],
    };
  });

  /** Ordenadas de mayor a menor para que la comparación entre carreras sea inmediata. */
  readonly datosCarrera = computed(() => {
    const porCarrera = this.resumen()?.porCarrera;
    if (!porCarrera) return null;
    return this.barras(Object.fromEntries(Object.entries(porCarrera).sort(([, a], [, b]) => b - a)));
  });
  readonly datosSemestre = computed(() => {
    const porSemestre = this.resumen()?.porSemestre;
    if (!porSemestre) return null;
    // Se muestran los 12 semestres aunque alguno tenga 0, para ver la distribución completa.
    const conteos = Object.fromEntries(
      Array.from({ length: 12 }, (_, i) => [`${i + 1}°`, porSemestre[String(i + 1)] ?? 0]),
    );
    return this.barras(conteos);
  });

  readonly datosFechas = computed(() => {
    const porFecha = this.resumen()?.registrosPorFecha;
    if (!porFecha) return null;
    return {
      labels: Object.keys(porFecha).map((f) => f.split('-').reverse().slice(0, 2).join('/')),
      datasets: [
        {
          label: 'Registros',
          data: Object.values(porFecha),
          borderColor: COLOR_SERIE,
          backgroundColor: 'rgba(42, 120, 214, 0.08)',
          borderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          pointBackgroundColor: COLOR_SERIE,
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          fill: true,
          tension: 0.3,
        },
      ],
    };
  });

  readonly opcionesDona = {
    cutout: '68%',
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { usePointStyle: true, pointStyle: 'circle', color: '#334155', font: fuente, padding: 16 },
      },
    },
  };

  readonly opcionesBarrasHorizontales = {
    indexAxis: 'y',
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: ejesMagnitud(true),
  };

  readonly opcionesBarras = {
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: ejesMagnitud(),
  };

  readonly opcionesLinea = {
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: { legend: { display: false } },
    scales: ejesMagnitud(),
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(false);
    this.servicio.resumen().subscribe({
      next: (r) => this.resumen.set(r),
      error: () => this.error.set(true),
    });
  }

  exportarCsv(): void {
    this.exportando.set(true);
    this.servicio.exportarCsv().subscribe({
      next: (archivo) => {
        guardarArchivo(archivo, 'practicantes.csv');
        this.exportando.set(false);
      },
      error: () => {
        this.exportando.set(false);
        this.mensajes.add({ severity: 'error', summary: 'No se pudo exportar el CSV' });
      },
    });
  }

  private barras(conteos: Record<string, number> | undefined) {
    if (!conteos) return null;
    return {
      labels: Object.keys(conteos),
      datasets: [
        {
          label: 'Practicantes',
          data: Object.values(conteos),
          backgroundColor: COLOR_SERIE,
          hoverBackgroundColor: '#1f5fae',
          borderRadius: 4,
          borderSkipped: 'start',
          maxBarThickness: 28,
        },
      ],
    };
  }
}
