package bj.ifri.parente.domaine;

/** Cause de dissolution d'un mariage (valeurs de f:causeDissolution). */
public enum CauseDissolution {
    deces,
    divorce;

    /** Cause à partir de sa valeur textuelle, ou null si elle est inconnue. */
    public static CauseDissolution depuis(String valeur) {
        for (CauseDissolution c : values()) {
            if (c.name().equals(valeur)) {
                return c;
            }
        }
        return null;
    }
}
