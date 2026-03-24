import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipoService } from '../../core/services/equipo.service';
import { CategoriaService } from '../../core/services/categoria.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { ReporteService } from '../../core/services/reporte.service';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';
import type { Equipo, EquipoCrear, TipoUsoEquipo } from '../../core/models/equipo.model';
import type { Categoria } from '../../core/models/categoria.model';
import type { Ambiente } from '../../core/models/ambiente.model';

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

  equipos = signal<Equipo[]>([]);
  categorias = signal<Categoria[]>([]);
  ambientes = signal<Ambiente[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  editingId = signal<number | null>(null);

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

  form: EquipoCrear = { nombre: '', descripcion: '', codigoUnico: '', categoriaId: 0, ambienteId: 0, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };

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
    this.form = { nombre: '', descripcion: '', codigoUnico: '', categoriaId: this.categorias()[0]?.id ?? 0, ambienteId: this.ambientes()[0]?.id ?? 0, cantidadTotal: 1, tipoUso: 'NO_CONSUMIBLE', umbralMinimo: 0 };
    this.modalOpen.set(true);
  }

  openEdit(e: Equipo) {
    this.editingId.set(e.id);
    this.form = { nombre: e.nombre, descripcion: e.descripcion ?? '', codigoUnico: e.codigoUnico, categoriaId: e.categoriaId, ambienteId: e.ambienteId, cantidadTotal: e.cantidadTotal, tipoUso: e.tipoUso ?? 'NO_CONSUMIBLE', umbralMinimo: e.umbralMinimo };
    this.modalOpen.set(true);
  }

  closeModal() { this.modalOpen.set(false); this.editingId.set(null); this.fotoArchivo = null; this.error.set(''); }

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
    if (esCrear) {
      this.equipoService.crear(this.form).subscribe({
        next: (nuevo) => {
          this.equipoService.subirFoto(nuevo.id, this.fotoArchivo!).subscribe({
            next: () => { this.closeModal(); this.loadEquipos(); },
            error: (err) => this.error.set(err.error?.message ?? 'Error al subir la foto'),
          });
        },
        error: (err) => this.error.set(err.error?.message ?? 'Error'),
      });
    } else {
      this.equipoService.actualizar(id!, this.form).subscribe({
        next: () => { this.closeModal(); this.loadEquipos(); },
        error: (err) => this.error.set(err.error?.message ?? 'Error'),
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

  darDeBaja(e: Equipo) {
    if (!confirm(`¿Dar de baja el equipo "${e.nombre}"?`)) return;
    this.equipoService.darDeBaja(e.id).subscribe({ next: () => this.loadEquipos(), error: (err) => this.error.set(err.error?.message ?? 'Error') });
  }

  activar(e: Equipo) {
    this.equipoService.activar(e.id).subscribe({ next: () => this.loadEquipos(), error: (err) => this.error.set(err.error?.message ?? 'Error') });
  }

  eliminarEquipo(e: Equipo) {
    if (!confirm(`¿Eliminar permanentemente el equipo "${e.nombre}"? No podrá deshacerse.`)) return;
    this.equipoService.eliminar(e.id).subscribe({
      next: () => this.loadEquipos(),
      error: (err) => this.error.set(err.error?.message ?? 'Error al eliminar'),
    });
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
  recuperarEquipo(e: Equipo) {
    if (!confirm(`¿Recuperar el equipo "${e.nombre}" a tu inventario?`)) return;
    this.equipoService.recuperarEquipo(e.id).subscribe({
      next: () => this.loadEquipos(),
      error: (err) => this.error.set(err.error?.message ?? 'Error al recuperar equipo'),
    });
  }

  /** true si el equipo fue transferido (está en otro inventario distinto al propietario). */
  estaTransferido(e: Equipo): boolean {
    return !!e.propietarioId && !!e.inventarioActualInstructorId && e.propietarioId !== e.inventarioActualInstructorId;
  }
}
