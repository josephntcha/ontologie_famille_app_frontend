package bj.ifri.parente.web.dto;

import java.util.List;

/** Réponse de GET /api/arbre (SPEC_API.md 6) ; classes des nœuds inférées par HermiT. */
public record ArbreDto(List<Noeud> noeuds, List<Arete> aretes, List<Long> chemin, Long ancetreCommunId) {

    public record Noeud(Long id, String nom, String prenom, String sexe, List<String> classes) {
    }

    public record Arete(Long source, Long cible, String type) {
    }
}
