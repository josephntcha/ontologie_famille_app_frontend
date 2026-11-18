import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { degreEnLettres, TYPES_LIEN, VERDICT_REGLE, VERDICTS } from '../../core/libelles';
import { Completude, Verification } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ErreursChamp } from '../../shared/erreurs-champ';
import { PersonneSelect } from '../../shared/personne-select';
import { EchelleDegres } from './echelle-degres';

@Component({
  selector: 'app-verification-page',
  imports: [ReactiveFormsModule, RouterLink, PersonneSelect, ErreursChamp, EchelleDegres],
  templateUrl: './verification-page.html',
  styleUrl: './verification-page.scss',
})
export class VerificationPage {
  private readonly api = inject(ParenteApi);
  private readonly router = inject(Router);
  protected readonly store = inject(PersonnesStore);

  /** Paramètres d'URL ?p1=..&p2=.. : une vérification se partage par simple lien. */
  readonly p1 = input<string | undefined>();
  readonly p2 = input<string | undefined>();

  protected readonly c1 = new FormControl<number | null>(null);
  protected readonly c2 = new FormControl<number | null>(null);

  protected readonly resultat = signal<Verification | null>(null);
  protected readonly chemin = signal<number[]>([]);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);

  protected readonly verdicts = VERDICTS;
  protected readonly verdictRegle = VERDICT_REGLE;
  protected readonly typesLien = TYPES_LIEN;
  protected readonly degreEnLettres = degreEnLettres;

  /** Le résultat de HermiT (lienDeSangAvec) concorde-t-il avec l'algorithme de parcours ? */
  protected readonly concordance = computed(() => {
    const r = this.resultat();
    return r ? r.lien_de_sang.existe === r.lien_de_sang.infere_par_raisonneur : true;
  });

  constructor() {
    this.store.charger();
    effect(() => {
      const a = Number(this.p1());
      const b = Number(this.p2());
      untracked(() => {
        if (a && b) {
          this.c1.setValue(a);
          this.c2.setValue(b);
          this.lancer(a, b);
        } else {
          this.resultat.set(null);
        }
      });
    });
  }

  protected soumettre(): void {
    if (this.c1.value == null || this.c2.value == null) return;
    const memes = String(this.c1.value) === this.p1() && String(this.c2.value) === this.p2();
    if (memes) {
      this.lancer(this.c1.value, this.c2.value); // relancer après une modification des données
    } else {
      this.router.navigate([], { queryParams: { p1: this.c1.value, p2: this.c2.value } });
    }
  }

  protected inverser(): void {
    const a = this.c1.value;
    this.c1.setValue(this.c2.value);
    this.c2.setValue(a);
    this.soumettre();
  }

  private lancer(a: number, b: number): void {
    this.enCours.set(true);
    this.erreur.set(null);
    forkJoin({
      verif: this.api.verifier(a, b),
      // L'arbre fournit le chemin nominatif par l'ancêtre commun ; il est facultatif ici.
      arbre: this.api.arbre(a, b).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ verif, arbre }) => {
        this.resultat.set(verif);
        this.chemin.set(arbre?.chemin ?? []);
        this.enCours.set(false);
      },
      error: (e) => {
        this.resultat.set(null);
        this.erreur.set(toApiError(e));
        this.enCours.set(false);
      },
    });
  }

  protected points(connus: number, attendus: number): boolean[] {
    return Array.from({ length: attendus }, (_, i) => i < connus);
  }

  protected resumeCompletude(c: Completude): string {
    if (c.generations_completes === 2) return 'Complet sur 2 générations';
    if (c.generations_completes === 1) return 'Complet sur 1 génération';
    return 'Parents incomplets';
  }
}
