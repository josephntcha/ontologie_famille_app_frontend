package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;

/**
 * /api/liens/beau-parente (beauParentDe) et ses refus (SPEC_API.md 4.2).
 */
@DisplayName("API /api/liens/beau-parente")
class BeauParenteApiTest extends BaseTestApi {

    private Reponse poster(Long beauParentId, Long enfantId) {
        return post("/api/liens/beau-parente", corps("beau_parent_id", beauParentId, "enfant_id", enfantId));
    }

    private int nombreDeLiens() {
        return get("/api/liens/beau-parente").at("/data").size();
    }

    @Test
    @DisplayName("POST, GET et DELETE d'un lien beauParentDe")
    void creerListerEtSupprimer() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");

        Reponse r = poster(beaupere, f.kid());
        assertStatut(r, 201);
        long id = r.entier("/data/id");
        assertThat(r.entier("/data/beau_parent_id")).isEqualTo(beaupere);
        assertThat(r.entier("/data/enfant_id")).isEqualTo(f.kid());

        Reponse liste = get("/api/liens/beau-parente");
        assertStatut(liste, 200);
        assertThat(liste.at("/data")).hasSize(1);
        assertThat(liste.entier("/data/0/id")).isEqualTo(id);
        assertThat(liste.entier("/data/0/beau_parent_id")).isEqualTo(beaupere);
        assertThat(liste.entier("/data/0/enfant_id")).isEqualTo(f.kid());

        assertStatut(delete("/api/liens/beau-parente/" + id), 204);
        assertThat(nombreDeLiens()).isZero();
        assert404(delete("/api/liens/beau-parente/" + id));
    }

    @Test
    @DisplayName("Un beau-parent marié au parent est accepté")
    void beauParentMarieAuParent() {
        FamilleDemo f = familleDemo();

        // Papa, époux de Maman, beau-parent de Demi (fils de Maman).
        assertStatut(poster(f.papa(), f.demi()), 201);
    }

    @Test
    @DisplayName("422 : champs requis et personnes existantes")
    void champsRequisEtPersonnesExistantes() {
        Long p = personne("Pierre", "M", "1960-01-01");

        assertErreurs422(post("/api/liens/beau-parente", Map.of()), "beau_parent_id", "enfant_id");
        assertErreurs422(poster(INEXISTANT, p), "beau_parent_id");
        assertErreurs422(poster(p, INEXISTANT), "enfant_id");
        assertThat(nombreDeLiens()).isZero();
    }

    @Test
    @DisplayName("422 enfant_id : même personne")
    void refusMemePersonne() {
        Long p = personne("Pierre", "M", "1960-01-01");

        assertErreurs422(poster(p, p), "enfant_id");
        assertThat(nombreDeLiens()).isZero();
    }

    @Test
    @DisplayName("422 enfant_id : doublon")
    void refusDoublon() {
        Long bp = personne("Beaupere", "M", "1950-01-01");
        Long enfant = personne("Enfant", "F", "1990-01-01");
        beauParent(bp, enfant);

        assertErreurs422(poster(bp, enfant), "enfant_id");
        assertThat(nombreDeLiens()).isEqualTo(1);
    }

    @Test
    @DisplayName("422 beau_parent_id : un lien parentDe existe déjà entre eux (dans un sens ou l'autre)")
    void refusSiUnLienParentDeExiste() {
        FamilleDemo f = familleDemo();

        assertErreurs422(poster(f.papa(), f.kid()), "beau_parent_id");
        assertErreurs422(poster(f.kid(), f.papa()), "beau_parent_id");
        assertThat(nombreDeLiens()).isZero();
    }
}
