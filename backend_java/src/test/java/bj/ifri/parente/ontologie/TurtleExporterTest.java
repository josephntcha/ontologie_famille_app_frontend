package bj.ifri.parente.ontologie;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.TripletsOwl;

/**
 * TurtleExporter.exporter() : ABox assertée au format Turtle (SPEC_API.md 9, SPEC_API_JAVA.md D.4).
 * L'export est relu avec la OWL API, puis les triplets sont vérifiés.
 */
@DisplayName("TurtleExporter")
class TurtleExporterTest extends BaseTestApi {

    private static final String XSD_DATETIME = OWL2Datatype.XSD_DATE_TIME.getIRI().toString();
    private static final String XSD_INT = OWL2Datatype.XSD_INT.getIRI().toString();

    @Autowired
    private TurtleExporter exporter;

    @Autowired
    private OntologieService ontologie;

    private static String p(Long id) {
        return NS + "personne_" + id;
    }

    private static String m(Long id) {
        return NS + "mariage_" + id;
    }

    private static String f(String local) {
        return NS + local;
    }

    private static List<String> valeurs(List<OWLLiteral> litteraux) {
        return litteraux.stream().map(OWLLiteral::getLiteral).toList();
    }

    @Test
    @DisplayName("L'export d'une base vide est un Turtle valide")
    void exportDUneBaseVide() {
        TripletsOwl t = TripletsOwl.lire(exporter.exporter());

        assertThat(t.format().getKey()).containsIgnoringCase("turtle");
        assertThat(t.ontologie().individualsInSignature().count()).isZero();
    }

    @Test
    @DisplayName("Chaque personne : NamedIndividual, Personne, sexe, nom, prénom, dateNaissance, age")
    void tripletsDUnePersonne() {
        FamilleDemo fam = familleDemo();

        TripletsOwl t = TripletsOwl.lire(exporter.exporter());
        String kid = p(fam.kid());

        assertThat(t.objetsIri(kid, TripletsOwl.RDF_TYPE))
                .contains(TripletsOwl.OWL_NAMED_INDIVIDUAL, f("Personne"), f("Feminin"))
                .doesNotContain(f("Masculin"));
        assertThat(t.objetsIri(p(fam.papa()), TripletsOwl.RDF_TYPE)).contains(f("Personne"), f("Masculin"));
        assertThat(valeurs(t.litteraux(kid, f("nom")))).containsExactly("Martin");
        assertThat(valeurs(t.litteraux(kid, f("prenom")))).containsExactly("Kid");

        List<OWLLiteral> naissance = t.litteraux(kid, f("dateNaissance"));
        assertThat(naissance).hasSize(1);
        assertThat(naissance.get(0).getLiteral()).isEqualTo("1990-04-12T00:00:00");
        assertThat(naissance.get(0).getDatatype().getIRI().toString()).isEqualTo(XSD_DATETIME);

        List<OWLLiteral> age = t.litteraux(kid, f("age"));
        assertThat(age).hasSize(1);
        assertThat(age.get(0).getLiteral()).isEqualTo("36");
        assertThat(age.get(0).getDatatype().getIRI().toString()).isEqualTo(XSD_INT);
    }

    @Test
    @DisplayName("Liens parentDe, beauParentDe, frereDe / soeurDe déduits et marieA dans les deux sens")
    void liensEntrePersonnes() {
        FamilleDemo fam = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, fam.kid());

        TripletsOwl t = TripletsOwl.lire(exporter.exporter());

        assertThat(t.contient(p(fam.papa()), f("parentDe"), p(fam.kid()))).isTrue();
        assertThat(t.contient(p(fam.maman()), f("parentDe"), p(fam.demi()))).isTrue();
        assertThat(t.contient(p(fam.grandma()), f("parentDe"), p(fam.tonton()))).isTrue();
        assertThat(t.contient(p(beaupere), f("beauParentDe"), p(fam.kid()))).isTrue();
        assertThat(t.contient(p(beaupere), f("parentDe"), p(fam.kid()))).isFalse();

        // frereDe si le sujet est masculin, soeurDe s'il est féminin (parents communs, demi compris).
        assertThat(t.contient(p(fam.demi()), f("frereDe"), p(fam.kid()))).isTrue();
        assertThat(t.contient(p(fam.kid()), f("soeurDe"), p(fam.demi()))).isTrue();
        assertThat(t.contient(p(fam.papa()), f("frereDe"), p(fam.tonton()))).isTrue();
        assertThat(t.contient(p(fam.tonton()), f("frereDe"), p(fam.papa()))).isTrue();
        assertThat(t.objetsIri(p(fam.kid()), f("soeurDe"))).containsExactly(p(fam.demi()));
        assertThat(t.objetsIri(p(fam.cousin()), f("frereDe"))).isEmpty();

        // marieA dans les deux sens, mariage en cours ou dissous.
        assertThat(t.contient(p(fam.papa()), f("marieA"), p(fam.maman()))).isTrue();
        assertThat(t.contient(p(fam.maman()), f("marieA"), p(fam.papa()))).isTrue();
        assertThat(t.contient(p(fam.tonton()), f("marieA"), p(fam.tantine()))).isTrue();
        assertThat(t.contient(p(fam.tantine()), f("marieA"), p(fam.tonton()))).isTrue();
    }

    @Test
    @DisplayName("Individus Mariage : conjoints, dateDebut, et dateFin / causeDissolution si dissous")
    void individusMariage() {
        FamilleDemo fam = familleDemo();

        TripletsOwl t = TripletsOwl.lire(exporter.exporter());
        String enCours = m(fam.mariagePapaMaman());
        String divorce = m(fam.mariageTontonTantine());

        assertThat(t.objetsIri(enCours, TripletsOwl.RDF_TYPE)).contains(TripletsOwl.OWL_NAMED_INDIVIDUAL, f("Mariage"));
        assertThat(t.objetsIri(enCours, f("aPourConjoint"))).containsExactlyInAnyOrder(p(fam.papa()), p(fam.maman()));
        List<OWLLiteral> debut = t.litteraux(enCours, f("dateDebut"));
        assertThat(debut).hasSize(1);
        assertThat(debut.get(0).getLiteral()).startsWith("1988-06-01");
        assertThat(debut.get(0).getDatatype().getIRI().toString()).isEqualTo(XSD_DATETIME);
        assertThat(t.litteraux(enCours, f("dateFin"))).isEmpty();
        assertThat(t.litteraux(enCours, f("causeDissolution"))).isEmpty();

        assertThat(t.objetsIri(divorce, f("aPourConjoint"))).containsExactlyInAnyOrder(p(fam.tonton()), p(fam.tantine()));
        List<OWLLiteral> fin = t.litteraux(divorce, f("dateFin"));
        assertThat(fin).hasSize(1);
        assertThat(fin.get(0).getLiteral()).startsWith("2010-03-15");
        assertThat(fin.get(0).getDatatype().getIRI().toString()).isEqualTo(XSD_DATETIME);
        assertThat(valeurs(t.litteraux(divorce, f("causeDissolution")))).containsExactly("divorce");
    }

    @Test
    @DisplayName("Tous les termes f: utilisés (hors individus) existent dans la TBox")
    void termesExistantsDansLaTbox() {
        FamilleDemo fam = familleDemo();
        beauParent(fam.papa(), fam.demi());
        dissoudre(fam.mariagePapaMaman(), "2020-01-10", "deces");

        TripletsOwl t = TripletsOwl.lire(exporter.exporter());

        OWLOntology tbox = ontologie.tbox();
        Set<String> signatureTbox = new HashSet<>();
        Stream.of(tbox.classesInSignature(), tbox.objectPropertiesInSignature(), tbox.dataPropertiesInSignature())
                .flatMap(s -> s.map(OWLEntity::getIRI))
                .forEach(i -> signatureTbox.add(i.toString()));

        Set<String> utilises = new HashSet<>(t.irisClassesEtProprietes());
        utilises.addAll(t.predicats());
        List<String> termesF = utilises.stream().filter(i -> i.startsWith(NS)).sorted().toList();

        assertThat(termesF).isNotEmpty();
        assertThat(signatureTbox).containsAll(termesF);
    }

    @Test
    @DisplayName("ABox assertée seulement : aucune classe ni propriété inférée")
    void sansInferences() {
        FamilleDemo fam = familleDemo();

        TripletsOwl t = TripletsOwl.lire(exporter.exporter());

        List<String> typesF = t.objetsIri(p(fam.kid()), TripletsOwl.RDF_TYPE).stream().filter(i -> i.startsWith(NS)).toList();
        assertThat(typesF).containsExactlyInAnyOrder(f("Personne"), f("Feminin"));
        assertThat(t.objetsIri(m(fam.mariageTontonTantine()), TripletsOwl.RDF_TYPE)).doesNotContain(f("MariageDissous"));
        assertThat(t.predicats()).doesNotContain(f("lienDeSangAvec"), f("ancetreDe"), f("descendantDe"), f("enfantDe"));
    }
}
