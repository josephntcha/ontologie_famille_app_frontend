import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { ParenteApi } from '../../core/api.service';
import { ApiError, toApiError } from '../../core/api-error';
import { classeLisible } from '../../core/libelles';
import { Coherence, Ontologie } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-ontologie-page',
  templateUrl: './ontologie-page.html',
  styleUrl: './ontologie-page.scss',
})
export class OntologiePage implements OnInit {
  private readonly api = inject(ParenteApi);
  private readonly toasts = inject(ToastService);

  protected readonly ontologie = signal<Ontologie | null>(null);
  protected readonly coherence = signal<Coherence | null>(null);
  protected readonly turtle = signal<string | null>(null);
  protected readonly erreur = signal<ApiError | null>(null);
  protected readonly verification = signal(false);
  protected readonly filtre = signal('');
  protected readonly lisible = classeLisible;

  protected readonly classesFiltrees = computed(() => {
    const q = this.filtre().trim().toLowerCase();
    const l = this.ontologie()?.classes ?? [];
    return q ? l.filter((c) => c.toLowerCase().includes(q) || classeLisible(c).toLowerCase().includes(q)) : l;
  });

  protected readonly turtleStats = computed(() => {
    const t = this.turtle();
    if (!t) return null;
    return {
      lignes: t.split('\n').length,
      personnes: (t.match(/^f:personne_\d+/gm) ?? []).length,
      mariages: (t.match(/^f:mariage_\d+/gm) ?? []).length,
    };
  });

  ngOnInit(): void {
    this.api.ontologie().subscribe({
      next: (o) => this.ontologie.set(o),
      error: (e) => this.erreur.set(toApiError(e)),
    });
    this.verifierCoherence();
    this.chargerTurtle();
  }

  protected verifierCoherence(): void {
    this.verification.set(true);
    this.api.coherence().subscribe({
      next: (c) => {
        this.coherence.set(c);
        this.verification.set(false);
      },
      error: (e) => {
        this.verification.set(false);
        this.toasts.erreur(toApiError(e).message);
      },
    });
  }

  private chargerTurtle(): void {
    this.api.exportTurtle().subscribe({ next: (t) => this.turtle.set(t), error: () => this.turtle.set(null) });
  }

  protected telechargerTurtle(): void {
    this.api.exportTurtle().subscribe({
      next: (t) => {
        this.turtle.set(t);
        const url = URL.createObjectURL(new Blob([t], { type: 'text/turtle;charset=utf-8' }));
        const a = document.createElement('a');
        a.href = url;
        a.download = 'abox-famille.ttl';
        a.click();
        URL.revokeObjectURL(url);
        this.toasts.succes('ABox exportée : abox-famille.ttl');
      },
      error: (e) => this.toasts.erreur(toApiError(e).message),
    });
  }

  protected copierNamespace(): void {
    const ns = this.ontologie()?.namespace;
    if (ns) navigator.clipboard?.writeText(ns).then(() => this.toasts.info('Espace de noms copié.'));
  }
}
