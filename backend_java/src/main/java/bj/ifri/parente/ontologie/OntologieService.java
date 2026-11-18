package bj.ifri.parente.ontologie;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * TBox {@code famille.owl}, chargée une seule fois au démarrage par la OWL API (elle est
 * immuable). Elle n'est jamais modifiée : chaque raisonnement travaille sur une ontologie ABox
 * distincte, avec son propre gestionnaire (voir {@link ConstructeurABox}).
 */
@Service
public class OntologieService {

    public static final String RESSOURCE = "ontologie/famille.owl";

    private final OWLOntology tbox;
    private final String namespace;

    public OntologieService() {
        try (InputStream in = new ClassPathResource(RESSOURCE).getInputStream()) {
            this.tbox = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(in);
        } catch (IOException | OWLOntologyCreationException e) {
            throw new IllegalStateException("Impossible de charger la TBox " + RESSOURCE, e);
        }
        IRI iri = tbox.getOntologyID().getOntologyIRI()
                .orElseThrow(() -> new IllegalStateException("La TBox n'a pas d'IRI d'ontologie"));
        this.namespace = iri.toString() + "#";
    }

    /** Espace de noms de l'ontologie (préfixe f:). */
    public String namespace() {
        return namespace;
    }

    /** Classes nommées déclarées dans la signature de la TBox (noms locaux triés). */
    public List<String> classes() {
        return nomsLocaux(tbox.classesInSignature());
    }

    public List<String> proprietesObjet() {
        return nomsLocaux(tbox.objectPropertiesInSignature());
    }

    public List<String> proprietesDonnees() {
        return nomsLocaux(tbox.dataPropertiesInSignature());
    }

    /** TBox lue par la OWL API (à ne jamais modifier). */
    public OWLOntology tbox() {
        return tbox;
    }

    /** Nom local d'une IRI de l'espace de noms, ou null si elle est hors de l'ontologie. */
    public String nomLocal(IRI iri) {
        String s = iri.toString();
        return s.startsWith(namespace) && s.length() > namespace.length() ? s.substring(namespace.length()) : null;
    }

    private List<String> nomsLocaux(Stream<? extends OWLEntity> entites) {
        return entites.map(e -> nomLocal(e.getIRI()))
                .filter(n -> n != null)
                .distinct()
                .sorted()
                .toList();
    }
}
