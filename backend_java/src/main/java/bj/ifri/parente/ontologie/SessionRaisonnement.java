package bj.ifri.parente.ontologie;

import java.util.List;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * Un raisonneur HermiT ouvert sur TBox + ABox, pour la durée d'une requête HTTP. Toutes les
 * questions d'une même requête (classes de plusieurs individus, lienDeSangAvec) réutilisent la
 * même instance ; {@link #close()} libère le raisonneur ({@code dispose()}).
 */
public final class SessionRaisonnement implements AutoCloseable {

    private final OWLReasoner reasoner;
    private final OWLDataFactory df;
    private final OntologieService ontologie;
    private Boolean coherente;

    SessionRaisonnement(OWLReasoner reasoner, OWLDataFactory df, OntologieService ontologie) {
        this.reasoner = reasoner;
        this.df = df;
        this.ontologie = ontologie;
    }

    private OWLNamedIndividual individu(String nomLocal) {
        return df.getOWLNamedIndividual(IRI.create(ontologie.namespace() + nomLocal));
    }

    public boolean estCoherente() {
        if (coherente == null) {
            coherente = reasoner.isConsistent();
        }
        return coherente;
    }

    /** Calcule en une fois les types de tous les individus (utile pour l'arbre ou une liste). */
    public void precalculerTypes() {
        if (estCoherente()) {
            reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        }
    }

    /** Classes nommées de l'ontologie (types directs et indirects inférés), sans owl:Thing, triées. */
    public List<String> classesPersonne(Long personneId) {
        return classes(ProjectionABox.individuPersonne(personneId));
    }

    public List<String> classesMariage(Long mariageId) {
        return classes(ProjectionABox.individuMariage(mariageId));
    }

    private List<String> classes(String nomLocal) {
        if (!estCoherente()) {
            throw new OntologieIncoherente();
        }
        return reasoner.getTypes(individu(nomLocal), false).entities()
                .filter(c -> !c.isOWLThing() && !c.isOWLNothing())
                .map(c -> ontologie.nomLocal(c.getIRI()))
                .filter(n -> n != null)
                .distinct()
                .sorted()
                .toList();
    }

    /** Vrai si HermiT infère f:lienDeSangAvec(p1, p2) ; toujours faux pour p1 = p2. */
    public boolean lienDeSangInfere(Long personne1Id, Long personne2Id) {
        if (personne1Id.equals(personne2Id)) {
            return false;
        }
        if (!estCoherente()) {
            throw new OntologieIncoherente();
        }
        OWLObjectProperty lienDeSang = df.getOWLObjectProperty(IRI.create(ontologie.namespace() + "lienDeSangAvec"));
        return reasoner.isEntailed(df.getOWLObjectPropertyAssertionAxiom(lienDeSang,
                individu(ProjectionABox.individuPersonne(personne1Id)),
                individu(ProjectionABox.individuPersonne(personne2Id))));
    }

    /** Classes nommées insatisfiables (hors owl:Nothing), triées ; suppose l'ontologie cohérente. */
    List<String> classesInsatisfiables() {
        return reasoner.getUnsatisfiableClasses().entities()
                .filter(c -> !c.isOWLNothing())
                .map(OWLClass::getIRI)
                .map(iri -> {
                    String n = ontologie.nomLocal(iri);
                    return n != null ? n : iri.toString();
                })
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public void close() {
        reasoner.dispose();
    }
}
