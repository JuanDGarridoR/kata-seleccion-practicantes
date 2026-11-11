import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EstadoPracticante,
  Practicante,
  RegistroPracticante,
  Resumen,
} from '../shared/models/practicante.model';

@Injectable({ providedIn: 'root' })
export class PracticanteService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/practicantes';

  registrar(datos: RegistroPracticante, hojaVida: File): Observable<Practicante> {
    const formulario = new FormData();
    formulario.append('nombreCompleto', datos.nombreCompleto);
    formulario.append('correo', datos.correo);
    formulario.append('carrera', datos.carrera);
    formulario.append('semestre', String(datos.semestre));
    formulario.append('hojaVida', hojaVida, hojaVida.name);
    return this.http.post<Practicante>(this.base, formulario);
  }

  listar(estado?: EstadoPracticante | null): Observable<Practicante[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<Practicante[]>(this.base, { params });
  }

  obtener(id: number): Observable<Practicante> {
    return this.http.get<Practicante>(`${this.base}/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoPracticante, observacion: string | null): Observable<Practicante> {
    return this.http.patch<Practicante>(`${this.base}/${id}/estado`, { estado, observacion });
  }

  descargarHojaVida(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/${id}/hoja-vida`, { responseType: 'blob' });
  }

  resumen(): Observable<Resumen> {
    return this.http.get<Resumen>('/api/reportes/resumen');
  }

  exportarCsv(): Observable<Blob> {
    return this.http.get('/api/reportes/practicantes.csv', { responseType: 'blob' });
  }
}
