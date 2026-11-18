package bj.ifri.parente.erreur;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Erreur de validation ou de règle métier : HTTP 422, corps
 * {@code { "message": "...", "errors": { "champ": ["..."] } }} (champs en snake_case).
 */
public class ErreurValidation extends RuntimeException {

    private final Map<String, List<String>> erreurs;

    public ErreurValidation(Map<String, List<String>> erreurs) {
        super(premierMessage(erreurs));
        this.erreurs = Collections.unmodifiableMap(new LinkedHashMap<>(erreurs));
    }

    public static ErreurValidation sur(String champ, String message) {
        return new ErreurValidation(Map.of(champ, List.of(message)));
    }

    public Map<String, List<String>> erreurs() {
        return erreurs;
    }

    private static String premierMessage(Map<String, List<String>> erreurs) {
        return erreurs.values().stream().flatMap(List::stream).findFirst().orElse("Données invalides.");
    }
}
