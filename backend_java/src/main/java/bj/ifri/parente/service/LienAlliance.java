package bj.ifri.parente.service;

/** Lien d'alliance retenu (SPEC_API.md 5.3) ; champs null si aucune alliance. */
public record LienAlliance(boolean existe, String type, Integer degre, String code, String libelle, Long conjointId,
        Long mariageId, boolean mariageDissousParDeces, String message) {

    public static final String AUCUNE = "Aucun lien d'alliance trouvé dans les données connues";

    public static LienAlliance aucune() {
        return new LienAlliance(false, null, null, null, null, null, null, false, AUCUNE);
    }
}
