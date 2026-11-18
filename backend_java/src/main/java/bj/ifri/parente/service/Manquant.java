package bj.ifri.parente.service;

/** Personne dont au moins un parent est inconnu (liste « manquants » de la vérification). */
public record Manquant(Long personneId, String description) {
}
