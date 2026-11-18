import { Component, inject } from '@angular/core';

import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="pile" aria-live="polite" aria-atomic="false">
      @for (t of service.toasts(); track t.id) {
        <div class="toast" [attr.data-kind]="t.kind" role="status">
          <span>{{ t.text }}</span>
          <button type="button" (click)="service.fermer(t.id)" aria-label="Fermer le message">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    .pile {
      position: fixed;
      right: 1.25rem;
      bottom: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      z-index: 50;
      max-width: min(420px, calc(100vw - 2.5rem));
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem 0.9rem 0.75rem 1rem;
      border-radius: 8px;
      background: var(--indigo-900);
      color: #fff;
      font-size: 0.9rem;
      box-shadow: 0 6px 24px rgba(23, 32, 74, 0.25);
      border-left: 4px solid var(--indigo-600);
      animation: entree 180ms ease-out;
    }
    .toast[data-kind='succes'] { border-left-color: #5fd394; }
    .toast[data-kind='erreur'] { border-left-color: #ff7a7a; }
    span { flex: 1; }
    button {
      background: none;
      border: none;
      color: inherit;
      font-size: 1.2rem;
      line-height: 1;
      cursor: pointer;
      opacity: 0.7;
    }
    @keyframes entree {
      from { transform: translateY(8px); opacity: 0; }
    }
  `,
})
export class Toasts {
  protected readonly service = inject(ToastService);
}
