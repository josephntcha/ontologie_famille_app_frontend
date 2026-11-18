import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { Filiation } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ToastService } from '../../core/toast.service';
import { ErreursChamp, erreursHorsChamps } from '../../shared/erreurs-champ';
import { PersonneSelect } from '../../shared/personne-select';

@Component({
  selector: 'app-filiations',
  imports: [ReactiveFormsModule, RouterLink, PersonneSelect, ErreursChamp],
  template: `
    <div class="grille-form-liste">
      <section class="panneau" aria-labelledby="fil-titre">
        <h2 id="fil-titre">Déclarer une filiation</h2>
        @for (m of erreursGenerales(); track $index) {
          <div class="alerte erreur" role="alert">{{ m }}</div>
        }
        <form [formGroup]="form" (ngSubmit)="creer()">
          <div class="champ">
            <label for="fil-parent">Parent</label>
            <app-personne-select inputId="fil-parent" formControlName="parent_id" [exclure]="form.value.enfant_id ?? null" [invalide]="!!erreur()?.errors['parent_id']" />
            <app-erreurs-champ [erreur]="erreur()" champ="parent_id" />
          </div>
          <div class="champ">
            <label for="fil-enfant">Enfant</label>
            <app-personne-select inputId="fil-enfant" formControlName="enfant_id" [exclure]="form.value.parent_id ?? null" [invalide]="!!erreur()?.errors['enfant_id']" />
            <app-erreurs-champ [erreur]="erreur()" champ="enfant_id" />
          </div>
          <button class="btn plein" type="submit" [disabled]="form.invalid || enCours()">Déclarer « est parent de »</button>
        </form>
        <p class="aide-bas muted">
          Le registre refuse un troisième parent, un cycle (un descendant déclaré parent de son ancêtre), un parent né
          après l'enfant, et un lien déjà déclaré comme beau-parent.
        </p>
      </section>

      <section class="panneau" aria-labelledby="fil-liste">
        <h2 id="fil-liste">Filiations déclarées ({{ liens().length }})</h2>
        @if (lignes().length) {
          <div class="table-wrap">
            <table class="registre">
              <thead>
                <tr><th scope="col">Parent</th><th scope="col">Enfant</th><th scope="col"><span class="sr-only">Actions</span></th></tr>
              </thead>
              <tbody>
                @for (l of lignes(); track l.id) {
                  <tr>
                    <td><a [routerLink]="['/personnes', l.parent_id]">{{ store.nom(l.parent_id) }}</a></td>
                    <td><a [routerLink]="['/personnes', l.enfant_id]">{{ store.nom(l.enfant_id) }}</a></td>
                    <td class="num">
                      @if (aSupprimer() === l.id) {
                        <span class="actions fin">
                          <button class="btn danger petit" type="button" (click)="supprimer(l)">Confirmer</button>
                          <button class="lien-discret" type="button" (click)="aSupprimer.set(null)">Annuler</button>
                        </span>
                      } @else {
                        <button class="lien-discret danger" type="button" (click)="aSupprimer.set(l.id)">Supprimer</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else if (chargement()) {
          <p class="chargement">Chargement…</p>
        } @else {
          <div class="vide"><p>Aucune filiation. Déclarez d'abord les parents des deux personnes à comparer, puis leurs grands-parents.</p></div>
        }
      </section>
    </div>
  `,
  styles: `
    .aide-bas { font-size: 0.82rem; margin-top: 1rem; }
    .fin { justify-content: flex-end; }
  `,
})
export class Filiations implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly toasts = inject(ToastService);
  protected readonly store = inject(PersonnesStore);

  protected readonly liens = signal<Filiation[]>([]);
  protected readonly chargement = signal(true);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly aSupprimer = signal<number | null>(null);

  protected readonly form = new FormGroup({
    parent_id: new FormControl<number | null>(null, Validators.required),
    enfant_id: new FormControl<number | null>(null, Validators.required),
  });

  /** Triées par enfant : les deux parents d'une même personne se suivent. */
  protected readonly lignes = computed(() =>
    [...this.liens()].sort((a, b) => this.store.nom(a.enfant_id).localeCompare(this.store.nom(b.enfant_id), 'fr')),
  );
  protected readonly erreursGenerales = computed(() => erreursHorsChamps(this.erreur(), ['parent_id', 'enfant_id']));

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.api.filiations().subscribe({
      next: (l) => {
        this.liens.set(l);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected creer(): void {
    const { parent_id, enfant_id } = this.form.getRawValue();
    if (parent_id == null || enfant_id == null) return;
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.creerFiliation(parent_id, enfant_id).subscribe({
      next: () => {
        this.enCours.set(false);
        this.toasts.succes(`${this.store.nom(parent_id)} est déclaré(e) parent de ${this.store.nom(enfant_id)}.`);
        this.form.reset();
        this.charger();
      },
      error: (e) => {
        this.enCours.set(false);
        this.erreur.set(toApiError(e));
      },
    });
  }

  protected supprimer(l: Filiation): void {
    this.api.supprimerFiliation(l.id).subscribe({
      next: () => {
        this.aSupprimer.set(null);
        this.toasts.succes('Filiation supprimée.');
        this.charger();
      },
      error: (e) => this.toasts.erreur(toApiError(e).message),
    });
  }
}
