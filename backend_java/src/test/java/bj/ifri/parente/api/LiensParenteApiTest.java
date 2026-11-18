package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import tools.jackson.databind.JsonNode;

/**
 * /api/liens/parente (parentDe) et ses refus (SPEC_API.md 4.1).
 */
@DisplayName("API /api/liens/parente")
class LiensParenteApiTest extends BaseTestApi {

    private Reponse poster(Long parentId, Long enfantId) {
        return post("/api/liens/parente", corps("parent_id", parentId, "enfant_id", enfantId));
    }

    private int nombreDeLiens() {
        return get("/api/liens/parente").at("/data").size();
    }

    @Test
    @DisplayName("POST : 201 avec {id, parent_id, enfant_id} ; un second parent est accepté")
    void creerUnLienParentDe() {
        Long pere = personne("Pierre", "M", "1960-01-01");
        Long enfant = personne("Paul", "M", "1990-01-01");

        Reponse r = poster(pere, enfant);

        assertStatut(r, 201);
        assertThat(r.at("/data/id").isIntegralNumber()).isTrue();
        assertThat(r.entier("/data/parent_id")).isEqualTo(pere);
        assertThat(r.entier("/data/enfant_id")).isEqualTo(enfant);

        Long mere = personne("Marie", "F", "1962-01-01");
        assertStatut(poster(mere, enfant), 201);
        assertThat(nombreDeLiens()).isEqualTo(2);
        assertThat(ids(get("/api/personnes/" + enfant).at("/data/parents"))).isEqualTo(idsTries(pere, mere));
    }

    @Test
    @DisplayName("GET : liste des liens parentDe")
    void listerLesLiens() {
        FamilleDemo f = familleDemo();

        Reponse r = get("/api/liens/parente");

        assertStatut(r, 200);
        assertThat(r.at("/data")).hasSize(10);
        boolean trouve = false;
        for (JsonNode l : r.at("/data")) {
            assertThat(l.propertyNames()).contains("id", "parent_id", "enfant_id");
            trouve |= l.get("parent_id").asLong() == f.papa() && l.get("enfant_id").asLong() == f.kid();
        }
        assertThat(trouve).as("lien Papa parentDe Kid").isTrue();
    }

    @Test
    @DisplayName("DELETE : 204, la personne reste ; 404 si le lien n'existe pas")
    void supprimerUnLien() {
        Long pere = personne("Pierre", "M", "1960-01-01");
        Long enfant = personne("Paul", "M", "1990-01-01");
        Long lien = parent(pere, enfant);

        assertStatut(delete("/api/liens/parente/" + lien), 204);

        assertThat(nombreDeLiens()).isZero();
        assertStatut(get("/api/personnes/" + pere), 200);
        assert404(delete("/api/liens/parente/" + lien));
        assert404(delete("/api/liens/parente/" + INEXISTANT));
    }

    @Test
    @DisplayName("422 : champs requis et personnes existantes")
    void champsRequisEtPersonnesExistantes() {
        Long p = personne("Pierre", "M", "1960-01-01");

        assertErreurs422(post("/api/liens/parente", Map.of()), "parent_id", "enfant_id");
        assertErreurs422(poster(INEXISTANT, p), "parent_id");
        assertErreurs422(poster(p, INEXISTANT), "enfant_id");
        assertThat(nombreDeLiens()).isZero();
    }

    @Test
    @DisplayName("422 enfant_id : une personne n'est pas son propre parent")
    void refusParentDeSoiMeme() {
        Long p = personne("Pierre", "M", "1960-01-01");

        assertErreurs422(poster(p, p), "enfant_id");
        assertThat(nombreDeLiens()).isZero();
    }

    @Test
    @DisplayName("422 enfant_id : lien déjà existant")
    void refusLienDejaExistant() {
        Long pere = personne("Pierre", "M", "1960-01-01");
        Long enfant = personne("Paul", "M", "1990-01-01");
        parent(pere, enfant);

        assertErreurs422(poster(pere, enfant), "enfant_id");
        assertThat(nombreDeLiens()).isEqualTo(1);
    }

    @Test
    @DisplayName("422 enfant_id : l'enfant a déjà 2 parents")
    void refusDUnTroisiemeParent() {
        FamilleDemo f = familleDemo();

        // Kid a déjà Papa et Maman ; Autre est né avant elle et n'a aucun lien avec elle.
        assertErreurs422(poster(f.autre(), f.kid()), "enfant_id");
        assertThat(nombreDeLiens()).isEqualTo(10);
    }

    @Test
    @DisplayName("422 parent_id : cycle (l'enfant est déjà ancêtre du parent), asymétrie comprise")
    void refusDUnCycle() {
        FamilleDemo f = familleDemo();

        // Grandpa est déjà ancêtre de Kid : Kid parentDe Grandpa créerait un cycle.
        assertErreurs422(poster(f.kid(), f.grandpa()), "parent_id");
        // Asymétrie : Papa parentDe Grandpa.
        assertErreurs422(poster(f.papa(), f.grandpa()), "parent_id");
        assertThat(nombreDeLiens()).isEqualTo(10);
        assertThat(get("/api/personnes/" + f.grandpa()).at("/data/parents")).isEmpty();
    }

    @Test
    @DisplayName("422 parent_id : le parent doit être né strictement avant l'enfant")
    void refusSiLeParentNEstPasNeAvant() {
        Long jeune = personne("Jeune", "M", "2000-01-01");
        Long vieux = personne("Vieux", "M", "1990-01-01");
        Long jumeau = personne("Jumeau", "M", "1990-01-01");

        assertErreurs422(poster(jeune, vieux), "parent_id");
        assertErreurs422(poster(jumeau, vieux), "parent_id");
        assertThat(nombreDeLiens()).isZero();
    }

    @Test
    @DisplayName("422 parent_id : un lien beauParentDe existe déjà entre eux (dans un sens ou l'autre)")
    void refusSiUnLienBeauParentDeExiste() {
        Long x = personne("X", "M", "1950-01-01");
        Long y = personne("Y", "F", "1980-01-01");
        Long z = personne("Z", "M", "1982-01-01");
        beauParent(x, y);
        beauParent(z, x);

        assertErreurs422(poster(x, y), "parent_id");
        assertErreurs422(poster(x, z), "parent_id");
        assertThat(nombreDeLiens()).isZero();
    }
}
