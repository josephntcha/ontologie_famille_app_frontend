import { CauseDissolution, Sexe, Verdict } from './models';

/** Textes affichés pour les codes renvoyés par l'API. */

export const VERDICTS: Record<Verdict, { titre: string; resume: string }> = {
  interdit: {
    titre: 'Mariage interdit',
    resume: 'Au moins un empêchement certain a été trouvé dans les données.',
  },
  autorisation_requise: {
    titre: 'Autorisation requise',
    resume: 'Le mariage est possible avec l’autorisation du procureur de la République, pour motif grave.',
  },
  verification_incomplete: {
    titre: 'Vérification incomplète',
    resume:
      'Aucun empêchement trouvé, mais l’arbre connu ne couvre pas deux générations pour les deux personnes : un lien peut exister sans être saisi.',
  },
  autorise: {
    titre: 'Mariage autorisé',
    resume: 'Aucun empêchement, et l’arbre est complet sur deux générations pour les deux personnes.',
  },
};

export const VERDICT_REGLE: Record<string, string> = {
  interdit: 'Interdit',
  autorisation_requise: 'Autorisation requise',
  autorise: 'Autorisé',
};

export const TYPES_LIEN: Record<string, string> = {
  ligne_directe: 'Ligne directe',
  collaterale: 'Ligne collatérale',
};

export const CAUSES: Record<CauseDissolution, string> = {
  deces: 'Décès',
  divorce: 'Divorce',
};

export const SEXES: Record<Sexe, string> = { M: 'Masculin', F: 'Féminin' };

/** « 1er degré », « 4e degré ». */
export function degreEnLettres(d: number | null | undefined): string {
  if (d == null) return '';
  return (d === 1 ? '1er' : `${d}e`) + ' degré';
}

/** Date ISO yyyy-MM-dd → « 12 avril 1990 ». */
export function dateLongue(iso: string | null | undefined): string {
  if (!iso) return '';
  const [y, m, d] = iso.split('-').map(Number);
  const texte = new Date(y, m - 1, d).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  return d === 1 ? texte.replace(/^1 /, '1er ') : texte;
}

/**
 * Découpe un nom de classe OWL en mots lisibles : « DemiFrere » → « Demi frère »,
 * « MariageDissousParDeces » → « Mariage dissous par décès ».
 */
const ACCENTS: Record<string, string> = {
  frere: 'frère',
  mere: 'mère',
  pere: 'père',
  soeur: 'sœur',
  niece: 'nièce',
  deces: 'décès',
  feminin: 'féminin',
  garcon: 'garçon',
};
export function classeLisible(nom: string): string {
  const mots = nom.split(/(?=[A-Z])/).map((m) => m.toLowerCase());
  const accentues = mots.map((m) => ACCENTS[m] ?? m);
  const texte = accentues.join(' ');
  return texte.charAt(0).toUpperCase() + texte.slice(1);
}

/** Classes génériques peu informatives, masquées par défaut dans les listes de classes. */
export const CLASSES_GENERIQUES = new Set(['Personne', 'Sexe', 'Masculin', 'Feminin']);
