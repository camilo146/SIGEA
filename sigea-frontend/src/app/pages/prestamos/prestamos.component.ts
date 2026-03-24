import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Pipe, PipeTransform } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { EquipoService } from '../../core/services/equipo.service';
import type { Prestamo, PrestamoCrear } from '../../core/models/prestamo.model';

@Pipe({ name: 'dateFormat', standalone: true })
export class DateFormatPipe implements PipeTransform {
  transform(value: string | undefined): string {
    if (!value) return '-';
    return new Date(value).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }
}

type TabKey = 'todos' | 'activos' | 'pendientes' | 'historial' | 'vencidos';

@Component({
  selector: 'app-prestamos',
  standalone: true,
  imports: [CommonModule, FormsModule, DateFormatPipe],
  templateUrl: './prestamos.component.html',
  styleUrl: './prestamos.component.scss',
})
export class PrestamosComponent implements OnInit {
  private auth = inject(AuthService);
  private prestamoService = inject(PrestamoService);
  private equipoService = inject(EquipoService);
  private router = inject(Router);

  prestamos = signal<Prestamo[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string; cantidadDisponible: number }[]>([]);
  loading = signal(true);
  error = signal('');
  modalSolicitar = signal(false);
  activeTab = signal<TabKey>('todos');
  searchTerm = signal('');

  isAdmin = this.auth.isAdmin;

  form: PrestamoCrear = { fechaHoraDevolucionEstimada: '', observacionesGenerales: '', detalles: [] };
  detalleEquipoId = 0;
  detalleCantidad = 1;

  tabs = [
    { key: 'todos' as TabKey, label: 'Todos', count: null as number | null },
    { key: 'activos' as TabKey, label: 'Activos', count: null as number | null },
    { key: 'pendientes' as TabKey, label: 'Pendientes', count: null as number | null },
    { key: 'historial' as TabKey, label: 'Historial', count: null as number | null },
    { key: 'vencidos' as TabKey, label: 'Vencidos', count: null as number | null },
  ];

  stats = { mora: 0, devueltos: 0 };

  filteredPrestamos = computed(() => {
    const list = this.tabFilteredPrestamos();
    const q = this.searchTerm().toLowerCase().trim();
    if (!q) return list;
    return list.filter(
      (p) =>
        p.nombreUsuarioSolicitante?.toLowerCase().includes(q) ||
        p.detalles?.some((d) => d.nombreEquipo?.toLowerCase().includes(q)) ||
        String(p.id).includes(q)
    );
  });

  private tabFilteredPrestamos = computed(() => {
    const all = this.prestamos();
    switch (this.activeTab()) {
      case 'activos': return all.filter((p) => p.estado === 'ACTIVO' || p.estado === 'APROBADO');
      case 'pendientes': return all.filter((p) => p.estado === 'SOLICITADO');
      case 'historial': return all.filter((p) => p.estado === 'DEVUELTO' || p.estado === 'RECHAZADO');
      case 'vencidos': return all.filter((p) => p.estado === 'EN_MORA');
      default: return all;
    }
  });

  ngOnInit() {
    this.loadPrestamos();
    const state = (typeof history !== 'undefined' && history.state) as { solicitarEquipoId?: number } | undefined;
    const equipoIdPreseleccionado = state?.solicitarEquipoId;
    this.equipoService.listarActivos().subscribe({
      next: (list) => {
        this.equipos.set(list.map((e) => ({ id: e.id, nombre: e.nombre, codigoUnico: e.codigoUnico, cantidadDisponible: e.cantidadDisponible })));
        if (equipoIdPreseleccionado && list.some((e) => e.id === equipoIdPreseleccionado)) {
          this.form = { fechaHoraDevolucionEstimada: this.fechaMinDevolucion(), observacionesGenerales: '', detalles: [] };
          this.detalleEquipoId = equipoIdPreseleccionado;
          this.detalleCantidad = 1;
          this.modalSolicitar.set(true);
          this.router.navigate(['/prestamos'], { replaceUrl: true });
        }
      },
      error: () => {},
    });
  }

  loadPrestamos() {
    this.loading.set(true);
    this.error.set('');
    const obs = this.isAdmin() ? this.prestamoService.listarTodos() : this.prestamoService.listarMisPrestamos();
    obs.subscribe({
      next: (list) => {
        this.prestamos.set(list);
        this.loading.set(false);
        this.updateTabCounts(list);
        this.stats.mora = list.filter((p) => p.estado === 'EN_MORA').length;
        this.stats.devueltos = list.filter((p) => p.estado === 'DEVUELTO').length;
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar préstamos');
        this.loading.set(false);
      },
    });
  }

  private updateTabCounts(list: Prestamo[]) {
    this.tabs[0].count = list.length;
    this.tabs[1].count = list.filter((p) => p.estado === 'ACTIVO' || p.estado === 'APROBADO').length;
    this.tabs[2].count = list.filter((p) => p.estado === 'SOLICITADO').length;
    this.tabs[3].count = list.filter((p) => p.estado === 'DEVUELTO' || p.estado === 'RECHAZADO').length;
    this.tabs[4].count = list.filter((p) => p.estado === 'EN_MORA').length;
  }

  setTab(tab: TabKey) {
    this.activeTab.set(tab);
    this.searchTerm.set('');
  }

  openSolicitar() {
    this.form = { fechaHoraDevolucionEstimada: this.fechaMinDevolucion(), observacionesGenerales: '', detalles: [] };
    this.detalleEquipoId = this.equipos()[0]?.id ?? 0;
    this.detalleCantidad = 1;
    this.modalSolicitar.set(true);
  }

  fechaMinDevolucion(): string {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().slice(0, 16);
  }

  agregarDetalle() {
    if (this.detalleEquipoId <= 0 || this.detalleCantidad < 1) return;
    const existing = this.form.detalles?.find((d) => d.equipoId === this.detalleEquipoId);
    if (existing) { existing.cantidad += this.detalleCantidad; return; }
    this.form.detalles = [...(this.form.detalles ?? []), { equipoId: this.detalleEquipoId, cantidad: this.detalleCantidad }];
  }

  quitarDetalle(i: number) {
    this.form.detalles = this.form.detalles?.filter((_, idx) => idx !== i) ?? [];
  }

  getEquipoName(id: number): string {
    return this.equipos().find((e) => e.id === id)?.nombre ?? `Equipo #${id}`;
  }

  submitSolicitud() {
    if (!this.form.fechaHoraDevolucionEstimada || !this.form.detalles?.length) {
      this.error.set('Seleccione fecha de devolución y al menos un equipo.');
      return;
    }
    this.prestamoService.solicitar(this.form).subscribe({
      next: () => { this.modalSolicitar.set(false); this.loadPrestamos(); },
      error: (err) => this.error.set(err.error?.message ?? 'Error al solicitar'),
    });
  }

  aprobar(p: Prestamo) {
    this.prestamoService.aprobar(p.id).subscribe({ next: () => this.loadPrestamos(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }
  rechazar(p: Prestamo) {
    if (!confirm('¿Rechazar esta solicitud?')) return;
    this.prestamoService.rechazar(p.id).subscribe({ next: () => this.loadPrestamos(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }
  registrarSalida(p: Prestamo) {
    this.prestamoService.registrarSalida(p.id).subscribe({ next: () => this.loadPrestamos(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }
  registrarDevolucion(p: Prestamo) {
    this.prestamoService.registrarDevolucion(p.id).subscribe({ next: () => this.loadPrestamos(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }

  eliminar(p: Prestamo) {
    if (!confirm('¿Eliminar esta solicitud de préstamo? Solo se puede eliminar solicitudes en estado SOLICITADO.')) return;
    this.prestamoService.eliminar(p.id).subscribe({
      next: () => this.loadPrestamos(),
      error: (e) => this.error.set(e.error?.message ?? 'Error al eliminar'),
    });
  }

  estadoLabel(estado: string): string {
    const m: Record<string, string> = {
      SOLICITADO: 'Solicitado', APROBADO: 'Aprobado', ACTIVO: 'Activo',
      DEVUELTO: 'Devuelto', RECHAZADO: 'Rechazado', EN_MORA: 'En mora',
    };
    return m[estado] ?? estado;
  }

  tipoUsoLabel(tipoUso: string | undefined): string {
    return tipoUso === 'CONSUMIBLE' ? 'Consumible' : 'No consumible';
  }

  isOverdue(p: Prestamo): boolean {
    if (p.estado === 'DEVUELTO') return false;
    return p.fechaHoraDevolucionEstimada ? new Date(p.fechaHoraDevolucionEstimada) < new Date() : false;
  }
}
