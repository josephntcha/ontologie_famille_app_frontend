import { computed, inject, Injectable, signal } from '@angular/core';

import { ParenteApi } from './api.service';
import { toApiError } from './api-error';
import { Personne } from './models';

/**
 * Liste des personnes partagée par tous les écrans (sélecteurs, tableaux de liens, arbre).
 * Rechargée après chaque création, modification ou suppression.
 */
@Injectable({ providedIn: 'root' })
export class PersonnesStore {
  private readonly api = inject(ParenteApi);

  readonly personnes = signal<Personne[]>([]);
  readonly chargement = signal(false);
  readonly erreur = signal<string | null>(null);

  /** Triées par nom puis prénom, pour les listes déroulantes. */
  readonly triees = computed(() =>
    [...this.personnes()].sort(
      (a, b) => a.nom.localeCompare(b.nom, 'fr') || a.prenom.localeCompare(b.prenom, 'fr'),
    ),
  );

  private readonly parId = computed(() => new Map(this.personnes().map((p) => [p.id, p])));

  charger(): void {
    this.chargement.set(true);
    this.api.personnes().subscribe({
      next: (liste) => {
        this.personnes.set(liste);
        this.erreur.set(null);
        this.chargement.set(false);
      },
      error: (e) => {
        this.erreur.set(toApiError(e).message);
        this.chargement.set(false);
      },
    });
  }

  get(id: number | null | undefined): Personne | undefined {
    return id == null ? undefined : this.parId().get(id);
  }

  /** « Prénom Nom », ou « #id » si la personne n'est pas (encore) chargée. */
  nom(id: number | null | undefined): string {
    const p = this.get(id);
    return p ? `${p.prenom} ${p.nom}` : id == null ? '—' : `#${id}`;
  }
}
