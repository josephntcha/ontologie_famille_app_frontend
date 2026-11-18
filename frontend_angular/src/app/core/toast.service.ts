import { Injectable, signal } from '@angular/core';

export type ToastKind = 'succes' | 'erreur' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  text: string;
}

/** Messages courts de confirmation ou d'erreur, affichés en bas à droite. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  succes(text: string): void {
    this.push('succes', text);
  }
  erreur(text: string): void {
    this.push('erreur', text, 7000);
  }
  info(text: string): void {
    this.push('info', text);
  }

  fermer(id: number): void {
    this.toasts.update((l) => l.filter((t) => t.id !== id));
  }

  private push(kind: ToastKind, text: string, duree = 4000): void {
    const id = ++this.seq;
    this.toasts.update((l) => [...l, { id, kind, text }]);
    setTimeout(() => this.fermer(id), duree);
  }
}
