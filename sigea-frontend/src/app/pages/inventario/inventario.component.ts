import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipoService, ObservacionEquipo } from '../../core/services/equipo.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { MarcaService } from '../../core/services/marca.service';
import { ReporteService } from '../../core/services/reporte.service';
import { AuthService } from '../../core/services/auth.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import { ImageUploadService } from '../../core/services/image-upload.service';
import { environment } from '../../../environments/environment';
import type { Equipo, EquipoCrear, TipoUsoEquipo } from '../../core/models/equipo.model';
import type { Categoria } from '../../core/models/categoria.model';
import type { Ambiente } from '../../core/models/ambiente.model';
import type { SubUbicacionResumen } from '../../core/models/ambiente.model';
import type { Marca } from '../../core/models/marca.model';
import type { Usuario } from '../../core/models/usuario.model';

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
  private marcaService = inject(MarcaService);
  private reporteService = inject(ReporteService);
  private auth = inject(AuthService);
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);
  private ui = inject(UiFeedbackService);
  private imageUpload = inject(ImageUploadService);

  equipos = signal<Equipo[]>([]);
  categorias = signal<Categoria[]>([]);
  ambientes = signal<Ambiente[]>([]);
  marcas = signal<Marca[]>([]);
  usuarios = signal<Usuario[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  editingId = signal<number | null>(null);
  savingForm = signal(false);
  fotoProcesando = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  /** Observaciones del equipo en el panel de detalle. */
  observaciones = signal<ObservacionEquipo[]>([]);
  loadingObs = signal(false);
  /** Pestaña activa del panel de detalle: 'info' | 'observaciones' */
  detailTab = signal<'info' | 'observaciones'>('info');

  filterCategoria = signal<number | ''>('');
  filterEstado = signal<string>('');
  filterAmbiente = signal<number | ''>('');
  searchTerm = signal('');
  /** Si false, no se muestran equipos dados de baja (activo=false). Para admin/instructor puede activarse. */
  incluirDadosDeBaja = signal(false);
  /** Vista actual: 'todos' | 'mi-inventario' | 'mis-equipos' */
  vistaActual = signal<'todos' | 'mi-inventario' | 'mis-equipos'>('todos');
  isAdmin = this.auth.isAdmin;
  isOperativo = this.auth.isOperativo;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;
  isAlimentadorEquipos = this.auth.isAlimentadorEquipos;
  isSuperAdmin = this.auth.isSuperAdmin;
  /** Equipo seleccionado para ver detalle en card. */
  selectedEquipo = signal<Equipo | null>(null);

  /** Archivo de foto seleccionado al crear (obligatorio). */
  fotoArchivo: File | null = null;

  /** Estado para editar foto de un equipo existente. */
  editingFotoEquipoId = signal<number | null>(null);
  editFotoArchivo: File | null = null;
  editFotoProcesando = signal(false);
  editFotoSaving = signal(false);

  /** Paginación configurable. */
  readonly pageSizeOptions = [10, 20, 30, 60];
  pageSize = signal(10);
  currentPage = signal(1);

  /** Sub-ubicaciones disponibles según la ubicación seleccionada en el formulario. */
  subUbicacionesAmbiente = signal<SubUbicacionResumen[]>([]);

  form: EquipoCrear = { nombre: '', descripcion: '', codigoUnico: '', placa: '', serial: '', modelo: '', marcaId: null, categoriaId: 0, ambienteId: 0, subUbicacionId: null, propietarioId: null, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };

  canAssignOwner = computed(() => this.isAlimentadorEquipos() || this.isSuperAdmin() || this.isAdmin());
  usuariosDisponibles = computed(() =>
    this.usuarios()
      .filter((usuario) => usuario.activo)
      .sort((left, right) => left.nombreCompleto.localeCompare(right.nombreCompleto))
  );
  superAdminPredeterminado = computed(() =>
    this.usuariosDisponibles().find((usuario) => usuario.esSuperAdmin) ?? null
  );

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
      if (q && !e.nombre.toLowerCase().includes(q) && !(e.placa ?? '').toLowerCase().includes(q)) return false;
      return true;
    });
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filteredEquipos().length / this.pageSize())));

  paginatedEquipos = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.filteredEquipos().slice(start, start + this.pageSize());
  });

  setPageSize(size: number) {
    this.pageSize.set(size);
    this.currentPage.set(1);
  }

  goToPage(page: number) {
    const total = this.totalPages();
    if (page < 1 || page > total) return;
    this.currentPage.set(page);
  }

  readonly ESTADOS = [
    { value: '', label: 'Todos los estados' },
    { value: 'ACTIVO', label: 'Disponible' },
    { value: 'EN_MANTENIMIENTO', label: 'En mantenimiento' },
  ];

  ngOnInit() {
    this.categoriaService.listarActivas().subscribe({ next: (c) => this.categorias.set(c), error: () => {} });
    this.ambienteService.listar().subscribe({ next: (a) => this.ambientes.set(a), error: () => {} });
    this.marcaService.listarActivas().subscribe({ next: (m) => this.marcas.set(m), error: () => {} });
    this.usuarioService.listarTodos().subscribe({ next: (u) => this.usuarios.set(u), error: () => {} });
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
    // Si el usuario es ADMIN, el propietario por defecto es él mismo; si es ALIMENTADOR, el backend asignará el superadmin.
    const defaultPropietarioId = this.isAdmin() ? (this.auth.user()?.id ?? null) : null;
    this.form = { nombre: '', descripcion: '', codigoUnico: '', placa: '', serial: '', modelo: '', marcaId: null, categoriaId: this.categorias()[0]?.id ?? 0, ambienteId: defaultAmbienteId, subUbicacionId: null, propietarioId: defaultPropietarioId, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };
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
    this.form = { nombre: e.nombre, descripcion: e.descripcion ?? '', codigoUnico: e.codigoUnico, placa: e.placa ?? '', serial: e.serial ?? '', modelo: e.modelo ?? '', marcaId: e.marcaId ?? null, categoriaId: e.categoriaId, ambienteId: e.ambienteId, subUbicacionId: e.subUbicacionId ?? null, propietarioId: e.propietarioId ?? null, cantidadTotal: e.cantidadTotal, tipoUso: e.tipoUso ?? 'NO_CONSUMIBLE', umbralMinimo: e.umbralMinimo };
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

  openEditFoto(e: Equipo) {
    this.editingFotoEquipoId.set(e.id);
    this.editFotoArchivo = null;
    this.editFotoProcesando.set(false);
    this.editFotoSaving.set(false);
  }

  closeEditFoto() {
    this.editingFotoEquipoId.set(null);
    this.editFotoArchivo = null;
  }

  async onEditFotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) { this.editFotoArchivo = null; return; }
    this.editFotoProcesando.set(true);
    try {
      const prepared = await this.imageUpload.prepareForUpload(file);
      this.editFotoArchivo = prepared.file;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No fue posible preparar la foto.';
      this.editFotoArchivo = null;
      input.value = '';
      this.ui.error(message, 'Inventario');
    } finally {
      this.editFotoProcesando.set(false);
    }
  }

  submitEditFoto() {
    const id = this.editingFotoEquipoId();
    if (!id || !this.editFotoArchivo) return;
    this.editFotoSaving.set(true);
    this.equipoService.subirFoto(id, this.editFotoArchivo).subscribe({
      next: () => {
        this.editFotoSaving.set(false);
        this.closeEditFoto();
        this.loadEquipos();
        this.ui.success('Foto del equipo actualizada correctamente.', 'Inventario');
      },
      error: (err) => {
        this.editFotoSaving.set(false);
        const message = err.error?.message ?? 'Error al subir la foto';
        this.ui.error(message, 'Inventario');
      },
    });
  }

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

  async onFotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      this.fotoArchivo = null;
      return;
    }

    this.fotoProcesando.set(true);
    this.error.set('');

    try {
      const prepared = await this.imageUpload.prepareForUpload(file);
      this.fotoArchivo = prepared.file;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No fue posible preparar la foto.';
      this.fotoArchivo = null;
      input.value = '';
      this.error.set(message);
      this.ui.error(message, 'Inventario');
    } finally {
      this.fotoProcesando.set(false);
    }
  }

  submitForm() {
    const payload = this.normalizarFormulario();

    if (!payload.nombre?.trim() || !payload.categoriaId || !payload.ambienteId) {
      this.error.set('Complete nombre, categoría y ubicación.');
      return;
    }
    const id = this.editingId();
    const esCrear = id == null;
    if (esCrear && this.isAdminOrInstructor() && !this.fotoArchivo) {
      this.error.set('Debe adjuntar al menos una foto del equipo (JPG, PNG).');
      return;
    }
    this.savingForm.set(true);
    if (esCrear) {
      this.equipoService.crear(payload).subscribe({
        next: (nuevo) => {
          if (this.fotoArchivo) {
            this.equipoService.subirFoto(nuevo.id, this.fotoArchivo).subscribe({
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
            return;
          }

          this.savingForm.set(false);
          this.closeModal();
          this.loadEquipos();
          this.ui.success(`El equipo "${nuevo.nombre}" fue registrado correctamente.`, 'Inventario');
        },
        error: (err) => {
          this.savingForm.set(false);
          const message = err.error?.message ?? 'Error';
          this.error.set(message);
          this.ui.error(message, 'Inventario');
        },
      });
    } else {
      this.equipoService.actualizar(id!, payload).subscribe({
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

  getNombrePropietarioSeleccionado(): string {
    const propietarioId = this.form.propietarioId;
    if (propietarioId == null) {
      return this.superAdminPredeterminado()?.nombreCompleto ?? 'Superadmin';
    }
    return this.usuariosDisponibles().find((usuario) => usuario.id === propietarioId)?.nombreCompleto ?? 'Usuario seleccionado';
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
    this.detailTab.set('info');
    this.observaciones.set([]);
  }

  closeDetalle() {
    this.selectedEquipo.set(null);
    this.observaciones.set([]);
  }

  switchDetailTab(tab: 'info' | 'observaciones') {
    this.detailTab.set(tab);
    if (tab === 'observaciones') {
      const eq = this.selectedEquipo();
      if (!eq) return;
      this.loadingObs.set(true);
      this.equipoService.listarObservaciones(eq.id).subscribe({
        next: (list) => { this.observaciones.set(list); this.loadingObs.set(false); },
        error: () => { this.loadingObs.set(false); },
      });
    }
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

  private normalizarFormulario(): EquipoCrear {
    const ambienteId = Number(this.form.ambienteId);
    const categoriaId = Number(this.form.categoriaId);
    const cantidadTotal = Number(this.form.cantidadTotal);
    const umbralMinimo = Number(this.form.umbralMinimo);
    const marcaId = this.form.marcaId != null ? Number(this.form.marcaId) : null;
    const propietarioId = this.form.propietarioId != null ? Number(this.form.propietarioId) : null;
    const subUbicacionId = this.form.subUbicacionId != null
      ? Number(this.form.subUbicacionId)
      : null;

    const subUbicacionValida = subUbicacionId != null
      && this.subUbicacionesAmbiente().some((sub) => Number(sub.id) === subUbicacionId);

    return {
      ...this.form,
      nombre: this.form.nombre?.trim() ?? '',
      descripcion: this.form.descripcion?.trim() ?? '',
      codigoUnico: this.form.codigoUnico?.trim() ?? '',
      placa: this.form.placa?.trim() ?? '',
      serial: this.form.serial?.trim() ?? '',
      modelo: this.form.modelo?.trim() ?? '',
      categoriaId,
      ambienteId,
      cantidadTotal,
      umbralMinimo,
      marcaId,
      propietarioId,
      subUbicacionId: subUbicacionValida ? subUbicacionId : null,
    };
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
