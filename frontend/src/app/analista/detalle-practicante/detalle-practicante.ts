import { Component, computed, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { SkeletonModule } from 'primeng/skeleton';
import { PracticanteService } from '../../core/practicante.service';
import {
  CategoriaHabilidades,
  ErrorApi,
  ESTADOS,
  EstadoPracticante,
  Practicante,
} from '../../shared/models/practicante.model';
import { guardarArchivo } from '../../shared/utils/descargas';

@Component({
  selector: 'app-detalle-practicante',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    DialogModule,
    MessageModule,
    ProgressSpinnerModule,
    TagModule,
    TextareaModule,
    SkeletonModule,
  ],
  templateUrl: './detalle-practicante.html',
  styleUrl: './detalle-practicante.scss',
})
export class DetallePracticante implements OnInit, OnDestroy {
  private readonly servicio = inject(PracticanteService);
  private readonly confirmacion = inject(ConfirmationService);
  private readonly mensajes = inject(MessageService);
  private readonly sanitizador = inject(DomSanitizer);

  /** Viene del parámetro :id de la ruta (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly estados = ESTADOS;
  readonly practicante = signal<Practicante | null>(null);
  readonly noEncontrado = signal(false);
  readonly descargando = signal(false);
  readonly guardando = signal<EstadoPracticante | null>(null);
  readonly reanalizando = signal(false);
  observacion = '';

  private readonly catalogo = signal<CategoriaHabilidades[]>([]);

  /** Habilidades del practicante agrupadas según las categorías del catálogo. */
  readonly habilidadesPorCategoria = computed(() => {
    const detectadas = new Set(this.practicante()?.habilidadesDetectadas ?? []);
    const grupos = this.catalogo()
      .map((c) => ({ categoria: c.categoria, habilidades: c.habilidades.filter((h) => detectadas.has(h)) }))
      .filter((g) => g.habilidades.length > 0);
    // Habilidades guardadas que ya no están en el catálogo (si este cambió) no se pierden.
    const enCatalogo = new Set(this.catalogo().flatMap((c) => c.habilidades));
    const otras = [...detectadas].filter((h) => !enCatalogo.has(h));
    return otras.length ? [...grupos, { categoria: 'Otras', habilidades: otras }] : grupos;
  });

  // --- Vista previa de la hoja de vida ---
  readonly vistaVisible = signal(false);
  readonly cargandoVista = signal(false);
  readonly errorVista = signal(false);
  readonly urlVista = signal<SafeResourceUrl | null>(null);
  /** URL blob: original, necesaria para liberarla con revokeObjectURL. */
  private urlBlob: string | null = null;

  ngOnInit(): void {
    this.servicio.obtener(Number(this.id())).subscribe({
      next: (p) => {
        this.practicante.set(p);
        this.observacion = p.observacion ?? '';
      },
      error: () => this.noEncontrado.set(true),
    });
    this.servicio.catalogoHabilidades().subscribe((catalogo) => this.catalogo.set(catalogo));
  }

  ngOnDestroy(): void {
    this.liberarVista();
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

  /**
   * Un <iframe src="/api/..."> no enviaría el header Authorization del interceptor.
   * Por eso el PDF se pide con HttpClient como Blob y el iframe muestra una URL local blob:.
   */
  abrirVista(): void {
    const p = this.practicante();
    if (!p) return;
    this.vistaVisible.set(true);
    this.cargandoVista.set(true);
    this.errorVista.set(false);

    this.servicio.verHojaVida(p.id).subscribe({
      next: (archivo) => {
        this.liberarVista();
        this.urlBlob = URL.createObjectURL(archivo);
        // Angular bloquea URLs en [src] de iframes por seguridad; esta la generamos nosotros.
        this.urlVista.set(this.sanitizador.bypassSecurityTrustResourceUrl(this.urlBlob));
        this.cargandoVista.set(false);
      },
      error: () => {
        this.cargandoVista.set(false);
        this.errorVista.set(true);
      },
    });
  }

  /**
   * p-dialog avisa con visibleChange(false) cuando el usuario lo cierra (X, Escape o clic fuera);
   * con [visible] de una sola vía es el componente quien debe ocultarlo y liberar el PDF.
   */
  alCambiarVisibilidad(visible: boolean): void {
    if (!visible) {
      this.vistaVisible.set(false);
      this.liberarVista();
    }
  }

  reanalizar(): void {
    const p = this.practicante();
    if (!p) return;
    this.reanalizando.set(true);
    this.servicio.reanalizar(p.id).subscribe({
      next: (actualizado) => {
        this.practicante.set(actualizado);
        this.reanalizando.set(false);
        this.mensajes.add({
          severity: actualizado.textoExtraible ? 'success' : 'warn',
          summary: 'Análisis actualizado',
          detail: actualizado.textoExtraible
            ? `${actualizado.habilidadesDetectadas.length} habilidades detectadas.`
            : 'No fue posible leer el texto de la hoja de vida.',
        });
      },
      error: () => {
        this.reanalizando.set(false);
        this.mensajes.add({ severity: 'error', summary: 'No se pudo reanalizar la hoja de vida' });
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

  /** Libera la memoria del PDF en el navegador; sin esto el blob vive hasta cerrar la pestaña. */
  private liberarVista(): void {
    if (this.urlBlob) {
      URL.revokeObjectURL(this.urlBlob);
      this.urlBlob = null;
    }
    this.urlVista.set(null);
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
