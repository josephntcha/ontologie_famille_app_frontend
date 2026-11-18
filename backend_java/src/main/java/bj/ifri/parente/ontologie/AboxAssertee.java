package bj.ifri.parente.ontologie;

import java.util.List;

/**
 * ABox assertée, indépendante de toute syntaxe : la même projection des données sert à
 * construire l'ontologie raisonnée par HermiT ({@link ConstructeurABox}) et l'export Turtle
 * ({@link TurtleExporter}). Les noms sont des noms locaux de l'espace de noms f:.
 */
public record AboxAssertee(List<Individu> individus) {

    /** Types de littéraux utilisés (tous pris en charge par HermiT). */
    public enum TypeLitteral {
        STRING, INT, DATE_TIME
    }

    public record Donnee(String propriete, String valeur, TypeLitteral type) {
    }

    public record Lien(String propriete, String cible) {
    }

    public record Individu(String nom, List<String> classes, List<Donnee> donnees, List<Lien> liens) {
    }
}
