package bj.ifri.parente.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreationBeauParente(
        @NotNull(message = "Le beau-parent est requis.") Long beauParentId,
        @NotNull(message = "L'enfant est requis.") Long enfantId) {
}
