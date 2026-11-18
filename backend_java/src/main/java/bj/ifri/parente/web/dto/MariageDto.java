package bj.ifri.parente.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Format JSON d'un mariage ; {@code classes} inférées par HermiT. */
public record MariageDto(Long id, Long conjoint1Id, Long conjoint2Id, LocalDate dateDebut, LocalDate dateFin,
        String causeDissolution, boolean enCours, List<String> classes) {
}
