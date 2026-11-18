package bj.ifri.parente.support;

/**
 * Identifiants de la famille de démonstration créée par {@link BaseTestApi#familleDemo()}.
 *
 * <pre>
 *   Grandpa (M, 1930-03-10) ── Grandma (F, 1932-07-21)          Autre (M, 1955-11-30)
 *                │   (non mariés entre eux)                          │
 *        ┌───────┴──────────────┐                                    │
 *   Papa (M, 1958-05-02)   Tonton (M, 1960-09-15)                    │
 *     ║ marié le 1988-06-01   ║ marié le 1989-09-09,                 │
 *     ║ (en cours)            ║ divorcé le 2010-03-15                │
 *   Maman (F, 1962-02-14)  Tantine (F, 1963-12-01)                   │
 *     │         └─────────────┼──────────────────────────────────────┤
 *     │                       │                                      │
 *   Kid (F, 1990-04-12)    Cousin (M, 1991-10-05)          Demi (M, 1982-08-30)
 *   enfant de Papa+Maman   enfant de Tonton+Tantine        enfant de Maman+Autre
 * </pre>
 *
 * Kid et Cousin : cousins germains (Grandpa et Grandma) ; Kid et Demi : demi-frère et
 * demi-sœur (Maman) ; Papa et Tonton : frères ; Tonton et Kid : oncle et nièce.
 * Maman, Tantine, Autre, Grandpa et Grandma n'ont aucun parent connu.
 */
public record FamilleDemo(
        Long grandpa, Long grandma, Long papa, Long tonton, Long maman,
        Long autre, Long demi, Long tantine, Long kid, Long cousin,
        Long mariagePapaMaman, Long mariageTontonTantine) {
}
