package bj.ifri.parente.support;

import java.util.List;

/**
 * Signature attendue de la TBox famille.owl, relevée avec la OWL API (entités nommées de
 * l'espace de noms f:) : 39 classes, 22 propriétés objet, 7 propriétés de données.
 * Les listes sont triées (String.compareTo).
 */
public final class OntologieAttendue {

    private OntologieAttendue() {
    }

    public static final List<String> CLASSES = List.of(
            "BeauFils", "BeauParent", "BeauPere", "BelleFille", "BelleMere", "Cousin", "Cousine",
            "DemiFrere", "DemiSoeur", "Enfant", "Feminin", "Femme", "Fille", "Fillette", "Fils", "Frere",
            "Garcon", "GrandMere", "GrandParent", "GrandPere", "Homme", "Mariage", "MariageDissous",
            "MariageDissousParDeces", "MariageDissousParDivorce", "Masculin", "Mere", "Neveu", "Niece",
            "Oncle", "Parent", "Pere", "Personne", "PetitEnfant", "PetitFils", "PetiteFille", "Sexe",
            "Soeur", "Tante");

    public static final List<String> PROPRIETES_OBJET = List.of(
            "aPourConjoint", "ancetreDe", "beauParentDe", "conjointDans", "descendantDe", "enfantDe",
            "enfantDe1", "enfantDe2", "enfantDe3", "frereDe", "grandParentDe", "lienDeSangAvec", "marieA",
            "marieA1", "marieA2", "marieA3", "marieA4", "parentDe", "parentDe1", "parentDe2", "parentDe3",
            "soeurDe");

    public static final List<String> PROPRIETES_DONNEES = List.of(
            "age", "causeDissolution", "dateDebut", "dateFin", "dateNaissance", "nom", "prenom");
}
