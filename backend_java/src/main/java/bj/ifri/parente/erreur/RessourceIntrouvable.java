package bj.ifri.parente.erreur;

/** Ressource d'URL inexistante : HTTP 404 {@code { "message": "..." }}. */
public class RessourceIntrouvable extends RuntimeException {

    public RessourceIntrouvable(String message) {
        super(message);
    }

    public static RessourceIntrouvable personne(Long id) {
        return new RessourceIntrouvable("Personne " + id + " introuvable.");
    }

    public static RessourceIntrouvable mariage(Long id) {
        return new RessourceIntrouvable("Mariage " + id + " introuvable.");
    }
}
