import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verificar-email',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="verificar-email-page">
      <div class="verificar-card">
        @if (mensaje()) {
          <div class="success">
            <i class="fas fa-check-circle"></i>
            <h2>Correo verificado</h2>
            <p>{{ mensaje() }}</p>
            <a routerLink="/login" class="btn btn-primary">Ir a iniciar sesión</a>
          </div>
        } @else {
          <h2><i class="fas fa-envelope-circle-check"></i> Verificar correo</h2>
          <p class="text-muted">Ingresa tu correo y el código de 6 dígitos que te enviamos al registrarte.</p>
          @if (error()) {
            <div class="alert alert-danger"><i class="fas fa-exclamation-circle"></i> {{ error() }}</div>
          }
          <form (ngSubmit)="enviar()" class="verificar-form">
            <div class="form-group">
              <label>Correo electrónico</label>
              <input type="email" [(ngModel)]="correo" name="correo" placeholder="correo@ejemplo.com" required />
            </div>
            <div class="form-group">
              <label>Código de 6 dígitos</label>
              <input type="text" [(ngModel)]="codigo" name="codigo" placeholder="123456" maxlength="6" (input)="onCodigoInput()" inputmode="numeric" />
            </div>
            <button type="submit" class="btn btn-primary" [disabled]="loading()">
              @if (loading()) { <i class="fas fa-spinner fa-spin"></i> Verificando... } @else { Verificar }
            </button>
          </form>
          <a routerLink="/login" class="link-back">Volver al inicio de sesión</a>
        }
      </div>
    </div>
  `,
  styles: [`
    .verificar-email-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
      background: var(--bg-body, #f5f5f5);
    }
    .verificar-card {
      background: var(--bg-card, #fff);
      border-radius: 12px;
      padding: 2rem;
      max-width: 420px;
      text-align: center;
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }
    .verificar-card h2 { margin: 0 0 0.5rem; font-size: 1.25rem; }
    .text-muted { color: #64748b; font-size: 0.9rem; margin-bottom: 1.25rem; }
    .verificar-form { text-align: left; margin-bottom: 1rem; }
    .verificar-form .form-group { margin-bottom: 1rem; }
    .verificar-form label { display: block; margin-bottom: 0.35rem; font-size: 0.9rem; font-weight: 500; }
    .verificar-form input {
      width: 100%;
      padding: 0.6rem 0.75rem;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      font-size: 1rem;
    }
    .verificar-form .btn { width: 100%; margin-top: 0.5rem; }
    .verificar-card .success i { font-size: 3rem; display: block; margin-bottom: 1rem; color: var(--color-success, #28a745); }
    .verificar-card .success h2 { margin: 0 0 0.5rem; }
    .verificar-card .success p { margin-bottom: 1.5rem; color: #64748b; }
    .verificar-card .btn { text-decoration: none; display: inline-block; }
    .link-back { display: block; margin-top: 1rem; font-size: 0.9rem; color: #64748b; }
    .alert-danger { background: #fee2e2; color: #991b1b; padding: 0.75rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.9rem; }
  `],
})
export class VerificarEmailComponent {
  private auth = inject(AuthService);

  correo = '';
  codigo = '';
  loading = signal(false);
  mensaje = signal('');
  error = signal('');

  onCodigoInput() {
    this.codigo = this.codigo.replace(/\D/g, '').slice(0, 6);
  }

  enviar() {
    this.error.set('');
    const codigoLimpio = this.codigo.replace(/\D/g, '').slice(0, 6);
    if (codigoLimpio.length !== 6) {
      this.error.set('El código debe tener 6 dígitos.');
      return;
    }
    if (!this.correo.trim()) {
      this.error.set('Ingresa tu correo electrónico.');
      return;
    }
    this.loading.set(true);
    this.auth.verificarCodigo(this.correo.trim(), codigoLimpio).subscribe({
      next: (msg) => {
        this.loading.set(false);
        this.mensaje.set(msg ?? 'Correo verificado correctamente. Ya puedes iniciar sesión.');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Código incorrecto o expirado.');
      },
    });
  }
}
