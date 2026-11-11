import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { PracticanteService } from '../../core/practicante.service';
import { CARRERAS_SUGERIDAS, ErrorApi } from '../../shared/models/practicante.model';

const TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;

/** Rechaza textos que solo tienen espacios (Validators.required los deja pasar). */
function noSoloEspacios(control: AbstractControl): ValidationErrors | null {
  const valor = control.value;
  return typeof valor === 'string' && valor.length > 0 && valor.trim().length === 0
    ? { required: true }
    : null;
}

function archivoPdfValido(control: AbstractControl<File | null>): ValidationErrors | null {
  const archivo = control.value;
  if (!archivo) {
    return null;
  }
  const esPdf = archivo.type === 'application/pdf' || archivo.name.toLowerCase().endsWith('.pdf');
  if (!esPdf) {
    return { tipo: true };
  }
  return archivo.size > TAMANO_MAXIMO_BYTES ? { tamano: true } : null;
}

const MENSAJES: Record<string, Record<string, string>> = {
  nombreCompleto: { required: 'Ingresa tu nombre completo', maxlength: 'Máximo 120 caracteres' },
  correo: {
    required: 'Ingresa tu correo electrónico',
    email: 'El correo no tiene un formato válido',
    maxlength: 'Máximo 150 caracteres',
  },
  carrera: { required: 'Indica tu carrera universitaria', maxlength: 'Máximo 120 caracteres' },
  semestre: { required: 'Selecciona tu semestre actual' },
  hojaVida: {
    required: 'Adjunta tu hoja de vida en PDF',
    tipo: 'El archivo debe ser un PDF',
    tamano: 'El archivo no puede superar 5 MB',
  },
};

@Component({
  selector: 'app-formulario-registro',
  imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, SelectModule],
  templateUrl: './formulario-registro.html',
  styleUrl: './formulario-registro.scss',
})
export class FormularioRegistro {
  private readonly practicantes = inject(PracticanteService);
  private readonly mensajes = inject(MessageService);

  readonly carreras = CARRERAS_SUGERIDAS;
  readonly semestres = Array.from({ length: 12 }, (_, i) => ({ etiqueta: `${i + 1}° semestre`, valor: i + 1 }));

  readonly enviando = signal(false);
  readonly registrado = signal<string | null>(null);
  readonly arrastrando = signal(false);

  readonly formulario = new FormGroup({
    nombreCompleto: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, noSoloEspacios, Validators.maxLength(120)],
    }),
    correo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(150)],
    }),
    carrera: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, noSoloEspacios, Validators.maxLength(120)],
    }),
    semestre: new FormControl<number | null>(null, [Validators.required, Validators.min(1), Validators.max(12)]),
    hojaVida: new FormControl<File | null>(null, [Validators.required, archivoPdfValido]),
  });

  /** Mensaje del primer error del campo, solo cuando el usuario ya interactuó con él. */
  error(campo: keyof FormularioRegistro['formulario']['controls']): string | null {
    const control = this.formulario.controls[campo];
    if (!control.errors || !(control.touched || control.dirty)) {
      return null;
    }
    const [clave] = Object.keys(control.errors);
    return clave === 'servidor' ? control.errors['servidor'] : (MENSAJES[campo]?.[clave] ?? 'Valor inválido');
  }

  alSeleccionarArchivo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.asignarArchivo(entrada.files?.item(0) ?? null);
    entrada.value = ''; // permite volver a elegir el mismo archivo tras quitarlo
  }

  alSoltarArchivo(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastrando.set(false);
    this.asignarArchivo(evento.dataTransfer?.files.item(0) ?? null);
  }

  quitarArchivo(): void {
    this.asignarArchivo(null);
  }

  enviar(): void {
    this.formulario.markAllAsTouched();
    if (this.formulario.invalid || this.enviando()) {
      return;
    }

    const { nombreCompleto, correo, carrera, semestre, hojaVida } = this.formulario.getRawValue();
    this.enviando.set(true);
    this.practicantes
      .registrar({ nombreCompleto, correo, carrera, semestre: semestre! }, hojaVida!)
      .subscribe({
        next: (practicante) => {
          this.enviando.set(false);
          this.registrado.set(practicante.nombreCompleto);
        },
        error: (respuesta: HttpErrorResponse) => {
          this.enviando.set(false);
          this.mostrarErrorServidor(respuesta);
        },
      });
  }

  registrarOtro(): void {
    this.formulario.reset();
    this.registrado.set(null);
  }

  tamanoLegible(bytes: number): string {
    return bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  private asignarArchivo(archivo: File | null): void {
    const control = this.formulario.controls.hojaVida;
    control.setValue(archivo);
    control.markAsTouched();
    control.markAsDirty();
  }

  /** Lleva los errores por campo del backend (400/409/413) a los controles del formulario. */
  private mostrarErrorServidor(respuesta: HttpErrorResponse): void {
    const error = respuesta.error as ErrorApi | null;
    const campos = error?.campos ?? {};
    for (const [campo, mensaje] of Object.entries(campos)) {
      const control = this.formulario.get(campo);
      control?.setErrors({ servidor: mensaje });
      control?.markAsTouched();
    }
    this.mensajes.add({
      severity: 'error',
      summary: 'No pudimos registrar tus datos',
      detail: error?.mensaje ?? 'Intenta de nuevo en unos minutos.',
      life: 6000,
    });
  }
}
