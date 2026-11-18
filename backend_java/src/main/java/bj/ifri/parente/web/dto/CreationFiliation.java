package bj.ifri.parente.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreationFiliation(
        @NotNull(message = "Le parent est requis.") Long parentId,
        @NotNull(message = "L'enfant est requis.") Long enfantId) {
}
