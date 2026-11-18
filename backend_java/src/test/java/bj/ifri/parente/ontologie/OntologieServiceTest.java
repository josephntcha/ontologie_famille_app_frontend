package bj.ifri.parente.ontologie;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.OntologieAttendue;

/**
 * OntologieService : signature de la TBox lue par la OWL API (SPEC_API_JAVA.md D.3 et F).
 */
@DisplayName("OntologieService")
class OntologieServiceTest extends BaseTestApi {

    @Autowired
    private OntologieService ontologie;

    @Autowired
    private RaisonneurService raisonneur;

    private static OWLOntology lireFichierOwl() throws Exception {
        try (InputStream in = OntologieServiceTest.class.getResourceAsStream("/ontologie/famille.owl")) {
            assertThat(in).as("famille.owl attendu sur le classpath").isNotNull();
            return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(in);
        }
    }

    @Test
    @DisplayName("namespace")
    void namespace() {
        assertThat(ontologie.namespace()).isEqualTo(NS);
    }

    @Test
    @DisplayName("classes : les 39 classes nommées, triées")
    void classesExactesEtTriees() {
        assertThat(ontologie.classes()).hasSize(39).containsExactlyElementsOf(OntologieAttendue.CLASSES);
    }

    @Test
    @DisplayName("proprietesObjet : les 22 propriétés objet, triées")
    void proprietesObjetExactesEtTriees() {
        assertThat(ontologie.proprietesObjet()).hasSize(22).containsExactlyElementsOf(OntologieAttendue.PROPRIETES_OBJET);
    }

    @Test
    @DisplayName("proprietesDonnees : les 7 propriétés de données, triées")
    void proprietesDonneesExactesEtTriees() {
        assertThat(ontologie.proprietesDonnees()).hasSize(7).containsExactlyElementsOf(OntologieAttendue.PROPRIETES_DONNEES);
    }

    @Test
    @DisplayName("tbox : axiomes du fichier famille.owl, dont la transitivité de ancetreDe")
    void tboxContientLesAxiomesDuFichier() throws Exception {
        OWLOntology tbox = ontologie.tbox();
        OWLDataFactory df = OWLManager.getOWLDataFactory();

        assertThat(tbox).isNotNull();
        assertThat(tbox.containsAxiom(df.getOWLTransitiveObjectPropertyAxiom(
                df.getOWLObjectProperty(IRI.create(NS + "ancetreDe"))))).isTrue();

        Set<OWLAxiom> attendus = lireFichierOwl().logicalAxioms().collect(Collectors.toSet());
        Set<OWLAxiom> presents = tbox.logicalAxioms().collect(Collectors.toSet());
        assertThat(presents).containsAll(attendus);
    }

    @Test
    @DisplayName("tbox : ne contient aucun individu, même après un raisonnement sur des données")
    void tboxSansIndividuApresRaisonnement() {
        FamilleDemo f = familleDemo();
        raisonneur.classesPersonne(f.kid());

        assertThat(ontologie.tbox().individualsInSignature().count()).isZero();
        assertThat(ontologie.tbox().getABoxAxioms(org.semanticweb.owlapi.model.parameters.Imports.INCLUDED)).isEmpty();
    }
}
