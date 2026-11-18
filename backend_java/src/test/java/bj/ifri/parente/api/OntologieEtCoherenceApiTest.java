package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.OntologieAttendue;
import bj.ifri.parente.support.Reponse;

/**
 * GET /api/ontologie (SPEC_API.md 8, SPEC_API_JAVA.md D.3) et
 * GET /api/raisonneur/coherence (SPEC_API_JAVA.md D.2).
 */
@DisplayName("API /api/ontologie et /api/raisonneur/coherence")
class OntologieEtCoherenceApiTest extends BaseTestApi {

    @Test
    @DisplayName("GET /api/ontologie : namespace et listes exactes, triées, sans doublon")
    void ontologie() {
        Reponse r = get("/api/ontologie");

        assertStatut(r, 200);
        assertThat(r.texte("/namespace")).isEqualTo(NS);
        assertThat(chaines(r.at("/classes"))).containsExactlyElementsOf(OntologieAttendue.CLASSES);
        assertThat(chaines(r.at("/proprietes_objet"))).containsExactlyElementsOf(OntologieAttendue.PROPRIETES_OBJET);
        assertThat(chaines(r.at("/proprietes_donnees"))).containsExactlyElementsOf(OntologieAttendue.PROPRIETES_DONNEES);
    }

    @Test
    @DisplayName("GET /api/raisonneur/coherence : base vide cohérente")
    void coherenceBaseVide() {
        Reponse r = get("/api/raisonneur/coherence");

        assertStatut(r, 200);
        assertThat(r.texte("/raisonneur")).isEqualTo("HermiT");
        assertThat(r.booleen("/coherente")).isTrue();
        assertThat(r.at("/classes_insatisfiables").isArray()).isTrue();
        assertThat(r.at("/classes_insatisfiables")).isEmpty();
    }

    @Test
    @DisplayName("GET /api/raisonneur/coherence : famille recomposée cohérente")
    void coherenceFamilleDemo() {
        FamilleDemo f = familleDemo();
        beauParent(f.papa(), f.demi());
        dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

        Reponse r = get("/api/raisonneur/coherence");

        assertStatut(r, 200);
        assertThat(r.texte("/raisonneur")).isEqualTo("HermiT");
        assertThat(r.booleen("/coherente")).isTrue();
        assertThat(r.at("/classes_insatisfiables")).isEmpty();
    }
}
