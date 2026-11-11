import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';

const CLAVE_CREDENCIALES = 'analista.credenciales';
const CLAVE_USUARIO = 'analista.usuario';

/**
 * Con HTTP Basic no hay token: se guardan las credenciales codificadas en sessionStorage
 * (se borran al cerrar la pestaña) y el interceptor las adjunta a cada petición.
 * Es suficiente para el alcance del reto; el siguiente paso documentado es JWT.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly credenciales = signal<string | null>(sessionStorage.getItem(CLAVE_CREDENCIALES));
  readonly usuario = signal<string | null>(sessionStorage.getItem(CLAVE_USUARIO));
  readonly autenticado = computed(() => this.credenciales() !== null);

  get headerAutorizacion(): string | null {
    const credenciales = this.credenciales();
    return credenciales ? `Basic ${credenciales}` : null;
  }

  iniciarSesion(usuario: string, password: string): Observable<void> {
    const credenciales = codificarBase64(`${usuario}:${password}`);
    return this.http
      .get<{ usuario: string }>('/api/auth/sesion', {
        headers: { Authorization: `Basic ${credenciales}` },
      })
      .pipe(
        tap((respuesta) => {
          sessionStorage.setItem(CLAVE_CREDENCIALES, credenciales);
          sessionStorage.setItem(CLAVE_USUARIO, respuesta.usuario);
          this.credenciales.set(credenciales);
          this.usuario.set(respuesta.usuario);
        }),
        map(() => undefined),
      );
  }

  cerrarSesion(): void {
    sessionStorage.removeItem(CLAVE_CREDENCIALES);
    sessionStorage.removeItem(CLAVE_USUARIO);
    this.credenciales.set(null);
    this.usuario.set(null);
    this.router.navigate(['/analista/login']);
  }
}

/** btoa solo acepta Latin-1; se pasa por UTF-8 para soportar contraseñas con tildes o ñ. */
function codificarBase64(texto: string): string {
  const bytes = new TextEncoder().encode(texto);
  return btoa(String.fromCharCode(...bytes));
}
