import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Protege /analista/**. Es solo para la experiencia de usuario: la seguridad real la aplica el backend. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  if (auth.autenticado()) {
    return true;
  }
  return inject(Router).createUrlTree(['/analista/login'], {
    queryParams: { volverA: estado.url },
  });
};
