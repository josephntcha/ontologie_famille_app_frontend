package bj.ifri.parente.web.dto;

import java.util.List;

public record CreationPersonneReponse(PersonneDto data, List<PersonneDto> doublonsProbables) {
}
