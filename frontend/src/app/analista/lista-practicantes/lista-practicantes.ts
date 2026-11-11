import { Component, inject, OnInit, signal } from '@angular/core';
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

  readonly practicantes = signal<Practicante[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  filtroEstado: EstadoPracticante | null = null;

  ngOnInit(): void {
    this.cargar();
  }

  /** El filtro por estado se resuelve en el backend (?estado=); la búsqueda de texto, en la tabla. */
  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.servicio.listar(this.filtroEstado).subscribe({
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

  verDetalle(practicante: Practicante): void {
    this.router.navigate(['/analista/practicantes', practicante.id]);
  }
}
