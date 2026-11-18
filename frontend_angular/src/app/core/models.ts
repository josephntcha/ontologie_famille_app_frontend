/**
 * Modèles TypeScript du contrat de l'API Spring Boot (SPEC_API.md + SPEC_API_JAVA.md).
 * Le backend parle en snake_case : on garde les mêmes noms côté front pour éviter toute
 * conversion (ce que l'on lit dans l'onglet Réseau du navigateur = ce que l'on manipule ici).
 */

export type Sexe = 'M' | 'F';
export type CauseDissolution = 'deces' | 'divorce';
export type TypeLien = 'ligne_directe' | 'collaterale';
export type Verdict = 'interdit' | 'autorisation_requise' | 'verification_incomplete' | 'autorise';

/** Enveloppe { "data": ... } utilisée par la plupart des routes. */
export interface Donnees<T> {
  data: T;
}

export interface Personne {
  id: number;
  nom: string;
  prenom: string;
  sexe: Sexe;
  date_naissance: string; // yyyy-MM-dd
  age: number;
  uri: string; // IRI de l'individu dans l'ABox (f:personne_{id})
}

export interface PersonneSaisie {
  nom: string;
  prenom: string;
  sexe: Sexe;
  date_naissance: string;
}

export interface CreationPersonneReponse {
  data: Personne;
  doublons_probables: Personne[];
}

export interface GroupeDoublons {
  nom: string;
  date_naissance: string;
  personnes: Personne[];
}

export interface Mariage {
  id: number;
  conjoint1_id: number;
  conjoint2_id: number;
  date_debut: string;
  date_fin: string | null;
  cause_dissolution: CauseDissolution | null;
  en_cours: boolean;
  classes: string[]; // inférées par HermiT (Mariage, MariageDissous…)
}

export interface MariageSaisie {
  conjoint1_id: number;
  conjoint2_id: number;
  date_debut: string;
  date_fin?: string | null;
  cause_dissolution?: CauseDissolution | null;
}

export interface PersonneDetail extends Personne {
  parents: Personne[];
  enfants: Personne[];
  beaux_parents: Personne[];
  beaux_enfants: Personne[];
  freres: Personne[]; // frereDe déduit (parent commun)
  soeurs: Personne[]; // soeurDe déduit (parent commun)
  conjoints: Personne[];
  mariages: Mariage[];
  classes: string[]; // inférées par HermiT
}

export interface Filiation {
  id: number;
  parent_id: number;
  enfant_id: number;
}

export interface BeauParente {
  id: number;
  beau_parent_id: number;
  enfant_id: number;
}

// ---------- Vérification ----------

export interface AncetreCommun {
  id: number;
  nom: string;
  prenom: string;
}

export interface LienDeSang {
  existe: boolean;
  type: TypeLien | null;
  degre: number | null;
  generation_personne1: number | null;
  generation_personne2: number | null;
  code: string | null;
  libelle: string | null;
  demi: boolean;
  ancetres_communs: AncetreCommun[];
  message: string;
  infere_par_raisonneur: boolean; // HermiT infère-t-il f:lienDeSangAvec(x, y) ?
}

export interface LienAlliance {
  existe: boolean;
  type: TypeLien | null;
  degre: number | null;
  code: string | null;
  libelle: string | null;
  conjoint_id: number | null;
  mariage_id: number | null;
  mariage_dissous_par_deces: boolean;
  message: string;
}

export interface RegleMariage {
  code: string;
  article: string;
  verdict: Exclude<Verdict, 'verification_incomplete'>;
  message: string;
}

export interface VerdictMariage {
  verdict: Verdict;
  regles: RegleMariage[];
  avertissement: string;
}

export interface Completude {
  parents_connus: number;
  parents_attendus: number;
  grands_parents_connus: number;
  grands_parents_attendus: number;
  generations_completes: number;
  complet: boolean;
}

export interface Manquant {
  personne_id: number;
  description: string;
}

export interface Verification {
  personne1: Personne;
  personne2: Personne;
  lien_de_sang: LienDeSang;
  lien_alliance: LienAlliance;
  mariage: VerdictMariage;
  completude: {
    personne1: Completude;
    personne2: Completude;
    complet: boolean;
    manquants: Manquant[];
  };
}

// ---------- Arbre ----------

export type TypeArete = 'parentDe' | 'beauParentDe' | 'marieA';

export interface NoeudArbre {
  id: number;
  nom: string;
  prenom: string;
  sexe: Sexe;
  classes: string[];
}

export interface AreteArbre {
  source: number;
  cible: number;
  type: TypeArete;
}

export interface Arbre {
  noeuds: NoeudArbre[];
  aretes: AreteArbre[];
  chemin: number[];
  ancetre_commun_id: number | null;
}

// ---------- Ontologie et raisonneur ----------

export interface Ontologie {
  namespace: string;
  classes: string[];
  proprietes_objet: string[];
  proprietes_donnees: string[];
}

export interface Coherence {
  raisonneur: string;
  coherente: boolean;
  classes_insatisfiables: string[];
}
