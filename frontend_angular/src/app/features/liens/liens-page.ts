import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { PersonnesStore } from '../../core/personnes.store';

@Component({
  selector: 'app-liens-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="page-tete">
      <div>
        <h1>Liens</h1>
        <p class="chapo">
          Les liens déclarés ici forment l'ABox. Les frères et sœurs ne se déclarent pas : l'application les déduit des
          parents communs avant de lancer HermiT.
        </p>
      </div>
    </header>
    <nav class="onglets page" aria-label="Types de liens">
      <a routerLink="filiations" routerLinkActive="actif" ariaCurrentWhenActive="page">
        Filiation <span class="prop">parentDe</span>
      </a>
      <a routerLink="beaux-parents" routerLinkActive="actif" ariaCurrentWhenActive="page">
        Beau-parent <span class="prop">beauParentDe</span>
      </a>
      <a routerLink="mariages" routerLinkActive="actif" ariaCurrentWhenActive="page">
        Mariage <span class="prop">marieA</span>
      </a>
    </nav>
    @if (store.personnes().length < 2 && !store.chargement()) {
      <p class="alerte info page">
        Il faut au moins deux personnes pour déclarer un lien. <a routerLink="/personnes">Ajouter des personnes</a>
      </p>
    }
    <div class="page">
      <router-outlet />
    </div>
  `,
  styles: `
    .onglets {
      display: flex;
      gap: 0.25rem;
      border-bottom: 1px solid var(--trait-fort);
      margin-bottom: 1.5rem;
      overflow-x: auto;
    }
    .onglets a {
      padding: 0.7rem 1rem;
      text-decoration: none;
      color: var(--gris);
      font-weight: 600;
      border-bottom: 3px solid transparent;
      margin-bottom: -1px;
      white-space: nowrap;
    }
    .onglets a:hover { color: var(--encre); }
    .onglets a.actif { color: var(--indigo-700); border-bottom-color: var(--indigo-700); }
    .prop { font-family: var(--mono); font-size: 0.75rem; font-weight: 400; margin-left: 0.3rem; color: var(--gris); }
  `,
})
export class LiensPage implements OnInit {
  protected readonly store = inject(PersonnesStore);
  ngOnInit(): void {
    this.store.charger();
  }
}
