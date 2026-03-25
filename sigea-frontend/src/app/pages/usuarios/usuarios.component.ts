import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../core/services/usuario.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';
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
  private ui = inject(UiFeedbackService);

  usuarios = signal<Usuario[]>([]);
  usuariosPendientes = signal<Usuario[]>([]);
  vistaActual = signal<VistaUsuarios>('todos');
  prestamosCounts = signal<Record<number, number>>({});
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  modalEditOpen = signal(false);
  editingUser = signal<Usuario | null>(null);
  createSaving = signal(false);
  editSaving = signal(false);
  actionPending = signal<Record<string, boolean>>({});
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

  async aprobar(u: Usuario) {
    const confirmed = await this.ui.confirm(`¿Aprobar el registro de ${u.nombreCompleto}? Podrá iniciar sesión con el rol asignado.`, {
      title: 'Aprobar usuario',
      confirmText: 'Aprobar',
      tone: 'success',
    });
    if (!confirmed) return;

    this.runAction('approve', u.id, () => this.usuarioService.aprobar(u.id), () => {
      this.loadUsuarios();
      this.loadPendientes();
    }, `El usuario ${u.nombreCompleto} fue aprobado.`);
  }

  async rechazar(u: Usuario) {
    const confirmed = await this.ui.confirm(`¿Rechazar y eliminar el registro de ${u.nombreCompleto}? Esta acción es irreversible.`, {
      title: 'Rechazar usuario',
      confirmText: 'Rechazar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('reject', u.id, () => this.usuarioService.rechazar(u.id), () => {
      this.loadPendientes();
    }, `El registro de ${u.nombreCompleto} fue rechazado.`);
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

  closeModal() { this.modalOpen.set(false); this.error.set(''); this.createSaving.set(false); }

  submitForm() {
    if (!this.form.nombreCompleto?.trim() || !this.form.correoElectronico?.trim() || !this.form.contrasena || this.form.contrasena.length < 8) {
      this.error.set('Nombre, correo y contraseña (mín. 8 caracteres) son obligatorios.');
      return;
    }
    this.createSaving.set(true);
    this.usuarioService.crear(this.form).subscribe({
      next: () => {
        this.createSaving.set(false);
        this.closeModal();
        this.loadUsuarios();
        this.ui.success(`El usuario ${this.form.nombreCompleto} fue registrado.`, 'Usuarios');
      },
      error: (err) => {
        this.createSaving.set(false);
        const message = err.error?.message ?? 'Error al crear usuario';
        this.error.set(message);
        this.ui.error(message, 'Usuarios');
      },
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
    this.runAction('activate', u.id, () => this.usuarioService.activar(u.id), () => this.loadUsuarios(), `El usuario ${u.nombreCompleto} fue activado.`);
  }

  async desactivar(u: Usuario) {
    const confirmed = await this.ui.confirm(`¿Desactivar a ${u.nombreCompleto}?`, {
      title: 'Desactivar usuario',
      confirmText: 'Desactivar',
      tone: 'warning',
    });
    if (!confirmed) return;

    this.runAction('deactivate', u.id, () => this.usuarioService.desactivar(u.id), () => this.loadUsuarios(), `El usuario ${u.nombreCompleto} fue desactivado.`);
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
    this.editSaving.set(false);
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
    this.editSaving.set(true);
    this.usuarioService.actualizar(u.id, payload).subscribe({
      next: () => {
        this.editSaving.set(false);
        this.closeEditModal();
        this.loadUsuarios();
        this.ui.success(`Los datos de ${u.nombreCompleto} fueron actualizados.`, 'Usuarios');
      },
      error: (e) => {
        this.editSaving.set(false);
        const message = e.error?.message ?? 'Error al actualizar';
        this.error.set(message);
        this.ui.error(message, 'Usuarios');
      },
    });
  }

  async eliminar(u: Usuario) {
    const confirmed = await this.ui.confirm(`¿Eliminar (desactivar) a ${u.nombreCompleto}? No podrá iniciar sesión.`, {
      title: 'Eliminar usuario',
      confirmText: 'Eliminar',
      tone: 'danger',
    });
    if (!confirmed) return;

    this.runAction('delete', u.id, () => this.usuarioService.eliminar(u.id), () => this.loadUsuarios(), `El usuario ${u.nombreCompleto} fue desactivado.`);
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

  isActionPending(action: string, id: number): boolean {
    return !!this.actionPending()[`${action}-${id}`];
  }

  private runAction(action: string, id: number, request: () => import('rxjs').Observable<unknown>, onSuccess: () => void, successMessage: string) {
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
        onSuccess();
        this.ui.success(successMessage, 'Usuarios');
      },
      error: (e) => {
        this.actionPending.update((state) => {
          const next = { ...state };
          delete next[key];
          return next;
        });
        const message = e.error?.message ?? 'No fue posible completar la acción.';
        this.error.set(message);
        this.ui.error(message, 'Usuarios');
      },
    });
  }
}
