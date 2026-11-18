package bj.ifri.parente.service;

import java.util.List;

/** Verdict global sur le mariage, règles appliquées et avertissement. */
public record VerdictMariage(String verdict, List<RegleMariage> regles, String avertissement) {
}
