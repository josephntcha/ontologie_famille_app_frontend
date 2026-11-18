package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLLiteral;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import bj.ifri.parente.support.TripletsOwl;

/**
 * GET /api/export/turtle (SPEC_API.md 9, SPEC_API_JAVA.md D.4) : relu par la OWL API.
 */
@DisplayName("API /api/export/turtle")
class ExportTurtleApiTest extends BaseTestApi {

    private Reponse exporter() {
        Reponse r = get("/api/export/turtle");
        assertStatut(r, 200);
        return r;
    }

    private static String p(Long id) {
        return NS + "personne_" + id;
    }

    private static List<String> valeurs(List<OWLLiteral> litteraux) {
        return litteraux.stream().map(OWLLiteral::getLiteral).toList();
    }

    @Test
    @DisplayName("Content-Type text/turtle et préfixes f, rdf, rdfs, xsd, owl")
    void contentTypeEtPrefixes() {
        Reponse r = exporter();

        assertThat(r.typeContenu()).startsWith("text/turtle");
        assertThat(r.corps()).containsPattern("@prefix\\s+f:\\s*<" + Pattern.quote(NS) + ">\\s*\\.");
        for (String prefixe : List.of("rdf", "rdfs", "xsd", "owl")) {
            assertThat(r.corps()).as("préfixe %s", prefixe).containsPattern("@prefix\\s+" + prefixe + ":\\s*<");
        }
        TripletsOwl.lire(r.corps());
    }

    @Test
    @DisplayName("Le document est relu par la OWL API et contient la famille")
    void exportReluParLaOwlApi() {
        FamilleDemo f = familleDemo();

        TripletsOwl t = TripletsOwl.lire(exporter().corps());

        assertThat(t.format().getKey()).containsIgnoringCase("turtle");
        assertThat(t.objetsIri(p(f.cousin()), TripletsOwl.RDF_TYPE)).contains(NS + "Personne", NS + "Masculin");
        assertThat(valeurs(t.litteraux(p(f.cousin()), NS + "prenom"))).containsExactly("Cousin");
        assertThat(t.contient(p(f.tonton()), NS + "parentDe", p(f.cousin()))).isTrue();
        assertThat(t.contient(p(f.demi()), NS + "frereDe", p(f.kid()))).isTrue();
        assertThat(t.objetsIri(NS + "mariage_" + f.mariagePapaMaman(), TripletsOwl.RDF_TYPE)).contains(NS + "Mariage");
    }

    @Test
    @DisplayName("Les chaînes sont échappées (guillemet, antislash)")
    void chainesEchappees() {
        Long id = personne("Jean\\Baptiste", "M", "1980-01-01", "D\"Arc");

        TripletsOwl t = TripletsOwl.lire(exporter().corps());

        assertThat(valeurs(t.litteraux(p(id), NS + "nom"))).containsExactly("D\"Arc");
        assertThat(valeurs(t.litteraux(p(id), NS + "prenom"))).containsExactly("Jean\\Baptiste");
    }

    @Test
    @DisplayName("Une dissolution enregistrée par PATCH apparaît dans l'export suivant (pas de cache)")
    void dissolutionVisibleDansLExport() {
        FamilleDemo f = familleDemo();
        String mariage = NS + "mariage_" + f.mariagePapaMaman();
        assertThat(TripletsOwl.lire(exporter().corps()).litteraux(mariage, NS + "dateFin")).isEmpty();

        dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

        TripletsOwl t = TripletsOwl.lire(exporter().corps());
        assertThat(valeurs(t.litteraux(mariage, NS + "causeDissolution"))).containsExactly("deces");
        assertThat(valeurs(t.litteraux(mariage, NS + "dateFin"))).singleElement().asString().startsWith("2020-01-10");
        assertThat(valeurs(t.litteraux(NS + "mariage_" + f.mariageTontonTantine(), NS + "causeDissolution")))
                .containsExactly("divorce");
    }
}
