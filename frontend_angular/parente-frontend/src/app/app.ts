import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ParenteApi } from './core/api.service';
import { PersonnesStore } from './core/personnes.store';
import { Toasts } from './shared/toasts';

type EtatApi = 'inconnu' | 'ok' | 'incoherente' | 'injoignable';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Toasts],
  template: `
    <a class="skip" href="#contenu">Aller au contenu</a>
    <div class="shell" [class.menu-ouvert]="menuOuvert()">
      <aside class="sidebar">
        <a routerLink="/" class="marque" (click)="menuOuvert.set(false)">
          <svg viewBox="0 0 40 40" width="34" height="34" aria-hidden="true">
            <rect x="15" y="3" width="10" height="10" rx="1.5" fill="none" stroke="currentColor" stroke-width="2" />
            <circle cx="8" cy="32" r="5" fill="none" stroke="currentColor" stroke-width="2" />
            <rect x="27" y="27" width="10" height="10" rx="1.5" fill="currentColor" />
            <path d="M20 13v7M8 20h24M8 20v7M32 20v7" fill="none" stroke="currentColor" stroke-width="2" />
          </svg>
          <span>
            <strong>Registre de parenté</strong>
            <small>Ontologie famille et HermiT</small>
          </span>
        </a>
        <button class="burger" type="button" (click)="menuOuvert.set(!menuOuvert())" [attr.aria-expanded]="menuOuvert()">
          Menu
        </button>
        <nav aria-label="Navigation principale" (click)="menuOuvert.set(false)">
          <a routerLink="/" routerLinkActive="actif" [routerLinkActiveOptions]="{ exact: true }">Accueil</a>
          <a routerLink="/personnes" routerLinkActive="actif">Personnes</a>
          <a routerLink="/liens" routerLinkActive="actif">Liens</a>
          <a routerLink="/verification" routerLinkActive="actif">Vérification</a>
          <a routerLink="/arbre" routerLinkActive="actif">Arbre</a>
          <a routerLink="/ontologie" routerLinkActive="actif">Ontologie</a>
        </nav>
        <div class="etat-api" [attr.data-etat]="etatApi()">
          <span class="pastille" aria-hidden="true"></span>
          @switch (etatApi()) {
            @case ('ok') { API connectée, ontologie cohérente }
            @case ('incoherente') { API connectée, ontologie incohérente }
            @case ('injoignable') { API injoignable sur le port 8080 }
            @default { Connexion à l'API… }
          }
          @if (etatApi() === 'injoignable') {
            <button type="button" class="lien-discret" (click)="sonder()">Réessayer</button>
          }
        </div>
      </aside>
      <main id="contenu" tabindex="-1">
        <router-outlet />
      </main>
    </div>
    <app-toasts />
  `,
  styleUrl: './app.scss',
})
export class App implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly store = inject(PersonnesStore);

  protected readonly menuOuvert = signal(false);
  protected readonly etatApi = signal<EtatApi>('inconnu');

  ngOnInit(): void {
    this.store.charger();
    this.sonder();
  }

  protected sonder(): void {
    this.etatApi.set('inconnu');
    this.api.coherence().subscribe({
      next: (c) => {
        this.etatApi.set(c.coherente ? 'ok' : 'incoherente');
        this.store.charger();
      },
      error: () => this.etatApi.set('injoignable'),
    });
  }
}
