package bj.ifri.parente.service;

import java.util.List;

/**
 * Résultat de l'algorithme du lien de sang (SPEC_API.md 5.1), calculé en Java par parcours en
 * largeur. {@code ancetresCommuns} contient des ids. Champs null si aucun lien n'existe.
 */
public record LienDeSang(boolean existe, String type, Integer degre, Integer generationPersonne1,
        Integer generationPersonne2, String code, String libelle, boolean demi, List<Long> ancetresCommuns,
        String message) {

    public static final String AUCUN = "Aucun lien de sang trouvé dans les données connues";

    public static LienDeSang aucun() {
        return new LienDeSang(false, null, null, null, null, null, null, false, List.of(), AUCUN);
    }
}
