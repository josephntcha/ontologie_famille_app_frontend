package bj.ifri.parente.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;
import bj.ifri.parente.support.Reponse;
import tools.jackson.databind.JsonNode;

/**
 * POST /api/verification (SPEC_API.md 5, SPEC_API_JAVA.md D.1) sur la famille de démonstration
 * (voir {@link FamilleDemo}). Date de référence : 2026-11-18.
 */
@DisplayName("API /api/verification")
class VerificationApiTest extends BaseTestApi {

    private Reponse verifier(Long x, Long y) {
        Reponse r = post("/api/verification", corps("personne1_id", x, "personne2_id", y));
        assertStatut(r, 200);
        return r;
    }

    private static List<String> codes(Reponse r) {
        List<String> codes = new ArrayList<>();
        for (JsonNode regle : r.at("/mariage/regles")) {
            codes.add(regle.get("code").stringValue());
        }
        return codes;
    }

    private static JsonNode regle(Reponse r, String code) {
        for (JsonNode regle : r.at("/mariage/regles")) {
            if (code.equals(regle.get("code").stringValue())) {
                return regle;
            }
        }
        throw new AssertionError("Règle " + code + " absente : " + codes(r));
    }

    private static void assertRegle(Reponse r, String code, String verdict, String article) {
        JsonNode regle = regle(r, code);
        assertThat(regle.get("verdict").stringValue()).as("verdict de %s", code).isEqualTo(verdict);
        assertThat(regle.get("article").stringValue()).as("article de %s", code).isEqualTo(article);
    }

    private static void assertAncetresCommuns(Reponse r, Long... attendus) {
        JsonNode ancetres = r.at("/lien_de_sang/ancetres_communs");
        assertThat(ids(ancetres)).isEqualTo(idsTries(attendus));
        for (JsonNode a : ancetres) {
            assertThat(a.propertyNames()).contains("id", "nom", "prenom");
        }
    }

    private static TreeSet<Long> idsManquants(Reponse r) {
        TreeSet<Long> ids = new TreeSet<>();
        for (JsonNode m : r.at("/completude/manquants")) {
            assertThat(m.propertyNames()).contains("personne_id", "description");
            ids.add(m.get("personne_id").asLong());
        }
        return ids;
    }

    // ================================================================== structure et validation

    @Test
    @DisplayName("Structure complète de la réponse")
    void structureCompleteDeLaReponse() {
        FamilleDemo f = familleDemo();

        Reponse r = verifier(f.kid(), f.cousin());

        assertThat(r.entier("/personne1/id")).isEqualTo(f.kid());
        assertThat(r.entier("/personne2/id")).isEqualTo(f.cousin());
        for (String p : List.of("/personne1", "/personne2")) {
            assertThat(r.at(p).propertyNames()).contains("id", "nom", "prenom", "sexe", "date_naissance", "age", "uri");
        }
        assertThat(r.at("/lien_de_sang").propertyNames()).contains(
                "existe", "type", "degre", "generation_personne1", "generation_personne2", "code", "libelle",
                "demi", "ancetres_communs", "message", "infere_par_raisonneur");
        assertThat(r.at("/lien_alliance").propertyNames()).contains(
                "existe", "type", "degre", "code", "libelle", "conjoint_id", "mariage_id",
                "mariage_dissous_par_deces", "message");
        assertThat(r.at("/mariage").propertyNames()).contains("verdict", "regles", "avertissement");
        assertThat(r.at("/mariage/regles")).isNotEmpty();
        for (JsonNode regle : r.at("/mariage/regles")) {
            assertThat(regle.propertyNames()).contains("code", "article", "verdict", "message");
            assertThat(regle.get("message").isString()).isTrue();
        }
        for (String p : List.of("/completude/personne1", "/completude/personne2")) {
            assertThat(r.at(p).propertyNames()).contains("parents_connus", "parents_attendus", "grands_parents_connus",
                    "grands_parents_attendus", "generations_completes", "complet");
        }
        assertThat(r.at("/completude").propertyNames()).contains("complet", "manquants");
        assertThat(r.texte("/lien_de_sang/message")).isNotBlank();
    }

    @Test
    @DisplayName("Avertissement : indication non juridique qui renvoie à l'officier d'état civil")
    void avertissement() {
        FamilleDemo f = familleDemo();

        for (Reponse r : List.of(verifier(f.kid(), f.cousin()), verifier(f.demi(), f.cousin()))) {
            assertThat(r.texte("/mariage/avertissement")).isNotBlank().containsIgnoringCase("officier");
        }
    }

    @Test
    @DisplayName("422 : personnes requises, existantes et distinctes")
    void validation() {
        FamilleDemo f = familleDemo();

        assertErreurs422(post("/api/verification", Map.of()), "personne1_id", "personne2_id");
        assertErreurs422(post("/api/verification", corps("personne1_id", INEXISTANT, "personne2_id", f.kid())), "personne1_id");
        assertErreurs422(post("/api/verification", corps("personne1_id", f.kid(), "personne2_id", INEXISTANT)), "personne2_id");
        assertErreurs422(post("/api/verification", corps("personne1_id", f.kid(), "personne2_id", f.kid())), "personne2_id");
    }

    // ================================================================== lien de sang

    @Nested
    @DisplayName("Lien de sang")
    class LienDeSang {

        @Test
        @DisplayName("Parent et enfant : ligne directe, degré 1, interdit")
        void parentEtEnfant() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.kid(), f.papa());

            assertThat(r.booleen("/lien_de_sang/existe")).isTrue();
            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(1);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(1);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isZero();
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("parent_enfant");
            assertThat(r.texte("/lien_de_sang/libelle")).isEqualTo("Parent et enfant");
            assertThat(r.booleen("/lien_de_sang/demi")).isFalse();
            assertAncetresCommuns(r, f.papa());
            assertThat(r.texte("/lien_de_sang/ancetres_communs/0/prenom")).isEqualTo("Papa");
            assertThat(r.texte("/lien_de_sang/ancetres_communs/0/nom")).isEqualTo("Martin");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "ligne_directe_sang", "interdit", "Art. 122");
        }

        @Test
        @DisplayName("Grand-parent et petit-enfant : générations dans l'ordre des personnes")
        void grandParentEtPetitEnfant() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.grandpa(), f.kid());

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(2);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isZero();
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isEqualTo(2);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("grand_parent_petit_enfant");
            assertThat(r.texte("/lien_de_sang/libelle")).isEqualTo("Grand-parent et petit-enfant");
            assertAncetresCommuns(r, f.grandpa());
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertThat(codes(r)).contains("ligne_directe_sang");
        }

        @Test
        @DisplayName("Arrière-grand-parent : ascendant_descendant de degré 3")
        void arriereGrandParent() {
            FamilleDemo f = familleDemo();
            Long aieul = personne("Aieul", "M", "1900-01-01", "Martin");
            parent(aieul, f.grandpa());

            Reponse r = verifier(f.kid(), aieul);

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(3);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(3);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isZero();
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("ascendant_descendant");
            assertAncetresCommuns(r, aieul);
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "ligne_directe_sang", "interdit", "Art. 122");
        }

        @Test
        @DisplayName("Frères germains : collatéral, degré 2, les deux parents en ancêtres communs")
        void freresGermains() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.papa(), f.tonton());

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(2);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(1);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isEqualTo(1);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("freres_soeurs");
            assertThat(r.texte("/lien_de_sang/libelle")).isEqualTo("Frère et sœur");
            assertThat(r.booleen("/lien_de_sang/demi")).isFalse();
            assertAncetresCommuns(r, f.grandpa(), f.grandma());
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "collaterale_sang", "interdit", "Art. 122");
        }

        @Test
        @DisplayName("Demi-frère et demi-sœur : un seul parent commun, demi = true, interdit")
        void demiFrereEtDemiSoeur() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.kid(), f.demi());

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(2);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("demi_freres_soeurs");
            assertThat(r.texte("/lien_de_sang/libelle")).isEqualTo("Demi-frère et demi-sœur");
            assertThat(r.booleen("/lien_de_sang/demi")).isTrue();
            assertAncetresCommuns(r, f.maman());
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "collaterale_sang", "interdit", "Art. 122");
        }

        @Test
        @DisplayName("Oncle et nièce : collatéral, degré 3, interdit")
        void oncleEtNiece() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.tonton(), f.kid());

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(3);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(1);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isEqualTo(2);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("oncle_neveu");
            assertThat(r.booleen("/lien_de_sang/demi")).isFalse();
            assertAncetresCommuns(r, f.grandpa(), f.grandma());
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "collaterale_sang", "interdit", "Art. 122");
        }

        @Test
        @DisplayName("Cousins germains avec arbres complets : collaterale_eloignee, autorisé")
        void cousinsGermainsAvecArbresComplets() {
            FamilleDemo f = familleDemo();
            completerAscendance(f.kid());
            completerAscendance(f.cousin());

            Reponse r = verifier(f.kid(), f.cousin());

            assertThat(r.booleen("/lien_de_sang/existe")).isTrue();
            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(4);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(2);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isEqualTo(2);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("cousins_germains");
            assertThat(r.texte("/lien_de_sang/libelle")).isEqualTo("Cousins germains");
            assertThat(r.booleen("/lien_de_sang/demi")).isFalse();
            assertAncetresCommuns(r, f.grandpa(), f.grandma());
            assertThat(r.booleen("/lien_alliance/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isTrue();
            assertThat(r.at("/completude/manquants")).isEmpty();
            assertThat(r.entier("/completude/personne1/parents_connus")).isEqualTo(2);
            assertThat(r.entier("/completude/personne1/grands_parents_connus")).isEqualTo(4);
            assertThat(r.entier("/completude/personne1/generations_completes")).isEqualTo(2);
            assertThat(r.booleen("/completude/personne2/complet")).isTrue();
            assertThat(codes(r)).containsExactly("collaterale_eloignee");
            assertRegle(r, "collaterale_eloignee", "autorise", "Art. 122");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("autorise");
        }

        @Test
        @DisplayName("Cousins germains avec arbre incomplet : verification_incomplete et personnes manquantes")
        void cousinsGermainsAvecArbreIncomplet() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.kid(), f.cousin());

            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("cousins_germains");
            assertThat(r.booleen("/completude/complet")).isFalse();
            assertThat(r.entier("/completude/personne1/parents_connus")).isEqualTo(2);
            assertThat(r.entier("/completude/personne1/parents_attendus")).isEqualTo(2);
            assertThat(r.entier("/completude/personne1/grands_parents_connus")).isEqualTo(2);
            assertThat(r.entier("/completude/personne1/grands_parents_attendus")).isEqualTo(4);
            assertThat(r.entier("/completude/personne1/generations_completes")).isEqualTo(1);
            assertThat(r.booleen("/completude/personne1/complet")).isFalse();
            assertThat(codes(r)).containsExactly("collaterale_eloignee");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("verification_incomplete");
            // Maman (mère de Kid) et Tantine (mère de Cousin) n'ont aucun parent connu.
            assertThat(idsManquants(r)).containsExactlyElementsOf(idsTries(f.maman(), f.tantine()));
        }

        @Test
        @DisplayName("Grand-oncle et petit-neveu : degré 4, collatéral éloigné")
        void grandOncleEtPetitNeveu() {
            FamilleDemo f = familleDemo();
            Long bebe = personne("Bebe", "M", "2015-06-01", "Martin");
            parent(f.kid(), bebe);

            Reponse r = verifier(f.tonton(), bebe);

            assertThat(r.texte("/lien_de_sang/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(4);
            assertThat(r.entier("/lien_de_sang/generation_personne1")).isEqualTo(1);
            assertThat(r.entier("/lien_de_sang/generation_personne2")).isEqualTo(3);
            assertThat(r.texte("/lien_de_sang/code")).isEqualTo("grand_oncle_petit_neveu");
            assertThat(codes(r)).contains("collaterale_eloignee").doesNotContain("collaterale_sang");
        }

        @Test
        @DisplayName("Aucun lien de sang ni d'alliance « dans les données connues »")
        void aucunLienDansLesDonneesConnues() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.demi(), f.cousin());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            for (String champ : List.of("type", "degre", "generation_personne1", "generation_personne2", "code", "libelle")) {
                assertThat(r.estNull("/lien_de_sang/" + champ)).as(champ).isTrue();
            }
            assertThat(r.at("/lien_de_sang/ancetres_communs")).isEmpty();
            assertThat(r.booleen("/lien_de_sang/demi")).isFalse();
            assertThat(r.texte("/lien_de_sang/message")).contains("dans les données connues");
            assertThat(r.booleen("/lien_alliance/existe")).isFalse();
            for (String champ : List.of("type", "degre", "code", "libelle", "conjoint_id", "mariage_id")) {
                assertThat(r.estNull("/lien_alliance/" + champ)).as(champ).isTrue();
            }
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(r.texte("/lien_alliance/message")).contains("dans les données connues");
        }

        @Test
        @DisplayName("Seuls les ancêtres communs les plus proches sont retenus")
        void ancetresCommunsLesPlusProches() {
            FamilleDemo f = familleDemo();
            Long aieul = personne("Aieul", "M", "1900-01-01", "Martin");
            parent(aieul, f.grandpa());

            Reponse r = verifier(f.kid(), f.cousin());

            assertThat(r.entier("/lien_de_sang/degre")).isEqualTo(4);
            assertAncetresCommuns(r, f.grandpa(), f.grandma());
        }
    }

    // ================================================================== infere_par_raisonneur

    @Nested
    @DisplayName("infere_par_raisonneur (HermiT, lienDeSangAvec)")
    class InfereParRaisonneur {

        @Test
        @DisplayName("Vrai en ligne directe (parent, grand-parent), dans les deux sens")
        void vraiEnLigneDirecte() {
            FamilleDemo f = familleDemo();

            assertThat(verifier(f.kid(), f.papa()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
            assertThat(verifier(f.papa(), f.kid()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
            assertThat(verifier(f.grandpa(), f.kid()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
        }

        @Test
        @DisplayName("Vrai en collatérale (frères, demi, oncle, cousins)")
        void vraiEnCollaterale() {
            FamilleDemo f = familleDemo();

            assertThat(verifier(f.papa(), f.tonton()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
            assertThat(verifier(f.kid(), f.demi()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
            assertThat(verifier(f.tonton(), f.kid()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
            assertThat(verifier(f.kid(), f.cousin()).booleen("/lien_de_sang/infere_par_raisonneur")).isTrue();
        }

        @Test
        @DisplayName("Faux sans lien de sang connu")
        void fauxSansLien() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.demi(), f.cousin());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/lien_de_sang/infere_par_raisonneur")).isFalse();
        }

        @Test
        @DisplayName("Faux pour une simple alliance (beau-père, belle-sœur, beauParentDe, époux)")
        void fauxPourUneAlliance() {
            FamilleDemo f = familleDemo();
            Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
            beauParent(beaupere, f.kid());

            for (Long[] paire : new Long[][] {
                    {f.grandpa(), f.maman()}, {f.tonton(), f.maman()}, {beaupere, f.kid()}, {f.papa(), f.maman()}}) {
                Reponse r = verifier(paire[0], paire[1]);
                assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
                assertThat(r.booleen("/lien_de_sang/infere_par_raisonneur")).isFalse();
            }
        }
    }

    // ================================================================== alliance

    @Nested
    @DisplayName("Lien d'alliance")
    class Alliance {

        @Test
        @DisplayName("Beau-père et belle-fille : ligne directe degré 1, interdit")
        void beauPereEtBelleFille() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.grandpa(), f.maman());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/lien_alliance/existe")).isTrue();
            assertThat(r.texte("/lien_alliance/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(1);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_parent_bel_enfant");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(f.papa());
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(f.mariagePapaMaman());
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "ligne_directe_alliance", "interdit", "Art. 122");

            // Dans l'autre sens : le conjoint de Maman (Papa) est l'enfant de Grandpa.
            Reponse inverse = verifier(f.maman(), f.grandpa());
            assertThat(inverse.texte("/lien_alliance/code")).isEqualTo("beau_parent_bel_enfant");
            assertThat(inverse.entier("/lien_alliance/conjoint_id")).isEqualTo(f.papa());
        }

        @Test
        @DisplayName("Beau-père et belle-fille restent interdits après dissolution par décès")
        void beauPereEtBelleFilleApresDeces() {
            FamilleDemo f = familleDemo();
            dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

            Reponse r = verifier(f.maman(), f.grandpa());

            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_parent_bel_enfant");
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isTrue();
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertThat(codes(r)).contains("ligne_directe_alliance").doesNotContain("mariage_en_cours");
        }

        @Test
        @DisplayName("Ascendant du conjoint au 2e degré : ascendant_descendant_par_alliance")
        void ascendantDescendantParAlliance() {
            FamilleDemo f = familleDemo();
            Long aieul = personne("Aieul", "M", "1900-01-01", "Martin");
            parent(aieul, f.grandpa());

            // Maman est l'épouse de Papa, petit-fils d'Aieul.
            Reponse r = verifier(aieul, f.maman());

            assertThat(r.texte("/lien_alliance/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(2);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("ascendant_descendant_par_alliance");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertThat(codes(r)).contains("ligne_directe_alliance");
        }

        @Test
        @DisplayName("beauParentDe déclaré : alliance en ligne directe sans conjoint ni mariage")
        void beauParentDeDeclare() {
            FamilleDemo f = familleDemo();
            Long beaupere = personne("Beaupere", "M", "1950-01-01", "Leroy");
            beauParent(beaupere, f.kid());

            for (Reponse r : List.of(verifier(beaupere, f.kid()), verifier(f.kid(), beaupere))) {
                assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
                assertThat(r.booleen("/lien_alliance/existe")).isTrue();
                assertThat(r.texte("/lien_alliance/type")).isEqualTo("ligne_directe");
                assertThat(r.entier("/lien_alliance/degre")).isEqualTo(1);
                assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_parent_bel_enfant");
                assertThat(r.estNull("/lien_alliance/conjoint_id")).isTrue();
                assertThat(r.estNull("/lien_alliance/mariage_id")).isTrue();
                assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
                assertThat(codes(r)).contains("ligne_directe_alliance");
            }
        }

        @Test
        @DisplayName("Beau-frère et belle-sœur, mariage en cours : interdit (alliance et mariage en cours)")
        void beauFrereBelleSoeurMariageEnCours() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.tonton(), f.maman());

            assertThat(r.texte("/lien_alliance/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(2);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_frere_belle_soeur");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(f.papa());
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(f.mariagePapaMaman());
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertThat(codes(r)).contains("collaterale_alliance", "mariage_en_cours");
        }

        @Test
        @DisplayName("Beau-frère et belle-sœur après divorce : l'interdiction demeure")
        void beauFrereBelleSoeurApresDivorce() {
            FamilleDemo f = familleDemo();
            dissoudre(f.mariagePapaMaman(), "2015-01-01", "divorce");

            Reponse r = verifier(f.tonton(), f.maman());

            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_frere_belle_soeur");
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertRegle(r, "collaterale_alliance", "interdit", "Art. 122");
            assertThat(codes(r)).doesNotContain("beau_frere_belle_soeur_deces", "mariage_en_cours");
        }

        @Test
        @DisplayName("Beau-frère et belle-sœur après décès : autorisation_requise, même si l'arbre est incomplet")
        void beauFrereBelleSoeurApresDeces() {
            FamilleDemo f = familleDemo();
            dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");

            Reponse r = verifier(f.maman(), f.tonton());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_frere_belle_soeur");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(f.papa());
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(f.mariagePapaMaman());
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isTrue();
            assertThat(r.booleen("/completude/complet")).isFalse();
            assertThat(codes(r)).containsExactly("beau_frere_belle_soeur_deces");
            assertRegle(r, "beau_frere_belle_soeur_deces", "autorisation_requise", "Art. 122");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("autorisation_requise");
        }

        @Test
        @DisplayName("Oncle et nièce par alliance (via un mariage dissous par divorce) : interdit")
        void oncleEtNieceParAlliance() {
            FamilleDemo f = familleDemo();

            // Tantine (divorcée de Tonton) et Kid, nièce de Tonton.
            Reponse r = verifier(f.tantine(), f.kid());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.texte("/lien_alliance/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(3);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("oncle_neveu_par_alliance");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(f.tonton());
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(f.mariageTontonTantine());
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(codes(r)).containsExactly("collaterale_alliance");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
        }

        @Test
        @DisplayName("Allié au 4e degré : allie_eloigne, sans règle d'alliance")
        void allieEloigne() {
            FamilleDemo f = familleDemo();
            Long gendre = personne("Gendre", "M", "1988-03-03", "Moreau");
            mariage(gendre, f.kid(), "2014-05-17");

            // L'épouse de Gendre (Kid) est cousine germaine de Cousin.
            Reponse r = verifier(gendre, f.cousin());

            assertThat(r.texte("/lien_alliance/type")).isEqualTo("collaterale");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(4);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("allie_eloigne");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(f.kid());
            assertThat(codes(r)).doesNotContain("collaterale_alliance", "ligne_directe_alliance");
        }

        @Test
        @DisplayName("Deux ex-époux divorcés, sans autre lien et arbres complets : autorisés")
        void exEpouxAutorises() {
            FamilleDemo f = familleDemo();
            completerAscendance(f.tonton());
            completerAscendance(f.tantine());

            Reponse r = verifier(f.tonton(), f.tantine());

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/lien_alliance/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isTrue();
            assertThat(codes(r)).containsExactly("aucun_empechement");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("autorise");
        }

        @Test
        @DisplayName("Plusieurs alliances : la ligne directe l'emporte sur la collatérale")
        void ligneDirecteAvantCollaterale() {
            // P parent de Y et de A (Y et A frères) ; Y parent de B.
            // X a épousé A (divorce) puis B (décès) : collatérale via A, ligne directe via B.
            Long p = personne("P", "M", "1910-01-01", "Famille");
            Long y = personne("Y", "M", "1940-01-01", "Famille");
            Long a = personne("A", "M", "1942-01-01", "Famille");
            Long b = personne("B", "M", "1965-01-01", "Famille");
            Long x = personne("X", "F", "1966-01-01", "Autrenom");
            parent(p, y);
            parent(p, a);
            parent(y, b);
            mariage(x, a, "1985-01-01", "1990-01-01", "divorce");
            Long mariageB = mariage(x, b, "1992-01-01", "2000-01-01", "deces");

            Reponse r = verifier(x, y);

            assertThat(r.texte("/lien_alliance/type")).isEqualTo("ligne_directe");
            assertThat(r.entier("/lien_alliance/degre")).isEqualTo(1);
            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_parent_bel_enfant");
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(b);
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(mariageB);
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isTrue();
            assertThat(codes(r)).contains("ligne_directe_alliance");
        }

        @Test
        @DisplayName("Plusieurs alliances de même degré : un mariage non dissous par décès l'emporte")
        void aDegreEgalLeMariageNonDissousParDecesLEmporte() {
            // Frere1, Frere2 et Frere3 sont frères. X, veuve de Frere1 puis divorcée de Frere2,
            // est belle-sœur de Frere3 par deux mariages : on retient celui dissous par divorce.
            Long g1 = personne("G1", "M", "1930-01-01", "Freres");
            Long g2 = personne("G2", "F", "1932-01-01", "Freres");
            Long frere1 = personne("Frere1", "M", "1955-01-01", "Freres");
            Long frere2 = personne("Frere2", "M", "1957-01-01", "Freres");
            Long frere3 = personne("Frere3", "M", "1959-01-01", "Freres");
            for (Long frere : List.of(frere1, frere2, frere3)) {
                parent(g1, frere);
                parent(g2, frere);
            }
            Long x = personne("X", "F", "1960-01-01", "Veuve");
            mariage(x, frere1, "1980-01-01", "1985-01-01", "deces");
            Long mariageDivorce = mariage(x, frere2, "1987-01-01", "1995-01-01", "divorce");

            Reponse r = verifier(x, frere3);

            assertThat(r.texte("/lien_alliance/code")).isEqualTo("beau_frere_belle_soeur");
            assertThat(r.entier("/lien_alliance/mariage_id")).isEqualTo(mariageDivorce);
            assertThat(r.entier("/lien_alliance/conjoint_id")).isEqualTo(frere2);
            assertThat(r.booleen("/lien_alliance/mariage_dissous_par_deces")).isFalse();
            assertThat(codes(r)).contains("collaterale_alliance").doesNotContain("beau_frere_belle_soeur_deces");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
        }
    }

    // ================================================================== autres règles et priorité

    @Nested
    @DisplayName("Âge, mariage en cours, absence d'empêchement et priorité des verdicts")
    class AutresRegles {

        @Test
        @DisplayName("age_minimum (Art. 123) : l'une des personnes a moins de 18 ans")
        void ageMinimum() {
            Long adulte = personne("Adulte", "M", "1990-02-02", "Alpha");
            Long mineure = personne("Mineure", "F", "2010-05-05", "Beta");
            completerAscendance(adulte);
            completerAscendance(mineure);

            Reponse r = verifier(adulte, mineure);

            assertThat(r.entier("/personne2/age")).isEqualTo(16);
            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isTrue();
            assertThat(codes(r)).containsExactly("age_minimum");
            assertRegle(r, "age_minimum", "interdit", "Art. 123");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
        }

        @Test
        @DisplayName("age_minimum : 18 ans révolus à la date de référence suffisent")
        void dixHuitAnsRevolus() {
            Long adulte = personne("Adulte", "M", "1990-02-02", "Alpha");
            Long majeure = personne("Majeure", "F", "2008-11-18", "Beta");
            Long presque = personne("Presque", "F", "2008-11-19", "Gamma");

            assertThat(codes(verifier(adulte, majeure))).doesNotContain("age_minimum");
            assertThat(codes(verifier(adulte, presque))).contains("age_minimum");
        }

        @Test
        @DisplayName("mariage_en_cours (Art. 125 et 143) : l'une des personnes est mariée")
        void mariageEnCours() {
            Long x = personne("Xavier", "M", "1985-03-01", "Alpha");
            Long y = personne("Yvette", "F", "1987-07-01", "Beta");
            Long epouse = personne("Epouse", "F", "1986-01-01", "Gamma");
            completerAscendance(x);
            completerAscendance(y);
            mariage(x, epouse, "2012-09-09");

            Reponse r = verifier(y, x);

            assertThat(r.booleen("/lien_alliance/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isTrue();
            assertThat(codes(r)).containsExactly("mariage_en_cours");
            assertRegle(r, "mariage_en_cours", "interdit", "Art. 125 et 143");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
        }

        @Test
        @DisplayName("aucun_empechement : personnes complètes sans lien, autorisé")
        void aucunEmpechementAutorise() {
            Long x = personne("Xavier", "M", "1985-03-01", "Alpha");
            Long y = personne("Yvette", "F", "1987-07-01", "Beta");
            completerAscendance(x);
            completerAscendance(y);

            Reponse r = verifier(x, y);

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/lien_alliance/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isTrue();
            assertThat(r.at("/completude/manquants")).isEmpty();
            assertThat(codes(r)).containsExactly("aucun_empechement");
            assertRegle(r, "aucun_empechement", "autorise", "Art. 122");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("autorise");
        }

        @Test
        @DisplayName("aucun_empechement mais ascendance inconnue : verification_incomplete")
        void aucunEmpechementMaisArbreIncomplet() {
            Long x = personne("Xavier", "M", "1985-03-01", "Alpha");
            Long y = personne("Yvette", "F", "1987-07-01", "Beta");

            Reponse r = verifier(x, y);

            assertThat(r.booleen("/lien_de_sang/existe")).isFalse();
            assertThat(r.booleen("/completude/complet")).isFalse();
            assertThat(r.entier("/completude/personne1/parents_connus")).isZero();
            assertThat(r.entier("/completude/personne1/grands_parents_connus")).isZero();
            assertThat(r.entier("/completude/personne1/generations_completes")).isZero();
            assertThat(r.booleen("/completude/personne2/complet")).isFalse();
            assertThat(codes(r)).containsExactly("aucun_empechement");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("verification_incomplete");
            assertThat(idsManquants(r)).containsExactlyElementsOf(idsTries(x, y));
        }

        @Test
        @DisplayName("Priorité : un empêchement certain l'emporte sur l'incomplétude")
        void interditAvantIncomplet() {
            FamilleDemo f = familleDemo();

            Reponse r = verifier(f.demi(), f.kid());

            assertThat(r.booleen("/completude/complet")).isFalse();
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
            assertThat(codes(r)).doesNotContain("aucun_empechement");
        }

        @Test
        @DisplayName("Priorité : interdit l'emporte sur autorisation_requise")
        void interditAvantAutorisationRequise() {
            FamilleDemo f = familleDemo();
            dissoudre(f.mariagePapaMaman(), "2020-01-10", "deces");
            Long nouvelle = personne("Nouvelle", "F", "1970-01-01", "Faure");
            mariage(f.tonton(), nouvelle, "2012-04-04");

            Reponse r = verifier(f.maman(), f.tonton());

            assertThat(codes(r)).contains("beau_frere_belle_soeur_deces", "mariage_en_cours");
            assertRegle(r, "beau_frere_belle_soeur_deces", "autorisation_requise", "Art. 122");
            assertThat(r.texte("/mariage/verdict")).isEqualTo("interdit");
        }
    }
}
