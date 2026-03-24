import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Reserva, ReservaCrear } from '../models/reserva.model';

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private readonly apiUrl = `${environment.apiUrl}/reservas`;

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(this.apiUrl);
  }

  listarMisReservas(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.apiUrl}/mis-reservas`);
  }

  listarPorEstado(estado: string): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.apiUrl}/estado/${estado}`);
  }

  buscarPorId(id: number): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.apiUrl}/${id}`);
  }

  crear(dto: ReservaCrear): Observable<Reserva> {
    return this.http.post<Reserva>(this.apiUrl, dto);
  }

  cancelar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/cancelar`, {});
  }

  marcarEquipoRecogido(id: number, fechaHoraDevolucion: string): Observable<Reserva> {
    return this.http.patch<Reserva>(`${this.apiUrl}/${id}/equipo-recogido`, {
      fechaHoraDevolucion: fechaHoraDevolucion,
    });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
