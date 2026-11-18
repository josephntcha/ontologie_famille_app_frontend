package bj.ifri.parente.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * Relit un export Turtle avec la OWL API et le présente comme un ensemble de triplets
 * (sujet, prédicat, objet), indépendamment de la mise en forme du document.
 *
 * Selon que l'export déclare ou non ses propriétés, la OWL API produit des assertions de
 * propriétés objet / de données ou des assertions d'annotation : les deux sont acceptées,
 * seuls les triplets comptent. Un objet est soit une IRI ({@link IRI}), soit un {@link OWLLiteral}.
 */
public final class TripletsOwl {

    public static final String RDF_TYPE = OWLRDFVocabulary.RDF_TYPE.getIRI().toString();
    public static final String OWL_NAMED_INDIVIDUAL = OWLRDFVocabulary.OWL_NAMED_INDIVIDUAL.getIRI().toString();

    /** Triplet relu ; objet = IRI ou OWLLiteral. */
    public record Triplet(String sujet, String predicat, Object objet) {
    }

    private final OWLOntology ontologie;
    private final OWLDocumentFormat format;
    private final List<Triplet> triplets = new ArrayList<>();

    private TripletsOwl(OWLOntology ontologie, OWLDocumentFormat format) {
        this.ontologie = ontologie;
        this.format = format;
        ontologie.axioms().forEach(ax -> {
            if (ax instanceof OWLClassAssertionAxiom ca && ca.getIndividual().isNamed() && ca.getClassExpression().isNamed()) {
                ajouter(ca.getIndividual().asOWLNamedIndividual().getIRI(), RDF_TYPE,
                        ca.getClassExpression().asOWLClass().getIRI());
            } else if (ax instanceof OWLDeclarationAxiom d && d.getEntity().isOWLNamedIndividual()) {
                ajouter(d.getEntity().getIRI(), RDF_TYPE, IRI.create(OWL_NAMED_INDIVIDUAL));
            } else if (ax instanceof OWLObjectPropertyAssertionAxiom op && op.getSubject().isNamed() && op.getObject().isNamed()
                    && op.getProperty().isNamed()) {
                ajouter(op.getSubject().asOWLNamedIndividual().getIRI(),
                        op.getProperty().asOWLObjectProperty().getIRI().toString(),
                        op.getObject().asOWLNamedIndividual().getIRI());
            } else if (ax instanceof OWLDataPropertyAssertionAxiom dp && dp.getSubject().isNamed()) {
                ajouter(dp.getSubject().asOWLNamedIndividual().getIRI(),
                        dp.getProperty().asOWLDataProperty().getIRI().toString(), dp.getObject());
            } else if (ax instanceof OWLAnnotationAssertionAxiom an && an.getSubject().isIRI()) {
                Object valeur = an.getValue().isIRI() ? an.getValue().asIRI().get()
                        : an.getValue().asLiteral().orElse(null);
                ajouter((IRI) an.getSubject(), an.getProperty().getIRI().toString(), valeur);
            }
        });
    }

    private void ajouter(IRI sujet, String predicat, Object objet) {
        triplets.add(new Triplet(sujet.toString(), predicat, objet));
    }

    /** Analyse le Turtle avec la OWL API (lève une exception si le document est invalide). */
    public static TripletsOwl lire(String turtle) {
        try {
            OWLOntologyManager man = OWLManager.createOWLOntologyManager();
            OWLOntology o = man.loadOntologyFromOntologyDocument(new StringDocumentSource(turtle));
            return new TripletsOwl(o, man.getOntologyFormat(o));
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError("Turtle illisible par la OWL API : " + e.getMessage() + "\n" + turtle, e);
        }
    }

    public OWLOntology ontologie() {
        return ontologie;
    }

    public OWLDocumentFormat format() {
        return format;
    }

    public List<Triplet> triplets() {
        return triplets;
    }

    /** Vrai si le triplet (sujet, prédicat, objet IRI) est présent. */
    public boolean contient(String sujet, String predicat, String objetIri) {
        return objetsIri(sujet, predicat).contains(objetIri);
    }

    /** Objets IRI des triplets (sujet, prédicat, ?). */
    public List<String> objetsIri(String sujet, String predicat) {
        return objets(sujet, predicat).filter(o -> o instanceof IRI).map(Object::toString).toList();
    }

    /** Objets littéraux des triplets (sujet, prédicat, ?). */
    public List<OWLLiteral> litteraux(String sujet, String predicat) {
        return objets(sujet, predicat).filter(o -> o instanceof OWLLiteral).map(o -> (OWLLiteral) o).toList();
    }

    private Stream<Object> objets(String sujet, String predicat) {
        return triplets.stream()
                .filter(t -> t.sujet().equals(sujet) && t.predicat().equals(predicat))
                .map(Triplet::objet)
                .filter(Objects::nonNull);
    }

    /** Prédicats utilisés (IRI complètes), sans doublon. */
    public Set<String> predicats() {
        Set<String> r = new HashSet<>();
        triplets.forEach(t -> r.add(t.predicat()));
        return r;
    }

    /**
     * IRI de toutes les classes et propriétés (objet, données, annotation) du document, ainsi
     * que des classes utilisées comme objet de rdf:type.
     */
    public Set<String> irisClassesEtProprietes() {
        Set<String> r = new HashSet<>();
        Stream.of(ontologie.classesInSignature(), ontologie.objectPropertiesInSignature(),
                        ontologie.dataPropertiesInSignature(), ontologie.annotationPropertiesInSignature())
                .flatMap(s -> s.map(OWLEntity::getIRI))
                .forEach(i -> r.add(i.toString()));
        triplets.stream().filter(t -> t.predicat().equals(RDF_TYPE) && t.objet() instanceof IRI)
                .forEach(t -> r.add(t.objet().toString()));
        return r;
    }
}
