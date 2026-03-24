import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../core/services/usuario.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import type { Usuario, UsuarioCrear } from '../../core/models/usuario.model';

type VistaUsuarios = 'todos' | 'pendientes';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.scss',
})
export class UsuariosComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private prestamoService = inject(PrestamoService);

  usuarios = signal<Usuario[]>([]);
  usuariosPendientes = signal<Usuario[]>([]);
  vistaActual = signal<VistaUsuarios>('todos');
  prestamosCounts = signal<Record<number, number>>({});
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  modalEditOpen = signal(false);
  editingUser = signal<Usuario | null>(null);
  filterRol = signal('');
  filterEstado = signal('');
  searchTerm = signal('');

  form: UsuarioCrear = {
    nombreCompleto: '', tipoDocumento: 'CC', numeroDocumento: '',
    correoElectronico: '', programaFormacion: '', numeroFicha: '', telefono: '', contrasena: '', rol: 'USUARIO_ESTANDAR',
  };
  editForm: Partial<Usuario> & { numeroTelefono?: string; numeroFicha?: string } = {};
  showPassword = false;

  readonly ROLES = [
    { value: 'ADMINISTRADOR', label: 'Administrador', color: 'green' },
    { value: 'INSTRUCTOR', label: 'Instructor', color: 'blue' },
    { value: 'APRENDIZ', label: 'Aprendiz', color: 'purple' },
    { value: 'FUNCIONARIO', label: 'Funcionario', color: 'orange' },
    { value: 'USUARIO_ESTANDAR', label: 'Usuario estándar', color: 'gray' },
  ];
  readonly TIPOS_DOC = ['CC', 'TI', 'CE', 'PP', 'PEP'];

  private readonly ROLES_VALIDOS = new Set(this.ROLES.map((r) => r.value));

  filteredUsuarios = computed(() => {
    let list = this.usuarios();
    const rol = this.filterRol();
    const estado = this.filterEstado();
    const q = this.searchTerm().toLowerCase();
    if (rol) list = list.filter((u) => u.rol === rol);
    if (estado === 'activo') list = list.filter((u) => u.activo);
    if (estado === 'inactivo') list = list.filter((u) => !u.activo);
    if (q) list = list.filter((u) => u.nombreCompleto.toLowerCase().includes(q) || u.correoElectronico.toLowerCase().includes(q) || u.numeroDocumento?.includes(q));
    return list;
  });

  ngOnInit() {
    this.loadUsuarios();
    this.loadPendientes();
  }

  loadPendientes() {
    this.usuarioService.listarPendientes().subscribe({
      next: (list) => this.usuariosPendientes.set(list),
      error: () => {},
    });
  }

  loadUsuarios() {
    this.loading.set(true);
    this.usuarioService.listarTodos().subscribe({
      next: (list) => {
        this.usuarios.set(list);
        this.loading.set(false);
        this.loadPrestamosCounts(list);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar usuarios');
        this.loading.set(false);
      },
    });
  }

  cambiarVista(vista: VistaUsuarios) {
    this.vistaActual.set(vista);
    this.error.set('');
  }

  aprobar(u: Usuario) {
    if (!confirm(`¿Aprobar el registro de ${u.nombreCompleto}? Podrá iniciar sesión con el rol asignado.`)) return;
    this.usuarioService.aprobar(u.id).subscribe({
      next: () => { this.loadUsuarios(); this.loadPendientes(); },
      error: (e) => this.error.set(e.error?.message ?? 'Error al aprobar usuario'),
    });
  }

  rechazar(u: Usuario) {
    if (!confirm(`¿Rechazar y eliminar el registro de ${u.nombreCompleto}? Esta acción es irreversible.`)) return;
    this.usuarioService.rechazar(u.id).subscribe({
      next: () => { this.loadPendientes(); },
      error: (e) => this.error.set(e.error?.message ?? 'Error al rechazar usuario'),
    });
  }

  private loadPrestamosCounts(usuarios: Usuario[]) {
    this.prestamoService.listarTodos().subscribe({
      next: (prestamos) => {
        const counts: Record<number, number> = {};
        for (const p of prestamos) {
          const uid = p.usuarioSolicitanteId;
          counts[uid] = (counts[uid] ?? 0) + 1;
        }
        this.prestamosCounts.set(counts);
      },
      error: () => {},
    });
  }

  openCreate() {
    this.form = { nombreCompleto: '', tipoDocumento: 'CC', numeroDocumento: '', correoElectronico: '', programaFormacion: '', numeroFicha: '', telefono: '', contrasena: '', rol: 'USUARIO_ESTANDAR' };
    this.modalOpen.set(true);
  }

  closeModal() { this.modalOpen.set(false); this.error.set(''); }

  submitForm() {
    if (!this.form.nombreCompleto?.trim() || !this.form.correoElectronico?.trim() || !this.form.contrasena || this.form.contrasena.length < 8) {
      this.error.set('Nombre, correo y contraseña (mín. 8 caracteres) son obligatorios.');
      return;
    }
    this.usuarioService.crear(this.form).subscribe({
      next: () => { this.closeModal(); this.loadUsuarios(); },
      error: (err) => this.error.set(err.error?.message ?? 'Error al crear usuario'),
    });
  }

  cambiarRol(u: Usuario, event: Event) {
    const rol = (event.target as HTMLSelectElement).value;
    if (!this.ROLES_VALIDOS.has(rol)) {
      this.error.set('Rol seleccionado no válido.');
      return;
    }

    this.usuarioService.cambiarRol(u.id, rol).subscribe({
      next: (actualizado) => {
        const list = this.usuarios();
        const idx = list.findIndex((x) => x.id === actualizado.id);
        if (idx >= 0) {
          const copy = [...list];
          copy[idx] = { ...copy[idx], rol: actualizado.rol };
          this.usuarios.set(copy);
        }
        this.loadUsuarios();
      },
      error: (e) => this.error.set(e.error?.message ?? 'Error'),
    });
  }

  activar(u: Usuario) {
    this.usuarioService.activar(u.id).subscribe({ next: () => this.loadUsuarios(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }

  desactivar(u: Usuario) {
    if (!confirm(`¿Desactivar a ${u.nombreCompleto}?`)) return;
    this.usuarioService.desactivar(u.id).subscribe({ next: () => this.loadUsuarios(), error: (e) => this.error.set(e.error?.message ?? 'Error') });
  }

  openEdit(u: Usuario) {
    this.editingUser.set(u);
    this.editForm = {
      nombreCompleto: u.nombreCompleto,
      tipoDocumento: u.tipoDocumento,
      numeroDocumento: u.numeroDocumento,
      correoElectronico: u.correoElectronico,
      telefono: u.telefono ?? '',
      programaFormacion: u.programaFormacion ?? '',
      ficha: u.ficha ?? '',
      numeroTelefono: u.telefono ?? '',
      numeroFicha: u.ficha ?? '',
    };
    this.modalEditOpen.set(true);
  }

  closeEditModal() {
    this.modalEditOpen.set(false);
    this.editingUser.set(null);
    this.error.set('');
  }

  submitEdit() {
    const u = this.editingUser();
    if (!u) return;
    const payload = {
      nombreCompleto: this.editForm.nombreCompleto,
      tipoDocumento: this.editForm.tipoDocumento,
      numeroDocumento: this.editForm.numeroDocumento,
      correoElectronico: this.editForm.correoElectronico,
      numeroTelefono: this.editForm.numeroTelefono ?? this.editForm.telefono ?? '',
      programaFormacion: this.editForm.programaFormacion ?? '',
      numeroFicha: this.editForm.numeroFicha ?? this.editForm.ficha ?? '',
    };
    this.usuarioService.actualizar(u.id, payload).subscribe({
      next: () => { this.closeEditModal(); this.loadUsuarios(); },
      error: (e) => this.error.set(e.error?.message ?? 'Error al actualizar'),
    });
  }

  eliminar(u: Usuario) {
    if (!confirm(`¿Eliminar (desactivar) a ${u.nombreCompleto}? No podrá iniciar sesión.`)) return;
    this.usuarioService.eliminar(u.id).subscribe({
      next: () => this.loadUsuarios(),
      error: (e) => this.error.set(e.error?.message ?? 'Error al eliminar'),
    });
  }

  getInitials(nombre: string): string {
    return nombre.split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  }

  getRolColor(rol: string): string {
    const map: Record<string, string> = { ADMINISTRADOR: 'green', INSTRUCTOR: 'blue', APRENDIZ: 'purple', FUNCIONARIO: 'orange' };
    return map[rol] ?? 'gray';
  }

  getPrestamoCount(userId: number): number {
    return this.prestamosCounts()[userId] ?? 0;
  }

  estadoAprobacionLabel(estado?: string): string {
    return estado === 'PENDIENTE' ? 'Pendiente' : 'Aprobado';
  }

  estadoAprobacionBadgeClass(estado?: string): string {
    return estado === 'PENDIENTE' ? 'badge-yellow' : 'badge-green';
  }

  get totalActivos() { return this.usuarios().filter((u) => u.activo).length; }
  get totalMora() { return 0; }
}
