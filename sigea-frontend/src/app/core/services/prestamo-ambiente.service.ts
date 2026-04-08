import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type {
  PrestamoAmbiente,
  PrestamoAmbienteSolicitud,
  PrestamoAmbienteDevolucion,
  EstadoPrestamoAmbiente,
} from '../models/prestamo-ambiente.model';

@Injectable({ providedIn: 'root' })
export class PrestamoAmbienteService {
  private readonly apiUrl = `${environment.apiUrl}/prestamos-ambientes`;

  constructor(private http: HttpClient) {}

  solicitar(dto: PrestamoAmbienteSolicitud): Observable<PrestamoAmbiente> {
    return this.http.post<PrestamoAmbiente>(this.apiUrl, dto);
  }

  buscarPorId(id: number): Observable<PrestamoAmbiente> {
    return this.http.get<PrestamoAmbiente>(`${this.apiUrl}/${id}`);
  }

  listarMisSolicitudes(): Observable<PrestamoAmbiente[]> {
    return this.http.get<PrestamoAmbiente[]>(`${this.apiUrl}/mis-solicitudes`);
  }

  listarPorAmbiente(ambienteId: number): Observable<PrestamoAmbiente[]> {
    return this.http.get<PrestamoAmbiente[]>(`${this.apiUrl}/ambiente/${ambienteId}`);
  }

  listarPorEstado(estado: EstadoPrestamoAmbiente): Observable<PrestamoAmbiente[]> {
    return this.http.get<PrestamoAmbiente[]>(`${this.apiUrl}/estado/${estado}`);
  }

  aprobar(id: number): Observable<PrestamoAmbiente> {
    return this.http.put<PrestamoAmbiente>(`${this.apiUrl}/${id}/aprobar`, {});
  }

  rechazar(id: number, motivo?: string): Observable<PrestamoAmbiente> {
    return this.http.put<PrestamoAmbiente>(`${this.apiUrl}/${id}/rechazar`, {}, {
      params: { motivo: motivo ?? '' },
    });
  }

  registrarDevolucion(id: number, dto: PrestamoAmbienteDevolucion): Observable<PrestamoAmbiente> {
    return this.http.put<PrestamoAmbiente>(`${this.apiUrl}/${id}/devolver`, dto);
  }

  cancelar(id: number): Observable<PrestamoAmbiente> {
    return this.http.put<PrestamoAmbiente>(`${this.apiUrl}/${id}/cancelar`, {});
  }
}
