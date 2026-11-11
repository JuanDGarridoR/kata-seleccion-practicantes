import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { ConfirmationService, MessageService } from 'primeng/api';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { TemaSeleccion } from './tema';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: TemaSeleccion,
        // La app usa modo claro; se deja un selector explícito para que no cambie con el SO.
        options: { darkModeSelector: '.app-oscuro' },
      },
      translation: {
        accept: 'Sí',
        reject: 'No',
        emptyMessage: 'Sin resultados',
        emptyFilterMessage: 'Sin resultados',
      },
    }),
    MessageService,
    ConfirmationService,
  ],
};
