import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Permite acceso a ADMINISTRADOR o INSTRUCTOR (ambientes, reportes, transferencias, mantenimientos). */
export const adminOrInstructorGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn() && auth.isAdminOrInstructor()) return true;
  if (auth.isLoggedIn()) router.navigate(['/dashboard']);
  else router.navigate(['/login']);
  return false;
};
