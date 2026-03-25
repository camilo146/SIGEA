import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MantenimientoService } from '../../core/services/mantenimiento.service';
import { EquipoService } from '../../core/services/equipo.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import type { Mantenimiento, MantenimientoCrear, MantenimientoCerrar } from '../../core/models/mantenimiento.model';

type TabKey = 'todos' | 'en_curso' | 'preventivo' | 'correctivo' | 'finalizados';

@Component({
  selector: 'app-mantenimientos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mantenimientos.component.html',
  styleUrl: './mantenimientos.component.scss',
})
export class MantenimientosComponent implements OnInit {
  private mantenimientoService = inject(MantenimientoService);
  private equipoService = inject(EquipoService);
  private ui = inject(UiFeedbackService);

  mantenimientos = signal<Mantenimiento[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string }[]>([]);
  loading = signal(true);
  error = signal('');
  modalCrear = signal(false);
  modalCerrar = signal(false);
  editingId = signal<number | null>(null);
  selectedMantenimiento = signal<Mantenimiento | null>(null);
  activeTab = signal<TabKey>('todos');
  searchTerm = signal('');
  savingForm = signal(false);
  closingForm = signal(false);
  actionPending = signal<Record<string, boolean>>({});

  form: MantenimientoCrear = {
    equipoId: 0,
    tipo: 'PREVENTIVO',
    descripcion: '',
    fechaInicio: '',
    responsable: '',
    observaciones: '',
  };
  cerrarForm: MantenimientoCerrar = { fechaFin: '', observaciones: '' };

  readonly TIPOS = ['PREVENTIVO', 'CORRECTIVO'];

  readonly TABS = [
    { key: 'todos' as TabKey, label: 'Todos' },
    { key: 'en_curso' as TabKey, label: 'En Curso' },
    { key: 'preventivo' as TabKey, label: 'Preventivo' },
    { key: 'correctivo' as TabKey, label: 'Correctivo' },
    { key: 'finalizados' as TabKey, label: 'Finalizados' },
  ];

  tabFilteredMantenimientos = computed(() => {
    const all = this.mantenimientos();
    switch (this.activeTab()) {
      case 'en_curso': return all.filter((m) => !m.fechaFin);
      case 'preventivo': return all.filter((m) => m.tipo === 'PREVENTIVO');
      case 'correctivo': return all.filter((m) => m.tipo === 'CORRECTIVO');
      case 'finalizados': return all.filter((m) => !!m.fechaFin);
      default: return all;
    }
  });

  filteredMantenimientos = computed(() => {
    const q = this.searchTerm().toLowerCase().trim();
    if (!q) return this.tabFilteredMantenimientos();
    return this.tabFilteredMantenimientos().filter(
      (m) =>
        m.nombreEquipo.toLowerCase().includes(q) ||
        m.responsable.toLowerCase().includes(q) ||
        m.descripcion.toLowerCase().includes(q)
    );
  });

  tabCount(key: TabKey): number {
    const all = this.mantenimientos();
    switch (key) {
      case 'en_curso': return all.filter((m) => !m.fechaFin).length;
      case 'preventivo': return all.filter((m) => m.tipo === 'PREVENTIVO').length;
      case 'correctivo': return all.filter((m) => m.tipo === 'CORRECTIVO').length;
      case 'finalizados': return all.filter((m) => !!m.fechaFin).length;
      default: return all.length;
    }
  }

  ngOnInit() {
    this.loadMantenimientos();
    this.equipoService.listarActivos().subscribe({
      next: (l) => this.equipos.set(l.map((e) => ({ id: e.id, nombre: e.nombre, codigoUnico: e.codigoUnico }))),
      error: () => {},
    });
  }

  loadMantenimientos() {
    this.loading.set(true);
    this.mantenimientoService.listar().subscribe({
      next: (list) => {
        this.mantenimientos.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar mantenimientos');
        this.loading.set(false);
      },
    });
  }

  setTab(tab: TabKey) {
    this.activeTab.set(tab);
    this.searchTerm.set('');
  }

  openCrear() {
    this.editingId.set(null);
    this.form = {
      equipoId: this.equipos()[0]?.id ?? 0,
      tipo: 'PREVENTIVO',
      descripcion: '',
      fechaInicio: new Date().toISOString().slice(0, 10),
      responsable: '',
      observaciones: '',
    };
    this.error.set('');
    this.modalCrear.set(true);
  }

  openEditar(m: Mantenimiento) {
    this.editingId.set(m.id);
    this.form = {
      equipoId: m.equipoId,
      tipo: m.tipo ?? 'PREVENTIVO',
      descripcion: m.descripcion ?? '',
      fechaInicio: m.fechaInicio ? new Date(m.fechaInicio).toISOString().slice(0, 10) : '',
      responsable: m.responsable ?? '',
      observaciones: m.observaciones ?? '',
    };
    this.error.set('');
    this.modalCrear.set(true);
  }

  submitCrear() {
    if (!this.form.equipoId || !this.form.descripcion?.trim() || !this.form.fechaInicio || !this.form.responsable?.trim()) {
      this.error.set('Complete equipo, descripción, fecha de inicio y responsable.');
      return;
    }
    const id = this.editingId();
    const obs = id != null
      ? this.mantenimientoService.actualizar(id, this.form)
      : this.mantenimientoService.crear(this.form);
    this.savingForm.set(true);
    obs.subscribe({
      next: () => {
        this.savingForm.set(false);
        this.modalCrear.set(false);
        this.editingId.set(null);
        this.loadMantenimientos();
        this.ui.success(`El mantenimiento de ${this.equipos().find((equipo) => equipo.id === this.form.equipoId)?.nombre ?? 'equipo'} fue guardado.`, 'Mantenimientos');
      },
      error: (err) => {
        this.savingForm.set(false);
        const message = err.error?.message ?? (id != null ? 'Error al actualizar' : 'Error al crear');
        this.error.set(message);
        this.ui.error(message, 'Mantenimientos');
      },
    });
  }

  async eliminar(m: Mantenimiento) {
    const confirmed = await this.ui.confirm(`¿Eliminar el mantenimiento de "${m.nombreEquipo}"? Solo se puede eliminar si no está cerrado.`, {
      title: 'Eliminar mantenimiento',
      confirmText: 'Eliminar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('delete', m.id, () => this.mantenimientoService.eliminar(m.id), `El mantenimiento de ${m.nombreEquipo} fue eliminado.`);
  }

  openCerrar(m: Mantenimiento) {
    this.selectedMantenimiento.set(m);
    this.cerrarForm = { fechaFin: new Date().toISOString().slice(0, 10), observaciones: '' };
    this.error.set('');
    this.modalCerrar.set(true);
  }

  submitCerrar() {
    const m = this.selectedMantenimiento();
    if (!m || !this.cerrarForm.fechaFin) return;
    this.closingForm.set(true);
    this.mantenimientoService.cerrar(m.id, this.cerrarForm).subscribe({
      next: () => {
        this.closingForm.set(false);
        this.modalCerrar.set(false);
        this.selectedMantenimiento.set(null);
        this.loadMantenimientos();
        this.ui.success(`El mantenimiento de ${m.nombreEquipo} fue cerrado.`, 'Mantenimientos');
      },
      error: (err) => {
        this.closingForm.set(false);
        const message = err.error?.message ?? 'Error al cerrar';
        this.error.set(message);
        this.ui.error(message, 'Mantenimientos');
      },
    });
  }

  formatDate(s: string | undefined): string {
    if (!s) return '—';
    return new Date(s).toLocaleDateString('es-CO');
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
        this.loadMantenimientos();
        this.ui.success(successMessage, 'Mantenimientos');
      },
      error: (err) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = err.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Mantenimientos');
      },
    });
  }
}
