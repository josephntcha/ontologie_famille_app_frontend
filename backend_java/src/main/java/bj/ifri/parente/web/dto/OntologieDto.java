package bj.ifri.parente.web.dto;

import java.util.List;

public record OntologieDto(String namespace, List<String> classes, List<String> proprietesObjet,
        List<String> proprietesDonnees) {
}
