import { LowerCasePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { CAUSES, dateLongue } from '../../core/libelles';
import { Mariage, PersonneDetail, PersonneSaisie } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ToastService } from '../../core/toast.service';
import { ClassesOwl } from '../../shared/classes-owl';
import { PersonneSelect } from '../../shared/personne-select';
import { PersonneForm } from './personne-form';

@Component({
  selector: 'app-personne-detail-page',
  imports: [RouterLink, ReactiveFormsModule, LowerCasePipe, ClassesOwl, PersonneForm, PersonneSelect],
  templateUrl: './personne-detail-page.html',
  styleUrl: './personne-detail-page.scss',
})
export class PersonneDetailPage {
  private readonly api = inject(ParenteApi);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  protected readonly store = inject(PersonnesStore);

  /** Paramètre de route :id (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly detail = signal<PersonneDetail | null>(null);
  protected readonly erreurChargement = signal<ApiError | null>(null);
  protected readonly edition = signal(false);
  protected readonly erreurEdition = signal<ApiError | null>(null);
  protected readonly enregistrement = signal(false);
  protected readonly confirmerSuppression = signal(false);
  protected readonly autre = new FormControl<number | null>(null);

  protected readonly dateLongue = dateLongue;
  protected readonly causes = CAUSES;

  protected readonly freresSoeurs = computed(() => {
    const d = this.detail();
    return d ? [...d.freres, ...d.soeurs].sort((a, b) => a.date_naissance.localeCompare(b.date_naissance)) : [];
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.charger(id));
    });
  }

  private charger(id: number): void {
    this.detail.set(null);
    this.erreurChargement.set(null);
    this.edition.set(false);
    this.confirmerSuppression.set(false);
    this.api.personne(id).subscribe({
      next: (d) => this.detail.set(d),
      error: (e) => this.erreurChargement.set(toApiError(e)),
    });
  }

  protected conjointDe(m: Mariage): string {
    const d = this.detail();
    const autreId = m.conjoint1_id === d?.id ? m.conjoint2_id : m.conjoint1_id;
    return this.store.nom(autreId);
  }
  protected conjointIdDe(m: Mariage): number {
    return m.conjoint1_id === this.detail()?.id ? m.conjoint2_id : m.conjoint1_id;
  }

  protected enregistrer(saisie: PersonneSaisie): void {
    const d = this.detail();
    if (!d) return;
    this.enregistrement.set(true);
    this.erreurEdition.set(null);
    this.api.modifierPersonne(d.id, saisie).subscribe({
      next: (p) => {
        this.enregistrement.set(false);
        this.toasts.succes(`Fiche de ${p.prenom} ${p.nom} mise à jour.`);
        this.store.charger();
        this.charger(d.id);
      },
      error: (e) => {
        this.enregistrement.set(false);
        this.erreurEdition.set(toApiError(e));
      },
    });
  }

  protected supprimer(): void {
    const d = this.detail();
    if (!d) return;
    this.api.supprimerPersonne(d.id).subscribe({
      next: () => {
        this.toasts.succes(`${d.prenom} ${d.nom} a été supprimé(e), ainsi que ses liens et ses mariages.`);
        this.store.charger();
        this.router.navigate(['/personnes']);
      },
      error: (e) => this.toasts.erreur(toApiError(e).message),
    });
  }

  protected verifierAvec(): void {
    const d = this.detail();
    if (!d || this.autre.value == null) return;
    this.router.navigate(['/verification'], { queryParams: { p1: d.id, p2: this.autre.value } });
  }
}
