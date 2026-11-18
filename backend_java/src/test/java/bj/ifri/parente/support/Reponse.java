package bj.ifri.parente.support;

import tools.jackson.databind.JsonNode;

/**
 * Réponse HTTP capturée par MockMvc.
 *
 * @param statut      code HTTP
 * @param typeContenu en-tête Content-Type (peut être null)
 * @param corps       corps brut (UTF-8)
 * @param json        corps analysé si c'est du JSON, sinon null
 */
public record Reponse(int statut, String typeContenu, String corps, JsonNode json) {

    /** Nœud désigné par un pointeur JSON (ex. "/data/id") ; MissingNode s'il n'existe pas. */
    public JsonNode at(String pointeur) {
        if (json == null) {
            throw new AssertionError("La réponse n'est pas du JSON : " + corps);
        }
        return json.at(pointeur);
    }

    /** Valeur entière (id, âge, degré...) désignée par un pointeur JSON. */
    public long entier(String pointeur) {
        JsonNode n = at(pointeur);
        if (!n.isIntegralNumber()) {
            throw new AssertionError("Entier attendu en " + pointeur + " : " + n + " dans " + corps);
        }
        return n.asLong();
    }

    /** Chaîne désignée par un pointeur JSON. */
    public String texte(String pointeur) {
        JsonNode n = at(pointeur);
        if (!n.isString()) {
            throw new AssertionError("Chaîne attendue en " + pointeur + " : " + n + " dans " + corps);
        }
        return n.stringValue();
    }

    /** Booléen désigné par un pointeur JSON. */
    public boolean booleen(String pointeur) {
        JsonNode n = at(pointeur);
        if (!n.isBoolean()) {
            throw new AssertionError("Booléen attendu en " + pointeur + " : " + n + " dans " + corps);
        }
        return n.asBoolean();
    }

    /** Vrai si le pointeur désigne une valeur JSON null présente. */
    public boolean estNull(String pointeur) {
        JsonNode n = at(pointeur);
        return n.isNull();
    }
}
