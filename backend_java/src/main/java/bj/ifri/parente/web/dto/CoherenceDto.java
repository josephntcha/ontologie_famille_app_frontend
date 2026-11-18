package bj.ifri.parente.web.dto;

import java.util.List;

public record CoherenceDto(String raisonneur, boolean coherente, List<String> classesInsatisfiables) {
}
