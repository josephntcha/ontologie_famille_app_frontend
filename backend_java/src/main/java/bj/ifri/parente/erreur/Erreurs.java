package bj.ifri.parente.erreur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Accumulateur d'erreurs de validation par champ (snake_case), à la manière de Laravel. */
public final class Erreurs {

    private final Map<String, List<String>> parChamp = new LinkedHashMap<>();

    public Erreurs ajouter(String champ, String message) {
        parChamp.computeIfAbsent(champ, c -> new ArrayList<>()).add(message);
        return this;
    }

    public boolean contient(String champ) {
        return parChamp.containsKey(champ);
    }

    public boolean estVide() {
        return parChamp.isEmpty();
    }

    /** Lève une {@link ErreurValidation} si au moins une erreur a été ajoutée. */
    public void lever() {
        if (!parChamp.isEmpty()) {
            throw new ErreurValidation(parChamp);
        }
    }
}
