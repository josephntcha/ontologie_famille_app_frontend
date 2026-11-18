import { Component, computed, input } from '@angular/core';

import { ApiError } from '../core/api-error';

/** Affiche sous un champ les messages 422 renvoyés par l'API pour la clé `champ`. */
@Component({
  selector: 'app-erreurs-champ',
  template: `
    @for (m of messages(); track $index) {
      <span class="erreur-champ" role="alert">{{ m }}</span>
    }
  `,
  styles: `:host { display: contents; }`,
})
export class ErreursChamp {
  readonly erreur = input<ApiError | null>(null);
  readonly champ = input.required<string>();
  protected readonly messages = computed(() => this.erreur()?.errors[this.champ()] ?? []);
}

/** Messages 422 qui ne correspondent à aucun champ du formulaire (ex. « corps »). */
export function erreursHorsChamps(err: ApiError | null, champs: string[]): string[] {
  if (!err) return [];
  const hors = Object.entries(err.errors)
    .filter(([k]) => !champs.includes(k))
    .flatMap(([, v]) => v);
  // Erreur sans détail par champ (404, 409, réseau) : on affiche le message principal.
  return Object.keys(err.errors).length === 0 ? [err.message] : hors;
}
