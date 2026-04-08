import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrestamoAmbienteService } from '../../core/services/prestamo-ambiente.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { AuthService } from '../../core/services/auth.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import type {
  PrestamoAmbiente,
  PrestamoAmbienteSolicitud,
  EstadoPrestamoAmbiente,
  TipoActividad,
} from '../../core/models/prestamo-ambiente.model';
import type { Ambiente } from '../../core/models/ambiente.model';

@Component({
  selector: 'app-prestamos-ambientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prestamos-ambientes.component.html',
  styleUrl: './prestamos-ambientes.component.scss',
})
export class PrestamosAmbientesComponent implements OnInit {
  private svc = inject(PrestamoAmbienteService);
  private ambienteService = inject(AmbienteService);
  private auth = inject(AuthService);
  private ui = inject(UiFeedbackService);

  prestamos = signal<PrestamoAmbiente[]>([]);
  ambientes = signal<Ambiente[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  savingForm = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  /** 'mis' | 'todos' (admin/instructor ven todos) */
  vista = signal<'mis' | 'todos'>('mis');
  filterEstado = signal<EstadoPrestamoAmbiente | ''>('');

  isAdmin = this.auth.isAdmin;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;

  form: PrestamoAmbienteSolicitud = {
    ambienteId: 0,
    fechaInicio: '',
    fechaFin: '',
    horaInicio: '',
    horaFin: '',
    proposito: '',
    numeroParticipantes: 1,
    tipoActividad: 'CLASE',
    observacionesSolicitud: '',
  };

  readonly minFecha = new Date().toISOString().split('T')[0];

  readonly TIPOS_ACTIVIDAD: Array<{ value: TipoActividad; label: string }> = [
    { value: 'CLASE', label: 'Clase' },
    { value: 'TALLER', label: 'Taller' },
    { value: 'EVALUACION', label: 'Evaluación' },
    { value: 'REUNION', label: 'Reunión' },
    { value: 'OTRO', label: 'Otro' },
  ];

  readonly ESTADOS_FILTER: Array<{ value: EstadoPrestamoAmbiente | ''; label: string }> = [
    { value: '', label: 'Todos los estados' },
    { value: 'SOLICITADO', label: 'Solicitado' },
    { value: 'APROBADO', label: 'Aprobado' },
    { value: 'RECHAZADO', label: 'Rechazado' },
    { value: 'ACTIVO', label: 'Activo' },
    { value: 'DEVUELTO', label: 'Devuelto' },
    { value: 'CANCELADO', label: 'Cancelado' },
  ];

  ngOnInit() {
    this.ambienteService.listar().subscribe({ next: (a) => this.ambientes.set(a), error: () => {} });
    this.cargar();
  }

  cargar() {
    this.loading.set(true);
    this.error.set('');
    const obs = this.vista() === 'todos' && this.isAdminOrInstructor()
      ? this.svc.listarPorEstado('SOLICITADO')
      : this.svc.listarMisSolicitudes();
    obs.subscribe({
      next: (list) => { this.prestamos.set(list); this.loading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Error al cargar préstamos'); this.loading.set(false); },
    });
  }

  get filteredPrestamos(): PrestamoAmbiente[] {
    const est = this.filterEstado();
    return est ? this.prestamos().filter((p) => p.estado === est) : this.prestamos();
  }

  openSolicitar() {
    this.form = {
      ambienteId: this.ambientes()[0]?.id ?? 0,
      fechaInicio: this.minFecha,
      fechaFin: this.minFecha,
      horaInicio: '08:00',
      horaFin: '10:00',
      proposito: '',
      numeroParticipantes: 1,
      tipoActividad: 'CLASE',
      observacionesSolicitud: '',
    };
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
    this.savingForm.set(false);
    this.error.set('');
  }

  submitSolicitar() {
    if (!this.form.ambienteId || !this.form.fechaInicio || !this.form.fechaFin || !this.form.horaInicio || !this.form.horaFin || !this.form.proposito.trim() || !this.form.tipoActividad || !this.form.numeroParticipantes || this.form.numeroParticipantes < 1) {
      this.error.set('Complete ambiente, fechas, horas, tipo de actividad, participantes y propósito.');
      return;
    }
    if (this.form.fechaFin < this.form.fechaInicio) {
      this.error.set('La fecha de fin debe ser igual o posterior a la fecha de inicio.');
      return;
    }
    if (this.form.fechaInicio < this.minFecha) {
      this.error.set('La fecha de inicio no puede ser anterior a hoy.');
      return;
    }
    this.savingForm.set(true);
    this.svc.solicitar(this.form).subscribe({
      next: () => {
        this.savingForm.set(false);
        this.closeModal();
        this.cargar();
        this.ui.success('Solicitud de ambiente enviada correctamente.', 'Préstamos de Ambientes');
      },
      error: (err) => {
        this.savingForm.set(false);
        const detalle = Array.isArray(err.error?.detalles) ? err.error.detalles[0] : null;
        this.error.set(detalle ?? err.error?.message ?? 'Error al solicitar');
      },
    });
  }

  async aprobar(p: PrestamoAmbiente) {
    const ok = await this.ui.confirm(`¿Aprobar la solicitud de "${p.ambienteNombre}" de ${p.solicitanteNombre}?`, {
      title: 'Aprobar solicitud', confirmText: 'Aprobar', tone: 'success',
    });
    if (!ok) return;
    this.runAction('aprobar', p.id, () => this.svc.aprobar(p.id), 'Solicitud aprobada.');
  }

  async rechazar(p: PrestamoAmbiente) {
    const motivo = await this.ui.prompt('Motivo del rechazo (opcional):', '', { title: 'Rechazar solicitud', confirmText: 'Rechazar', tone: 'warning' });
    if (motivo === null) return;
    this.runAction('rechazar', p.id, () => this.svc.rechazar(p.id, motivo || undefined), 'Solicitud rechazada.');
  }

  async cancelar(p: PrestamoAmbiente) {
    const ok = await this.ui.confirm(`¿Cancelar tu solicitud de "${p.ambienteNombre}"?`, {
      title: 'Cancelar solicitud', confirmText: 'Cancelar', tone: 'warning',
    });
    if (!ok) return;
    this.runAction('cancelar', p.id, () => this.svc.cancelar(p.id), 'Solicitud cancelada.');
  }

  estadoLabel(estado: EstadoPrestamoAmbiente): string {
    const m: Record<string, string> = {
      SOLICITADO: 'Solicitado', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado',
      ACTIVO: 'Activo', DEVUELTO: 'Devuelto', CANCELADO: 'Cancelado',
    };
    return m[estado] ?? estado;
  }

  estadoBadgeClass(estado: EstadoPrestamoAmbiente): string {
    const m: Record<string, string> = {
      SOLICITADO: 'badge-blue', APROBADO: 'badge-success', RECHAZADO: 'badge-danger',
      ACTIVO: 'badge-success', DEVUELTO: 'badge-yellow', CANCELADO: 'badge-yellow',
    };
    return m[estado] ?? 'badge-blue';
  }

  isActionPending(action: string, id: number): boolean {
    return !!this.actionPending()[`${action}-${id}`];
  }

  private runAction(action: string, id: number, request: () => import('rxjs').Observable<unknown>, successMessage: string) {
    const key = `${action}-${id}`;
    if (this.actionPending()[key]) return;
    this.actionPending.update((s) => ({ ...s, [key]: true }));
    request().subscribe({
      next: () => {
        this.actionPending.update((s) => { const n = { ...s }; delete n[key]; return n; });
        this.cargar();
        this.ui.success(successMessage, 'Préstamos de Ambientes');
      },
      error: (err) => {
        this.actionPending.update((s) => { const n = { ...s }; delete n[key]; return n; });
        const msg = err.error?.message ?? 'Error al procesar la acción.';
        this.error.set(msg);
        this.ui.error(msg, 'Préstamos de Ambientes');
      },
    });
  }
}
