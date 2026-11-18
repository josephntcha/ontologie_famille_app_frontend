package bj.ifri.parente.web.dto;

import bj.ifri.parente.web.validation.DateIso;
import bj.ifri.parente.web.validation.PasDansLeFutur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreationPersonne(
        @NotBlank(message = "Le nom est requis.") @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères.") String nom,
        @NotBlank(message = "Le prénom est requis.") @Size(max = 255, message = "Le prénom ne doit pas dépasser 255 caractères.") String prenom,
        @NotNull(message = "Le sexe est requis.") @Pattern(regexp = "[MF]", message = "Le sexe doit valoir M ou F.") String sexe,
        @NotNull(message = "La date de naissance est requise.") @DateIso(message = "La date de naissance doit être une date valide (AAAA-MM-JJ).") @PasDansLeFutur(message = "La date de naissance ne doit pas être dans le futur.") String dateNaissance) {
}
