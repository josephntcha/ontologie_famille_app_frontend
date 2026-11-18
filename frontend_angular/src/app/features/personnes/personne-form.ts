import { Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiError } from '../../core/api-error';
import { Personne, PersonneSaisie, Sexe } from '../../core/models';
import { ErreursChamp } from '../../shared/erreurs-champ';

/** Formulaire de saisie d'une personne (création ou modification). */
@Component({
  selector: 'app-personne-form',
  imports: [ReactiveFormsModule, ErreursChamp],
  template: `
    <form [formGroup]="form" (ngSubmit)="soumettre()" novalidate>
      <div class="champs-ligne">
        <div class="champ">
          <label [for]="prefixe() + '-prenom'">Prénom</label>
          <input [id]="prefixe() + '-prenom'" formControlName="prenom" autocomplete="off" [class.invalide]="!!erreur()?.errors['prenom']" />
          <app-erreurs-champ [erreur]="erreur()" champ="prenom" />
        </div>
        <div class="champ">
          <label [for]="prefixe() + '-nom'">Nom</label>
          <input [id]="prefixe() + '-nom'" formControlName="nom" autocomplete="off" [class.invalide]="!!erreur()?.errors['nom']" />
          <app-erreurs-champ [erreur]="erreur()" champ="nom" />
        </div>
      </div>
      <fieldset class="champ sans-cadre">
        <legend>Sexe</legend>
        <div class="choix-sexe">
          <label><input type="radio" formControlName="sexe" value="F" /> Féminin</label>
          <label><input type="radio" formControlName="sexe" value="M" /> Masculin</label>
        </div>
        <app-erreurs-champ [erreur]="erreur()" champ="sexe" />
      </fieldset>
      <div class="champ">
        <label [for]="prefixe() + '-naissance'">Date de naissance</label>
        <input [id]="prefixe() + '-naissance'" type="date" formControlName="date_naissance" [max]="aujourdhui" [class.invalide]="!!erreur()?.errors['date_naissance']" />
        <app-erreurs-champ [erreur]="erreur()" champ="date_naissance" />
      </div>
      <div class="actions">
        <button class="btn" type="submit" [disabled]="form.invalid || enCours()">{{ libelleBouton() }}</button>
        @if (annulable()) {
          <button class="btn secondaire" type="button" (click)="annuler.emit()">Annuler</button>
        }
      </div>
    </form>
  `,
  styles: `
    .sans-cadre { border: 0; padding: 0; margin: 0 0 1rem; min-width: 0; }
    legend { font-weight: 600; font-size: 0.875rem; padding: 0; margin-bottom: 0.35rem; }
  `,
})
export class PersonneForm {
  readonly prefixe = input('pf');
  readonly initial = input<Personne | null>(null);
  readonly erreur = input<ApiError | null>(null);
  readonly enCours = input(false);
  readonly libelleBouton = input('Enregistrer la personne');
  readonly annulable = input(false);

  readonly valider = output<PersonneSaisie>();
  readonly annuler = output<void>();

  protected readonly aujourdhui = new Date().toISOString().slice(0, 10);

  protected readonly form = new FormGroup({
    prenom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    sexe: new FormControl<Sexe | ''>('', { nonNullable: true, validators: [Validators.required] }),
    date_naissance: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    effect(() => {
      const p = this.initial();
      if (p) this.form.setValue({ prenom: p.prenom, nom: p.nom, sexe: p.sexe, date_naissance: p.date_naissance });
    });
  }

  protected soumettre(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.valider.emit({ ...v, sexe: v.sexe as Sexe, nom: v.nom.trim(), prenom: v.prenom.trim() });
  }

  /** Vide le formulaire après une création réussie. */
  reinitialiser(): void {
    this.form.reset();
  }
}
