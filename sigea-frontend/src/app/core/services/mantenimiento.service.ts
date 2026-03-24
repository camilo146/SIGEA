import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Mantenimiento, MantenimientoCrear, MantenimientoCerrar } from '../models/mantenimiento.model';

@Injectable({ providedIn: 'root' })
export class MantenimientoService {
  private readonly apiUrl = `${environment.apiUrl}/mantenimientos`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(this.apiUrl);
  }

  listarPorEquipo(equipoId: number): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(`${this.apiUrl}/equipo/${equipoId}`);
  }

  listarPorTipo(tipo: string): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(`${this.apiUrl}/tipo/${tipo}`);
  }

  listarEnCurso(): Observable<Mantenimiento[]> {
    return this.http.get<Mantenimiento[]>(`${this.apiUrl}/en-curso`);
  }

  buscarPorId(id: number): Observable<Mantenimiento> {
    return this.http.get<Mantenimiento>(`${this.apiUrl}/${id}`);
  }

  crear(dto: MantenimientoCrear): Observable<Mantenimiento> {
    return this.http.post<Mantenimiento>(this.apiUrl, dto);
  }

  cerrar(id: number, dto: MantenimientoCerrar): Observable<Mantenimiento> {
    return this.http.patch<Mantenimiento>(`${this.apiUrl}/${id}/cerrar`, dto);
  }

  actualizar(id: number, dto: MantenimientoCrear): Observable<Mantenimiento> {
    return this.http.put<Mantenimiento>(`${this.apiUrl}/${id}`, dto);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
