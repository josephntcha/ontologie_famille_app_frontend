import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { ToastService } from './toast.service';

/**
 * Signale une seule fois (toutes les 10 s au plus) que le backend est injoignable.
 * Les erreurs 422 et 404 restent gérées par chaque écran, au plus près du formulaire.
 */
let dernierAvertissement = 0;

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toasts = inject(ToastService);
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && (err.status === 0 || err.status === 502 || err.status === 504)) {
        const now = Date.now();
        if (now - dernierAvertissement > 10_000) {
          dernierAvertissement = now;
          toasts.erreur(
            "L'API ne répond pas. Démarrez le backend (./mvnw spring-boot:run) puis rechargez la page.",
          );
        }
      }
      return throwError(() => err);
    }),
  );
};
