package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.ClassesAttendues;
import bj.ifri.parente.support.Reponse;

/**
 * /api/mariages (SPEC_API.md 4.3) : validation, en_cours, classes inférées par HermiT,
 * mariages en cours, historique, dissolution.
 */
@DisplayName("API /api/mariages")
class MariagesApiTest extends BaseTestApi {

    private Long albert;
    private Long berthe;
    private Long claire;
    private Long denis;

    @BeforeEach
    void creerLesPersonnes() {
        albert = personne("Albert", "M", "1970-01-01", "Alpha");
        berthe = personne("Berthe", "F", "1972-01-01", "Beta");
        claire = personne("Claire", "F", "1975-01-01", "Gamma");
        denis = personne("Denis", "M", "1968-01-01", "Delta");
    }

    private Reponse poster(Long x, Long y, String debut) {
        return post("/api/mariages", corps("conjoint1_id", x, "conjoint2_id", y, "date_debut", debut));
    }

    private Reponse poster(Long x, Long y, String debut, String fin, String cause) {
        Map<String, Object> c = corps("conjoint1_id", x, "conjoint2_id", y, "date_debut", debut);
        if (fin != null) {
            c.put("date_fin", fin);
        }
        if (cause != null) {
            c.put("cause_dissolution", cause);
        }
        return post("/api/mariages", c);
    }

    private int nombreDeMariages() {
        return get("/api/mariages").at("/data").size();
    }

    // ------------------------------------------------------------------ création et format

    @Test
    @DisplayName("POST : mariage en cours, format JSON et classes [Mariage]")
    void creerUnMariageEnCours() {
        Reponse r = poster(albert, berthe, "2000-06-01");

        assertStatut(r, 201);
        assertThat(r.at("/data/id").isIntegralNumber()).isTrue();
        assertThat(r.entier("/data/conjoint1_id")).isEqualTo(albert);
        assertThat(r.entier("/data/conjoint2_id")).isEqualTo(berthe);
        assertThat(r.texte("/data/date_debut")).isEqualTo("2000-06-01");
        assertThat(r.estNull("/data/date_fin")).isTrue();
        assertThat(r.estNull("/data/cause_dissolution")).isTrue();
        assertThat(r.booleen("/data/en_cours")).isTrue();
        assertThat(chaines(r.at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_EN_COURS);
    }

    @Test
    @DisplayName("POST : mariage dissous par divorce, classes MariageDissous et MariageDissousParDivorce")
    void mariageDissousParDivorce() {
        Reponse r = poster(albert, berthe, "1995-01-01", "2005-03-03", "divorce");

        assertStatut(r, 201);
        assertThat(r.texte("/data/date_fin")).isEqualTo("2005-03-03");
        assertThat(r.texte("/data/cause_dissolution")).isEqualTo("divorce");
        assertThat(r.booleen("/data/en_cours")).isFalse();
        assertThat(chaines(r.at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DIVORCE);
    }

    @Test
    @DisplayName("POST : mariage dissous par décès, classes MariageDissous et MariageDissousParDeces")
    void mariageDissousParDeces() {
        Reponse r = poster(albert, berthe, "1995-01-01", "2005-03-03", "deces");

        assertStatut(r, 201);
        assertThat(r.booleen("/data/en_cours")).isFalse();
        assertThat(chaines(r.at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DECES);
    }

    @Test
    @DisplayName("GET : liste et affichage d'un mariage ; 404 si inexistant")
    void listerEtAfficher() {
        Long m1 = mariage(albert, berthe, "2000-06-01");
        Long m2 = mariage(denis, claire, "1990-01-01", "1999-01-01", "deces");

        Reponse liste = get("/api/mariages");
        assertStatut(liste, 200);
        assertThat(ids(liste.at("/data"))).isEqualTo(idsTries(m1, m2));

        Reponse r = get("/api/mariages/" + m2);
        assertStatut(r, 200);
        assertThat(r.entier("/data/id")).isEqualTo(m2);
        assertThat(r.entier("/data/conjoint1_id")).isEqualTo(denis);
        assertThat(r.entier("/data/conjoint2_id")).isEqualTo(claire);
        assertThat(r.texte("/data/date_debut")).isEqualTo("1990-01-01");
        assertThat(r.texte("/data/date_fin")).isEqualTo("1999-01-01");
        assertThat(r.texte("/data/cause_dissolution")).isEqualTo("deces");
        assertThat(r.booleen("/data/en_cours")).isFalse();
        assertThat(chaines(r.at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DECES);

        assert404(get("/api/mariages/" + INEXISTANT));
    }

    @Test
    @DisplayName("DELETE : 204, les conjoints restent ; 404 ensuite")
    void supprimerUnMariage() {
        Long m = mariage(albert, berthe, "2000-06-01");

        assertStatut(delete("/api/mariages/" + m), 204);

        assert404(get("/api/mariages/" + m));
        assertStatut(get("/api/personnes/" + albert), 200);
        assert404(delete("/api/mariages/" + m));
    }

    // ------------------------------------------------------------------ validation

    @Test
    @DisplayName("422 : champs requis et conjoints existants")
    void champsRequisEtConjointsExistants() {
        assertErreurs422(post("/api/mariages", Map.of()), "conjoint1_id", "conjoint2_id", "date_debut");
        assertErreurs422(poster(INEXISTANT, berthe, "2000-01-01"), "conjoint1_id");
        assertErreurs422(poster(albert, INEXISTANT, "2000-01-01"), "conjoint2_id");
        assertErreurs422(poster(albert, berthe, "pas-une-date"), "date_debut");
        assertThat(nombreDeMariages()).isZero();
    }

    @Test
    @DisplayName("422 conjoint2_id : on ne se marie pas avec soi-même")
    void refusDeSeMarierAvecSoiMeme() {
        assertErreurs422(poster(albert, albert, "2000-06-01"), "conjoint2_id");
        assertThat(nombreDeMariages()).isZero();
    }

    @Test
    @DisplayName("422 date_fin : antérieure à date_debut")
    void refusDateDeFinAnterieure() {
        assertErreurs422(poster(albert, berthe, "2000-06-01", "1999-01-01", "divorce"), "date_fin");
        assertThat(nombreDeMariages()).isZero();
    }

    @Test
    @DisplayName("422 cause_dissolution : deces ou divorce, et toujours avec date_fin")
    void causeDeDissolutionValideEtLieeALaDateDeFin() {
        assertErreurs422(poster(albert, berthe, "2000-06-01", "2005-01-01", "annulation"), "cause_dissolution");
        assertErreurs422(poster(albert, berthe, "2000-06-01", "2005-01-01", null), "cause_dissolution");
        assertErreurs422(poster(albert, berthe, "2000-06-01", null, "divorce"), "cause_dissolution");
        assertThat(nombreDeMariages()).isZero();
    }

    // ------------------------------------------------------------------ mariages en cours

    @Test
    @DisplayName("422 conjoint1_id : conjoint1 a déjà un mariage en cours")
    void refusDUnSecondMariageEnCoursPourLeConjoint1() {
        mariage(albert, berthe, "2000-06-01");

        assertErreurs422(poster(albert, claire, "2010-01-01"), "conjoint1_id");
        // Berthe est conjoint2 dans son mariage en cours, mais conjoint1 ici.
        assertErreurs422(poster(berthe, denis, "2010-01-01"), "conjoint1_id");
        assertThat(nombreDeMariages()).isEqualTo(1);
    }

    @Test
    @DisplayName("422 conjoint2_id : conjoint2 a déjà un mariage en cours (conjoint1 libre)")
    void refusDUnSecondMariageEnCoursPourLeConjoint2() {
        mariage(albert, berthe, "2000-06-01");

        assertErreurs422(poster(denis, berthe, "2010-01-01"), "conjoint2_id");
        // Albert est conjoint1 dans son mariage en cours, mais conjoint2 ici.
        assertErreurs422(poster(claire, albert, "2010-01-01"), "conjoint2_id");
        assertThat(nombreDeMariages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un mariage déjà dissous peut être saisi pour l'historique malgré un mariage en cours")
    void historiqueDissousAccepte() {
        mariage(albert, berthe, "2000-06-01");

        Reponse r = poster(albert, claire, "1990-01-01", "1995-01-01", "divorce");

        assertStatut(r, 201);
        assertThat(r.booleen("/data/en_cours")).isFalse();
        assertThat(nombreDeMariages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un nouveau mariage est possible après un mariage dissous")
    void nouveauMariageApresDissolution() {
        mariage(albert, berthe, "1995-01-01", "2005-01-01", "divorce");

        Reponse r = poster(albert, claire, "2008-01-01");

        assertStatut(r, 201);
        assertThat(r.booleen("/data/en_cours")).isTrue();
    }

    // ------------------------------------------------------------------ dissolution (PATCH)

    @Test
    @DisplayName("PATCH : dissolution, classes inférées mises à jour, remariage possible")
    void dissolutionParPatch() {
        Long m = mariage(albert, berthe, "2000-06-01");

        Reponse r = patch("/api/mariages/" + m, corps("date_fin", "2020-02-02", "cause_dissolution", "deces"));

        assertStatut(r, 200);
        assertThat(r.entier("/data/id")).isEqualTo(m);
        assertThat(r.texte("/data/date_fin")).isEqualTo("2020-02-02");
        assertThat(r.texte("/data/cause_dissolution")).isEqualTo("deces");
        assertThat(r.booleen("/data/en_cours")).isFalse();
        assertThat(chaines(r.at("/data/classes"))).containsExactlyElementsOf(ClassesAttendues.MARIAGE_DECES);
        assertThat(get("/api/mariages/" + m).booleen("/data/en_cours")).isFalse();

        // Après dissolution, Albert peut se remarier.
        assertStatut(poster(albert, claire, "2021-01-01"), 201);
    }

    @Test
    @DisplayName("PATCH : dissolution validée (422) ; 404 si le mariage n'existe pas")
    void dissolutionParPatchValidee() {
        Long m = mariage(albert, berthe, "2000-06-01");

        assertErreurs422(patch("/api/mariages/" + m, corps("date_fin", "1999-01-01", "cause_dissolution", "divorce")), "date_fin");
        assertErreurs422(patch("/api/mariages/" + m, corps("date_fin", "2010-01-01", "cause_dissolution", "abandon")), "cause_dissolution");

        Reponse inchange = get("/api/mariages/" + m);
        assertThat(inchange.booleen("/data/en_cours")).isTrue();
        assertThat(inchange.estNull("/data/date_fin")).isTrue();
        assert404(patch("/api/mariages/" + INEXISTANT, corps("date_fin", "2010-01-01", "cause_dissolution", "divorce")));
    }
}
