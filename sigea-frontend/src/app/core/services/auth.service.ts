import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoginRequest, LoginResponse, UserSession, RegisterRequest } from '../models/auth.model';

const TOKEN_KEY = 'sigea_token';
const USER_KEY = 'sigea_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private currentUser = signal<UserSession | null>(this.loadStoredUser());
  sessionExpired = signal(false);
  isLoggedIn = computed(() => !!this.currentUser());
  user = computed(() => this.currentUser());
  isAdmin = computed(() => this.currentUser()?.rol === 'ADMINISTRADOR');
  isInstructor = computed(() => this.currentUser()?.rol === 'INSTRUCTOR');
  isAlimentadorEquipos = computed(() => this.currentUser()?.rol === 'ALIMENTADOR_EQUIPOS');
  isSuperAdmin = computed(() => !!this.currentUser()?.esSuperAdmin);
  /** true si es ADMIN o INSTRUCTOR (acceso a ambientes, reportes, etc., pero no a usuarios) */
  isAdminOrInstructor = computed(() => {
    const r = this.currentUser()?.rol;
    return r === 'ADMINISTRADOR' || r === 'INSTRUCTOR';
  });
  /** true para cualquier rol con acceso operativo (puede crear equipos/ambientes) */
  isOperativo = computed(() => {
    const r = this.currentUser()?.rol;
    return r === 'ADMINISTRADOR' || r === 'INSTRUCTOR' || r === 'ALIMENTADOR_EQUIPOS';
  });

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((res) => {
        const session: UserSession = {
          nombreCompleto: res.nombreCompleto,
          correoElectronico: '',
          numeroDocumento: credentials.numeroDocumento,
          rol: res.rol,
          token: res.token,
          esSuperAdmin: !!res.esSuperAdmin,
        };
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(USER_KEY, JSON.stringify(session));
        this.currentUser.set(session);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.sessionExpired.set(false);
    this.router.navigate(['/login']);
  }

  /** Marca la sesión como expirada (401) sin navegar — deja que el modal lo gestione. */
  markSessionExpired(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.sessionExpired.set(true);
  }

  clearSessionExpired(): void {
    this.sessionExpired.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  register(data: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/registro`, data);
  }

  /** Verifica el correo con el código de 6 dígitos enviado por email. */
  verificarCodigo(correo: string, codigo: string): Observable<string> {
    return this.http.post<{ mensaje: string }>(`${this.apiUrl}/verificar-email`, { correo, codigo }).pipe(
      map((res) => res.mensaje ?? 'Correo verificado correctamente.')
    );
  }

  private loadStoredUser(): UserSession | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserSession;
    } catch {
      return null;
    }
  }
}
