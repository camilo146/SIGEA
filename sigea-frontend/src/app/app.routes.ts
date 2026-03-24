import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { adminOrInstructorGuard } from './core/guards/admin-or-instructor.guard';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'verificar-email',
    loadComponent: () => import('./pages/verificar-email/verificar-email.component').then(m => m.VerificarEmailComponent),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'inventario',
        loadComponent: () => import('./pages/inventario/inventario.component').then(m => m.InventarioComponent),
      },
      {
        path: 'prestamos',
        loadComponent: () => import('./pages/prestamos/prestamos.component').then(m => m.PrestamosComponent),
      },
      {
        path: 'reservas',
        loadComponent: () => import('./pages/reservas/reservas.component').then(m => m.ReservasComponent),
      },
      {
        path: 'mi-ambiente',
        loadComponent: () => import('./pages/mi-ambiente/mi-ambiente.component').then(m => m.MiAmbienteComponent),
        canActivate: [adminOrInstructorGuard],
      },
      {
        path: 'ambientes',
        loadComponent: () => import('./pages/ambientes/ambientes.component').then(m => m.AmbientesComponent),
        canActivate: [adminOrInstructorGuard],
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./pages/usuarios/usuarios.component').then(m => m.UsuariosComponent),
        canActivate: [adminGuard],
      },
      {
        path: 'reportes',
        loadComponent: () => import('./pages/reportes/reportes.component').then(m => m.ReportesComponent),
        canActivate: [adminOrInstructorGuard],
      },
      {
        path: 'transferencias',
        loadComponent: () => import('./pages/transferencias/transferencias.component').then(m => m.TransferenciasComponent),
        canActivate: [adminOrInstructorGuard],
      },
      {
        path: 'mantenimientos',
        loadComponent: () => import('./pages/mantenimientos/mantenimientos.component').then(m => m.MantenimientosComponent),
        canActivate: [adminOrInstructorGuard],
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
