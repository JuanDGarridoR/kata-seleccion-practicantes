import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule, MessageModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Ruta a la que se intentaba entrar antes de ser enviado al login (query param). */
  readonly volverA = input<string>();

  readonly ingresando = signal(false);
  readonly error = signal<string | null>(null);

  readonly formulario = new FormGroup({
    usuario: new FormControl('', { nonNullable: true, validators: Validators.required }),
    password: new FormControl('', { nonNullable: true, validators: Validators.required }),
  });

  ingresar(): void {
    this.formulario.markAllAsTouched();
    if (this.formulario.invalid || this.ingresando()) {
      return;
    }
    const { usuario, password } = this.formulario.getRawValue();
    this.ingresando.set(true);
    this.error.set(null);

    this.auth.iniciarSesion(usuario.trim(), password).subscribe({
      next: () => {
        const destino = this.volverA()?.startsWith('/analista') ? this.volverA()! : '/analista/practicantes';
        this.router.navigateByUrl(destino);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.ingresando.set(false);
        this.error.set(
          respuesta.status === 401
            ? 'Usuario o contraseña incorrectos.'
            : 'No fue posible conectar con el servidor.',
        );
      },
    });
  }
}
