import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SelectModule } from 'primeng/select';
import { PracticanteService } from '../../core/practicante.service';
import { ESTADOS, EstadoPracticante, Practicante } from '../../shared/models/practicante.model';

@Component({
  selector: 'app-lista-practicantes',
  imports: [
    DatePipe,
    FormsModule,
    TableModule,
    TagModule,
    ButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    SelectButtonModule,
    SelectModule,
  ],
  templateUrl: './lista-practicantes.html',
  styleUrl: './lista-practicantes.scss',
})
export class ListaPracticantes implements OnInit {
  private readonly servicio = inject(PracticanteService);
  private readonly router = inject(Router);

  readonly opcionesFiltro: { etiqueta: string; valor: EstadoPracticante | null }[] = [
    { etiqueta: 'Todos', valor: null },
    { etiqueta: 'Pendientes', valor: 'PENDIENTE' },
    { etiqueta: 'Viables', valor: 'VIABLE' },
    { etiqueta: 'No viables', valor: 'NO_VIABLE' },
  ];

  /** Opciones agrupadas por categoría para el p-select de habilidades. */
  readonly opcionesHabilidad = signal<{ categoria: string; habilidades: { nombre: string }[] }[]>([]);

  readonly practicantes = signal<Practicante[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  filtroEstado: EstadoPracticante | null = null;
  filtroHabilidad: string | null = null;

  ngOnInit(): void {
    this.cargar();
    this.servicio.catalogoHabilidades().subscribe((catalogo) =>
      this.opcionesHabilidad.set(
        catalogo.map((c) => ({ categoria: c.categoria, habilidades: c.habilidades.map((nombre) => ({ nombre })) })),
      ),
    );
  }

  /**
   * Los filtros por estado y habilidad se resuelven en el backend (?estado=&habilidad=);
   * la búsqueda de texto, en la tabla.
   */
  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.servicio.listar(this.filtroEstado, this.filtroHabilidad).subscribe({
      next: (lista) => {
        this.practicantes.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  /** Las filas de p-table llegan sin tipo a la plantilla; este método devuelve el estado tipado. */
  infoEstado(estado: EstadoPracticante) {
    return ESTADOS[estado];
  }

  /** Posición de cada habilidad en el catálogo, para mostrar primero lenguajes, luego frameworks, etc. */
  private readonly ordenCatalogo = computed(
    () => new Map(this.opcionesHabilidad().flatMap((c) => c.habilidades).map((h, i) => [h.nombre, i])),
  );

  /** Muestra hasta 3 habilidades en la tabla (en orden de catálogo); el resto se resume como "+N". */
  resumenHabilidades(habilidades: string[]) {
    const orden = this.ordenCatalogo();
    const ordenadas = [...habilidades].sort((a, b) => (orden.get(a) ?? 999) - (orden.get(b) ?? 999));
    return { visibles: ordenadas.slice(0, 3), restantes: ordenadas.slice(3) };
  }

  verDetalle(practicante: Practicante): void {
    this.router.navigate(['/analista/practicantes', practicante.id]);
  }
}
