import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MarcaService } from '../../core/services/marca.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import type { Marca, MarcaCrear } from '../../core/models/marca.model';

@Component({
  selector: 'app-marcas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './marcas.component.html',
  styleUrl: './marcas.component.scss',
})
export class MarcasComponent implements OnInit {
  private marcaService = inject(MarcaService);
  private ui = inject(UiFeedbackService);

  marcas = signal<Marca[]>([]);
  loading = signal(true);
  saving = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  error = signal('');
  modalOpen = signal(false);
  editingId = signal<number | null>(null);
  searchTerm = signal('');
  filterEstado = signal<'todos' | 'activas' | 'inactivas'>('todos');

  form: MarcaCrear = {
    nombre: '',
    descripcion: '',
  };

  totalActivas   = computed(() => this.marcas().filter(m => m.activo).length);
  totalInactivas  = computed(() => this.marcas().filter(m => !m.activo).length);

  filteredMarcas = computed(() => {
    const q = this.searchTerm().trim().toLowerCase();
    const estado = this.filterEstado();

    return this.marcas().filter((marca) => {
      const pasaBusqueda = !q
        || marca.nombre.toLowerCase().includes(q)
        || (marca.descripcion ?? '').toLowerCase().includes(q);
      const pasaEstado = estado === 'todos'
        || (estado === 'activas' && marca.activo)
        || (estado === 'inactivas' && !marca.activo);
      return pasaBusqueda && pasaEstado;
    });
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set('');
    this.marcaService.listarTodas().subscribe({
      next: (marcas) => {
        const ordenadas = [...marcas].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
        this.marcas.set(ordenadas);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'No se pudieron cargar las marcas.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = { nombre: '', descripcion: '' };
    this.error.set('');
    this.modalOpen.set(true);
  }

  openEdit(marca: Marca): void {
    this.editingId.set(marca.id);
    this.form = {
      nombre: marca.nombre,
      descripcion: marca.descripcion ?? '',
    };
    this.error.set('');
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.saving.set(false);
    this.error.set('');
  }

  submitForm(): void {
    if (!this.form.nombre.trim()) {
      this.error.set('El nombre de la marca es obligatorio.');
      return;
    }

    this.saving.set(true);
    const request = this.editingId() == null
      ? this.marcaService.crear(this.form)
      : this.marcaService.actualizar(this.editingId()!, this.form);

    request.subscribe({
      next: () => {
        const titulo = this.editingId() == null ? 'Marca creada' : 'Marca actualizada';
        const mensaje = this.editingId() == null
          ? `La marca ${this.form.nombre} fue registrada.`
          : `La marca ${this.form.nombre} fue actualizada.`;
        this.saving.set(false);
        this.closeModal();
        this.cargar();
        this.ui.success(mensaje, titulo);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'No se pudo guardar la marca.');
      },
    });
  }

  async activar(marca: Marca): Promise<void> {
    await this.runStateChange('activar', marca, () => this.marcaService.activar(marca.id), 'Marca activada');
  }

  async desactivar(marca: Marca): Promise<void> {
    const ok = await this.ui.confirm(`¿Desactivar la marca ${marca.nombre}?`, {
      title: 'Desactivar marca',
      confirmText: 'Desactivar',
      tone: 'warning',
    });
    if (!ok) return;

    await this.runStateChange('desactivar', marca, () => this.marcaService.desactivar(marca.id), 'Marca desactivada');
  }

  estadoBadgeClass(activo: boolean): string {
    return activo ? 'badge-success' : 'badge-danger';
  }

  private async runStateChange(
    action: 'activar' | 'desactivar',
    marca: Marca,
    requestFactory: () => import('rxjs').Observable<void>,
    title: string,
  ): Promise<void> {
    const key = `${action}-${marca.id}`;
    if (this.actionPending()[key]) return;

    this.actionPending.update((state) => ({ ...state, [key]: true }));
    requestFactory().subscribe({
      next: () => {
        this.actionPending.update((state) => {
          const nextState = { ...state };
          delete nextState[key];
          return nextState;
        });
        this.cargar();
        this.ui.success(`La marca ${marca.nombre} fue ${action === 'activar' ? 'activada' : 'desactivada'}.`, title);
      },
      error: (err) => {
        this.actionPending.update((state) => {
          const nextState = { ...state };
          delete nextState[key];
          return nextState;
        });
        this.ui.error(err.error?.message ?? 'No se pudo actualizar el estado de la marca.', 'Marcas');
      },
    });
  }
}
