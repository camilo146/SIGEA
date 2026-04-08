import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Pipe, PipeTransform } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { EquipoService } from '../../core/services/equipo.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import type { Prestamo, PrestamoCrear, PrestamoDevolucionDetalle } from '../../core/models/prestamo.model';

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
  private ui = inject(UiFeedbackService);

  prestamos = signal<Prestamo[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string; cantidadDisponible: number }[]>([]);
  loading = signal(true);
  error = signal('');
  modalSolicitar = signal(false);
  modalDevolucion = signal<Prestamo | null>(null);
  activeTab = signal<TabKey>('todos');
  searchTerm = signal('');
  submitSaving = signal(false);
  devolucionSaving = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  devolucionDetalles = signal<PrestamoDevolucionDetalle[]>([]);

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
    this.submitSaving.set(true);
    this.prestamoService.solicitar(this.form).subscribe({
      next: () => {
        this.submitSaving.set(false);
        this.modalSolicitar.set(false);
        this.loadPrestamos();
        this.ui.success('La solicitud de préstamo fue enviada.', 'Préstamos');
      },
      error: (err) => {
        this.submitSaving.set(false);
        const message = err.error?.message ?? 'Error al solicitar';
        this.error.set(message);
        this.ui.error(message, 'Préstamos');
      },
    });
  }

  aprobar(p: Prestamo) {
    this.runAction('approve', p.id, () => this.prestamoService.aprobar(p.id), `El préstamo PR-${p.id.toString().padStart(3, '0')} fue aprobado.`);
  }
  async rechazar(p: Prestamo) {
    const confirmed = await this.ui.confirm('¿Rechazar esta solicitud?', {
      title: 'Rechazar solicitud',
      confirmText: 'Rechazar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('reject', p.id, () => this.prestamoService.rechazar(p.id), `La solicitud PR-${p.id.toString().padStart(3, '0')} fue rechazada.`);
  }
  registrarSalida(p: Prestamo) {
    this.runAction('checkout', p.id, () => this.prestamoService.registrarSalida(p.id), `Se registró la salida del préstamo PR-${p.id.toString().padStart(3, '0')}.`);
  }
  abrirDevolucion(p: Prestamo) {
    const detallesPendientes = (p.detalles ?? []).filter((detalle) => detalle.tipoUso !== 'CONSUMIBLE' && !detalle.devuelto);
    if (!detallesPendientes.length) {
      this.error.set('Este préstamo no tiene equipos pendientes por devolver.');
      return;
    }
    this.devolucionDetalles.set(
      detallesPendientes.map((detalle) => ({
        detalleId: detalle.id,
        estadoDevolucion: 8,
        observacionesDevolucion: '',
      }))
    );
    this.modalDevolucion.set(p);
    this.error.set('');
  }

  cerrarDevolucion() {
    this.modalDevolucion.set(null);
    this.devolucionDetalles.set([]);
    this.devolucionSaving.set(false);
  }

  actualizarDetalleDevolucion(index: number, patch: Partial<PrestamoDevolucionDetalle>) {
    this.devolucionDetalles.update((detalles) =>
      detalles.map((detalle, detalleIndex) => detalleIndex === index ? { ...detalle, ...patch } : detalle)
    );
  }

  detalleDevolucionNombre(detalleId: number): string {
    const prestamo = this.modalDevolucion();
    return prestamo?.detalles.find((detalle) => detalle.id === detalleId)?.nombreEquipo ?? `Equipo #${detalleId}`;
  }

  detalleDevolucionCantidad(detalleId: number): number {
    const prestamo = this.modalDevolucion();
    return prestamo?.detalles.find((detalle) => detalle.id === detalleId)?.cantidad ?? 1;
  }

  submitDevolucion() {
    const prestamo = this.modalDevolucion();
    if (!prestamo) return;

    const detalles = this.devolucionDetalles();
    const invalido = detalles.some((detalle) => !detalle.observacionesDevolucion.trim() || detalle.estadoDevolucion < 1 || detalle.estadoDevolucion > 10);
    if (!detalles.length || invalido) {
      this.error.set('Debes registrar calificación y observaciones en todos los equipos devueltos.');
      return;
    }

    this.devolucionSaving.set(true);
    this.prestamoService.registrarDevolucion(prestamo.id, { detalles }).subscribe({
      next: () => {
        this.devolucionSaving.set(false);
        this.cerrarDevolucion();
        this.loadPrestamos();
        this.ui.success(`Se registró la devolución evaluada del préstamo PR-${prestamo.id.toString().padStart(3, '0')}.`, 'Préstamos');
      },
      error: (e) => {
        this.devolucionSaving.set(false);
        const message = e.error?.message ?? 'No fue posible registrar la devolución.';
        this.error.set(message);
        this.ui.error(message, 'Préstamos');
      },
    });
  }

  async eliminar(p: Prestamo) {
    const confirmed = await this.ui.confirm('¿Eliminar esta solicitud de préstamo? Solo se puede eliminar solicitudes en estado SOLICITADO.', {
      title: 'Eliminar solicitud',
      confirmText: 'Eliminar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('delete', p.id, () => this.prestamoService.eliminar(p.id), `La solicitud PR-${p.id.toString().padStart(3, '0')} fue eliminada.`);
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

  isActionPending(action: string, id: number): boolean {
    return !!this.actionPending()[`${action}-${id}`];
  }

  private runAction(action: string, id: number, request: () => import('rxjs').Observable<unknown>, successMessage: string) {
    const key = `${action}-${id}`;
    if (this.actionPending()[key]) return;

    this.actionPending.update((state) => ({ ...state, [key]: true }));
    request().subscribe({
      next: () => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        this.loadPrestamos();
        this.ui.success(successMessage, 'Préstamos');
      },
      error: (e) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = e.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Préstamos');
      },
    });
  }
}
