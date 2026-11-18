package bj.ifri.parente.web.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupeDoublonsDto(String nom, LocalDate dateNaissance, List<PersonneDto> personnes) {
}
