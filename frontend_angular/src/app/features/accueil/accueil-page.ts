import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ParenteApi } from '../../core/api.service';
import { Coherence, GroupeDoublons } from '../../core/models';
import { PersonnesStore } from '../../core/personnes.store';
import { PersonneSelect } from '../../shared/personne-select';

@Component({
  selector: 'app-accueil-page',
  imports: [ReactiveFormsModule, RouterLink, PersonneSelect],
  templateUrl: './accueil-page.html',
  styleUrl: './accueil-page.scss',
})
export class AccueilPage implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly router = inject(Router);
  protected readonly store = inject(PersonnesStore);

  protected readonly p1 = new FormControl<number | null>(null);
  protected readonly p2 = new FormControl<number | null>(null);

  protected readonly stats = signal<{ filiations: number; beaux: number; mariages: number; enCours: number } | null>(
    null,
  );
  protected readonly coherence = signal<Coherence | null>(null);
  protected readonly doublons = signal<GroupeDoublons[]>([]);

  ngOnInit(): void {
    this.store.charger();
    forkJoin({
      filiations: this.api.filiations(),
      beaux: this.api.beauxLiens(),
      mariages: this.api.mariages(),
      doublons: this.api.doublons(),
      coherence: this.api.coherence(),
    }).subscribe({
      next: (r) => {
        this.stats.set({
          filiations: r.filiations.length,
          beaux: r.beaux.length,
          mariages: r.mariages.length,
          enCours: r.mariages.filter((m) => m.en_cours).length,
        });
        this.doublons.set(r.doublons);
        this.coherence.set(r.coherence);
      },
      error: () => this.stats.set(null),
    });
  }

  protected verifier(): void {
    if (this.p1.value == null || this.p2.value == null) return;
    this.router.navigate(['/verification'], { queryParams: { p1: this.p1.value, p2: this.p2.value } });
  }
}
