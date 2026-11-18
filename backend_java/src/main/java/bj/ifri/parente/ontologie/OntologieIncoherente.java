package bj.ifri.parente.ontologie;

/** Les données rendent l'ontologie incohérente : HermiT ne peut pas classer les individus. */
public class OntologieIncoherente extends RuntimeException {

    public OntologieIncoherente() {
        super("Les données rendent l'ontologie incohérente : classes inférées indisponibles "
                + "(voir GET /api/raisonneur/coherence).");
    }
}
