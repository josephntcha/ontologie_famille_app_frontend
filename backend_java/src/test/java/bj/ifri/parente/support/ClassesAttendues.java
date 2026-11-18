package bj.ifri.parente.support;

import java.util.List;

/**
 * Classes nommées inférées par HermiT (OWL API 5.1.20, HermiT 1.4.5.519) pour la famille de
 * démonstration, sur l'ABox décrite dans SPEC_API_JAVA.md section C (âge au 2026-11-18,
 * frereDe/soeurDe ajoutés, marieA pour tout mariage, DifferentIndividuals). Listes triées.
 *
 * Vérifiées en exécutant HermiT sur ces ABox, pas déduites à la main. Points notables :
 * <ul>
 *   <li>Sexe ≡ Feminin ⊔ Masculin : toute personne est aussi Sexe ;</li>
 *   <li>Maman et Tantine sont BelleFille (épouses, même divorcée, d'un Fils) ;</li>
 *   <li>Papa n'est pas BeauFils tant que Maman n'a pas de parent connu (elle n'est pas Fille) ;</li>
 *   <li>sans beauParentDe, ni Kid ni Demi ne sont DemiSoeur / DemiFrere (définition OWL).</li>
 * </ul>
 */
public final class ClassesAttendues {

    private ClassesAttendues() {
    }

    public static final List<String> GRANDPA = List.of(
            "GrandParent", "GrandPere", "Homme", "Masculin", "Parent", "Pere", "Personne", "Sexe");

    public static final List<String> GRANDMA = List.of(
            "Feminin", "Femme", "GrandMere", "GrandParent", "Mere", "Parent", "Personne", "Sexe");

    /** Papa et Tonton : frères, chacun parent, donc chacun Oncle. */
    public static final List<String> PAPA = List.of(
            "Enfant", "Fils", "Frere", "Homme", "Masculin", "Oncle", "Parent", "Pere", "Personne", "Sexe");

    public static final List<String> TONTON = PAPA;

    /** Maman et Tantine : mariées (Tantine divorcée) à un Fils de Grandpa et Grandma. */
    public static final List<String> MAMAN = List.of(
            "BelleFille", "Feminin", "Femme", "Mere", "Parent", "Personne", "Sexe");

    public static final List<String> TANTINE = MAMAN;

    public static final List<String> AUTRE = List.of(
            "Homme", "Masculin", "Parent", "Pere", "Personne", "Sexe");

    /** Demi : frère (demi) de Kid, mais pas DemiFrere au sens OWL sans beauParentDe. */
    public static final List<String> DEMI = List.of(
            "Enfant", "Fils", "Frere", "Homme", "Masculin", "Personne", "Sexe");

    public static final List<String> KID = List.of(
            "Cousine", "Enfant", "Feminin", "Femme", "Fille", "Niece", "Personne", "PetitEnfant",
            "PetiteFille", "Sexe", "Soeur");

    public static final List<String> COUSIN = List.of(
            "Cousin", "Enfant", "Fils", "Homme", "Masculin", "Neveu", "Personne", "PetitEnfant",
            "PetitFils", "Sexe");

    /** Beau-père isolé (né en 1950, sans conjoint ni enfant) déclaré beauParentDe Kid. */
    public static final List<String> BEAU_PERE_ISOLE = List.of(
            "BeauParent", "BeauPere", "Homme", "Masculin", "Personne", "Sexe");

    /** Tatie (F, 1965), troisième enfant de Grandpa et Grandma : sœur de Papa, qui est parent. */
    public static final List<String> TATIE = List.of(
            "Enfant", "Feminin", "Femme", "Fille", "Personne", "Sexe", "Soeur", "Tante");

    public static final List<String> MARIAGE_EN_COURS = List.of("Mariage");

    public static final List<String> MARIAGE_DIVORCE = List.of(
            "Mariage", "MariageDissous", "MariageDissousParDivorce");

    public static final List<String> MARIAGE_DECES = List.of(
            "Mariage", "MariageDissous", "MariageDissousParDeces");
}
