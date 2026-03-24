import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../../core/services/reporte.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { UsuarioService } from '../../core/services/usuario.service';
import type { Usuario } from '../../core/models/usuario.model';
import type { Categoria } from '../../core/models/categoria.model';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrl: './reportes.component.scss',
})
export class ReportesComponent implements OnInit {
  private reporteService = inject(ReporteService);
  private usuarioService = inject(UsuarioService);
  private categoriaService = inject(CategoriaService);

  instructores = signal<Usuario[]>([]);
  categorias = signal<Categoria[]>([]);

  selectedReport: string | null = null;
  params = {
    inventarioInstructorId: undefined as number | undefined,
    categoriaId: undefined as number | undefined,
    estado: '',
    desde: '',
    hasta: '',
    estadoPrestamo: '',
  };

  readonly reportTypes = [
    { key: 'inventario', title: 'Inventario General', desc: 'Listado completo de equipos con estado y stock', icon: 'fa-cube', color: 'green' },
    { key: 'prestamos', title: 'Préstamos e Historial', desc: 'Historial de préstamos realizados', icon: 'fa-exchange-alt', color: 'blue' },
    { key: 'mas-solicitados', title: 'Equipos más Solicitados', desc: 'Ranking de equipos por número de préstamos', icon: 'fa-trophy', color: 'yellow' },
    { key: 'mora', title: 'Usuarios en Mora', desc: 'Usuarios con préstamos pendientes o vencidos', icon: 'fa-exclamation-triangle', color: 'red' },
  ];

  ngOnInit() {
    this.usuarioService.listarPorRol('INSTRUCTOR').subscribe({ next: (u) => this.instructores.set(u), error: () => {} });
    this.categoriaService.listarActivas().subscribe({ next: (c) => this.categorias.set(c), error: () => {} });
  }

  selectReport(key: string) {
    this.selectedReport = this.selectedReport === key ? null : key;
  }

  getSelectedTitle(): string {
    return this.reportTypes.find((r) => r.key === this.selectedReport)?.title ?? '';
  }

  exportar(formato: 'xlsx' | 'pdf') {
    this.quickExport(this.selectedReport!, formato);
  }

  quickExport(key: string, formato: 'xlsx' | 'pdf') {
    switch (key) {
      case 'inventario':
        this.reporteService.reporteInventario(formato, this.params.inventarioInstructorId, this.params.categoriaId, this.params.estado || undefined);
        break;
      case 'prestamos':
        this.reporteService.reportePrestamos(formato, undefined, undefined, this.params.desde || undefined, this.params.hasta || undefined, this.params.estadoPrestamo || undefined);
        break;
      case 'mas-solicitados':
        this.reporteService.reporteEquiposMasSolicitados(formato);
        break;
      case 'mora':
        this.reporteService.reporteUsuariosEnMora(formato);
        break;
    }
  }
}
