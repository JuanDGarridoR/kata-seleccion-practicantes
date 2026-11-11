import { Component, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { SkeletonModule } from 'primeng/skeleton';
import { PracticanteService } from '../../core/practicante.service';
import { ErrorApi, ESTADOS, EstadoPracticante, Practicante } from '../../shared/models/practicante.model';
import { guardarArchivo } from '../../shared/utils/descargas';

@Component({
  selector: 'app-detalle-practicante',
  imports: [DatePipe, FormsModule, RouterLink, ButtonModule, TagModule, TextareaModule, SkeletonModule],
  templateUrl: './detalle-practicante.html',
  styleUrl: './detalle-practicante.scss',
})
export class DetallePracticante implements OnInit {
  private readonly servicio = inject(PracticanteService);
  private readonly confirmacion = inject(ConfirmationService);
  private readonly mensajes = inject(MessageService);

  /** Viene del parámetro :id de la ruta (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly estados = ESTADOS;
  readonly practicante = signal<Practicante | null>(null);
  readonly noEncontrado = signal(false);
  readonly descargando = signal(false);
  readonly guardando = signal<EstadoPracticante | null>(null);
  observacion = '';

  ngOnInit(): void {
    this.servicio.obtener(Number(this.id())).subscribe({
      next: (p) => {
        this.practicante.set(p);
        this.observacion = p.observacion ?? '';
      },
      error: () => this.noEncontrado.set(true),
    });
  }

  descargarHojaVida(): void {
    const p = this.practicante();
    if (!p) return;
    this.descargando.set(true);
    this.servicio.descargarHojaVida(p.id).subscribe({
      next: (archivo) => {
        guardarArchivo(archivo, p.nombreHojaVida);
        this.descargando.set(false);
      },
      error: () => {
        this.descargando.set(false);
        this.mensajes.add({ severity: 'error', summary: 'No se pudo descargar la hoja de vida' });
      },
    });
  }

  decidir(estado: Exclude<EstadoPracticante, 'PENDIENTE'>): void {
    const p = this.practicante();
    if (!p) return;
    const viable = estado === 'VIABLE';

    this.confirmacion.confirm({
      header: viable ? 'Marcar como viable' : 'Marcar como no viable',
      message: `¿Confirmas que ${p.nombreCompleto} es ${viable ? 'VIABLE' : 'NO VIABLE'} para el proceso?`,
      icon: viable ? 'pi pi-check-circle' : 'pi pi-times-circle',
      acceptLabel: 'Confirmar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: viable ? 'success' : 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () => this.guardarDecision(p.id, estado),
    });
  }

  private guardarDecision(id: number, estado: EstadoPracticante): void {
    this.guardando.set(estado);
    this.servicio.cambiarEstado(id, estado, this.observacion.trim() || null).subscribe({
      next: (actualizado) => {
        this.practicante.set(actualizado);
        this.guardando.set(null);
        this.mensajes.add({
          severity: 'success',
          summary: 'Decisión guardada',
          detail: `${actualizado.nombreCompleto} quedó como "${ESTADOS[actualizado.estado].texto}".`,
        });
      },
      error: (respuesta: HttpErrorResponse) => {
        this.guardando.set(null);
        this.mensajes.add({
          severity: 'error',
          summary: 'No se pudo guardar la decisión',
          detail: (respuesta.error as ErrorApi | null)?.mensaje,
        });
      },
    });
  }
}
