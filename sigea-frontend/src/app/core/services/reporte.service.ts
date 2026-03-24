import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly apiUrl = `${environment.apiUrl}/reportes`;

  constructor(private http: HttpClient) {}

  private descargar(url: string, params: HttpParams, nombreArchivo: string): void {
    this.http.get(url, { params, responseType: 'blob' }).subscribe({
      next: (blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = nombreArchivo;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: () => {},
    });
  }

  reporteInventario(formato: 'xlsx' | 'pdf' = 'xlsx', inventarioInstructorId?: number, categoriaId?: number, estado?: string): void {
    let params = new HttpParams().set('formato', formato);
    if (inventarioInstructorId != null) params = params.set('inventarioInstructorId', inventarioInstructorId);
    if (categoriaId != null) params = params.set('categoriaId', categoriaId);
    if (estado) params = params.set('estado', estado);
    this.descargar(`${this.apiUrl}/inventario`, params, `reporte-inventario.${formato}`);
  }

  reportePrestamos(formato: 'xlsx' | 'pdf' = 'xlsx', usuarioId?: number, equipoId?: number, desde?: string, hasta?: string, estado?: string): void {
    let params = new HttpParams().set('formato', formato);
    if (usuarioId != null) params = params.set('usuarioId', usuarioId);
    if (equipoId != null) params = params.set('equipoId', equipoId);
    if (desde) params = params.set('desde', desde);
    if (hasta) params = params.set('hasta', hasta);
    if (estado) params = params.set('estado', estado);
    this.descargar(`${this.apiUrl}/prestamos`, params, `reporte-prestamos.${formato}`);
  }

  reporteEquiposMasSolicitados(formato: 'xlsx' | 'pdf' = 'xlsx'): void {
    const params = new HttpParams().set('formato', formato);
    this.descargar(`${this.apiUrl}/equipos-mas-solicitados`, params, `reporte-equipos-mas-solicitados.${formato}`);
  }

  reporteUsuariosEnMora(formato: 'xlsx' | 'pdf' = 'xlsx'): void {
    const params = new HttpParams().set('formato', formato);
    this.descargar(`${this.apiUrl}/usuarios-en-mora`, params, `reporte-usuarios-en-mora.${formato}`);
  }
}
