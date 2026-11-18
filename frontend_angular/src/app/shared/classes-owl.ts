import { Component, computed, input, signal } from '@angular/core';

import { classeLisible, CLASSES_GENERIQUES } from '../core/libelles';

/**
 * Classes OWL inférées par HermiT pour un individu. Les classes génériques (Personne, Sexe…)
 * sont repliées par défaut pour laisser voir celles qui comptent (Oncle, DemiFrere…).
 */
@Component({
  selector: 'app-classes-owl',
  template: `
    <ul class="classes" [attr.aria-label]="'Classes inférées par HermiT'">
      @for (c of visibles(); track c) {
        <li [title]="'f:' + c">{{ lisible(c) }}</li>
      } @empty {
        <li class="aucune">Aucune classe spécifique</li>
      }
    </ul>
    @if (masquees() > 0) {
      <button type="button" class="lien-discret" (click)="tout.set(!tout())">
        {{ tout() ? 'Masquer les classes génériques' : 'Afficher aussi ' + masquees() + ' classe(s) générique(s)' }}
      </button>
    }
  `,
  styles: `
    .classes {
      list-style: none;
      margin: 0 0 0.4rem;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem;
    }
    li {
      font-size: 0.82rem;
      font-weight: 600;
      padding: 0.15rem 0.6rem;
      border-radius: 4px;
      background: var(--indigo-100);
      color: var(--indigo-700);
    }
    li.aucune { background: none; color: var(--gris); font-weight: 400; padding-left: 0; }
    .lien-discret { font-size: 0.8rem; }
  `,
})
export class ClassesOwl {
  readonly classes = input<string[]>([]);
  protected readonly tout = signal(false);
  protected readonly lisible = classeLisible;

  protected readonly visibles = computed(() =>
    this.tout() ? this.classes() : this.classes().filter((c) => !CLASSES_GENERIQUES.has(c)),
  );
  protected readonly masquees = computed(
    () => this.classes().filter((c) => CLASSES_GENERIQUES.has(c)).length,
  );
}
