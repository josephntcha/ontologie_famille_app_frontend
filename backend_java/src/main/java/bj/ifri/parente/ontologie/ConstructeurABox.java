package bj.ifri.parente.ontologie;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.springframework.stereotype.Component;

import bj.ifri.parente.ontologie.AboxAssertee.Donnee;
import bj.ifri.parente.ontologie.AboxAssertee.Individu;
import bj.ifri.parente.ontologie.AboxAssertee.Lien;

/**
 * Construit, pour un raisonnement, une ontologie neuve (gestionnaire OWL API dédié) qui
 * contient une copie des axiomes de la TBox et les assertions de l'ABox, plus
 * DifferentIndividuals sur tous les individus. La TBox partagée n'est jamais modifiée.
 */
@Component
public class ConstructeurABox {

    private final OntologieService ontologie;

    public ConstructeurABox(OntologieService ontologie) {
        this.ontologie = ontologie;
    }

    public OWLOntology construire(AboxAssertee abox) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = manager.getOWLDataFactory();
        String ns = ontologie.namespace();
        OWLOntology o;
        try {
            o = manager.createOntology(IRI.create(ns.substring(0, ns.length() - 1) + "/abox"));
        } catch (OWLOntologyCreationException e) {
            throw new IllegalStateException("Création de l'ontologie ABox impossible", e);
        }

        List<OWLAxiom> axiomes = new ArrayList<>();
        ontologie.tbox().axioms().forEach(axiomes::add);

        List<OWLNamedIndividual> tous = new ArrayList<>();
        for (Individu individu : abox.individus()) {
            OWLNamedIndividual ind = df.getOWLNamedIndividual(IRI.create(ns + individu.nom()));
            tous.add(ind);
            axiomes.add(df.getOWLDeclarationAxiom(ind));
            for (String classe : individu.classes()) {
                axiomes.add(df.getOWLClassAssertionAxiom(df.getOWLClass(IRI.create(ns + classe)), ind));
            }
            for (Donnee d : individu.donnees()) {
                axiomes.add(df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(IRI.create(ns + d.propriete())),
                        ind, litteral(df, d)));
            }
            for (Lien l : individu.liens()) {
                axiomes.add(df.getOWLObjectPropertyAssertionAxiom(
                        df.getOWLObjectProperty(IRI.create(ns + l.propriete())), ind,
                        df.getOWLNamedIndividual(IRI.create(ns + l.cible()))));
            }
        }
        if (tous.size() >= 2) {
            axiomes.add(df.getOWLDifferentIndividualsAxiom(tous));
        }
        o.addAxioms(axiomes);
        return o;
    }

    private static OWLLiteral litteral(OWLDataFactory df, Donnee d) {
        return switch (d.type()) {
            case STRING -> df.getOWLLiteral(d.valeur(), OWL2Datatype.XSD_STRING);
            case INT -> df.getOWLLiteral(d.valeur(), OWL2Datatype.XSD_INT);
            case DATE_TIME -> df.getOWLLiteral(d.valeur(), OWL2Datatype.XSD_DATE_TIME);
        };
    }
}
