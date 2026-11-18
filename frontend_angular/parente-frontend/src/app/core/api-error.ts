import { HttpErrorResponse } from '@angular/common/http';

/**
 * Erreur normalisée de l'API.
 * - 422 : { message, errors: { champ: [messages] } }  (validation et règles métier)
 * - 404 : { message }                                 (ressource inexistante)
 * - 409 : { message }                                 (données incohérentes pour HermiT)
 * - 0   : backend injoignable (non démarré, mauvais port, proxy absent)
 */
export interface ApiError {
  status: number;
  message: string;
  errors: Record<string, string[]>;
}

export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    const corpsApi = err.error && typeof err.error === 'object' && typeof err.error.message === 'string';
    // 0 : appel direct refusé ; 502/503/504 sans corps JSON : le proxy de `ng serve` ne joint pas le backend.
    if (err.status === 0 || ([502, 503, 504].includes(err.status) && !corpsApi)) {
      return {
        status: 0,
        message:
          "Impossible de joindre l'API sur http://localhost:8080. Vérifiez que le backend Spring Boot est démarré.",
        errors: {},
      };
    }
    const body = err.error;
    if (body && typeof body === 'object') {
      return {
        status: err.status,
        message: typeof body.message === 'string' ? body.message : err.message,
        errors: body.errors && typeof body.errors === 'object' ? body.errors : {},
      };
    }
    return { status: err.status, message: `Erreur ${err.status} renvoyée par l'API.`, errors: {} };
  }
  return { status: -1, message: String(err), errors: {} };
}
