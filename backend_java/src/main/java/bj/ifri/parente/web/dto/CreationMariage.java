package bj.ifri.parente.web.dto;

import bj.ifri.parente.web.validation.DateIso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreationMariage(
        @NotNull(message = "Le premier conjoint est requis.") Long conjoint1Id,
        @NotNull(message = "Le second conjoint est requis.") Long conjoint2Id,
        @NotNull(message = "La date de début est requise.") @DateIso(message = "La date de début doit être une date valide (AAAA-MM-JJ).") String dateDebut,
        @DateIso(message = "La date de fin doit être une date valide (AAAA-MM-JJ).") String dateFin,
        @Pattern(regexp = "deces|divorce", message = "La cause de dissolution doit valoir deces ou divorce.") String causeDissolution) {
}
