import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardEstadisticas {
  totalEquipos: number;
  equiposActivos: number;
  totalCategorias: number;
  totalAmbientes: number;
  totalUsuarios: number;
  prestamosSolicitados: number;
  prestamosActivos: number;
  prestamosEnMora: number;
  prestamosDevueltos: number;
  reservasActivas: number;
  mantenimientosEnCurso: number;
  totalTransferencias: number;
  equiposStockBajo: number;
}

export interface PrestamosPorMes {
  mes: string;
  cantidad: number;
}

export interface EquiposPorCategoria {
  categoriaNombre: string;
  cantidad: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getEstadisticas(): Observable<DashboardEstadisticas> {
    return this.http.get<DashboardEstadisticas>(`${this.apiUrl}/estadisticas`);
  }

  getPrestamosPorMes(): Observable<PrestamosPorMes[]> {
    return this.http.get<PrestamosPorMes[]>(`${this.apiUrl}/grafico-prestamos-por-mes`);
  }

  getEquiposPorCategoria(): Observable<EquiposPorCategoria[]> {
    return this.http.get<EquiposPorCategoria[]>(`${this.apiUrl}/grafico-equipos-por-categoria`);
  }
}
