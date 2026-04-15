import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Ambiente, AmbienteCrear, SubUbicacionResumen } from '../models/ambiente.model';

@Injectable({ providedIn: 'root' })
export class AmbienteService {
  private readonly apiUrl = `${environment.apiUrl}/ambientes`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Ambiente[]> {
    return this.http.get<Ambiente[]>(this.apiUrl);
  }

  listarTodos(): Observable<Ambiente[]> {
    return this.http.get<Ambiente[]>(`${this.apiUrl}/todos`);
  }

  /** Ambientes que el usuario actual (instructor) administra. */
  listarMiAmbiente(): Observable<Ambiente[]> {
    return this.http.get<Ambiente[]>(`${this.apiUrl}/mi-ambiente`);
  }

  buscarPorId(id: number): Observable<Ambiente> {
    return this.http.get<Ambiente>(`${this.apiUrl}/${id}`);
  }

  crear(dto: AmbienteCrear, archivo: File): Observable<Ambiente> {
    return this.http.post<Ambiente>(this.apiUrl, this.buildFormData(dto, archivo));
  }

  actualizar(id: number, dto: AmbienteCrear): Observable<Ambiente> {
    return this.http.put<Ambiente>(`${this.apiUrl}/${id}`, dto);
  }

  actualizarConFoto(id: number, dto: AmbienteCrear, archivo: File): Observable<Ambiente> {
    return this.http.put<Ambiente>(`${this.apiUrl}/${id}`, this.buildFormData(dto, archivo));
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/activar`, {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  /** Crea un ambiente sin foto (solo JSON). Compatible con rol ALIMENTADOR_EQUIPOS. */
  crearSinFoto(dto: AmbienteCrear): Observable<Ambiente> {
    return this.http.post<Ambiente>(this.apiUrl, dto);
  }

  private buildFormData(dto: AmbienteCrear, archivo?: File): FormData {
    const formData = new FormData();
    formData.append('nombre', dto.nombre);
    if (dto.ubicacion) formData.append('ubicacion', dto.ubicacion);
    if (dto.descripcion) formData.append('descripcion', dto.descripcion);
    if (dto.direccion) formData.append('direccion', dto.direccion);
    if (dto.padreId != null) formData.append('padreId', String(dto.padreId));
    if (dto.idInstructorResponsable != null) {
      formData.append('idInstructorResponsable', String(dto.idInstructorResponsable));
    }
    if (archivo) formData.append('archivo', archivo);
    return formData;
  }

  /** Lista las sub-ubicaciones hijas de un ambiente padre. */
  listarSubUbicaciones(padreId: number): Observable<SubUbicacionResumen[]> {
    return this.http.get<SubUbicacionResumen[]>(`${this.apiUrl}/${padreId}/sub-ubicaciones`);
  }

  /** Crea una sub-ubicación dentro del ambiente padre indicado. */
  crearSubUbicacion(padreId: number, dto: AmbienteCrear): Observable<Ambiente> {
    return this.http.post<Ambiente>(`${this.apiUrl}/${padreId}/sub-ubicaciones`, dto);
  }
}
