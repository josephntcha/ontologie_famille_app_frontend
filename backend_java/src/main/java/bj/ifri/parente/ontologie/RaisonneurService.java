package bj.ifri.parente.ontologie;

import java.util.List;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.erreur.RessourceIntrouvable;
import bj.ifri.parente.service.ChargeurGraphe;
import bj.ifri.parente.service.GrapheFamille;

/**
 * Raisonnement OWL avec HermiT sur la TBox famille.owl et l'ABox reconstruite depuis la base à
 * chaque appel (aucun cache entre deux requêtes). Une requête HTTP ouvre au plus une
 * {@link SessionRaisonnement} (un raisonneur), qu'elle ferme à la fin.
 */
@Service
@Transactional(readOnly = true)
public class RaisonneurService {

    public static final String NOM = "HermiT";

    private final OWLReasonerFactory fabrique = new ReasonerFactory();
    private final OntologieService ontologie;
    private final ChargeurGraphe chargeur;
    private final ProjectionABox projection;
    private final ConstructeurABox constructeur;

    public RaisonneurService(OntologieService ontologie, ChargeurGraphe chargeur, ProjectionABox projection,
            ConstructeurABox constructeur) {
        this.ontologie = ontologie;
        this.chargeur = chargeur;
        this.projection = projection;
        this.constructeur = constructeur;
    }

    /** Ouvre un raisonneur HermiT sur l'état courant de la base. */
    public SessionRaisonnement ouvrir() {
        return ouvrir(chargeur.charger());
    }

    /** Ouvre un raisonneur HermiT sur un instantané déjà chargé dans la requête. */
    public SessionRaisonnement ouvrir(GrapheFamille graphe) {
        OWLOntology abox = constructeur.construire(projection.projeter(graphe));
        OWLReasoner reasoner = fabrique.createReasoner(abox);
        return new SessionRaisonnement(reasoner, abox.getOWLOntologyManager().getOWLDataFactory(), ontologie);
    }

    public List<String> classesPersonne(Long personneId) {
        GrapheFamille graphe = chargeur.charger();
        if (!graphe.existe(personneId)) {
            throw RessourceIntrouvable.personne(personneId);
        }
        try (SessionRaisonnement session = ouvrir(graphe)) {
            return session.classesPersonne(personneId);
        }
    }

    public List<String> classesMariage(Long mariageId) {
        GrapheFamille graphe = chargeur.charger();
        if (graphe.mariages().stream().noneMatch(m -> m.getId().equals(mariageId))) {
            throw RessourceIntrouvable.mariage(mariageId);
        }
        try (SessionRaisonnement session = ouvrir(graphe)) {
            return session.classesMariage(mariageId);
        }
    }

    public boolean lienDeSangInfere(Long personne1Id, Long personne2Id) {
        try (SessionRaisonnement session = ouvrir()) {
            return session.lienDeSangInfere(personne1Id, personne2Id);
        }
    }

    /** Cohérence (isConsistent) et classes insatisfiables (getUnsatisfiableClasses). */
    public Coherence verifierCoherence() {
        try (SessionRaisonnement session = ouvrir()) {
            if (session.estCoherente()) {
                return new Coherence(true, session.classesInsatisfiables());
            }
        }
        // ABox incohérente : les classes insatisfiables se lisent sur la TBox seule.
        try (SessionRaisonnement tboxSeule = new SessionRaisonnement(fabrique.createReasoner(copieTbox()),
                OWLManager.getOWLDataFactory(), ontologie)) {
            return new Coherence(false, tboxSeule.estCoherente() ? tboxSeule.classesInsatisfiables() : List.of());
        }
    }

    private OWLOntology copieTbox() {
        try {
            OWLOntology o = OWLManager.createOWLOntologyManager().createOntology();
            o.addAxioms(ontologie.tbox().axioms());
            return o;
        } catch (OWLOntologyCreationException e) {
            throw new IllegalStateException(e);
        }
    }
}
