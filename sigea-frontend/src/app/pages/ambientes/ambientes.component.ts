import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { EquipoService } from '../../core/services/equipo.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
import { environment } from '../../../environments/environment';
import type { Ambiente, AmbienteCrear, SubUbicacionResumen } from '../../core/models/ambiente.model';
import type { Equipo } from '../../core/models/equipo.model';

@Component({
  selector: 'app-ambientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ambientes.component.html',
  styleUrl: './ambientes.component.scss',
})
export class AmbientesComponent implements OnInit {
  private auth = inject(AuthService);
  private ambienteService = inject(AmbienteService);
  private usuarioService = inject(UsuarioService);
  private equipoService = inject(EquipoService);
  private ui = inject(UiFeedbackService);

  isAdminOrInstructor = this.auth.isAdminOrInstructor;
  canCreateAmbientes = this.auth.isOperativo;

  ambientes = signal<Ambiente[]>([]);
  usuarios = signal<{ id: number; nombreCompleto: string; rol: string }[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  modalEquiposOpen = signal(false);
  editingId = signal<number | null>(null);
  searchTerm = signal('');
  equiposAmbiente = signal<Equipo[]>([]);
  ambienteEquiposNombre = signal('');
  loadingEquipos = signal(false);
  formSaving = signal(false);
  actionPending = signal<Record<string, boolean>>({});
  /** Ambiente seleccionado para la card de detalle (al hacer clic en Ver equipos / fila). */
  selectedAmbiente = signal<Ambiente | null>(null);
  fotoArchivo: File | null = null;

  // Sub-ubicaciones
  modalSubUbicacionesOpen = signal(false);
  ambientePadreSeleccionado = signal<Ambiente | null>(null);
  subUbicaciones = signal<SubUbicacionResumen[]>([]);
  loadingSubUbicaciones = signal(false);
  modalCrearSubOpen = signal(false);
  formSubSaving = signal(false);
  formSub: AmbienteCrear = { nombre: '', idInstructorResponsable: null };
  /** Sub-ubicación seleccionada en el panel de detalle (null = mostrar todos los equipos). */
  selectedSubUbicacion = signal<SubUbicacionResumen | null>(null);

  form: AmbienteCrear = {
    nombre: '',
    ubicacion: '',
    descripcion: '',
    idInstructorResponsable: 0,
  };

  filteredAmbientes = computed(() => {
    const q = this.searchTerm().toLowerCase();
    // Solo mostrar ubicaciones raíz (sin padre): las sub-ubicaciones se ven dentro de la card
    const padres = this.ambientes().filter((a) => !a.padreId);
    if (!q) return padres;
    return padres.filter(
      (a) =>
        a.nombre.toLowerCase().includes(q) ||
        (a.ubicacion ?? '').toLowerCase().includes(q) ||
        a.instructorResponsableNombre.toLowerCase().includes(q)
    );
  });

  /** Equipos de la card filtrados por la sub-ubicación seleccionada. */
  equiposFiltradosPorSub = computed(() => {
    const sub = this.selectedSubUbicacion();
    const equipos = this.equiposAmbiente();
    if (!sub) return equipos;
    return equipos.filter((e) => e.subUbicacionId === sub.id);
  });

  /** Usuarios aptos como responsables: instructores y administradores */
  instructoresParaSelect = computed(() =>
    this.usuarios().filter((u) => u.rol === 'INSTRUCTOR' || u.rol === 'ADMINISTRADOR')
  );

  ngOnInit() {
    this.loadAmbientes();
    this.usuarioService.listarTodos().subscribe({
      next: (list) =>
        this.usuarios.set(
          list.map((u) => ({
            id: u.id,
            nombreCompleto: u.nombreCompleto,
            rol: u.rol,
          }))
        ),
      error: () => {
        // Si falla, continuamos sin la lista de responsables
      },
    });
  }

  loadAmbientes() {
    this.loading.set(true);
    this.error.set('');
    this.ambienteService.listarTodos().subscribe({
      next: (list) => {
        this.ambientes.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar ambientes');
        this.loading.set(false);
      },
    });
  }

  openCreate() {
    this.editingId.set(null);
    this.fotoArchivo = null;
    this.form = {
      nombre: '',
      ubicacion: '',
      descripcion: '',
      idInstructorResponsable: this.instructoresParaSelect()[0]?.id ?? 0,
    };
    this.modalOpen.set(true);
    this.error.set('');
  }

  openEdit(a: Ambiente) {
    this.editingId.set(a.id);
    this.form = {
      nombre: a.nombre,
      ubicacion: a.ubicacion ?? '',
      descripcion: a.descripcion ?? '',
      idInstructorResponsable: a.instructorResponsableId,
    };
    this.modalOpen.set(true);
    this.error.set('');
  }

  closeModal() {
    this.modalOpen.set(false);
    this.editingId.set(null);
    this.fotoArchivo = null;
    this.formSaving.set(false);
  }

  onFotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file && file.type.startsWith('image/')) this.fotoArchivo = file;
    else this.fotoArchivo = null;
  }

  submitForm() {
    if (!this.form.nombre?.trim()) {
      this.error.set('El nombre del ambiente es obligatorio.');
      return;
    }
    const id = this.editingId();
    const isInstructorCreating = !id && this.auth.isInstructor();
    if (!isInstructorCreating && !this.form.idInstructorResponsable) {
      this.error.set('Seleccione un instructor responsable.');
      return;
    }
    const payload: AmbienteCrear = isInstructorCreating
      ? { ...this.form, idInstructorResponsable: null }
      : this.form;

    if (id == null && this.isAdminOrInstructor() && !this.fotoArchivo) {
      this.error.set('Debe adjuntar la foto de la ubicación para crearla.');
      return;
    }

    const obs =
      id != null
        ? this.ambienteService.actualizar(id, this.form)
        : this.fotoArchivo
          ? this.ambienteService.crear(payload, this.fotoArchivo)
          : this.ambienteService.crearSinFoto(payload);

    this.formSaving.set(true);
    obs.subscribe({
      next: () => {
        this.formSaving.set(false);
        this.closeModal();
        this.loadAmbientes();
        this.ui.success(`La ubicación "${this.form.nombre}" fue guardada.`, 'Ubicaciones');
      },
      error: (err) => {
        this.formSaving.set(false);
        const message = err.error?.message ?? 'Error al guardar';
        this.error.set(message);
        this.ui.error(message, 'Ubicaciones');
      },
    });
  }

  activar(a: Ambiente) {
    this.runAction('activate', a.id, () => this.ambienteService.activar(a.id), `La ubicación "${a.nombre}" fue activada.`);
  }

  async desactivar(a: Ambiente) {
    const confirmed = await this.ui.confirm(`¿Desactivar el ambiente "${a.nombre}"?`, {
      title: 'Desactivar ubicación',
      confirmText: 'Desactivar',
      tone: 'warning',
    });
    if (!confirmed) return;

    this.runAction('deactivate', a.id, () => this.ambienteService.desactivar(a.id), `La ubicación "${a.nombre}" fue desactivada.`);
  }

  get totalActivos() {
    return this.ambientes().filter((a) => a.activo).length;
  }

  getInitials(nombre: string): string {
    if (!nombre) return 'NA';
    return nombre.split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  }

  openEquipos(a: Ambiente) {
    this.selectedAmbiente.set(a);
    this.selectedSubUbicacion.set(null);
    this.ambienteEquiposNombre.set(a.nombre);
    this.modalEquiposOpen.set(true);
    this.loadingEquipos.set(true);
    this.equiposAmbiente.set([]);
    this.subUbicaciones.set([]);
    this.equipoService.listarPorAmbiente(a.id).subscribe({
      next: (list) => {
        this.equiposAmbiente.set(list);
        this.loadingEquipos.set(false);
      },
      error: () => {
        this.loadingEquipos.set(false);
        this.equiposAmbiente.set([]);
      },
    });
    // Cargar sub-ubicaciones del ambiente para mostrarlas en la card
    this.ambienteService.listarSubUbicaciones(a.id).subscribe({
      next: (list) => this.subUbicaciones.set(list),
      error: () => {},
    });
  }

  closeEquiposModal() {
    this.modalEquiposOpen.set(false);
    this.selectedAmbiente.set(null);
    this.selectedSubUbicacion.set(null);
    this.subUbicaciones.set([]);
  }

  selectSubUbicacion(sub: SubUbicacionResumen | null) {
    this.selectedSubUbicacion.set(sub);
  }

  /** URL de la foto del ambiente (opcional). */
  getAmbienteFotoUrl(a: Ambiente): string {
    if (!a?.rutaFoto) return '';
    const ruta = a.rutaFoto.trim();
    if (ruta.startsWith('http://') || ruta.startsWith('https://')) return ruta;
    if (ruta.startsWith('/uploads/')) return `${environment.apiUrl}${ruta}`;
    if (ruta.startsWith('uploads/')) return `${environment.apiUrl}/${ruta}`;
    const path = ruta.startsWith('/') ? ruta.slice(1) : ruta;
    return `${environment.apiUrl}/uploads/${path}`;
  }

  /** URL de la primera foto del equipo (para listado en card). */
  getEquipoFotoUrl(e: Equipo): string {
    if (!e?.fotos?.length || !e.fotos[0]?.rutaArchivo) return '';
    const ruta = e.fotos[0].rutaArchivo.trim();
    if (ruta.startsWith('http://') || ruta.startsWith('https://')) return ruta;
    if (ruta.startsWith('/uploads/')) return `${environment.apiUrl}${ruta}`;
    if (ruta.startsWith('uploads/')) return `${environment.apiUrl}/${ruta}`;
    const path = ruta.startsWith('/') ? ruta.slice(1) : ruta;
    return `${environment.apiUrl}/uploads/${path}`;
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
        this.loadAmbientes();
        this.ui.success(successMessage, 'Ubicaciones');
      },
      error: (e) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = e.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Ubicaciones');
      },
    });
  }

  // -------- Sub-ubicaciones --------

  openSubUbicaciones(padre: Ambiente) {
    this.ambientePadreSeleccionado.set(padre);
    this.modalSubUbicacionesOpen.set(true);
    this.loadSubUbicaciones(padre.id);
  }

  closeSubUbicaciones() {
    this.modalSubUbicacionesOpen.set(false);
    this.ambientePadreSeleccionado.set(null);
    this.subUbicaciones.set([]);
  }

  private loadSubUbicaciones(padreId: number) {
    this.loadingSubUbicaciones.set(true);
    this.ambienteService.listarSubUbicaciones(padreId).subscribe({
      next: (list) => {
        this.subUbicaciones.set(list);
        this.loadingSubUbicaciones.set(false);
      },
      error: () => this.loadingSubUbicaciones.set(false),
    });
  }

  openCrearSubUbicacion() {
    const padre = this.ambientePadreSeleccionado();
    this.formSub = {
      nombre: '',
      ubicacion: '',
      descripcion: '',
      idInstructorResponsable: padre?.instructorResponsableId ?? null,
    };
    this.modalCrearSubOpen.set(true);
  }

  closeCrearSubUbicacion() {
    this.modalCrearSubOpen.set(false);
    this.formSubSaving.set(false);
  }

  submitSubUbicacion() {
    const padre = this.ambientePadreSeleccionado();
    if (!padre || !this.formSub.nombre?.trim()) return;
    this.formSubSaving.set(true);
    this.ambienteService.crearSubUbicacion(padre.id, this.formSub).subscribe({
      next: () => {
        this.formSubSaving.set(false);
        this.closeCrearSubUbicacion();
        this.loadSubUbicaciones(padre.id);
        this.ui.success(`Sub-ubicación "${this.formSub.nombre}" creada.`, 'Ubicaciones');
      },
      error: (err) => {
        this.formSubSaving.set(false);
        const msg = err.error?.mensaje ?? 'Error al crear la sub-ubicación.';
        this.error.set(msg);
        this.ui.error(msg, 'Ubicaciones');
      },
    });
  }
}
