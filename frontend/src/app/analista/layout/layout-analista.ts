import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth.service';

/** Estructura del panel inspirada en Sakai-NG: barra superior + menú lateral + contenido. */
@Component({
  selector: 'app-layout-analista',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule],
  templateUrl: './layout-analista.html',
  styleUrl: './layout-analista.scss',
})
export class LayoutAnalista {
  readonly auth = inject(AuthService);
  readonly menuAbierto = signal(false);

  readonly opciones = [
    { etiqueta: 'Practicantes', icono: 'pi pi-users', ruta: '/analista/practicantes' },
    { etiqueta: 'Reportes', icono: 'pi pi-chart-bar', ruta: '/analista/reportes' },
  ];
}
