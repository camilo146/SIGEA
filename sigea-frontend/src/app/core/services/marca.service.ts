import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Marca, MarcaCrear } from '../models/marca.model';

@Injectable({ providedIn: 'root' })
export class MarcaService {
  private readonly apiUrl = `${environment.apiUrl}/marcas`;

  constructor(private http: HttpClient) {}

  listarActivas(): Observable<Marca[]> {
    return this.http.get<Marca[]>(this.apiUrl);
  }

  listarTodas(): Observable<Marca[]> {
    return this.http.get<Marca[]>(`${this.apiUrl}/todas`);
  }

  buscarPorId(id: number): Observable<Marca> {
    return this.http.get<Marca>(`${this.apiUrl}/${id}`);
  }

  crear(dto: MarcaCrear): Observable<Marca> {
    return this.http.post<Marca>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: MarcaCrear): Observable<Marca> {
    return this.http.put<Marca>(`${this.apiUrl}/${id}`, dto);
  }

  activar(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/activar`, {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/desactivar`, {});
  }
}
