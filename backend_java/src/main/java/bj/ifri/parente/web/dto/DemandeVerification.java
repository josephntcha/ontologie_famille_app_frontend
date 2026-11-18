package bj.ifri.parente.web.dto;

import jakarta.validation.constraints.NotNull;

public record DemandeVerification(
        @NotNull(message = "La première personne est requise.") Long personne1Id,
        @NotNull(message = "La seconde personne est requise.") Long personne2Id) {
}
