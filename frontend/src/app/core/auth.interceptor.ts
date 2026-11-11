import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const RUTA_VERIFICAR_SESION = '/api/auth/sesion';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api')) {
    return next(req);
  }

  const auth = inject(AuthService);
  // X-Requested-With hace que Spring Security no envíe WWW-Authenticate,
  // así el navegador no muestra su ventana nativa de usuario/contraseña.
  let headers = req.headers.set('X-Requested-With', 'XMLHttpRequest');
  const autorizacion = auth.headerAutorizacion;
  if (autorizacion && !headers.has('Authorization')) {
    headers = headers.set('Authorization', autorizacion);
  }

  return next(req.clone({ headers })).pipe(
    catchError((error: HttpErrorResponse) => {
      // Credenciales vencidas o cambiadas en el servidor: se vuelve al login.
      if (error.status === 401 && !req.url.startsWith(RUTA_VERIFICAR_SESION)) {
        auth.cerrarSesion();
      }
      return throwError(() => error);
    }),
  );
};
