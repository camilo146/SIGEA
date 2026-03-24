import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Transferencia, TransferenciaCrear } from '../models/transferencia.model';

@Injectable({ providedIn: 'root' })
export class TransferenciaService {
  private readonly apiUrl = `${environment.apiUrl}/transferencias`;

  constructor(private http: HttpClient) {}

  listar(): Observable<Transferencia[]> {
    return this.http.get<Transferencia[]>(this.apiUrl);
  }

  buscarPorId(id: number): Observable<Transferencia> {
    return this.http.get<Transferencia>(`${this.apiUrl}/${id}`);
  }

  crear(dto: TransferenciaCrear): Observable<Transferencia> {
    return this.http.post<Transferencia>(this.apiUrl, dto);
  }

  listarPorInstructorOrigen(instructorId: number): Observable<Transferencia[]> {
    return this.http.get<Transferencia[]>(`${this.apiUrl}/inventario-origen/${instructorId}`);
  }

  listarPorInstructorDestino(instructorId: number): Observable<Transferencia[]> {
    return this.http.get<Transferencia[]>(`${this.apiUrl}/inventario-destino/${instructorId}`);
  }
}
