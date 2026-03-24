import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { EquipoService } from '../../core/services/equipo.service';
import { environment } from '../../../environments/environment';
import type { Ambiente, AmbienteCrear } from '../../core/models/ambiente.model';
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
  /** Ambiente seleccionado para la card de detalle (al hacer clic en Ver equipos / fila). */
  selectedAmbiente = signal<Ambiente | null>(null);
  fotoArchivo: File | null = null;

  form: AmbienteCrear = {
    nombre: '',
    ubicacion: '',
    descripcion: '',
    idInstructorResponsable: 0,
  };

  filteredAmbientes = computed(() => {
    const q = this.searchTerm().toLowerCase();
    if (!q) return this.ambientes();
    return this.ambientes().filter(
      (a) =>
        a.nombre.toLowerCase().includes(q) ||
        (a.ubicacion ?? '').toLowerCase().includes(q) ||
        a.instructorResponsableNombre.toLowerCase().includes(q)
    );
  });

  /** Usuarios aptos como responsables: instructores y administradores */
  instructoresParaSelect = computed(() =>
    this.usuarios().filter((u) => u.rol === 'INSTRUCTOR' || u.rol === 'ADMINISTRADOR')
  );

  ngOnInit() {
    this.loadAmbientes();
    // listarTodos() requiere ADMIN — esta página ya tiene adminGuard, así que es seguro
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

    if (id == null && !this.fotoArchivo) {
      this.error.set('Debe adjuntar la foto de la ubicación para crearla.');
      return;
    }

    const obs =
      id != null
        ? this.ambienteService.actualizar(id, this.form)
        : this.ambienteService.crear(payload, this.fotoArchivo!);

    obs.subscribe({
      next: () => {
        this.closeModal();
        this.loadAmbientes();
      },
      error: (err) => this.error.set(err.error?.message ?? 'Error al guardar'),
    });
  }

  activar(a: Ambiente) {
    this.ambienteService
      .activar(a.id)
      .subscribe({ next: () => this.loadAmbientes(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }

  desactivar(a: Ambiente) {
    if (!confirm(`¿Desactivar el ambiente "${a.nombre}"?`)) return;
    this.ambienteService
      .desactivar(a.id)
      .subscribe({ next: () => this.loadAmbientes(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
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
    this.ambienteEquiposNombre.set(a.nombre);
    this.modalEquiposOpen.set(true);
    this.loadingEquipos.set(true);
    this.equiposAmbiente.set([]);
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
  }

  closeEquiposModal() {
    this.modalEquiposOpen.set(false);
    this.selectedAmbiente.set(null);
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
}
