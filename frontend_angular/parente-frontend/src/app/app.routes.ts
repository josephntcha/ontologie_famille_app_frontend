import { Routes } from '@angular/router';

/** Les écrans sont chargés à la demande (l'arbre embarque Cytoscape, inutile ailleurs). */
export const routes: Routes = [
  {
    path: '',
    title: 'Accueil – Registre de parenté',
    loadComponent: () => import('./features/accueil/accueil-page').then((m) => m.AccueilPage),
  },
  {
    path: 'personnes',
    title: 'Personnes – Registre de parenté',
    loadComponent: () => import('./features/personnes/personnes-page').then((m) => m.PersonnesPage),
  },
  {
    path: 'personnes/:id',
    title: 'Fiche personne – Registre de parenté',
    loadComponent: () => import('./features/personnes/personne-detail-page').then((m) => m.PersonneDetailPage),
  },
  {
    path: 'liens',
    loadComponent: () => import('./features/liens/liens-page').then((m) => m.LiensPage),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'filiations' },
      {
        path: 'filiations',
        title: 'Filiations – Registre de parenté',
        loadComponent: () => import('./features/liens/filiations').then((m) => m.Filiations),
      },
      {
        path: 'beaux-parents',
        title: 'Beaux-parents – Registre de parenté',
        loadComponent: () => import('./features/liens/beaux-liens').then((m) => m.BeauxLiens),
      },
      {
        path: 'mariages',
        title: 'Mariages – Registre de parenté',
        loadComponent: () => import('./features/liens/mariages').then((m) => m.Mariages),
      },
    ],
  },
  {
    path: 'verification',
    title: 'Vérification – Registre de parenté',
    loadComponent: () => import('./features/verification/verification-page').then((m) => m.VerificationPage),
  },
  {
    path: 'arbre',
    title: 'Arbre – Registre de parenté',
    loadComponent: () => import('./features/arbre/arbre-page').then((m) => m.ArbrePage),
  },
  {
    path: 'ontologie',
    title: 'Ontologie – Registre de parenté',
    loadComponent: () => import('./features/ontologie/ontologie-page').then((m) => m.OntologiePage),
  },
  { path: '**', redirectTo: '' },
];
