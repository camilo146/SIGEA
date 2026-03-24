import { Component, signal, inject, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import type { Notificacion } from '../../core/models/notificacion.model';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private notificacionService = inject(NotificacionService);

  sidebarOpen = signal(true);
  notificationOpen = signal(false);
  userMenuOpen = signal(false);
  darkMode = signal(false);

  notificaciones = signal<Notificacion[]>([]);
  contadorNoLeidas = signal(0);

  user = this.auth.user;
  isAdmin = this.auth.isAdmin;
  isInstructor = this.auth.isInstructor;
  isAdminOrInstructor = this.auth.isAdminOrInstructor;

  ngOnInit() {
    this.loadContador();
    this.loadNotificaciones();
  }

  loadContador() {
    this.notificacionService.contadorNoLeidas().subscribe({
      next: (r) => this.contadorNoLeidas.set(r.noLeidas),
      error: () => {},
    });
  }

  loadNotificaciones() {
    this.notificacionService.listarMisNotificaciones().subscribe({
      next: (list) => this.notificaciones.set(list),
      error: () => {},
    });
  }

  toggleNotifications() {
    const open = !this.notificationOpen();
    this.notificationOpen.set(open);
    this.userMenuOpen.set(false);
    if (open) this.loadNotificaciones();
  }

  marcarLeida(n: Notificacion) {
    this.notificacionService.marcarLeida(n.id).subscribe({
      next: () => {
        this.loadNotificaciones();
        this.loadContador();
      },
    });
  }

  toggleSidebar() {
    this.sidebarOpen.update((v) => !v);
  }

  toggleUserMenu() {
    this.userMenuOpen.update((v) => !v);
    this.notificationOpen.set(false);
  }

  toggleTheme() {
    this.darkMode.update((v) => !v);
    document.documentElement.classList.toggle('dark', this.darkMode());
  }

  logout() {
    this.auth.logout();
  }

  getBreadcrumb(): string {
    const url = this.router.url.split('?')[0];
    const segments = url.split('/').filter(Boolean);
    if (segments.length <= 1) return 'Dashboard';
    const map: Record<string, string> = {
      dashboard: 'Dashboard',
      inventario: 'Inventario',
      prestamos: 'Préstamos',
      reservas: 'Reservas',
      'mi-ambiente': 'Mi ubicación',
      ambientes: 'Ubicaciones',
      usuarios: 'Usuarios',
      reportes: 'Reportes',
      transferencias: 'Transferencias',
      mantenimientos: 'Mantenimientos',
    };
    return map[segments[1]] ?? segments[1];
  }

  getRolLabel(rol?: string): string {
    const map: Record<string, string> = {
      ADMINISTRADOR: 'Administrador',
      INSTRUCTOR: 'Instructor',
      APRENDIZ: 'Aprendiz',
      FUNCIONARIO: 'Funcionario',
    };
    return rol ? (map[rol] ?? rol) : '';
  }

  closeDropdowns() {
    this.notificationOpen.set(false);
    this.userMenuOpen.set(false);
  }
}
