import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

import { toApiError } from './core/api-error';
import { classeLisible, dateLongue, degreEnLettres } from './core/libelles';

describe('libellés', () => {
  it('écrit le degré comme le backend (NommageLien.degreEnLettres)', () => {
    expect(degreEnLettres(1)).toBe('1er degré');
    expect(degreEnLettres(4)).toBe('4e degré');
    expect(degreEnLettres(null)).toBe('');
  });

  it('rend lisibles les noms de classes OWL', () => {
    expect(classeLisible('DemiFrere')).toBe('Demi frère');
    expect(classeLisible('MariageDissousParDeces')).toBe('Mariage dissous par décès');
    expect(classeLisible('Niece')).toBe('Nièce');
  });

  it('formate les dates ISO en français', () => {
    expect(dateLongue('1988-06-01')).toBe('1er juin 1988');
    expect(dateLongue('1990-04-12')).toBe('12 avril 1990');
  });
});

describe('toApiError', () => {
  it('reprend message et erreurs par champ d’une réponse 422', () => {
    const e = toApiError(
      new HttpErrorResponse({
        status: 422,
        error: { message: 'Kid Martin a déjà deux parents.', errors: { enfant_id: ['Kid Martin a déjà deux parents.'] } },
      }),
    );
    expect(e.status).toBe(422);
    expect(e.errors['enfant_id']).toEqual(['Kid Martin a déjà deux parents.']);
  });

  it('signale un backend injoignable (statut 0 ou 502 du proxy)', () => {
    expect(toApiError(new HttpErrorResponse({ status: 0 })).message).toContain('localhost:8080');
    expect(toApiError(new HttpErrorResponse({ status: 502, error: 'Bad gateway' })).message).toContain('backend');
  });

  it('garde le message d’une 404 de l’API', () => {
    const e = toApiError(new HttpErrorResponse({ status: 404, error: { message: 'Personne 99 introuvable.' } }));
    expect(e).toEqual({ status: 404, message: 'Personne 99 introuvable.', errors: {} });
  });
});
