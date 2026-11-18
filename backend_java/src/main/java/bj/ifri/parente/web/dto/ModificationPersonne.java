package bj.ifri.parente.web.dto;

import bj.ifri.parente.web.validation.DateIso;
import bj.ifri.parente.web.validation.PasDansLeFutur;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mise à jour partielle (PUT ou PATCH) : seuls les champs fournis sont validés et modifiés. */
public record ModificationPersonne(
        @Pattern(regexp = "(?s).*\\S.*", message = "Le nom ne doit pas être vide.") @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères.") String nom,
        @Pattern(regexp = "(?s).*\\S.*", message = "Le prénom ne doit pas être vide.") @Size(max = 255, message = "Le prénom ne doit pas dépasser 255 caractères.") String prenom,
        @Pattern(regexp = "[MF]", message = "Le sexe doit valoir M ou F.") String sexe,
        @DateIso(message = "La date de naissance doit être une date valide (AAAA-MM-JJ).") @PasDansLeFutur(message = "La date de naissance ne doit pas être dans le futur.") String dateNaissance) {
}
