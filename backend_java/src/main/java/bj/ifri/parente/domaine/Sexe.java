package bj.ifri.parente.domaine;

/** Sexe d'une personne : M (classe f:Masculin) ou F (classe f:Feminin). */
public enum Sexe {
    M("Masculin"),
    F("Feminin");

    private final String classeOwl;

    Sexe(String classeOwl) {
        this.classeOwl = classeOwl;
    }

    /** Nom local de la classe OWL correspondante. */
    public String classeOwl() {
        return classeOwl;
    }
}
