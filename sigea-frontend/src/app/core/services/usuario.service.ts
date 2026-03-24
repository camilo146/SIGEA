import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Usuario, UsuarioCrear } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.apiUrl);
  }

  listarTodos(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/todos`);
  }

  listarPorRol(rol: string): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/rol/${rol}`);
  }

  buscarPorId(id: number): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/${id}`);
  }

  crear(dto: UsuarioCrear): Observable<Usuario> {
    return this.http.post<Usuario>(this.apiUrl, dto);
  }

  actualizar(id: number, dto: Partial<Usuario>): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/${id}`, dto);
  }

  cambiarRol(id: number, rol: string): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.apiUrl}/${id}/rol`, { nuevoRol: rol });
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/activar`, {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/desactivar`, {});
  }

  desbloquear(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/desbloquear`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  listarPendientes(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/pendientes`);
  }

  aprobar(id: number): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.apiUrl}/${id}/aprobar`, {});
  }

  rechazar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/rechazar`);
  }
}
