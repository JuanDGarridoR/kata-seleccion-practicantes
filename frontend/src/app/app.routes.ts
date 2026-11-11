import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

/** Las pantallas del analista se cargan bajo demanda: el practicante solo descarga el formulario. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'registro' },
  {
    path: 'registro',
    title: 'Registro de practicantes',
    loadComponent: () =>
      import('./registro/formulario-registro/formulario-registro').then((m) => m.FormularioRegistro),
  },
  {
    path: 'analista/login',
    title: 'Ingreso analistas',
    loadComponent: () => import('./analista/login/login').then((m) => m.Login),
  },
  {
    path: 'analista',
    canActivate: [authGuard],
    loadComponent: () => import('./analista/layout/layout-analista').then((m) => m.LayoutAnalista),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'practicantes' },
      {
        path: 'practicantes',
        title: 'Practicantes',
        loadComponent: () =>
          import('./analista/lista-practicantes/lista-practicantes').then((m) => m.ListaPracticantes),
      },
      {
        path: 'practicantes/:id',
        title: 'Detalle del practicante',
        loadComponent: () =>
          import('./analista/detalle-practicante/detalle-practicante').then(
            (m) => m.DetallePracticante,
          ),
      },
      {
        path: 'reportes',
        title: 'Reportes',
        loadComponent: () => import('./analista/reportes/reportes').then((m) => m.Reportes),
      },
    ],
  },
  { path: '**', redirectTo: 'registro' },
];
