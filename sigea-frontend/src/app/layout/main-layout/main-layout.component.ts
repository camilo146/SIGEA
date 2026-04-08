import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import type { Notificacion } from '../../core/models/notificacion.model';
import { UiFeedbackService } from '../../core/services/ui-feedback.service';

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
  private ui = inject(UiFeedbackService);

  sidebarOpen = signal(true);
  notificationOpen = signal(false);
  userMenuOpen = signal(false);
  darkMode = signal(false);

  notificaciones = signal<Notificacion[]>([]);
  contadorNoLeidas = signal(0);
  notificationsLoading = signal(false);
  markAllLoading = signal(false);

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
    this.notificationsLoading.set(true);
    this.notificacionService.listarMisNotificaciones().subscribe({
      next: (list) => {
        this.notificaciones.set(list);
        this.notificationsLoading.set(false);
      },
      error: () => {
        this.notificationsLoading.set(false);
      },
    });
  }

  toggleNotifications(event?: Event) {
    event?.stopPropagation();
    const open = !this.notificationOpen();
    this.notificationOpen.set(open);
    this.userMenuOpen.set(false);
    if (open) this.loadNotificaciones();
  }

  marcarLeida(n: Notificacion) {
    if (n.leida) return;

    this.notificaciones.update((items) => items.map((item) => item.id === n.id ? { ...item, leida: true } : item));
    this.contadorNoLeidas.update((value) => Math.max(0, value - 1));

    this.notificacionService.marcarLeida(n.id).subscribe({
      next: () => {
        this.ui.success('Notificación marcada como leída.', 'Notificaciones');
      },
      error: () => {
        this.loadNotificaciones();
        this.loadContador();
        this.ui.error('No fue posible actualizar la notificación.', 'Notificaciones');
      },
    });
  }

  toggleSidebar() {
    this.sidebarOpen.update((v) => !v);
  }

  toggleUserMenu(event?: Event) {
    event?.stopPropagation();
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
      'prestamos-ambientes': 'Préstamos de Ambientes',
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

  marcarTodasComoLeidas() {
    const ids = this.notificaciones().filter((item) => !item.leida).map((item) => item.id);
    if (!ids.length || this.markAllLoading()) {
      if (!ids.length) this.ui.info('No tienes notificaciones pendientes.', 'Notificaciones');
      return;
    }

    this.markAllLoading.set(true);
    this.notificaciones.update((items) => items.map((item) => ({ ...item, leida: true })));
    this.contadorNoLeidas.set(0);

    this.notificacionService.marcarTodasLeidas(ids).subscribe({
      next: () => {
        this.markAllLoading.set(false);
        this.ui.success('Todas las notificaciones fueron marcadas como leídas.', 'Notificaciones');
      },
      error: () => {
        this.markAllLoading.set(false);
        this.loadNotificaciones();
        this.loadContador();
        this.ui.error('No fue posible marcar todas las notificaciones.', 'Notificaciones');
      },
    });
  }

  verTodasNotificaciones() {
    this.loadNotificaciones();
    this.ui.info('Mostrando las notificaciones recientes disponibles.', 'Notificaciones');
  }

  getNotificationTone(tipo: string): 'success' | 'info' | 'warning' | 'danger' {
    const normalized = tipo?.toUpperCase() ?? 'GENERAL';
    if (normalized === 'RESERVA_CREADA' || normalized === 'EQUIPO_RECOGIDO') return 'success';
    if (normalized === 'MORA' || normalized === 'STOCK_BAJO') return 'warning';
    if (normalized === 'SOLICITUD_PRESTAMO') return 'info';
    return 'danger';
  }

  getNotificationIcon(tipo: string): string {
    const normalized = tipo?.toUpperCase() ?? 'GENERAL';
    const map: Record<string, string> = {
      RESERVA_CREADA: 'fa-calendar-check',
      EQUIPO_RECOGIDO: 'fa-hand-holding',
      STOCK_BAJO: 'fa-box-open',
      SOLICITUD_PRESTAMO: 'fa-file-signature',
      MORA: 'fa-clock',
      RECORDATORIO_VENCIMIENTO: 'fa-bell',
      GENERAL: 'fa-circle-info',
    };
    return map[normalized] ?? 'fa-circle-info';
  }

  relativeTime(value?: string): string {
    if (!value) return 'Ahora';
    const date = new Date(value);
    const diffMs = date.getTime() - Date.now();
    const diffMinutes = Math.round(diffMs / 60000);
    const formatter = new Intl.RelativeTimeFormat('es', { numeric: 'auto' });

    if (Math.abs(diffMinutes) < 60) return formatter.format(diffMinutes, 'minute');

    const diffHours = Math.round(diffMinutes / 60);
    if (Math.abs(diffHours) < 24) return formatter.format(diffHours, 'hour');

    const diffDays = Math.round(diffHours / 24);
    return formatter.format(diffDays, 'day');
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement | null;
    if (!target) return;
    if (!target.closest('.notifications-popover')) this.notificationOpen.set(false);
    if (!target.closest('.user-popover')) this.userMenuOpen.set(false);
  }
}
