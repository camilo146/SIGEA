import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransferenciaService } from '../../core/services/transferencia.service';
import { EquipoService } from '../../core/services/equipo.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { UsuarioService } from '../../core/services/usuario.service';
import type { Transferencia, TransferenciaCrear } from '../../core/models/transferencia.model';

@Component({
  selector: 'app-transferencias',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transferencias.component.html',
  styleUrl: './transferencias.component.scss',
})
export class TransferenciasComponent implements OnInit {
  private transferenciaService = inject(TransferenciaService);
  private equipoService = inject(EquipoService);
  private ambienteService = inject(AmbienteService);
  private usuarioService = inject(UsuarioService);

  transferencias = signal<Transferencia[]>([]);
  equipos = signal<{ id: number; nombre: string; codigoUnico: string }[]>([]);
  ubicaciones = signal<{ id: number; nombre: string }[]>([]);
  instructores = signal<{ id: number; nombre: string }[]>([]);
  loading = signal(true);
  error = signal('');
  modalOpen = signal(false);
  searchTerm = signal('');

  form: TransferenciaCrear = {
    equipoId: 0,
    instructorDestinoId: 0,
    ubicacionDestinoId: undefined,
    cantidad: 1,
    motivo: '',
    fechaTransferencia: '',
  };

  filteredTransferencias = computed(() => {
    const q = this.searchTerm().toLowerCase().trim();
    if (!q) return this.transferencias();
    return this.transferencias().filter(
      (t) =>
        t.nombreEquipo.toLowerCase().includes(q) ||
        t.nombreInventarioOrigenInstructor.toLowerCase().includes(q) ||
        t.nombreInventarioDestinoInstructor.toLowerCase().includes(q) ||
        (t.nombrePropietarioEquipo ?? '').toLowerCase().includes(q) ||
        (t.nombreAdministrador ?? '').toLowerCase().includes(q)
    );
  });

  ngOnInit() {
    this.loadTransferencias();
    this.equipoService.listarActivos().subscribe({
      next: (l) => this.equipos.set(l.map((e) => ({ id: e.id, nombre: e.nombre, codigoUnico: e.codigoUnico }))),
      error: () => {},
    });
    this.ambienteService.listar().subscribe({
      next: (l) => this.ubicaciones.set(l.map((a) => ({ id: a.id, nombre: a.nombre }))),
      error: () => {},
    });
    this.usuarioService.listarPorRol('INSTRUCTOR').subscribe({
      next: (l) => this.instructores.set(l.map((u) => ({ id: u.id, nombre: u.nombreCompleto }))),
      error: () => {},
    });
  }

  loadTransferencias() {
    this.loading.set(true);
    this.transferenciaService.listar().subscribe({
      next: (list) => {
        this.transferencias.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar transferencias');
        this.loading.set(false);
      },
    });
  }

  openCreate() {
    this.form = {
      equipoId: this.equipos()[0]?.id ?? 0,
      instructorDestinoId: this.instructores()[0]?.id ?? 0,
      ubicacionDestinoId: this.ubicaciones()[0]?.id,
      cantidad: 1,
      motivo: '',
      fechaTransferencia: new Date().toISOString().slice(0, 16),
    };
    this.error.set('');
    this.modalOpen.set(true);
  }

  submitForm() {
    if (!this.form.equipoId || !this.form.instructorDestinoId) {
      this.error.set('Complete equipo e instructor destino.');
      return;
    }
    this.transferenciaService.crear(this.form).subscribe({
      next: () => {
        this.modalOpen.set(false);
        this.loadTransferencias();
      },
      error: (err) => this.error.set(err.error?.message ?? 'Error al registrar transferencia'),
    });
  }

  formatDate(s: string | undefined): string {
    if (!s) return '—';
    return new Date(s).toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
  }
}
