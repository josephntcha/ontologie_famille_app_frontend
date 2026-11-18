package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.ClassesAttendues;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import tools.jackson.databind.JsonNode;

/**
 * GET /api/personnes/{id} : PersonneDetail (SPEC_API.md 3) ; classes inférées par HermiT
 * (SPEC_API_JAVA.md C).
 */
@DisplayName("API /api/personnes/{id} (détail)")
class PersonneDetailApiTest extends BaseTestApi {

    private Reponse detail(Long id) {
        Reponse r = get("/api/personnes/" + id);
        assertStatut(r, 200);
        return r;
    }

    @Test
    @DisplayName("Le détail contient la personne et toutes ses relations")
    void detailCompletDeKid() {
        FamilleDemo f = familleDemo();

        Reponse r = detail(f.kid());

        assertThat(r.at("/data").propertyNames()).contains(
                "id", "nom", "prenom", "sexe", "date_naissance", "age", "uri",
                "parents", "enfants", "beaux_parents", "beaux_enfants", "freres", "soeurs",
                "conjoints", "mariages", "classes");
        assertThat(r.entier("/data/id")).isEqualTo(f.kid());
        assertThat(r.texte("/data/prenom")).isEqualTo("Kid");
        assertThat(r.texte("/data/date_naissance")).isEqualTo("1990-04-12");
        assertThat(r.entier("/data/age")).isEqualTo(36);
        assertThat(r.texte("/data/uri")).isEqualTo(uriPersonne(f.kid()));
        assertThat(ids(r.at("/data/parents"))).isEqualTo(idsTries(f.papa(), f.maman()));
        assertThat(r.at("/data/parents/0").propertyNames()).contains("id", "nom", "prenom", "sexe", "date_naissance", "age", "uri");
        assertThat(r.at("/data/enfants")).isEmpty();
        assertThat(r.at("/data/beaux_parents")).isEmpty();
        assertThat(r.at("/data/beaux_enfants")).isEmpty();
        assertThat(r.at("/data/conjoints")).isEmpty();
        assertThat(r.at("/data/mariages")).isEmpty();
    }

    @Test
    @DisplayName("Frères et sœurs déduits des parents communs, demi compris")
    void freresEtSoeursDemiCompris() {
        FamilleDemo f = familleDemo();

        Reponse kid = detail(f.kid());
        assertThat(ids(kid.at("/data/freres"))).containsExactly(f.demi());
        assertThat(kid.at("/data/soeurs")).isEmpty();

        Reponse demi = detail(f.demi());
        assertThat(demi.at("/data/freres")).isEmpty();
        assertThat(ids(demi.at("/data/soeurs"))).containsExactly(f.kid());

        assertThat(detail(f.cousin()).at("/data/freres")).isEmpty();
        assertThat(detail(f.cousin()).at("/data/soeurs")).isEmpty();
    }

    @Test
    @DisplayName("Frères et sœurs séparés selon le sexe ; enfants")
    void freresEtSoeursSeparesSelonLeSexe() {
        FamilleDemo f = familleDemo();
        Long tatie = personne("Tatie", "F", "1965-01-20", "Martin");
        parent(f.grandpa(), tatie);
        parent(f.grandma(), tatie);

        Reponse papa = detail(f.papa());

        assertThat(ids(papa.at("/data/freres"))).containsExactly(f.tonton());
        assertThat(ids(papa.at("/data/soeurs"))).containsExactly(tatie);
        assertThat(ids(papa.at("/data/enfants"))).containsExactly(f.kid());
        assertThat(ids(detail(f.grandpa()).at("/data/enfants"))).isEqualTo(idsTries(f.papa(), f.tonton(), tatie));
    }

    @Test
    @DisplayName("Conjoints et mariages, en cours ou dissous, avec leurs classes")
    void conjointsEtMariages() {
        FamilleDemo f = familleDemo();
        Long nouvelle = personne("Nouvelle", "F", "1970-01-01", "Faure");
        Long m2 = mariage(f.tonton(), nouvelle, "2012-04-04");

        Reponse r = detail(f.tonton());

        assertThat(ids(r.at("/data/conjoints"))).isEqualTo(idsTries(f.tantine(), nouvelle));
        assertThat(ids(r.at("/data/mariages"))).isEqualTo(idsTries(f.mariageTontonTantine(), m2));
        Map<Long, JsonNode> parId = new HashMap<>();
        for (JsonNode m : r.at("/data/mariages")) {
            parId.put(m.get("id").asLong(), m);
        }
        assertThat(parId.get(f.mariageTontonTantine()).get("en_cours").asBoolean()).isFalse();
        assertThat(chaines(parId.get(f.mariageTontonTantine()).get("classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DIVORCE);
        assertThat(parId.get(m2).get("en_cours").asBoolean()).isTrue();
        assertThat(chaines(parId.get(m2).get("classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_EN_COURS);
    }

    @Test
    @DisplayName("Beaux-parents et beaux-enfants")
    void beauxParentsEtBeauxEnfants() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, f.kid());

        Reponse kid = detail(f.kid());
        Reponse bp = detail(beaupere);

        assertThat(ids(kid.at("/data/beaux_parents"))).containsExactly(beaupere);
        assertThat(kid.at("/data/beaux_enfants")).isEmpty();
        assertThat(ids(bp.at("/data/beaux_enfants"))).containsExactly(f.kid());
        assertThat(bp.at("/data/parents")).isEmpty();
        assertThat(bp.at("/data/enfants")).isEmpty();
    }

    @Test
    @DisplayName("classes : inférées par HermiT, triées (Kid, Cousin, Maman)")
    void classesInfereesParHermit() {
        FamilleDemo f = familleDemo();

        assertThat(chaines(detail(f.kid()).at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.KID);
        assertThat(chaines(detail(f.cousin()).at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.COUSIN);
        assertThat(chaines(detail(f.maman()).at("/data/classes"))).contains("BelleFille", "Mere", "Sexe").doesNotContain("Fille");
    }

    @Test
    @DisplayName("classes : recalculées à chaque appel (DemiFrere / DemiSoeur après un beauParentDe)")
    void classesRecalculeesApresUnNouveauLien() {
        FamilleDemo f = familleDemo();
        assertThat(chaines(detail(f.demi()).at("/data/classes"))).doesNotContain("DemiFrere");
        assertThat(chaines(detail(f.kid()).at("/data/classes"))).doesNotContain("DemiSoeur");

        beauParent(f.papa(), f.demi());

        assertThat(chaines(detail(f.demi()).at("/data/classes"))).contains("DemiFrere");
        assertThat(chaines(detail(f.kid()).at("/data/classes"))).contains("DemiSoeur");
        assertThat(chaines(detail(f.papa()).at("/data/classes"))).contains("BeauParent", "BeauPere");
        assertThat(ids(detail(f.papa()).at("/data/beaux_enfants"))).containsExactly(f.demi());
    }
}
