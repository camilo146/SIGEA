import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipoService } from '../../core/services/equipo.service';
import { AmbienteService } from '../../core/services/ambiente.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { AuthService } from '../../core/services/auth.service';
import type { Ambiente } from '../../core/models/ambiente.model';
import type { Usuario } from '../../core/models/usuario.model';

@Component({
  selector: 'app-alimentador',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './alimentador.component.html',
  styleUrls: ['./alimentador.component.scss'],
})
export class AlimentadorComponent implements OnInit {
  private fb = inject(FormBuilder);
  private equipoService = inject(EquipoService);
  private ambienteService = inject(AmbienteService);
  private usuarioService = inject(UsuarioService);
  private authService = inject(AuthService);
  private router = inject(Router);

  ambientes = signal<Ambiente[]>([]);
  instructores = signal<Usuario[]>([]);
  cargando = signal(false);
  mensajeExito = signal<string | null>(null);
  mensajeError = signal<string | null>(null);

  /** Pestaña activa: 'equipo' | 'ambiente' */
  tabActiva = signal<'equipo' | 'ambiente'>('equipo');

  equipoForm: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    descripcion: [''],
    codigoUnico: ['', Validators.required],
    categoriaId: [null, Validators.required],
    ambienteId: [null, Validators.required],
    propietarioId: [null, Validators.required],
    cantidadTotal: [1, [Validators.required, Validators.min(1)]],
    tipoUso: ['NO_CONSUMIBLE', Validators.required],
    umbralMinimo: [1, [Validators.required, Validators.min(0)]],
  });

  ambienteForm: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    ubicacion: [''],
    descripcion: [''],
    direccion: [''],
    idInstructorResponsable: [null, Validators.required],
  });

  ngOnInit(): void {
    this.ambienteService.listar().subscribe({
      next: (lista) => this.ambientes.set(lista),
      error: () => this.mensajeError.set('No se pudieron cargar los ambientes.'),
    });
    this.usuarioService.listarPorRol('INSTRUCTOR').subscribe({
      next: (lista) => this.instructores.set(lista),
      error: () => {},
    });
  }

  seleccionarTab(tab: 'equipo' | 'ambiente'): void {
    this.tabActiva.set(tab);
    this.mensajeExito.set(null);
    this.mensajeError.set(null);
  }

  crearEquipo(): void {
    if (this.equipoForm.invalid || this.cargando()) return;
    this.cargando.set(true);
    this.mensajeExito.set(null);
    this.mensajeError.set(null);

    this.equipoService.crear(this.equipoForm.value).subscribe({
      next: (equipo) => {
        this.mensajeExito.set(`Equipo "${equipo.nombre}" creado con éxito.`);
        this.equipoForm.reset({ tipoUso: 'NO_CONSUMIBLE', cantidadTotal: 1, umbralMinimo: 1 });
        this.cargando.set(false);
      },
      error: (err) => {
        if (err.status === 409) {
          this.mensajeError.set('Ya existe un equipo con ese código único.');
        } else {
          this.mensajeError.set(err.error?.mensaje ?? 'Error al crear el equipo.');
        }
        this.cargando.set(false);
      },
    });
  }

  crearAmbiente(): void {
    if (this.ambienteForm.invalid || this.cargando()) return;
    this.cargando.set(true);
    this.mensajeExito.set(null);
    this.mensajeError.set(null);

    this.ambienteService.crearSinFoto(this.ambienteForm.value).subscribe({
      next: (amb) => {
        this.mensajeExito.set(`Ambiente "${amb.nombre}" creado con éxito.`);
        this.ambienteForm.reset();
        // Recargar lista de ambientes
        this.ambienteService.listar().subscribe((lista) => this.ambientes.set(lista));
        this.cargando.set(false);
      },
      error: (err) => {
        if (err.status === 409) {
          this.mensajeError.set('Ya existe un ambiente con ese nombre.');
        } else {
          this.mensajeError.set(err.error?.mensaje ?? 'Error al crear el ambiente.');
        }
        this.cargando.set(false);
      },
    });
  }

  cerrarSesion(): void {
    this.authService.logout();
  }
}
