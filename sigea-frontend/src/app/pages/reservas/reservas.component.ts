import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ReservaService } from '../../core/services/reserva.service';
import { EquipoService } from '../../core/services/equipo.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import type { Reserva, ReservaCrear } from '../../core/models/reserva.model';

type TabKey = 'todas' | 'activas' | 'canceladas' | 'cumplidas';

@Component({
  selector: 'app-reservas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservas.component.html',
  styleUrl: './reservas.component.scss',
})
export class ReservasComponent implements OnInit {
  private auth = inject(AuthService);
  private reservaService = inject(ReservaService);
  private equipoService = inject(EquipoService);
  private router = inject(Router);
  private ui = inject(UiFeedbackService);

  reservas = signal<Reserva[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string; cantidadDisponible: number }[]>([]);
  loading = signal(true);
  error = signal('');
  modalCrear = signal(false);
  modalEquipoRecogido = signal<Reserva | null>(null);
  fechaHoraDevolucion = signal('');
  activeTab = signal<TabKey>('todas');
  searchTerm = signal('');
  createSaving = signal(false);
  pickupSaving = signal(false);
  actionPending = signal<Record<string, boolean>>({});

  isAdmin = this.auth.isAdmin;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;

  form: ReservaCrear = { equipoId: 0, cantidad: 1, fechaHoraInicio: '' };

  readonly TABS = [
    { key: 'todas' as TabKey, label: 'Todas', color: 'gray' },
    { key: 'activas' as TabKey, label: 'Activas', color: 'green' },
    { key: 'canceladas' as TabKey, label: 'Canceladas', color: 'red' },
    { key: 'cumplidas' as TabKey, label: 'Cumplidas / Vencidas', color: 'blue' },
  ];

  tabFilteredReservas = computed(() => {
    const all = this.reservas();
    switch (this.activeTab()) {
      case 'activas': return all.filter((r) => r.estado === 'ACTIVA');
      case 'canceladas': return all.filter((r) => r.estado === 'CANCELADA');
      case 'cumplidas': return all.filter((r) => ['COMPLETADA', 'EXPIRADA', 'PRESTADO'].includes(r.estado));
      default: return all;
    }
  });

  filteredReservas = computed(() => {
    const q = this.searchTerm().toLowerCase().trim();
    if (!q) return this.tabFilteredReservas();
    return this.tabFilteredReservas().filter(
      (r) =>
        r.nombreEquipo.toLowerCase().includes(q) ||
        (r.nombreUsuario ?? '').toLowerCase().includes(q) ||
        r.codigoEquipo?.toLowerCase().includes(q)
    );
  });

  tabCount(key: TabKey): number {
    const all = this.reservas();
    switch (key) {
      case 'activas': return all.filter((r) => r.estado === 'ACTIVA').length;
      case 'canceladas': return all.filter((r) => r.estado === 'CANCELADA').length;
      case 'cumplidas': return all.filter((r) => ['COMPLETADA', 'EXPIRADA', 'PRESTADO'].includes(r.estado)).length;
      default: return all.length;
    }
  }

  ngOnInit() {
    this.loadReservas();
    this.equipoService.listarActivos().subscribe({
      next: (list) => {
        this.equipos.set(
          list.map((e) => ({
            id: e.id,
            nombre: e.nombre,
            codigoUnico: e.codigoUnico,
            cantidadDisponible: e.cantidadDisponible,
          }))
        );
        const state = history.state as { reservarEquipoId?: number } | undefined;
        if (state?.reservarEquipoId && list.some((e) => e.id === state.reservarEquipoId)) {
          this.form = { equipoId: state.reservarEquipoId, cantidad: 1, fechaHoraInicio: this.fechaMinInicio() };
          this.modalCrear.set(true);
          this.error.set('');
        }
      },
      error: () => {},
    });
  }

  loadReservas() {
    this.loading.set(true);
    this.error.set('');
    const obs = this.isAdmin()
      ? this.reservaService.listarTodos()
      : this.reservaService.listarMisReservas();

    obs.subscribe({
      next: (list) => {
        this.reservas.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar reservas');
        this.loading.set(false);
      },
    });
  }

  setTab(tab: TabKey) {
    this.activeTab.set(tab);
    this.searchTerm.set('');
  }

  fechaMinInicio(): string {
    const d = new Date();
    d.setMinutes(d.getMinutes() + 30);
    return d.toISOString().slice(0, 16);
  }

  openCrear() {
    this.form = {
      equipoId: this.equipos()[0]?.id ?? 0,
      cantidad: 1,
      fechaHoraInicio: this.fechaMinInicio(),
    };
    this.error.set('');
    this.modalCrear.set(true);
  }

  submitCrear() {
    if (!this.form.equipoId || this.form.cantidad < 1 || !this.form.fechaHoraInicio) {
      this.error.set('Complete equipo, cantidad y fecha de inicio.');
      return;
    }
    this.createSaving.set(true);
    this.reservaService.crear(this.form).subscribe({
      next: () => {
        this.createSaving.set(false);
        this.modalCrear.set(false);
        this.loadReservas();
        this.ui.success('La reserva fue creada correctamente.', 'Reservas');
      },
      error: (err) => {
        this.createSaving.set(false);
        const message = err.error?.message ?? 'Error al crear reserva';
        this.error.set(message);
        this.ui.error(message, 'Reservas');
      },
    });
  }

  async cancelar(r: Reserva) {
    const confirmed = await this.ui.confirm(`¿Cancelar la reserva de "${r.nombreEquipo}"?`, {
      title: 'Cancelar reserva',
      confirmText: 'Cancelar reserva',
      tone: 'warning',
    });
    if (!confirmed) return;

    this.runAction('cancel', r.id, () => this.reservaService.cancelar(r.id), `La reserva de ${r.nombreEquipo} fue cancelada.`);
  }

  async eliminarReserva(r: Reserva) {
    const confirmed = await this.ui.confirm(`¿Eliminar la reserva de "${r.nombreEquipo}"? No se puede eliminar si ya se convirtió en préstamo.`, {
      title: 'Eliminar reserva',
      confirmText: 'Eliminar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('delete', r.id, () => this.reservaService.eliminar(r.id), `La reserva de ${r.nombreEquipo} fue eliminada.`);
  }

  estadoLabel(estado: string): string {
    const m: Record<string, string> = {
      ACTIVA: 'Activa', CANCELADA: 'Cancelada', COMPLETADA: 'Completada', EXPIRADA: 'Expirada',
      PRESTADO: 'Prestado',
    };
    return m[estado] ?? estado;
  }

  tipoUsoLabel(tipoUso: string | undefined): string {
    return tipoUso === 'CONSUMIBLE' ? 'Consumible' : 'No consumible';
  }

  openEquipoRecogido(r: Reserva) {
    this.modalEquipoRecogido.set(r);
    const devolucion = new Date();
    devolucion.setDate(devolucion.getDate() + 1);
    devolucion.setHours(17, 0, 0, 0);
    this.fechaHoraDevolucion.set(devolucion.toISOString().slice(0, 16));
    this.error.set('');
  }

  closeEquipoRecogido() {
    this.modalEquipoRecogido.set(null);
    this.error.set('');
  }

  submitEquipoRecogido() {
    const r = this.modalEquipoRecogido();
    const fecha = this.fechaHoraDevolucion();
    if (!r || !fecha) {
      this.error.set('Indique la fecha y hora de devolución.');
      return;
    }
    this.pickupSaving.set(true);
    this.reservaService.marcarEquipoRecogido(r.id, fecha).subscribe({
      next: () => {
        this.pickupSaving.set(false);
        this.closeEquipoRecogido();
        this.loadReservas();
        this.ui.success(`La reserva de ${r.nombreEquipo} fue convertida en préstamo.`, 'Reservas');
      },
      error: (err) => {
        this.pickupSaving.set(false);
        const message = err.error?.message ?? 'Error al registrar equipo recogido';
        this.error.set(message);
        this.ui.error(message, 'Reservas');
      },
    });
  }

  isVencida(r: Reserva): boolean {
    return r.estado === 'ACTIVA' && new Date(r.fechaHoraFin) < new Date();
  }

  formatDate(s: string | undefined): string {
    if (!s) return '—';
    return new Date(s).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }

  getInitials(nombre: string | undefined): string {
    if (!nombre) return 'NA';
    return nombre.split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase();
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
        this.loadReservas();
        this.ui.success(successMessage, 'Reservas');
      },
      error: (err) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = err.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Reservas');
      },
    });
  }
}
