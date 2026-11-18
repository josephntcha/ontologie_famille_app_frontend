import { LowerCasePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { CAUSES, dateLongue } from '../../core/libelles';
import { CauseDissolution, Mariage } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ToastService } from '../../core/toast.service';
import { ClassesOwl } from '../../shared/classes-owl';
import { ErreursChamp, erreursHorsChamps } from '../../shared/erreurs-champ';
import { PersonneSelect } from '../../shared/personne-select';

const CHAMPS = ['conjoint1_id', 'conjoint2_id', 'date_debut', 'date_fin', 'cause_dissolution'];

@Component({
  selector: 'app-mariages',
  imports: [ReactiveFormsModule, RouterLink, LowerCasePipe, PersonneSelect, ErreursChamp, ClassesOwl],
  templateUrl: './mariages.html',
  styleUrl: './mariages.scss',
})
export class Mariages implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly toasts = inject(ToastService);
  protected readonly store = inject(PersonnesStore);

  protected readonly mariages = signal<Mariage[]>([]);
  protected readonly chargement = signal(true);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly aSupprimer = signal<number | null>(null);
  protected readonly aDissoudre = signal<number | null>(null);
  protected readonly erreurDissolution = signal<ApiError | null>(null);

  protected readonly causes = CAUSES;
  protected readonly dateLongue = dateLongue;

  protected readonly form = new FormGroup({
    conjoint1_id: new FormControl<number | null>(null, Validators.required),
    conjoint2_id: new FormControl<number | null>(null, Validators.required),
    date_debut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    dissous: new FormControl(false, { nonNullable: true }),
    date_fin: new FormControl('', { nonNullable: true }),
    cause_dissolution: new FormControl<CauseDissolution | ''>('', { nonNullable: true }),
  });

  protected readonly dissolution = new FormGroup({
    date_fin: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    cause_dissolution: new FormControl<CauseDissolution | ''>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  /** Mariages en cours d'abord, puis par date de début. */
  protected readonly lignes = computed(() =>
    [...this.mariages()].sort(
      (a, b) => Number(b.en_cours) - Number(a.en_cours) || a.date_debut.localeCompare(b.date_debut),
    ),
  );
  protected readonly erreursGenerales = computed(() => erreursHorsChamps(this.erreur(), CHAMPS));

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.api.mariages().subscribe({
      next: (l) => {
        this.mariages.set(l);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  protected creer(): void {
    const v = this.form.getRawValue();
    if (v.conjoint1_id == null || v.conjoint2_id == null) return;
    this.enCours.set(true);
    this.erreur.set(null);
    this.api
      .creerMariage({
        conjoint1_id: v.conjoint1_id,
        conjoint2_id: v.conjoint2_id,
        date_debut: v.date_debut,
        date_fin: v.dissous ? v.date_fin || null : null,
        cause_dissolution: v.dissous ? v.cause_dissolution || null : null,
      })
      .subscribe({
        next: () => {
          this.enCours.set(false);
          this.toasts.succes(`Mariage de ${this.store.nom(v.conjoint1_id)} et ${this.store.nom(v.conjoint2_id)} enregistré.`);
          this.form.reset();
          this.charger();
        },
        error: (e) => {
          this.enCours.set(false);
          this.erreur.set(toApiError(e));
        },
      });
  }

  protected ouvrirDissolution(m: Mariage): void {
    this.aDissoudre.set(m.id);
    this.aSupprimer.set(null);
    this.erreurDissolution.set(null);
    this.dissolution.reset();
  }

  protected dissoudre(m: Mariage): void {
    const v = this.dissolution.getRawValue();
    if (!v.date_fin || !v.cause_dissolution) return;
    this.api.dissoudreMariage(m.id, v.date_fin, v.cause_dissolution).subscribe({
      next: () => {
        this.aDissoudre.set(null);
        this.toasts.succes(`Mariage n° ${m.id} dissous par ${CAUSES[v.cause_dissolution as CauseDissolution].toLowerCase()}.`);
        this.charger();
      },
      error: (e) => this.erreurDissolution.set(toApiError(e)),
    });
  }

  protected supprimer(m: Mariage): void {
    this.api.supprimerMariage(m.id).subscribe({
      next: () => {
        this.aSupprimer.set(null);
        this.toasts.succes(`Mariage n° ${m.id} supprimé.`);
        this.charger();
      },
      error: (e) => this.toasts.erreur(toApiError(e).message),
    });
  }
}
