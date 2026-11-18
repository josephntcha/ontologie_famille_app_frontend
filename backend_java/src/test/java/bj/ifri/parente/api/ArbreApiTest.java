package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.ClassesAttendues;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import tools.jackson.databind.JsonNode;

/**
 * GET /api/arbre?personne1_id=..&personne2_id=.. (SPEC_API.md 6) ; classes des nœuds
 * inférées par HermiT (SPEC_API_JAVA.md C).
 */
@DisplayName("API /api/arbre")
class ArbreApiTest extends BaseTestApi {

    /** Arête (source, cible, type). */
    private record Arete(long source, long cible, String type) {
    }

    private Reponse arbre(Long x, Long y) {
        Reponse r = get("/api/arbre?personne1_id=" + x + "&personne2_id=" + y);
        assertStatut(r, 200);
        return r;
    }

    private static List<Arete> aretes(Reponse r) {
        List<Arete> aretes = new ArrayList<>();
        for (JsonNode a : r.at("/aretes")) {
            aretes.add(new Arete(a.get("source").asLong(), a.get("cible").asLong(), a.get("type").stringValue()));
        }
        return aretes;
    }

    private static List<Long> chemin(Reponse r) {
        List<Long> chemin = new ArrayList<>();
        for (JsonNode id : r.at("/chemin")) {
            chemin.add(id.asLong());
        }
        return chemin;
    }

    private static Map<Long, JsonNode> noeudsParId(Reponse r) {
        Map<Long, JsonNode> m = new HashMap<>();
        for (JsonNode n : r.at("/noeuds")) {
            m.put(n.get("id").asLong(), n);
        }
        return m;
    }

    @Test
    @DisplayName("Deux cousins : nœuds, arêtes, chemin par l'ancêtre commun le plus proche")
    void arbreDeDeuxCousins() {
        FamilleDemo f = familleDemo();

        Reponse r = arbre(f.kid(), f.cousin());

        // Chacune, ses ancêtres, ses descendants et ses conjoints (+ le chemin) ; ni Demi ni Autre.
        List<Long> ids = new ArrayList<>();
        for (JsonNode n : r.at("/noeuds")) {
            ids.add(n.get("id").asLong());
        }
        assertThat(ids).as("pas de doublon").doesNotHaveDuplicates();
        assertThat(ids).containsExactlyInAnyOrder(
                f.kid(), f.cousin(), f.papa(), f.maman(), f.tonton(), f.tantine(), f.grandpa(), f.grandma());

        assertThat(aretes(r)).containsExactlyInAnyOrder(
                new Arete(f.grandpa(), f.papa(), "parentDe"),
                new Arete(f.grandma(), f.papa(), "parentDe"),
                new Arete(f.grandpa(), f.tonton(), "parentDe"),
                new Arete(f.grandma(), f.tonton(), "parentDe"),
                new Arete(f.papa(), f.kid(), "parentDe"),
                new Arete(f.maman(), f.kid(), "parentDe"),
                new Arete(f.tonton(), f.cousin(), "parentDe"),
                new Arete(f.tantine(), f.cousin(), "parentDe"),
                new Arete(f.papa(), f.maman(), "marieA"),
                new Arete(f.tonton(), f.tantine(), "marieA"));

        // Kid -> Papa -> (Grandpa ou Grandma) -> Tonton -> Cousin.
        List<Long> chemin = chemin(r);
        assertThat(chemin).hasSize(5);
        assertThat(chemin.get(0)).isEqualTo(f.kid());
        assertThat(chemin.get(1)).isEqualTo(f.papa());
        assertThat(chemin.get(2)).isIn(f.grandpa(), f.grandma());
        assertThat(chemin.get(3)).isEqualTo(f.tonton());
        assertThat(chemin.get(4)).isEqualTo(f.cousin());
        assertThat(r.entier("/ancetre_commun_id")).isEqualTo(chemin.get(2));
    }

    @Test
    @DisplayName("Les nœuds portent nom, prénom, sexe et les classes inférées par HermiT")
    void lesNoeudsPortentLesClassesInferees() {
        FamilleDemo f = familleDemo();

        Reponse r = arbre(f.kid(), f.cousin());
        Map<Long, JsonNode> noeuds = noeudsParId(r);

        JsonNode cousin = noeuds.get(f.cousin());
        assertThat(cousin.propertyNames()).contains("id", "nom", "prenom", "sexe", "classes");
        assertThat(cousin.get("prenom").stringValue()).isEqualTo("Cousin");
        assertThat(cousin.get("nom").stringValue()).isEqualTo("Martin");
        assertThat(cousin.get("sexe").stringValue()).isEqualTo("M");
        assertThat(chaines(cousin.get("classes"))).containsExactlyElementsOf(ClassesAttendues.COUSIN);
        assertThat(chaines(noeuds.get(f.kid()).get("classes"))).containsExactlyElementsOf(ClassesAttendues.KID);
        assertThat(chaines(noeuds.get(f.tonton()).get("classes"))).contains("Oncle", "Pere");
        assertThat(chaines(noeuds.get(f.grandpa()).get("classes"))).contains("GrandPere");
        assertThat(chaines(noeuds.get(f.tantine()).get("classes"))).contains("BelleFille").doesNotContain("Tante");
    }

    @Test
    @DisplayName("Ligne directe : chemin de l'un à l'autre, ancêtre commun = l'ascendant")
    void arbreEnLigneDirecte() {
        FamilleDemo f = familleDemo();

        Reponse r = arbre(f.grandpa(), f.kid());

        // Grandpa : ses descendants ; Kid : ses ancêtres. Aucun des deux n'est marié.
        assertThat(ids(r.at("/noeuds"))).isEqualTo(idsTries(
                f.grandpa(), f.papa(), f.tonton(), f.kid(), f.cousin(), f.maman(), f.grandma()));
        assertThat(chemin(r)).containsExactly(f.grandpa(), f.papa(), f.kid());
        assertThat(r.entier("/ancetre_commun_id")).isEqualTo(f.grandpa());
    }

    @Test
    @DisplayName("Les conjoints des deux personnes sont des nœuds, avec l'arête marieA")
    void lesConjointsSontDesNoeuds() {
        FamilleDemo f = familleDemo();

        Reponse r = arbre(f.tonton(), f.kid());

        List<Long> ids = ids(r.at("/noeuds"));
        assertThat(ids).contains(f.tantine()).doesNotContain(f.demi(), f.autre());
        assertThat(aretes(r)).contains(new Arete(f.tonton(), f.tantine(), "marieA"));
        assertThat(chemin(r)).hasSize(4).startsWith(f.tonton()).endsWith(f.papa(), f.kid());
    }

    @Test
    @DisplayName("Arête beauParentDe ; pas de lien de sang : chemin vide et ancêtre commun null")
    void areteBeauParentDe() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(beaupere, f.kid());

        Reponse r = arbre(beaupere, f.kid());

        assertThat(aretes(r)).contains(new Arete(beaupere, f.kid(), "beauParentDe"));
        assertThat(r.at("/chemin").isArray()).isTrue();
        assertThat(r.at("/chemin")).isEmpty();
        assertThat(r.estNull("/ancetre_commun_id")).isTrue();
    }

    @Test
    @DisplayName("Sans lien de sang : chemin vide, mais les arbres des deux personnes sont présents")
    void sansLienDeSang() {
        FamilleDemo f = familleDemo();

        Reponse r = arbre(f.demi(), f.cousin());

        assertThat(r.at("/chemin")).isEmpty();
        assertThat(r.estNull("/ancetre_commun_id")).isTrue();
        assertThat(ids(r.at("/noeuds"))).contains(f.demi(), f.cousin(), f.maman(), f.autre(), f.tantine(), f.tonton());
    }

    @Test
    @DisplayName("422 : identifiants manquants ou inexistants")
    void identifiantsManquantsOuInexistants() {
        FamilleDemo f = familleDemo();

        assertErreurs422(get("/api/arbre"), "personne1_id", "personne2_id");
        assertErreurs422(get("/api/arbre?personne1_id=" + f.kid()), "personne2_id");
        assertErreurs422(get("/api/arbre?personne1_id=" + INEXISTANT + "&personne2_id=" + f.kid()), "personne1_id");
    }
}
