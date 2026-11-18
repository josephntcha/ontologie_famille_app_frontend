import {
  afterNextRender,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import cytoscape, { Core, ElementDefinition, StylesheetJson } from 'cytoscape';
import dagre from 'cytoscape-dagre';

import { Arbre } from '../../core/models';

cytoscape.use(dagre);

/** Couleurs reprises des variables CSS (Cytoscape dessine sur un canvas, sans accès au CSS). */
const C = {
  encre: '#232845',
  gris: '#8a90a6',
  trait: '#b9bfd2',
  indigo: '#2b3a80',
  indigoPale: '#e2e6f6',
  or: '#c9971c',
  orPale: '#f8efd6',
  blanc: '#ffffff',
};

const STYLE: StylesheetJson = [
  {
    selector: 'node',
    style: {
      width: 30,
      height: 30,
      'background-color': C.blanc,
      'border-width': 2.5,
      'border-color': C.indigo,
      label: 'data(label)',
      'text-wrap': 'wrap',
      'text-valign': 'bottom',
      'text-halign': 'center',
      'text-margin-y': 6,
      'font-family': 'Public Sans Variable, system-ui, sans-serif',
      'font-size': 12,
      color: C.encre,
      'line-height': 1.25,
      'text-background-color': C.blanc,
      'text-background-opacity': 0.92,
      'text-background-padding': '2px',
      'text-background-shape': 'roundrectangle',
    },
  },
  // Convention généalogique : carré pour un homme, cercle pour une femme.
  { selector: 'node[sexe = "M"]', style: { shape: 'round-rectangle', 'corner-radius': '3' } },
  { selector: 'node[sexe = "F"]', style: { shape: 'ellipse' } },
  { selector: 'node.chemin', style: { 'background-color': C.indigo, 'border-color': C.indigo } },
  {
    selector: 'node.extremite',
    style: { 'border-width': 4, width: 36, height: 36, 'font-weight': 700, 'font-size': 13 },
  },
  {
    selector: 'node.ancetre',
    style: { 'background-color': C.or, 'border-color': '#8f6a10', 'border-width': 3 },
  },
  { selector: 'node.estompe', style: { opacity: 0.6 } },
  { selector: 'node:selected', style: { 'overlay-color': C.or, 'overlay-opacity': 0.25, 'overlay-padding': 6 } },
  {
    selector: 'edge',
    style: { width: 1.8, 'line-color': C.trait, 'curve-style': 'bezier' },
  },
  {
    selector: 'edge[type = "parentDe"]',
    style: { 'curve-style': 'taxi', 'taxi-direction': 'downward', 'taxi-turn': '50%' },
  },
  {
    selector: 'edge[type = "marieA"]',
    style: { 'curve-style': 'straight', 'line-style': 'dashed', 'line-dash-pattern': [7, 4], 'line-color': C.gris, width: 1.6 },
  },
  {
    selector: 'edge[type = "beauParentDe"]',
    style: {
      'line-style': 'dotted',
      'line-color': C.or,
      width: 2.2,
      'target-arrow-shape': 'triangle',
      'target-arrow-color': C.or,
      'arrow-scale': 0.8,
      'curve-style': 'unbundled-bezier',
      'control-point-distances': [40],
    },
  },
  { selector: 'edge.chemin', style: { 'line-color': C.indigo, width: 4, 'z-index': 10 } },
  { selector: 'edge.estompe', style: { opacity: 0.4 } },
];

/** Graphe Cytoscape de GET /api/arbre, avec le chemin par l'ancêtre commun surligné. */
@Component({
  selector: 'app-arbre-graphe',
  template: `<div #zone class="zone" role="img" [attr.aria-label]="description()"></div>`,
  styles: `
    :host { display: block; }
    .zone { width: 100%; height: 100%; min-height: 520px; }
  `,
})
export class ArbreGraphe {
  readonly arbre = input.required<Arbre>();
  readonly personne1 = input.required<number>();
  readonly personne2 = input.required<number>();
  readonly selection = output<number | null>();

  private readonly zone = viewChild.required<ElementRef<HTMLDivElement>>('zone');
  private cy: Core | null = null;

  constructor() {
    afterNextRender(() => {
      this.cy = cytoscape({
        container: this.zone().nativeElement,
        style: STYLE,
        minZoom: 0.25,
        maxZoom: 2.5,
        boxSelectionEnabled: false,
        autoungrabify: false,
      });
      this.cy.on('tap', 'node', (e) => this.selection.emit(Number(e.target.id())));
      this.cy.on('tap', (e) => {
        if (e.target === this.cy) this.selection.emit(null);
      });
      this.dessiner();
    });

    effect(() => {
      this.arbre();
      this.personne1();
      this.personne2();
      this.dessiner();
    });

    inject(DestroyRef).onDestroy(() => this.cy?.destroy());
  }

  protected description(): string {
    const a = this.arbre();
    return `Arbre de ${a.noeuds.length} personnes ; chemin de ${a.chemin.length} personnes par l'ancêtre commun.`;
  }

  private dessiner(): void {
    const cy = this.cy;
    if (!cy) return;
    const a = this.arbre();
    const chemin = new Set(a.chemin);
    const extremites = new Set([this.personne1(), this.personne2()]);
    const avecChemin = a.chemin.length > 0;

    // Paires consécutives du chemin (dans un sens ou l'autre) → arêtes à surligner.
    const paires = new Set<string>();
    for (let i = 0; i + 1 < a.chemin.length; i++) {
      paires.add(`${a.chemin[i]}-${a.chemin[i + 1]}`);
      paires.add(`${a.chemin[i + 1]}-${a.chemin[i]}`);
    }

    const elements: ElementDefinition[] = [
      ...a.noeuds.map((n) => {
        const classes = [
          chemin.has(n.id) ? 'chemin' : '',
          extremites.has(n.id) ? 'extremite' : '',
          n.id === a.ancetre_commun_id ? 'ancetre' : '',
          avecChemin && !chemin.has(n.id) && !extremites.has(n.id) ? 'estompe' : '',
        ];
        return {
          group: 'nodes' as const,
          data: { id: String(n.id), label: `${n.prenom}\n${n.nom}`, sexe: n.sexe },
          classes: classes.filter(Boolean).join(' '),
        };
      }),
      ...a.aretes.map((e, i) => {
        const surligne = e.type === 'parentDe' && paires.has(`${e.source}-${e.cible}`);
        return {
          group: 'edges' as const,
          data: { id: `e${i}`, source: String(e.source), target: String(e.cible), type: e.type },
          classes: surligne ? 'chemin' : avecChemin ? 'estompe' : '',
        };
      }),
    ];

    cy.elements().remove();
    cy.add(elements);

    // Les générations sont calculées sur parentDe seulement : les conjoints restent côte à côte.
    const filiation = cy.elements('node, edge[type = "parentDe"]');
    filiation
      .layout({
        name: 'dagre',
        rankDir: 'TB',
        nodeSep: 70,
        rankSep: 95,
        ranker: 'network-simplex',
        animate: false,
        fit: false,
      } as cytoscape.LayoutOptions)
      .run();

    // Un conjoint sans filiation dans le graphe est placé à côté de son époux ou épouse.
    cy.nodes().forEach((n) => {
      if (n.connectedEdges('[type = "parentDe"]').length > 0) return;
      const conjoints = n.connectedEdges('[type = "marieA"]').connectedNodes().not(n).nodes();
      if (conjoints.nonempty()) {
        const c = conjoints.first().position();
        n.position({ x: c.x + 110, y: c.y });
      }
    });

    cy.fit(undefined, 40);
  }

  recentrer(): void {
    this.cy?.fit(undefined, 40);
  }

  png(): string | null {
    return this.cy ? this.cy.png({ full: true, scale: 2, bg: '#ffffff' }) : null;
  }
}
