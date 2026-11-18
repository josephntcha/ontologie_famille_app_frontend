import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, InjectionToken } from '@angular/core';
import { map, Observable } from 'rxjs';

import {
  Arbre,
  BeauParente,
  Coherence,
  CreationPersonneReponse,
  Donnees,
  Filiation,
  GroupeDoublons,
  Mariage,
  MariageSaisie,
  Ontologie,
  Personne,
  PersonneDetail,
  PersonneSaisie,
  CauseDissolution,
  Verification,
} from './models';

/**
 * Préfixe des routes. En développement, « /api » est relayé vers http://localhost:8080 par
 * proxy.conf.json (pas de CORS à configurer). En production, remplacez par l'URL complète si le
 * front et l'API ne sont pas servis par le même domaine.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api',
});

/** Un appel par route de l'API. Aucune logique métier ici : tout est calculé par le backend. */
@Injectable({ providedIn: 'root' })
export class ParenteApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);

  // ----- Personnes -----
  personnes(): Observable<Personne[]> {
    return this.http.get<Donnees<Personne[]>>(`${this.base}/personnes`).pipe(map((r) => r.data));
  }
  creerPersonne(p: PersonneSaisie): Observable<CreationPersonneReponse> {
    return this.http.post<CreationPersonneReponse>(`${this.base}/personnes`, p);
  }
  doublons(): Observable<GroupeDoublons[]> {
    return this.http.get<Donnees<GroupeDoublons[]>>(`${this.base}/personnes/doublons`).pipe(map((r) => r.data));
  }
  personne(id: number): Observable<PersonneDetail> {
    return this.http.get<Donnees<PersonneDetail>>(`${this.base}/personnes/${id}`).pipe(map((r) => r.data));
  }
  modifierPersonne(id: number, champs: Partial<PersonneSaisie>): Observable<Personne> {
    return this.http.patch<Donnees<Personne>>(`${this.base}/personnes/${id}`, champs).pipe(map((r) => r.data));
  }
  supprimerPersonne(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/personnes/${id}`);
  }

  // ----- Liens parentDe -----
  filiations(): Observable<Filiation[]> {
    return this.http.get<Donnees<Filiation[]>>(`${this.base}/liens/parente`).pipe(map((r) => r.data));
  }
  creerFiliation(parent_id: number, enfant_id: number): Observable<Filiation> {
    return this.http
      .post<Donnees<Filiation>>(`${this.base}/liens/parente`, { parent_id, enfant_id })
      .pipe(map((r) => r.data));
  }
  supprimerFiliation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/liens/parente/${id}`);
  }

  // ----- Liens beauParentDe -----
  beauxLiens(): Observable<BeauParente[]> {
    return this.http.get<Donnees<BeauParente[]>>(`${this.base}/liens/beau-parente`).pipe(map((r) => r.data));
  }
  creerBeauLien(beau_parent_id: number, enfant_id: number): Observable<BeauParente> {
    return this.http
      .post<Donnees<BeauParente>>(`${this.base}/liens/beau-parente`, { beau_parent_id, enfant_id })
      .pipe(map((r) => r.data));
  }
  supprimerBeauLien(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/liens/beau-parente/${id}`);
  }

  // ----- Mariages -----
  mariages(): Observable<Mariage[]> {
    return this.http.get<Donnees<Mariage[]>>(`${this.base}/mariages`).pipe(map((r) => r.data));
  }
  creerMariage(m: MariageSaisie): Observable<Mariage> {
    return this.http.post<Donnees<Mariage>>(`${this.base}/mariages`, m).pipe(map((r) => r.data));
  }
  dissoudreMariage(id: number, date_fin: string, cause_dissolution: CauseDissolution): Observable<Mariage> {
    return this.http
      .patch<Donnees<Mariage>>(`${this.base}/mariages/${id}`, { date_fin, cause_dissolution })
      .pipe(map((r) => r.data));
  }
  supprimerMariage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/mariages/${id}`);
  }

  // ----- Vérification et arbre -----
  verifier(personne1_id: number, personne2_id: number): Observable<Verification> {
    return this.http.post<Verification>(`${this.base}/verification`, { personne1_id, personne2_id });
  }
  arbre(personne1_id: number, personne2_id: number): Observable<Arbre> {
    const params = new HttpParams().set('personne1_id', personne1_id).set('personne2_id', personne2_id);
    return this.http.get<Arbre>(`${this.base}/arbre`, { params });
  }

  // ----- Ontologie, raisonneur, export -----
  ontologie(): Observable<Ontologie> {
    return this.http.get<Ontologie>(`${this.base}/ontologie`);
  }
  coherence(): Observable<Coherence> {
    return this.http.get<Coherence>(`${this.base}/raisonneur/coherence`);
  }
  exportTurtle(): Observable<string> {
    return this.http.get(`${this.base}/export/turtle`, { responseType: 'text' });
  }
}
