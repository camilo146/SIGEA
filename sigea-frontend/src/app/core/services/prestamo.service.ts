import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Prestamo, PrestamoCrear, PrestamoDevolucion } from '../models/prestamo.model';

@Injectable({ providedIn: 'root' })
export class PrestamoService {
  private readonly apiUrl = `${environment.apiUrl}/prestamos`;

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(this.apiUrl);
  }

  listarMisPrestamos(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(`${this.apiUrl}/mis-prestamos`);
  }

  listarPorEstado(estado: string): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(`${this.apiUrl}/estado/${estado}`);
  }

  buscarPorId(id: number): Observable<Prestamo> {
    return this.http.get<Prestamo>(`${this.apiUrl}/${id}`);
  }

  solicitar(dto: PrestamoCrear): Observable<Prestamo> {
    return this.http.post<Prestamo>(this.apiUrl, dto);
  }

  aprobar(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.apiUrl}/${id}/aprobar`, {});
  }

  rechazar(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.apiUrl}/${id}/rechazar`, {});
  }

  registrarSalida(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.apiUrl}/${id}/registrar-salida`, {});
  }

  registrarDevolucion(id: number, dto: PrestamoDevolucion): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.apiUrl}/${id}/registrar-devolucion`, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
