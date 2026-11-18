import { LowerCasePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { degreEnLettres } from '../../core/libelles';
import { LienDeSang, Personne } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';

interface Marche {
  id: number;
  nom: string;
  sexe: 'M' | 'F' | null;
  generation: number;
}

/**
 * « Échelle des degrés » : dessine la remontée de chaque personne jusqu'à l'ancêtre commun le plus
 * proche, génération par génération, et le calcul civil du degré g(x) + g(y).
 * Le chemin vient de GET /api/arbre (chemin = [personne1, …, ancêtre, …, personne2]).
 */
@Component({
  selector: 'app-echelle-degres',
  imports: [RouterLink, LowerCasePipe],
  templateUrl: './echelle-degres.html',
  styleUrl: './echelle-degres.scss',
})
export class EchelleDegres {
  private readonly store = inject(PersonnesStore);

  readonly lien = input.required<LienDeSang>();
  readonly personne1 = input.required<Personne>();
  readonly personne2 = input.required<Personne>();
  readonly chemin = input<number[]>([]);

  protected readonly degreEnLettres = degreEnLettres;

  private marche(id: number, generation: number): Marche {
    const p = this.store.get(id);
    return { id, nom: p ? `${p.prenom} ${p.nom}` : `n° ${id}`, sexe: p?.sexe ?? null, generation };
  }

  protected readonly g1 = computed(() => this.lien().generation_personne1 ?? 0);
  protected readonly g2 = computed(() => this.lien().generation_personne2 ?? 0);
  protected readonly ligneDirecte = computed(() => this.lien().type === 'ligne_directe');

  /** Ancêtre(s) commun(s) au sommet : les deux grands-parents pour des cousins germains. */
  protected readonly sommet = computed(() => {
    const l = this.lien();
    const ids = l.ancetres_communs.map((a) => a.id);
    return {
      ids,
      noms: l.ancetres_communs.map((a) => `${a.prenom} ${a.nom}`),
      sexes: ids.map((id) => this.store.get(id)?.sexe ?? null),
    };
  });

  /** Branche de personne1 : du premier descendant de l'ancêtre jusqu'à personne1. */
  protected readonly gauche = computed<Marche[]>(() => this.branche(this.g1(), (i) => this.chemin()[this.g1() - i], this.personne1().id));
  /** Branche de personne2 : du premier descendant de l'ancêtre jusqu'à personne2. */
  protected readonly droite = computed<Marche[]>(() =>
    this.branche(this.g2(), (i) => this.chemin()[this.g1() + i], this.personne2().id),
  );

  private branche(g: number, idAuRang: (i: number) => number | undefined, cible: number): Marche[] {
    const res: Marche[] = [];
    for (let i = 1; i <= g; i++) {
      const id = idAuRang(i) ?? (i === g ? cible : undefined);
      res.push(id != null ? this.marche(id, i) : { id: -i, nom: 'Génération intermédiaire', sexe: null, generation: i });
    }
    return res;
  }

  protected readonly nomP1 = computed(() => this.personne1().prenom);
  protected readonly nomP2 = computed(() => this.personne2().prenom);
}
