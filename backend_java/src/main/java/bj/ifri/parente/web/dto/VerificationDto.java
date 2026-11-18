package bj.ifri.parente.web.dto;

import java.util.List;

import bj.ifri.parente.service.Completude;
import bj.ifri.parente.service.LienAlliance;
import bj.ifri.parente.service.Manquant;
import bj.ifri.parente.service.VerdictMariage;

/** Réponse de POST /api/verification (SPEC_API.md 5 et SPEC_API_JAVA.md D.1). */
public record VerificationDto(PersonneDto personne1, PersonneDto personne2, LienDeSangDto lienDeSang,
        LienAlliance lienAlliance, VerdictMariage mariage, CompletudeDto completude) {

    public record AncetreDto(Long id, String nom, String prenom) {
    }

    public record LienDeSangDto(boolean existe, String type, Integer degre, Integer generationPersonne1,
            Integer generationPersonne2, String code, String libelle, boolean demi, List<AncetreDto> ancetresCommuns,
            String message, boolean infereParRaisonneur) {
    }

    public record CompletudeDto(Completude personne1, Completude personne2, boolean complet,
            List<Manquant> manquants) {
    }
}
