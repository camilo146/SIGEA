import { Component, ElementRef, inject, OnInit, signal, computed, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ReservaService } from '../../core/services/reserva.service';
import { PrestamoAmbienteService } from '../../core/services/prestamo-ambiente.service';
import { EquipoService } from '../../core/services/equipo.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import { environment } from '../../../environments/environment';
import type { Reserva, ReservaCrear } from '../../core/models/reserva.model';
import type {
  PrestamoAmbiente,
  PrestamoAmbienteSolicitud,
  PrestamoAmbienteDevolucion,
  EstadoPrestamoAmbiente,
  TipoActividad,
} from '../../core/models/prestamo-ambiente.model';
import type { Ambiente } from '../../core/models/ambiente.model';

type TabKey = 'todas' | 'activas' | 'canceladas' | 'cumplidas';
type SeccionReserva = 'equipos' | 'ambientes';
type TabAmbientes = 'todas' | 'solicitadas' | 'aprobadas' | 'cerradas';

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
  private prestamoAmbienteService = inject(PrestamoAmbienteService);
  private equipoService = inject(EquipoService);
  private ambienteService = inject(AmbienteService);
  private router = inject(Router);
  private ui = inject(UiFeedbackService);

  reservas = signal<Reserva[]>([]);
  reservasAmbientes = signal<PrestamoAmbiente[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string; cantidadDisponible: number }[]>([]);
  ambientes = signal<Ambiente[]>([]);
  agendaAmbiente = signal<PrestamoAmbiente[]>([]);
  loading = signal(true);
  loadingAmbientes = signal(true);
  error = signal('');
  modalCrear = signal(false);
  modalCrearAmbiente = signal(false);
  modalEquipoRecogido = signal<Reserva | null>(null);
  modalDevolucionAmbiente = signal<PrestamoAmbiente | null>(null);
  fechaHoraDevolucion = signal('');
  activeTab = signal<TabKey>('todas');
  activeSection = signal<SeccionReserva>('equipos');
  activeTabAmbientes = signal<TabAmbientes>('todas');
  searchTerm = signal('');
  searchTermAmbientes = signal('');
  createSaving = signal(false);
  createAmbienteSaving = signal(false);
  pickupSaving = signal(false);
  devolucionAmbienteSaving = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  agendaFecha = signal('');
  calendarioMes = signal(`${new Date().toISOString().slice(0, 7)}-01`);
  mostrarCalendarioAmbiente = signal(false);

  isAdmin = this.auth.isAdmin;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;
  selectedAmbienteShellRef = viewChild<ElementRef<HTMLElement>>('selectedAmbienteShell');
  agendaCalendarCardRef = viewChild<ElementRef<HTMLElement>>('agendaCalendarCard');

  form: ReservaCrear = { equipoId: 0, cantidad: 1, fechaHoraInicio: '' };
  formAmbiente: PrestamoAmbienteSolicitud = {
    ambienteId: 0,
    fechaInicio: '',
    fechaFin: '',
    horaInicio: '08:00',
    horaFin: '10:00',
    proposito: '',
    numeroParticipantes: 1,
    tipoActividad: 'CLASE',
    observacionesSolicitud: '',
  };
  devolucionAmbienteForm: PrestamoAmbienteDevolucion = {
    observacionesDevolucion: '',
    estadoDevolucionAmbiente: 8,
  };

  readonly TABS = [
    { key: 'todas' as TabKey, label: 'Todas', color: 'gray' },
    { key: 'activas' as TabKey, label: 'Activas', color: 'green' },
    { key: 'canceladas' as TabKey, label: 'Canceladas', color: 'red' },
    { key: 'cumplidas' as TabKey, label: 'Cumplidas / Vencidas', color: 'blue' },
  ];
  readonly TABS_AMBIENTES = [
    { key: 'todas' as TabAmbientes, label: 'Todas', color: 'gray' },
    { key: 'solicitadas' as TabAmbientes, label: 'Solicitadas', color: 'blue' },
    { key: 'aprobadas' as TabAmbientes, label: 'Aprobadas', color: 'green' },
    { key: 'cerradas' as TabAmbientes, label: 'Cerradas', color: 'red' },
  ];
  readonly TIPOS_ACTIVIDAD: Array<{ value: TipoActividad; label: string }> = [
    { value: 'CLASE', label: 'Clase' },
    { value: 'TALLER', label: 'Taller' },
    { value: 'EVALUACION', label: 'Evaluación' },
    { value: 'REUNION', label: 'Reunión' },
    { value: 'OTRO', label: 'Otro' },
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

  tabFilteredReservasAmbientes = computed(() => {
    const all = this.reservasAmbientes();
    switch (this.activeTabAmbientes()) {
      case 'solicitadas':
        return all.filter((r) => r.estado === 'SOLICITADO');
      case 'aprobadas':
        return all.filter((r) => r.estado === 'APROBADO' || r.estado === 'ACTIVO');
      case 'cerradas':
        return all.filter((r) => ['RECHAZADO', 'DEVUELTO', 'CANCELADO'].includes(r.estado));
      default:
        return all;
    }
  });

  filteredReservasAmbientes = computed(() => {
    const q = this.searchTermAmbientes().toLowerCase().trim();
    if (!q) return this.tabFilteredReservasAmbientes();
    return this.tabFilteredReservasAmbientes().filter((r) => {
      const ambiente = this.getAmbienteInfo(r.ambienteId);
      return r.ambienteNombre.toLowerCase().includes(q)
        || (r.solicitanteNombre ?? '').toLowerCase().includes(q)
        || (r.proposito ?? '').toLowerCase().includes(q)
        || (r.observacionesSolicitud ?? '').toLowerCase().includes(q)
        || (ambiente?.ubicacion ?? '').toLowerCase().includes(q);
    });
  });

  ambientesReservables = computed(() => this.ambientes().filter((a) => a.activo && !a.padreId));

  ambienteSeleccionadoReserva = computed(() =>
    this.ambientesReservables().find((a) => a.id === this.formAmbiente.ambienteId) ?? null
  );

  agendaReservasDelDia = computed(() => {
    const fecha = this.agendaFecha();
    if (!fecha) return this.agendaAmbiente();
    return this.agendaAmbiente().filter((r) => {
      const inicio = (r.fechaInicio ?? '').toString().slice(0, 10);
      const fin = (r.fechaFin ?? '').toString().slice(0, 10);
      return fecha >= inicio && fecha <= fin;
    });
  });

  agendaMes = computed(() => {
    const inicioMes = new Date(`${this.calendarioMes()}T00:00:00`);
    const primerDia = inicioMes.getDay();
    const offset = primerDia === 0 ? 6 : primerDia - 1;
    const inicioGrid = new Date(inicioMes);
    inicioGrid.setDate(inicioMes.getDate() - offset);

    return Array.from({ length: 42 }, (_, index) => {
      const fecha = new Date(inicioGrid);
      fecha.setDate(inicioGrid.getDate() + index);
      const iso = this.toIsoDate(fecha);
      const items = this.agendaAmbiente()
        .filter((reserva) => this.reservaOcurreEnFecha(reserva, iso))
        .sort((left, right) => left.horaInicio.localeCompare(right.horaInicio));

      return {
        iso,
        dayNumber: fecha.getDate(),
        isCurrentMonth: fecha.getMonth() === inicioMes.getMonth(),
        isToday: iso === this.toIsoDate(new Date()),
        isSelected: iso === this.agendaFecha(),
        items,
      };
    });
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

  tabCountAmbientes(key: TabAmbientes): number {
    const all = this.reservasAmbientes();
    switch (key) {
      case 'solicitadas':
        return all.filter((r) => r.estado === 'SOLICITADO').length;
      case 'aprobadas':
        return all.filter((r) => r.estado === 'APROBADO' || r.estado === 'ACTIVO').length;
      case 'cerradas':
        return all.filter((r) => ['RECHAZADO', 'DEVUELTO', 'CANCELADO'].includes(r.estado)).length;
      default:
        return all.length;
    }
  }

  ngOnInit() {
    this.loadReservas();
    this.loadReservasAmbientes();
    this.loadAmbientes();
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

  loadReservasAmbientes() {
    this.loadingAmbientes.set(true);
    const obs = this.isAdminOrInstructor()
      ? this.prestamoAmbienteService.listarTodos()
      : this.prestamoAmbienteService.listarMisSolicitudes();

    obs.subscribe({
      next: (list) => {
        this.reservasAmbientes.set(list);
        this.loadingAmbientes.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar reservas de ambientes');
        this.loadingAmbientes.set(false);
      },
    });
  }

  loadAmbientes() {
    this.ambienteService.listarTodos().subscribe({
      next: (list) => this.ambientes.set(list),
      error: () => {},
    });
  }

  setTab(tab: TabKey) {
    this.activeTab.set(tab);
    this.searchTerm.set('');
  }

  setSection(section: SeccionReserva) {
    this.activeSection.set(section);
    this.error.set('');
  }

  setTabAmbientes(tab: TabAmbientes) {
    this.activeTabAmbientes.set(tab);
    this.searchTermAmbientes.set('');
  }

  fechaMinInicio(): string {
    const d = new Date();
    d.setMinutes(d.getMinutes() + 30);
    return d.toISOString().slice(0, 16);
  }

  fechaMinAmbiente(): string {
    return new Date().toISOString().slice(0, 10);
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

  openCrearAmbiente() {
    const hoy = this.fechaMinAmbiente();
    this.formAmbiente = {
      ambienteId: 0,
      fechaInicio: hoy,
      fechaFin: hoy,
      horaInicio: '08:00',
      horaFin: '10:00',
      proposito: '',
      numeroParticipantes: 1,
      tipoActividad: 'CLASE',
      observacionesSolicitud: '',
    };
    this.agendaFecha.set(hoy);
    this.calendarioMes.set(`${hoy.slice(0, 7)}-01`);
    this.agendaAmbiente.set([]);
    this.mostrarCalendarioAmbiente.set(false);
    this.error.set('');
    this.modalCrearAmbiente.set(true);
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

  onSelectAmbienteReserva(ambienteId: number | string) {
    const parsed = Number(ambienteId);
    this.formAmbiente = { ...this.formAmbiente, ambienteId: parsed };
    this.mostrarCalendarioAmbiente.set(false);
    const hoy = this.fechaMinAmbiente();
    this.agendaFecha.set(hoy);
    this.calendarioMes.set(`${hoy.slice(0, 7)}-01`);
    if (parsed > 0) this.cargarAgendaAmbiente(parsed);
  }

  selectAmbienteCard(ambiente: Ambiente, abrirCalendario = true) {
    this.onSelectAmbienteReserva(ambiente.id);
    this.mostrarCalendarioAmbiente.set(abrirCalendario);
    this.scrollToReservaSection(abrirCalendario ? 'calendar' : 'selected');
  }

  toggleCalendarioAmbiente() {
    if (!this.formAmbiente.ambienteId) return;
    const next = !this.mostrarCalendarioAmbiente();
    this.mostrarCalendarioAmbiente.set(next);
    if (next) {
      this.scrollToReservaSection('calendar');
    }
  }

  moverMes(offsetMeses: number) {
    const base = new Date(`${this.calendarioMes()}T00:00:00`);
    base.setMonth(base.getMonth() + offsetMeses);
    const nuevoMes = `${base.toISOString().slice(0, 7)}-01`;
    this.calendarioMes.set(nuevoMes);
    if (!this.agendaFecha().startsWith(nuevoMes.slice(0, 7))) {
      this.agendaFecha.set(nuevoMes);
    }
  }

  seleccionarDiaAgenda(iso: string) {
    this.agendaFecha.set(iso);
  }

  submitCrearAmbiente() {
    if (!this.formAmbiente.ambienteId || !this.formAmbiente.fechaInicio || !this.formAmbiente.fechaFin
      || !this.formAmbiente.horaInicio || !this.formAmbiente.horaFin || !this.formAmbiente.proposito.trim()
      || !this.formAmbiente.numeroParticipantes || this.formAmbiente.numeroParticipantes < 1 || !this.formAmbiente.tipoActividad) {
      this.error.set('Complete ambiente, fechas, horas, participantes, tipo de actividad y propósito.');
      return;
    }
    this.createAmbienteSaving.set(true);
    this.prestamoAmbienteService.solicitar(this.formAmbiente).subscribe({
      next: () => {
        this.createAmbienteSaving.set(false);
        this.modalCrearAmbiente.set(false);
        this.loadReservasAmbientes();
        this.ui.success('La reserva de ambiente fue creada correctamente.', 'Reservas de Ambientes');
      },
      error: (err) => {
        this.createAmbienteSaving.set(false);
        const detalle = Array.isArray(err.error?.detalles) ? err.error.detalles[0] : null;
        const message = detalle ?? err.error?.message ?? 'Error al crear reserva de ambiente';
        this.error.set(message);
        this.ui.error(message, 'Reservas de Ambientes');
      },
    });
  }

  cargarAgendaAmbiente(ambienteId: number) {
    this.agendaAmbiente.set([]);
    this.prestamoAmbienteService.listarPorAmbiente(ambienteId).subscribe({
      next: (list) => this.agendaAmbiente.set(list),
      error: () => this.agendaAmbiente.set([]),
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

  async aprobarAmbiente(r: PrestamoAmbiente) {
    const confirmed = await this.ui.confirm(`¿Aprobar la reserva del ambiente "${r.ambienteNombre}" para ${r.solicitanteNombre}?`, {
      title: 'Aprobar reserva de ambiente',
      confirmText: 'Aprobar',
      tone: 'success',
    });
    if (!confirmed) return;

    this.runActionAmbiente('approve-env', r.id, () => this.prestamoAmbienteService.aprobar(r.id), `La reserva del ambiente ${r.ambienteNombre} fue aprobada.`);
  }

  async rechazarAmbiente(r: PrestamoAmbiente) {
    const motivo = await this.ui.prompt('Motivo del rechazo:', '', {
      title: 'Rechazar reserva de ambiente',
      confirmText: 'Rechazar',
      tone: 'warning',
      placeholder: 'Escriba la razón del rechazo',
    });
    if (motivo === null) return;

    this.runActionAmbiente('reject-env', r.id, () => this.prestamoAmbienteService.rechazar(r.id, motivo || 'Sin observaciones'), `La reserva del ambiente ${r.ambienteNombre} fue rechazada.`);
  }

  openDevolucionAmbiente(r: PrestamoAmbiente) {
    this.modalDevolucionAmbiente.set(r);
    this.devolucionAmbienteForm = {
      observacionesDevolucion: r.observacionesDevolucion ?? '',
      estadoDevolucionAmbiente: r.estadoDevolucionAmbiente ?? 8,
    };
    this.error.set('');
  }

  closeDevolucionAmbiente() {
    this.modalDevolucionAmbiente.set(null);
    this.devolucionAmbienteSaving.set(false);
  }

  submitDevolucionAmbiente() {
    const reserva = this.modalDevolucionAmbiente();
    if (!reserva) return;
    if (!this.devolucionAmbienteForm.observacionesDevolucion?.trim()) {
      this.error.set('Describa el estado del ambiente al momento de la entrega.');
      return;
    }
    if (!this.devolucionAmbienteForm.estadoDevolucionAmbiente || this.devolucionAmbienteForm.estadoDevolucionAmbiente < 1 || this.devolucionAmbienteForm.estadoDevolucionAmbiente > 10) {
      this.error.set('La calificación del estado del ambiente debe estar entre 1 y 10.');
      return;
    }
    this.devolucionAmbienteSaving.set(true);
    this.prestamoAmbienteService.registrarDevolucion(reserva.id, this.devolucionAmbienteForm).subscribe({
      next: () => {
        this.devolucionAmbienteSaving.set(false);
        this.closeDevolucionAmbiente();
        this.loadReservasAmbientes();
        this.ui.success(`La devolución del ambiente ${reserva.ambienteNombre} fue registrada.`, 'Devolución de ambiente');
      },
      error: (err) => {
        this.devolucionAmbienteSaving.set(false);
        const message = err.error?.message ?? 'Error al registrar la devolución del ambiente';
        this.error.set(message);
        this.ui.error(message, 'Devolución de ambiente');
      },
    });
  }

  async cancelarAmbiente(r: PrestamoAmbiente) {
    const confirmed = await this.ui.confirm(`¿Cancelar la reserva del ambiente "${r.ambienteNombre}"?`, {
      title: 'Cancelar reserva de ambiente',
      confirmText: 'Cancelar reserva',
      tone: 'warning',
    });
    if (!confirmed) return;

    this.runActionAmbiente('cancel-env', r.id, () => this.prestamoAmbienteService.cancelar(r.id), `La reserva del ambiente ${r.ambienteNombre} fue cancelada.`);
  }

  estadoLabel(estado: string): string {
    const m: Record<string, string> = {
      ACTIVA: 'Activa', CANCELADA: 'Cancelada', COMPLETADA: 'Completada', EXPIRADA: 'Expirada',
      PRESTADO: 'Prestado',
    };
    return m[estado] ?? estado;
  }

  estadoAmbienteLabel(estado: EstadoPrestamoAmbiente): string {
    const m: Record<EstadoPrestamoAmbiente, string> = {
      SOLICITADO: 'Solicitado',
      APROBADO: 'Aprobado',
      RECHAZADO: 'Rechazado',
      ACTIVO: 'Activo',
      DEVUELTO: 'Devuelto',
      CANCELADO: 'Cancelado',
    };
    return m[estado] ?? estado;
  }

  estadoAmbienteBadgeClass(estado: EstadoPrestamoAmbiente): string {
    const m: Record<EstadoPrestamoAmbiente, string> = {
      SOLICITADO: 'badge-blue',
      APROBADO: 'badge-success',
      RECHAZADO: 'badge-danger',
      ACTIVO: 'badge-success',
      DEVUELTO: 'badge-yellow',
      CANCELADO: 'badge-yellow',
    };
    return m[estado] ?? 'badge-blue';
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

  formatDateOnly(s: string | undefined): string {
    if (!s) return '—';
    return new Date(`${s}T00:00:00`).toLocaleDateString('es-CO', { dateStyle: 'medium' });
  }

  getAmbienteInfo(id: number): Ambiente | undefined {
    return this.ambientes().find((a) => a.id === id);
  }

  getAmbienteFotoUrl(a: Ambiente): string {
    if (!a?.rutaFoto) return '';
    const ruta = a.rutaFoto.trim();
    if (ruta.startsWith('http://') || ruta.startsWith('https://')) return ruta;
    if (ruta.startsWith('/uploads/')) return `${environment.apiUrl}${ruta}`;
    if (ruta.startsWith('uploads/')) return `${environment.apiUrl}/${ruta}`;
    const path = ruta.startsWith('/') ? ruta.slice(1) : ruta;
    return `${environment.apiUrl}/uploads/${path}`;
  }

  getTituloCalendarioMes(): string {
    return new Date(`${this.calendarioMes()}T00:00:00`).toLocaleDateString('es-CO', {
      month: 'long',
      year: 'numeric',
    });
  }

  ambienteLabel(a: Ambiente): string {
    const partes = [a.nombre];
    if (a.ubicacion) partes.push(a.ubicacion);
    return partes.join(' · ');
  }

  puedeCancelarAmbiente(r: PrestamoAmbiente): boolean {
    if (r.estado !== 'SOLICITADO' && r.estado !== 'APROBADO') return false;
    return !this.isAdminOrInstructor() || this.isAdmin();
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

  private runActionAmbiente(action: string, id: number, request: () => import('rxjs').Observable<unknown>, successMessage: string) {
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
        this.loadReservasAmbientes();
        if (this.formAmbiente.ambienteId) this.cargarAgendaAmbiente(this.formAmbiente.ambienteId);
        this.ui.success(successMessage, 'Reservas de Ambientes');
      },
      error: (err) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = err.error?.message ?? 'No fue posible completar la acción en la reserva de ambiente.';
        this.error.set(message);
        this.ui.error(message, 'Reservas de Ambientes');
      },
    });
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private reservaOcurreEnFecha(reserva: PrestamoAmbiente, fechaIso: string): boolean {
    const inicio = (reserva.fechaInicio ?? '').toString().slice(0, 10);
    const fin = (reserva.fechaFin ?? '').toString().slice(0, 10);
    return !!inicio && !!fin && fechaIso >= inicio && fechaIso <= fin;
  }

  private scrollToReservaSection(target: 'selected' | 'calendar') {
    setTimeout(() => {
      const targetRef = target === 'calendar' ? this.agendaCalendarCardRef() : this.selectedAmbienteShellRef();
      const element = targetRef?.nativeElement;
      if (!element) return;

      const scrollContainer = element.closest('.modal-box');
      if (scrollContainer instanceof HTMLElement) {
        const top = element.offsetTop - 24;
        scrollContainer.scrollTo({ top, behavior: 'smooth' });
        return;
      }

      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }
}
