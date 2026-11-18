import { Component, computed, effect, inject, input, signal, untracked, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { Arbre } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ClassesOwl } from '../../shared/classes-owl';
import { PersonneSelect } from '../../shared/personne-select';
import { ArbreGraphe } from './arbre-graphe';

@Component({
  selector: 'app-arbre-page',
  imports: [ReactiveFormsModule, RouterLink, PersonneSelect, ArbreGraphe, ClassesOwl],
  templateUrl: './arbre-page.html',
  styleUrl: './arbre-page.scss',
})
export class ArbrePage {
  private readonly api = inject(ParenteApi);
  private readonly router = inject(Router);
  protected readonly store = inject(PersonnesStore);
  private readonly graphe = viewChild(ArbreGraphe);

  readonly p1 = input<string | undefined>();
  readonly p2 = input<string | undefined>();

  protected readonly c1 = new FormControl<number | null>(null);
  protected readonly c2 = new FormControl<number | null>(null);

  protected readonly arbre = signal<Arbre | null>(null);
  protected readonly ids = signal<{ a: number; b: number } | null>(null);
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly selectionId = signal<number | null>(null);

  protected readonly selection = computed(() => {
    const id = this.selectionId();
    return id == null ? null : (this.arbre()?.noeuds.find((n) => n.id === id) ?? null);
  });

  protected readonly cheminNoms = computed(() => (this.arbre()?.chemin ?? []).map((id) => this.store.nom(id)));

  constructor() {
    this.store.charger();
    effect(() => {
      const a = Number(this.p1());
      const b = Number(this.p2());
      untracked(() => {
        if (a && b) {
          this.c1.setValue(a);
          this.c2.setValue(b);
          this.charger(a, b);
        }
      });
    });
  }

  protected afficher(): void {
    if (this.c1.value == null || this.c2.value == null) return;
    this.router.navigate([], { queryParams: { p1: this.c1.value, p2: this.c2.value } });
    if (String(this.c1.value) === this.p1() && String(this.c2.value) === this.p2()) {
      this.charger(this.c1.value, this.c2.value);
    }
  }

  private charger(a: number, b: number): void {
    this.enCours.set(true);
    this.erreur.set(null);
    this.selectionId.set(null);
    this.api.arbre(a, b).subscribe({
      next: (r) => {
        this.ids.set({ a, b });
        this.arbre.set(r);
        this.enCours.set(false);
      },
      error: (e) => {
        this.erreur.set(toApiError(e));
        this.enCours.set(false);
      },
    });
  }

  protected recentrer(): void {
    this.graphe()?.recentrer();
  }

  protected telecharger(): void {
    const data = this.graphe()?.png();
    const ids = this.ids();
    if (!data || !ids) return;
    const a = document.createElement('a');
    a.href = data;
    a.download = `arbre-${ids.a}-${ids.b}.png`;
    a.click();
  }
}
