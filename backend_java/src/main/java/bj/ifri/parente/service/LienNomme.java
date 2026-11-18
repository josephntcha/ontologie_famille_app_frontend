package bj.ifri.parente.service;

/** Nom d'un lien de sang (SPEC_API.md 5.2) ; type = "ligne_directe" ou "collaterale", degre = g1 + g2. */
public record LienNomme(String code, String libelle, int degre, String type) {
}
