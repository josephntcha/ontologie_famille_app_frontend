import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { BeauParente } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ToastService } from '../../core/toast.service';
import { ErreursChamp, erreursHorsChamps } from '../../shared/erreurs-champ';
import { PersonneSelect } from '../../shared/personne-select';

@Component({
  selector: 'app-beaux-liens',
  imports: [ReactiveFormsModule, RouterLink, PersonneSelect, ErreursChamp],
  template: `
    <div class="grille-form-liste">
      <section class="panneau" aria-labelledby="bp-titre">
        <h2 id="bp-titre">Déclarer un beau-parent</h2>
        @for (m of erreursGenerales(); track $index) {
          <div class="alerte erreur" role="alert">{{ m }}</div>
        }
        <form [formGroup]="form" (ngSubmit)="creer()">
          <div class="champ">
            <label for="bp-parent">Beau-parent</label>
            <app-personne-select inputId="bp-parent" formControlName="beau_parent_id" [exclure]="form.value.enfant_id ?? null" [invalide]="!!erreur()?.errors['beau_parent_id']" />
            <app-erreurs-champ [erreur]="erreur()" champ="beau_parent_id" />
          </div>
          <div class="champ">
            <label for="bp-enfant">Bel-enfant</label>
            <app-personne-select inputId="bp-enfant" formControlName="enfant_id" [exclure]="form.value.beau_parent_id ?? null" [invalide]="!!erreur()?.errors['enfant_id']" />
            <app-erreurs-champ [erreur]="erreur()" champ="enfant_id" />
          </div>
          <button class="btn plein" type="submit" [disabled]="form.invalid || enCours()">Déclarer « est beau-parent de »</button>
        </form>
        <p class="aide-bas muted">
          Famille recomposée : le conjoint d'un parent, qui n'est pas lui-même parent de l'enfant. Ce lien crée une
          alliance en ligne directe et permet à HermiT de classer les demi-frères et demi-sœurs.
        </p>
      </section>

      <section class="panneau" aria-labelledby="bp-liste">
        <h2 id="bp-liste">Beaux-parents déclarés ({{ liens().length }})</h2>
        @if (liens().length) {
          <div class="table-wrap">
            <table class="registre">
              <thead>
                <tr><th scope="col">Beau-parent</th><th scope="col">Bel-enfant</th><th scope="col"><span class="sr-only">Actions</span></th></tr>
              </thead>
              <tbody>
                @for (l of liens(); track l.id) {
                  <tr>
                    <td><a [routerLink]="['/personnes', l.beau_parent_id]">{{ store.nom(l.beau_parent_id) }}</a></td>
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
          <div class="vide"><p>Aucun lien beau-parent. Il n'est utile que pour les familles recomposées.</p></div>
        }
      </section>
    </div>
  `,
  styles: `
    .aide-bas { font-size: 0.82rem; margin-top: 1rem; }
    .fin { justify-content: flex-end; }
  `,
})
export class BeauxLiens implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly toasts = inject(ToastService);
  protected readonly store = inject(PersonnesStore);

  protected readonly liens = signal<BeauParente[]>([]);
  protected readonly chargement = signal(true);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly aSupprimer = signal<number | null>(null);

  protected readonly form = new FormGroup({
    beau_parent_id: new FormControl<number | null>(null, Validators.required),
    enfant_id: new FormControl<number | null>(null, Validators.required),
  });

  protected readonly erreursGenerales = computed(() =>
    erreursHorsChamps(this.erreur(), ['beau_parent_id', 'enfant_id']),
  );

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.api.beauxLiens().subscribe({
      next: (l) => {
        this.liens.set(l);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected creer(): void {
    const { beau_parent_id, enfant_id } = this.form.getRawValue();
    if (beau_parent_id == null || enfant_id == null) return;
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.creerBeauLien(beau_parent_id, enfant_id).subscribe({
      next: () => {
        this.enCours.set(false);
        this.toasts.succes(`${this.store.nom(beau_parent_id)} est déclaré(e) beau-parent de ${this.store.nom(enfant_id)}.`);
        this.form.reset();
        this.charger();
      },
      error: (e) => {
        this.enCours.set(false);
        this.erreur.set(toApiError(e));
      },
    });
  }

  protected supprimer(l: BeauParente): void {
    this.api.supprimerBeauLien(l.id).subscribe({
      next: () => {
        this.aSupprimer.set(null);
        this.toasts.succes('Lien beau-parent supprimé.');
        this.charger();
      },
      error: (e) => this.toasts.erreur(toApiError(e).message),
    });
  }
}
