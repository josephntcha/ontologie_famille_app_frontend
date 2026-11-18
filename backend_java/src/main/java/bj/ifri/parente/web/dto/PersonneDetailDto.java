package bj.ifri.parente.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Personne et ses relations ; {@code classes} inférées par HermiT. */
public record PersonneDetailDto(Long id, String nom, String prenom, String sexe, LocalDate dateNaissance, int age,
        String uri, List<PersonneDto> parents, List<PersonneDto> enfants, List<PersonneDto> beauxParents,
        List<PersonneDto> beauxEnfants, List<PersonneDto> freres, List<PersonneDto> soeurs,
        List<PersonneDto> conjoints, List<MariageDto> mariages, List<String> classes) {
}
