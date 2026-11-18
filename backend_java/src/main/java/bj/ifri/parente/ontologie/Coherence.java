package bj.ifri.parente.ontologie;

import java.util.List;

/** Cohérence de TBox + ABox selon HermiT, et classes insatisfiables (hors owl:Nothing). */
public record Coherence(boolean coherente, List<String> classesInsatisfiables) {
}
