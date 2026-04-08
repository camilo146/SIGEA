import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Equipo, EquipoCrear } from '../models/equipo.model';

@Injectable({ providedIn: 'root' })
export class EquipoService {
  private readonly apiUrl = `${environment.apiUrl}/equipos`;

  constructor(private http: HttpClient) {}

  listarActivos(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(this.apiUrl);
  }

  listarTodos(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/todos`);
  }

  listarPorCategoria(categoriaId: number): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/categoria/${categoriaId}`);
  }

  listarPorAmbiente(ambienteId: number): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/ambiente/${ambienteId}`);
  }

  listarPorEstado(estado: string): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/estado/${estado}`);
  }

  buscarPorId(id: number): Observable<Equipo> {
    return this.http.get<Equipo>(`${this.apiUrl}/${id}`);
  }

  crear(dto: EquipoCrear): Observable<Equipo> {
    return this.http.post<Equipo>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: EquipoCrear): Observable<Equipo> {
    return this.http.put<Equipo>(`${this.apiUrl}/${id}`, dto);
  }

  cambiarEstado(id: number, nuevoEstado: string): Observable<Equipo> {
    return this.http.patch<Equipo>(`${this.apiUrl}/${id}/estado/${nuevoEstado}`, {});
  }

  darDeBaja(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/dar-de-baja`, {});
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/activar`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  subirFoto(equipoId: number, archivo: File): Observable<{ id: number; nombreArchivo: string; rutaArchivo: string }> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<{ id: number; nombreArchivo: string; rutaArchivo: string }>(
      `${this.apiUrl}/${equipoId}/fotos`,
      formData
    );
  }

  eliminarFoto(equipoId: number, fotoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${equipoId}/fotos/${fotoId}`);
  }

  /** Equipos actualmente en el inventario del instructor autenticado. */
  listarMiInventario(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/mi-inventario`);
  }

  /** Todos los equipos de los que el instructor autenticado es propietario. */
  listarMisEquipos(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.apiUrl}/mis-equipos`);
  }

  /** El propietario recupera un equipo transferido a otro inventario. */
  recuperarEquipo(id: number): Observable<Equipo> {
    return this.http.patch<Equipo>(`${this.apiUrl}/${id}/recuperar`, {});
  }

  listarObservaciones(equipoId: number): Observable<ObservacionEquipo[]> {
    return this.http.get<ObservacionEquipo[]>(
      `${environment.apiUrl}/observaciones-equipo/equipo/${equipoId}`
    );
  }
}

export interface ObservacionEquipo {
  id: number;
  equipoId: number;
  prestamoId: number;
  observaciones?: string;
  estadoDevolucion: number;
  fechaRegistro: string;
  usuarioRegistradorNombre?: string;
}
