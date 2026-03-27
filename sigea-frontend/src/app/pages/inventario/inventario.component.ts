import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipoService } from '../../core/services/equipo.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { ReporteService } from '../../core/services/reporte.service';
import { AuthService } from '../../core/services/auth.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import { environment } from '../../../environments/environment';
import type { Equipo, EquipoCrear, TipoUsoEquipo } from '../../core/models/equipo.model';
import type { Categoria } from '../../core/models/categoria.model';
import type { Ambiente } from '../../core/models/ambiente.model';
import type { SubUbicacionResumen } from '../../core/models/ambiente.model';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventario.component.html',
  styleUrl: './inventario.component.scss',
})
export class InventarioComponent implements OnInit {
  private equipoService = inject(EquipoService);
  private categoriaService = inject(CategoriaService);
  private ambienteService = inject(AmbienteService);
  private reporteService = inject(ReporteService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private ui = inject(UiFeedbackService);

  equipos = signal<Equipo[]>([]);
  categorias = signal<Categoria[]>([]);
  ambientes = signal<Ambiente[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  editingId = signal<number | null>(null);
  savingForm = signal(false);
  actionPending = signal<Record<string, boolean>>({});

  filterCategoria = signal<number | ''>('');
  filterEstado = signal<string>('');
  filterAmbiente = signal<number | ''>('');
  searchTerm = signal('');
  /** Si false, no se muestran equipos dados de baja (activo=false). Para admin/instructor puede activarse. */
  incluirDadosDeBaja = signal(false);
  /** Vista actual: 'todos' | 'mi-inventario' | 'mis-equipos' */
  vistaActual = signal<'todos' | 'mi-inventario' | 'mis-equipos'>('todos');
  isAdmin = this.auth.isAdmin;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;
  /** Equipo seleccionado para ver detalle en card. */
  selectedEquipo = signal<Equipo | null>(null);

  /** Archivo de foto seleccionado al crear (obligatorio). */
  fotoArchivo: File | null = null;

  /** Sub-ubicaciones disponibles según la ubicación seleccionada en el formulario. */
  subUbicacionesAmbiente = signal<SubUbicacionResumen[]>([]);

  form: EquipoCrear = { nombre: '', descripcion: '', codigoUnico: '', categoriaId: 0, ambienteId: 0, subUbicacionId: null, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };

  readonly TIPOS_USO: Array<{ value: TipoUsoEquipo; label: string }> = [
    { value: 'NO_CONSUMIBLE', label: 'No consumible' },
    { value: 'CONSUMIBLE', label: 'Consumible' },
  ];

  filteredEquipos = computed(() => {
    const list = this.equipos();
    const cat = this.filterCategoria();
    const est = this.filterEstado();
    const amb = this.filterAmbiente();
    const q = this.searchTerm().toLowerCase().trim();
    const incluirBaja = this.incluirDadosDeBaja();
    return list.filter((e) => {
      if (!incluirBaja && !e.activo) return false;
      if (cat !== '' && e.categoriaId !== cat) return false;
      if (est && e.estado !== est) return false;
      if (amb !== '' && e.ambienteId !== amb) return false;
      if (q && !e.nombre.toLowerCase().includes(q) && !e.codigoUnico.toLowerCase().includes(q)) return false;
      return true;
    });
  });

  readonly ESTADOS = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVO', label: 'Disponible' },
    { value: 'EN_MANTENIMIENTO', label: 'En mantenimiento' },
  ];

  ngOnInit() {
    this.categoriaService.listarActivas().subscribe({ next: (c) => this.categorias.set(c), error: () => {} });
    this.ambienteService.listar().subscribe({ next: (a) => this.ambientes.set(a), error: () => {} });
    this.loadEquipos();
  }

  loadEquipos() {
    this.loading.set(true);
    this.error.set('');
    const vista = this.vistaActual();
    const obs = vista === 'mi-inventario'
      ? this.equipoService.listarMiInventario()
      : vista === 'mis-equipos'
        ? this.equipoService.listarMisEquipos()
        : this.equipoService.listarTodos();
    obs.subscribe({
      next: (list) => { this.equipos.set(list); this.loading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Error al cargar equipos'); this.loading.set(false); },
    });
  }

  cambiarVista(vista: 'todos' | 'mi-inventario' | 'mis-equipos') {
    this.vistaActual.set(vista);
    this.loadEquipos();
  }

  openCreate() {
    this.editingId.set(null);
    this.fotoArchivo = null;
    const defaultAmbienteId = this.ambientes()[0]?.id ?? 0;
    this.form = { nombre: '', descripcion: '', codigoUnico: '', categoriaId: this.categorias()[0]?.id ?? 0, ambienteId: defaultAmbienteId, subUbicacionId: null, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };
    this.subUbicacionesAmbiente.set([]);
    if (defaultAmbienteId) {
      this.ambienteService.listarSubUbicaciones(defaultAmbienteId).subscribe({
        next: (list) => this.subUbicacionesAmbiente.set(list),
        error: () => {},
      });
    }
    this.modalOpen.set(true);
  }

  openEdit(e: Equipo) {
    this.editingId.set(e.id);
    this.form = { nombre: e.nombre, descripcion: e.descripcion ?? '', codigoUnico: e.codigoUnico, categoriaId: e.categoriaId, ambienteId: e.ambienteId, subUbicacionId: e.subUbicacionId ?? null, cantidadTotal: e.cantidadTotal, tipoUso: e.tipoUso ?? 'NO_CONSUMIBLE', umbralMinimo: e.umbralMinimo };
    this.subUbicacionesAmbiente.set([]);
    if (e.ambienteId) {
      this.ambienteService.listarSubUbicaciones(e.ambienteId).subscribe({
        next: (list) => this.subUbicacionesAmbiente.set(list),
        error: () => {},
      });
    }
    this.modalOpen.set(true);
  }

  closeModal() { this.modalOpen.set(false); this.editingId.set(null); this.fotoArchivo = null; this.error.set(''); this.savingForm.set(false); this.subUbicacionesAmbiente.set([]); }

  /** Cuando se cambia la ubicación en el formulario, recarga las sub-ubicaciones y limpia la selección anterior. */
  onAmbienteChanged(id: number | string) {
    this.form.ambienteId = +id;
    this.form.subUbicacionId = null;
    this.subUbicacionesAmbiente.set([]);
    if (+id) {
      this.ambienteService.listarSubUbicaciones(+id).subscribe({
        next: (list) => this.subUbicacionesAmbiente.set(list),
        error: () => {},
      });
    }
  }

  /** Nombre de la ubicación seleccionada actualmente en el formulario. */
  getNombreAmbiente(id: number): string {
    return this.ambientes().find((a) => a.id === +id)?.nombre ?? '';
  }

  onFotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file && file.type.startsWith('image/')) this.fotoArchivo = file;
    else this.fotoArchivo = null;
  }

  submitForm() {
    if (!this.form.nombre?.trim() || !this.form.categoriaId || !this.form.ambienteId) {
      this.error.set('Complete nombre, categoría y ubicación.');
      return;
    }
    const id = this.editingId();
    const esCrear = id == null;
    if (esCrear && !this.fotoArchivo) {
      this.error.set('Debe adjuntar al menos una foto del equipo (JPG, PNG).');
      return;
    }
    this.savingForm.set(true);
    if (esCrear) {
      this.equipoService.crear(this.form).subscribe({
        next: (nuevo) => {
          this.equipoService.subirFoto(nuevo.id, this.fotoArchivo!).subscribe({
            next: () => {
              this.savingForm.set(false);
              this.closeModal();
              this.loadEquipos();
              this.ui.success(`El equipo "${nuevo.nombre}" fue registrado correctamente.`, 'Inventario');
            },
            error: (err) => {
              this.savingForm.set(false);
              const message = err.error?.message ?? 'Error al subir la foto';
              this.error.set(message);
              this.ui.error(message, 'Inventario');
            },
          });
        },
        error: (err) => {
          this.savingForm.set(false);
          const message = err.error?.message ?? 'Error';
          this.error.set(message);
          this.ui.error(message, 'Inventario');
        },
      });
    } else {
      this.equipoService.actualizar(id!, this.form).subscribe({
        next: () => {
          this.savingForm.set(false);
          this.closeModal();
          this.loadEquipos();
          this.ui.success(`El equipo "${this.form.nombre}" fue actualizado.`, 'Inventario');
        },
        error: (err) => {
          this.savingForm.set(false);
          const message = err.error?.message ?? 'Error';
          this.error.set(message);
          this.ui.error(message, 'Inventario');
        },
      });
    }
  }

  getFotoUrl(e: Equipo): string {
    const f = e.fotos?.[0];
    return f ? environment.apiUrl + f.rutaArchivo : '';
  }

  exportar(formato: 'xlsx' | 'pdf') {
    const cat = this.filterCategoria();
    const est = this.filterEstado();
    this.reporteService.reporteInventario(formato, undefined, cat === '' ? undefined : cat, est || undefined);
  }

  async darDeBaja(e: Equipo) {
    const confirmed = await this.ui.confirm(`¿Dar de baja el equipo "${e.nombre}"?`, {
      title: 'Dar de baja equipo',
      confirmText: 'Dar de baja',
      tone: 'warning',
    });
    if (!confirmed) return;

    this.runAction('deactivate', e.id, () => this.equipoService.darDeBaja(e.id), `El equipo "${e.nombre}" fue dado de baja.`);
  }

  activar(e: Equipo) {
    this.runAction('activate', e.id, () => this.equipoService.activar(e.id), `El equipo "${e.nombre}" fue activado.`);
  }

  async eliminarEquipo(e: Equipo) {
    const confirmed = await this.ui.confirm(`¿Eliminar permanentemente el equipo "${e.nombre}"? No podrá deshacerse.`, {
      title: 'Eliminar equipo',
      confirmText: 'Eliminar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('delete', e.id, () => this.equipoService.eliminar(e.id), `El equipo "${e.nombre}" fue eliminado.`);
  }

  estadoLabel(estado: string): string {
    const m: Record<string, string> = {
      ACTIVO: 'Disponible',
      EN_MANTENIMIENTO: 'En mantenimiento',
      // Compatibilidad visual si algún registro legado llega con estados antiguos.
      DISPONIBLE: 'Disponible',
      EN_PRESTAMO: 'En préstamo',
      DADO_DE_BAJA: 'Dado de baja',
    };
    return m[estado] ?? estado;
  }

  tipoUsoLabel(tipoUso: string | undefined): string {
    return tipoUso === 'CONSUMIBLE' ? 'Consumible' : 'No consumible';
  }

  solicitarEquipo(e: Equipo) {
    this.router.navigate(['/prestamos'], { state: { solicitarEquipoId: e.id } });
  }

  openDetalle(e: Equipo) {
    this.selectedEquipo.set(e);
  }

  closeDetalle() {
    this.selectedEquipo.set(null);
  }

  irAReserva(e: Equipo) {
    this.closeDetalle();
    this.router.navigate(['/reservas'], { state: { reservarEquipoId: e.id } });
  }

  /** Recuperar un equipo que fue transferido a otro inventario. */
  async recuperarEquipo(e: Equipo) {
    const confirmed = await this.ui.confirm(`¿Recuperar el equipo "${e.nombre}" a tu inventario?`, {
      title: 'Recuperar equipo',
      confirmText: 'Recuperar',
      tone: 'success',
    });
    if (!confirmed) return;

    this.runAction('recover', e.id, () => this.equipoService.recuperarEquipo(e.id), `El equipo "${e.nombre}" volvió a tu inventario.`);
  }

  /** true si el equipo fue transferido (está en otro inventario distinto al propietario). */
  estaTransferido(e: Equipo): boolean {
    return !!e.propietarioId && !!e.inventarioActualInstructorId && e.propietarioId !== e.inventarioActualInstructorId;
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
        this.loadEquipos();
        this.ui.success(successMessage, 'Inventario');
      },
      error: (err) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = err.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Inventario');
      },
    });
  }
}
