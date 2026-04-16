import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();
  const isPublicAuthEndpoint = /\/auth\/(login|registro|recuperar-contrasena|restablecer-contrasena|verificar-email)$/.test(req.url);

  if (token && !isPublicAuthEndpoint) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && auth.isLoggedIn()) {
        // Activa el modal de sesión expirada — no redirige directamente.
        auth.markSessionExpired();
      }
      return throwError(() => err);
    })
  );
};
