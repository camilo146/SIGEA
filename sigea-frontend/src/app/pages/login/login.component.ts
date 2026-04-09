import { Component, inject, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import type { RegisterRequest } from '../../core/models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  activeTab: 'login' | 'register' = 'login';
  loading = false;
  error = '';
  successMsg = '';

  /** Tras registro exitoso: mostrar paso de verificación por código. */
  correoPendienteVerificar = '';
  codigoVerificacion = '';
  showVerificacionCodigo = false;
  recoveryStep: 'request' | 'reset' | null = null;
  recoveryCorreo = '';
  recoveryCodigo = '';
  recoveryNuevaContrasena = '';
  recoveryConfirmPassword = '';
  showRecoveryPassword = false;

  /* ---- Login ---- */
  numeroDocumento = '';
  contrasena = '';
  showPassword = false;

  /* ---- Registro ---- */
  reg: RegisterRequest = {
    nombre: '',
    tipoDocumento: 'CC',
    numeroDocumento: '',
    correoElectronico: '',
    programaFormacion: '',
    telefono: '',
    numeroFicha: '',
    contrasena: '',
  };
  regConfirmPassword = '';
  showRegPassword = false;

  readonly TIPOS_DOC = ['CC', 'TI', 'CE', 'PP', 'PEP'];

  /** Comprueba requisitos de la contraseña para el indicador visual. */
  get passwordStrength(): { upperCase: boolean; number: boolean; special: boolean; length: boolean } {
    return this.getPasswordStrength(this.reg.contrasena ?? '');
  }

  get passwordStrengthScore(): number {
    const s = this.passwordStrength;
    return [s.length, s.upperCase, s.number, s.special].filter(Boolean).length;
  }

  ngOnInit() {
    if (this.auth.isLoggedIn()) this.router.navigate(['/dashboard']);
    this.route.queryParams.subscribe((params) => {
      if (params['sessionExpired'] === 'true') {
        this.successMsg = '';
        this.error = 'Tu sesión ha expirado. Inicia sesión de nuevo.';
      }
    });
  }

  switchTab(tab: 'login' | 'register') {
    this.activeTab = tab;
    this.error = '';
    this.successMsg = '';
    this.showVerificacionCodigo = false;
    this.correoPendienteVerificar = '';
    this.codigoVerificacion = '';
    this.resetRecoveryState();
  }

  onCodigoInput() {
    this.codigoVerificacion = this.codigoVerificacion.replace(/\D/g, '').slice(0, 6);
  }

  onRecoveryCodigoInput() {
    this.recoveryCodigo = this.recoveryCodigo.replace(/\D/g, '').slice(0, 6);
  }

  openRecovery() {
    this.error = '';
    this.successMsg = '';
    this.showVerificacionCodigo = false;
    this.recoveryStep = 'request';
    this.recoveryCorreo = '';
    this.recoveryCodigo = '';
    this.recoveryNuevaContrasena = '';
    this.recoveryConfirmPassword = '';
  }

  cancelRecovery() {
    this.resetRecoveryState();
    this.error = '';
  }

  solicitarRecuperacion() {
    this.error = '';
    this.successMsg = '';
    const correo = this.recoveryCorreo.trim().toLowerCase();
    if (!correo || !correo.includes('@')) {
      this.error = 'Ingrese un correo electronico valido.';
      return;
    }

    this.loading = true;
    this.auth.recuperarContrasena({ correo }).subscribe({
      next: (mensaje) => {
        this.loading = false;
        this.recoveryCorreo = correo;
        this.recoveryStep = 'reset';
        this.successMsg = mensaje;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message ?? 'No fue posible iniciar la recuperacion de contrasena.';
      },
    });
  }

  restablecerContrasena() {
    this.error = '';
    this.successMsg = '';
    const codigo = this.recoveryCodigo.replace(/\D/g, '').slice(0, 6);
    if (codigo.length !== 6) {
      this.error = 'El codigo debe tener 6 digitos.';
      return;
    }

    const strength = this.getPasswordStrength(this.recoveryNuevaContrasena);
    if (!strength.length || !strength.upperCase || !strength.number || !strength.special) {
      this.error = 'La nueva contrasena debe tener minimo 8 caracteres, una mayuscula, un numero y un caracter especial.';
      return;
    }

    if (this.recoveryNuevaContrasena !== this.recoveryConfirmPassword) {
      this.error = 'Las contrasenas no coinciden.';
      return;
    }

    this.loading = true;
    this.auth.restablecerContrasena({
      correo: this.recoveryCorreo.trim().toLowerCase(),
      codigo,
      nuevaContrasena: this.recoveryNuevaContrasena,
    }).subscribe({
      next: (mensaje) => {
        this.loading = false;
        this.resetRecoveryState();
        this.activeTab = 'login';
        this.successMsg = mensaje;
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message ?? 'No fue posible restablecer la contrasena.';
      },
    });
  }

  enviarCodigoVerificacion() {
    this.error = '';
    const codigo = this.codigoVerificacion.replace(/\D/g, '').slice(0, 6);
    if (codigo.length !== 6) {
      this.error = 'El código debe tener 6 dígitos.';
      return;
    }
    if (!this.correoPendienteVerificar.trim()) {
      this.error = 'No hay correo pendiente de verificar.';
      return;
    }
    this.loading = true;
    this.auth.verificarCodigo(this.correoPendienteVerificar, codigo).subscribe({
      next: (mensaje) => {
        this.loading = false;
        this.successMsg = mensaje;
        this.showVerificacionCodigo = false;
        this.correoPendienteVerificar = '';
        this.codigoVerificacion = '';
        this.switchTab('login');
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message ?? 'Código incorrecto o expirado. Intenta de nuevo.';
      },
    });
  }

  login() {
    this.error = '';
    if (!this.numeroDocumento.trim() || !this.contrasena) {
      this.error = 'Ingrese su número de documento y contraseña.';
      return;
    }
    this.loading = true;
    this.auth.login({ numeroDocumento: this.numeroDocumento.trim(), contrasena: this.contrasena }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message ?? '';
        // Si el backend envía un mensaje de negocio, mostrarlo tal cual
        // (ej: pendiente de aprobación, cuenta bloqueada, correo no verificado).
        if (msg) {
          this.error = msg;
        } else if (err.status === 401 || err.status === 403 || err.status === 400) {
          this.error = 'Correo o contraseña incorrectos.';
        } else {
          this.error = msg || 'Error al iniciar sesión. Intente de nuevo.';
        }
      },
      complete: () => (this.loading = false),
    });
  }

  register() {
    this.error = '';
    this.successMsg = '';
    if (!this.reg.nombre.trim() || this.reg.nombre.length < 2) {
      this.error = 'El nombre debe tener al menos 2 caracteres.';
      return;
    }
    if (!this.reg.numeroDocumento.trim()) {
      this.error = 'El número de documento es obligatorio.';
      return;
    }
    if (!this.reg.correoElectronico.trim() || !this.reg.correoElectronico.includes('@')) {
      this.error = 'Ingrese un correo electrónico válido.';
      return;
    }
    if (!this.reg.contrasena || this.reg.contrasena.length < 8) {
      this.error = 'La contraseña debe tener al menos 8 caracteres.';
      return;
    }
    const s = this.passwordStrength;
    if (!s.upperCase || !s.number || !s.special) {
      this.error = 'La contraseña debe tener al menos una mayúscula, un número y un carácter especial.';
      return;
    }
    if (this.reg.contrasena !== this.regConfirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    this.loading = true;
    this.auth.register(this.reg).subscribe({
      next: () => {
        this.loading = false;
        this.correoPendienteVerificar = this.reg.correoElectronico.trim();
        this.codigoVerificacion = '';
        this.showVerificacionCodigo = true;
        this.successMsg = 'Cuenta creada. Revisa tu correo e ingresa el código de 6 dígitos que te enviamos.';
        this.reg = { nombre: '', tipoDocumento: 'CC', numeroDocumento: '', correoElectronico: '', programaFormacion: '', telefono: '', numeroFicha: '', contrasena: '' };
        this.regConfirmPassword = '';
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message ?? 'Error al registrarse. Intente de nuevo.';
      },
    });
  }

  private getPasswordStrength(password: string): { upperCase: boolean; number: boolean; special: boolean; length: boolean } {
    return {
      length: password.length >= 8,
      upperCase: /[A-Z]/.test(password),
      number: /\d/.test(password),
      special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password),
    };
  }

  private resetRecoveryState() {
    this.recoveryStep = null;
    this.recoveryCorreo = '';
    this.recoveryCodigo = '';
    this.recoveryNuevaContrasena = '';
    this.recoveryConfirmPassword = '';
    this.showRecoveryPassword = false;
  }
}
