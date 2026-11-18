package bj.ifri.parente.web.dto;

import java.time.LocalDate;

/** Format JSON d'une personne (SPEC_API.md 2). */
public record PersonneDto(Long id, String nom, String prenom, String sexe, LocalDate dateNaissance, int age,
        String uri) {
}
