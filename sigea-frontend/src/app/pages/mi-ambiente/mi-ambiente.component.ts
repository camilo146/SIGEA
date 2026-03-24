import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AmbienteService } from '../../core/services/ambiente.service';
import { AuthService } from '../../core/services/auth.service';
import type { Ambiente } from '../../core/models/ambiente.model';

@Component({
  selector: 'app-mi-ambiente',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mi-ambiente.component.html',
  styleUrl: './mi-ambiente.component.scss',
})
export class MiAmbienteComponent implements OnInit {
  private ambienteService = inject(AmbienteService);
  private auth = inject(AuthService);

  ambientes = signal<Ambiente[]>([]);
  loading = signal(true);
  error = signal('');

  isInstructor = this.auth.isInstructor;

  ngOnInit() {
    this.ambienteService.listarMiAmbiente().subscribe({
      next: (list) => {
        this.ambientes.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar tus ambientes.');
        this.loading.set(false);
      },
    });
  }
}
