package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import tools.jackson.databind.JsonNode;

/**
 * Écran « Personnes » (SPEC_API.md 2 et 3) : CRUD, validation 422, doublons probables, 404.
 */
@DisplayName("API /api/personnes")
class PersonnesApiTest extends BaseTestApi {

    private static Map<String, Object> corpsValide(Object... surcharges) {
        Map<String, Object> c = corps("nom", "Dupont", "prenom", "Alice", "sexe", "F", "date_naissance", "1990-05-12");
        c.putAll(corps(surcharges));
        return c;
    }

    private int nombreDePersonnes() {
        return get("/api/personnes").at("/data").size();
    }

    // ------------------------------------------------------------------ création et lecture

    @Test
    @DisplayName("POST : 201, format JSON d'une personne et doublons_probables vide")
    void creerUnePersonne() {
        Reponse r = post("/api/personnes", corpsValide());

        assertStatut(r, 201);
        long id = r.entier("/data/id");
        assertThat(r.texte("/data/nom")).isEqualTo("Dupont");
        assertThat(r.texte("/data/prenom")).isEqualTo("Alice");
        assertThat(r.texte("/data/sexe")).isEqualTo("F");
        assertThat(r.texte("/data/date_naissance")).isEqualTo("1990-05-12");
        assertThat(r.entier("/data/age")).isEqualTo(36);
        assertThat(r.texte("/data/uri")).isEqualTo(NS + "personne_" + id);
        assertThat(r.at("/doublons_probables").isArray()).isTrue();
        assertThat(r.at("/doublons_probables")).isEmpty();

        Reponse lue = get("/api/personnes/" + id);
        assertStatut(lue, 200);
        assertThat(lue.texte("/data/nom")).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("GET : liste de toutes les personnes au format Personne")
    void listerLesPersonnes() {
        Long a = personne("Alice", "F", "1990-05-12", "Dupont");
        Long b = personne("Bob", "M", "1985-01-01", "Durand");

        Reponse r = get("/api/personnes");

        assertStatut(r, 200);
        assertThat(ids(r.at("/data"))).isEqualTo(idsTries(a, b));
        for (JsonNode p : r.at("/data")) {
            assertThat(p.propertyNames()).contains("id", "nom", "prenom", "sexe", "date_naissance", "age", "uri");
        }
    }

    @Test
    @DisplayName("L'âge est calculé en années révolues à la date de référence (2026-11-18)")
    void ageEnAnneesRevolues() {
        assertThat(post("/api/personnes", corpsValide("date_naissance", "2008-11-18")).entier("/data/age")).isEqualTo(18);
        assertThat(post("/api/personnes", corpsValide("date_naissance", "2008-11-19")).entier("/data/age")).isEqualTo(17);
        assertThat(post("/api/personnes", corpsValide("date_naissance", "1990-11-19")).entier("/data/age")).isEqualTo(35);
    }

    // ------------------------------------------------------------------ validation

    @Test
    @DisplayName("422 : tous les champs sont requis")
    void tousLesChampsSontRequis() {
        assertErreurs422(post("/api/personnes", Map.of()), "nom", "prenom", "sexe", "date_naissance");
        assertThat(nombreDePersonnes()).isZero();
    }

    @Test
    @DisplayName("422 : nom et prénom non vides, 255 caractères au plus")
    void nomEtPrenomNonVidesEtLimites() {
        assertErreurs422(post("/api/personnes", corpsValide("nom", "", "prenom", "")), "nom", "prenom");
        assertErreurs422(post("/api/personnes", corpsValide("nom", "a".repeat(256))), "nom");
        assertErreurs422(post("/api/personnes", corpsValide("prenom", "b".repeat(256))), "prenom");
        assertThat(nombreDePersonnes()).isZero();

        assertStatut(post("/api/personnes", corpsValide("nom", "a".repeat(255))), 201);
    }

    @Test
    @DisplayName("422 : sexe vaut M ou F")
    void sexeMOuF() {
        assertErreurs422(post("/api/personnes", corpsValide("sexe", "X")), "sexe");
        assertThat(nombreDePersonnes()).isZero();

        assertStatut(post("/api/personnes", corpsValide("sexe", "M")), 201);
    }

    @Test
    @DisplayName("422 : date de naissance valide")
    void dateDeNaissanceValide() {
        assertErreurs422(post("/api/personnes", corpsValide("date_naissance", "pas-une-date")), "date_naissance");
        assertErreurs422(post("/api/personnes", corpsValide("date_naissance", "1990-02-30")), "date_naissance");
        assertThat(nombreDePersonnes()).isZero();
    }

    @Test
    @DisplayName("422 : date de naissance pas dans le futur (par rapport à la date de référence)")
    void dateDeNaissancePasDansLeFutur() {
        assertErreurs422(post("/api/personnes", corpsValide("date_naissance", "2030-01-01")), "date_naissance");
        assertErreurs422(post("/api/personnes", corpsValide("date_naissance", "2026-11-19")), "date_naissance");
        assertThat(nombreDePersonnes()).isZero();

        Reponse nee = post("/api/personnes", corpsValide("date_naissance", "2026-11-18"));
        assertStatut(nee, 201);
        assertThat(nee.entier("/data/age")).isZero();
    }

    // ------------------------------------------------------------------ doublons

    @Test
    @DisplayName("Doublon probable (même nom à la casse et aux espaces près, même date) signalé sans bloquer")
    void doublonProbableSignaleSansBloquer() {
        Long existant = personne("Alice", "F", "1990-05-12", "Dupont");
        personne("Autre", "F", "1991-05-12", "Dupont");
        personne("Autre2", "F", "1990-05-12", "Durand");

        Reponse r = post("/api/personnes", corpsValide("nom", "  DUPONT ", "prenom", "Alicia"));

        assertStatut(r, 201);
        assertThat(r.entier("/data/id")).isNotEqualTo(existant);
        assertThat(ids(r.at("/doublons_probables"))).containsExactly(existant);
        assertThat(nombreDePersonnes()).isEqualTo(4);
    }

    @Test
    @DisplayName("GET /api/personnes/doublons : groupes d'au moins 2 personnes")
    void groupesDeDoublons() {
        Reponse vide = get("/api/personnes/doublons");
        assertStatut(vide, 200);
        assertThat(vide.at("/data")).isEmpty();

        Long m1 = personne("Jean", "M", "1980-01-01", "Martin");
        Long m2 = personne("Jeanne", "F", "1980-01-01", "MARTIN");
        personne("Paul", "M", "1981-01-01", "Martin");
        personne("Luc", "M", "1980-01-01", "Durand");

        Reponse r = get("/api/personnes/doublons");

        assertStatut(r, 200);
        assertThat(r.at("/data")).hasSize(1);
        assertThat(r.texte("/data/0/nom").trim().toLowerCase(Locale.ROOT)).isEqualTo("martin");
        assertThat(r.texte("/data/0/date_naissance")).isEqualTo("1980-01-01");
        assertThat(ids(r.at("/data/0/personnes"))).isEqualTo(idsTries(m1, m2));
    }

    // ------------------------------------------------------------------ 404, modification, suppression

    @Test
    @DisplayName("404 : personne inexistante (GET, PUT, PATCH, DELETE)")
    void personneInexistante() {
        assert404(get("/api/personnes/" + INEXISTANT));
        assert404(put("/api/personnes/" + INEXISTANT, corps("prenom", "X")));
        assert404(patch("/api/personnes/" + INEXISTANT, corps("prenom", "X")));
        assert404(delete("/api/personnes/" + INEXISTANT));
    }

    @Test
    @DisplayName("PATCH et PUT : mise à jour partielle")
    void modificationPartielle() {
        Long id = personne("Alice", "F", "1990-05-12", "Dupont");

        Reponse r = patch("/api/personnes/" + id, corps("prenom", "Alicia"));
        assertStatut(r, 200);
        assertThat(r.entier("/data/id")).isEqualTo(id);
        assertThat(r.texte("/data/prenom")).isEqualTo("Alicia");
        assertThat(r.texte("/data/nom")).isEqualTo("Dupont");
        assertThat(r.texte("/data/date_naissance")).isEqualTo("1990-05-12");

        Reponse r2 = put("/api/personnes/" + id, corps("nom", "Durand"));
        assertStatut(r2, 200);
        assertThat(r2.texte("/data/nom")).isEqualTo("Durand");
        assertThat(r2.texte("/data/prenom")).isEqualTo("Alicia");

        Reponse lue = get("/api/personnes/" + id);
        assertThat(lue.texte("/data/nom")).isEqualTo("Durand");
        assertThat(lue.texte("/data/prenom")).isEqualTo("Alicia");
        assertThat(lue.texte("/data/sexe")).isEqualTo("F");
    }

    @Test
    @DisplayName("PATCH : les champs fournis sont validés (422) et rien n'est modifié")
    void modificationValidee() {
        Long id = personne("Alice", "F", "1990-05-12", "Dupont");

        assertErreurs422(patch("/api/personnes/" + id, corps("sexe", "Z")), "sexe");
        assertErreurs422(patch("/api/personnes/" + id, corps("date_naissance", "2099-01-01")), "date_naissance");
        assertErreurs422(patch("/api/personnes/" + id, corps("nom", "")), "nom");

        Reponse lue = get("/api/personnes/" + id);
        assertThat(lue.texte("/data/sexe")).isEqualTo("F");
        assertThat(lue.texte("/data/date_naissance")).isEqualTo("1990-05-12");
        assertThat(lue.texte("/data/nom")).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("DELETE : 204, et suppression de ses filiations, beaux-liens et mariages")
    void suppressionEnCascade() {
        FamilleDemo f = familleDemo();
        Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
        beauParent(f.papa(), f.demi());
        Long lienConserve = beauParent(beaupere, f.kid());

        Reponse r = delete("/api/personnes/" + f.papa());

        assertStatut(r, 204);
        assertThat(r.corps()).isEmpty();
        assert404(get("/api/personnes/" + f.papa()));

        for (JsonNode l : get("/api/liens/parente").at("/data")) {
            assertThat(List.of(l.get("parent_id").asLong(), l.get("enfant_id").asLong())).doesNotContain(f.papa());
        }
        // 10 filiations dans la famille de démonstration, dont 3 concernent Papa.
        assertThat(get("/api/liens/parente").at("/data")).hasSize(7);
        assertThat(ids(get("/api/liens/beau-parente").at("/data"))).containsExactly(lienConserve);
        assertThat(ids(get("/api/mariages").at("/data"))).containsExactly(f.mariageTontonTantine());
        assert404(get("/api/mariages/" + f.mariagePapaMaman()));
        assertThat(ids(get("/api/personnes/" + f.kid()).at("/data/parents"))).containsExactly(f.maman());
    }
}
