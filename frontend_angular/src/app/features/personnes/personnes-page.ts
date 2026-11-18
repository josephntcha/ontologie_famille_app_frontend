import { Component, computed, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { dateLongue } from '../../core/libelles';
import { GroupeDoublons, Personne, PersonneSaisie } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { ToastService } from '../../core/toast.service';
import { erreursHorsChamps } from '../../shared/erreurs-champ';
import { PersonneForm } from './personne-form';

@Component({
  selector: 'app-personnes-page',
  imports: [FormsModule, RouterLink, PersonneForm],
  templateUrl: './personnes-page.html',
  styles: `
    .recherche { max-width: 320px; margin-bottom: 1rem; }
    .liste-tete { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: baseline; gap: 1rem; margin-bottom: 1rem; }
    .doublon-groupe { margin-top: 0.4rem; }
    .doublon-groupe a + a::before { content: ', '; color: var(--encre); }
    tr.recent td { background: var(--or-pale); }
  `,
})
export class PersonnesPage implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly toasts = inject(ToastService);
  protected readonly store = inject(PersonnesStore);
  private readonly formulaire = viewChild.required(PersonneForm);

  protected readonly recherche = signal('');
  protected readonly enCours = signal(false);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly doublonsCreation = signal<{ cree: Personne; doublons: Personne[] } | null>(null);
  protected readonly groupesDoublons = signal<GroupeDoublons[]>([]);
  protected readonly derniereCreee = signal<number | null>(null);
  protected readonly dateLongue = dateLongue;

  protected readonly filtrees = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.store.triees();
    if (!q) return liste;
    return liste.filter((p) => `${p.prenom} ${p.nom} ${p.id}`.toLowerCase().includes(q));
  });

  protected readonly erreursGenerales = computed(() =>
    erreursHorsChamps(this.erreur(), ['nom', 'prenom', 'sexe', 'date_naissance']),
  );

  ngOnInit(): void {
    this.store.charger();
    this.chargerDoublons();
  }

  protected creer(saisie: PersonneSaisie): void {
    this.enCours.set(true);
    this.erreur.set(null);
    this.doublonsCreation.set(null);
    this.api.creerPersonne(saisie).subscribe({
      next: (r) => {
        this.enCours.set(false);
        this.formulaire().reinitialiser();
        this.derniereCreee.set(r.data.id);
        this.toasts.succes(`${r.data.prenom} ${r.data.nom} est enregistré(e) sous le n° ${r.data.id}.`);
        if (r.doublons_probables.length > 0) {
          this.doublonsCreation.set({ cree: r.data, doublons: r.doublons_probables });
        }
        this.store.charger();
        this.chargerDoublons();
      },
      error: (e) => {
        this.enCours.set(false);
        this.erreur.set(toApiError(e));
      },
    });
  }

  private chargerDoublons(): void {
    this.api.doublons().subscribe({ next: (g) => this.groupesDoublons.set(g), error: () => {} });
  }
}
