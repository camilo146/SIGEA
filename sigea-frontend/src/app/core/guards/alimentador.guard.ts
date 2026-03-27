import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Permite acceso exclusivamente al rol ALIMENTADOR_EQUIPOS. */
export const alimentadorGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn() && auth.isAlimentadorEquipos()) return true;
  if (auth.isLoggedIn()) router.navigate(['/dashboard']);
  else router.navigate(['/login']);
  return false;
};
