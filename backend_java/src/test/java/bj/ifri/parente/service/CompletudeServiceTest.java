package bj.ifri.parente.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;

/**
 * CompletudeService.evaluer (section 5.5 de SPEC_API.md).
 */
@DisplayName("CompletudeService")
class CompletudeServiceTest extends BaseTestApi {

    @Autowired
    private CompletudeService completude;

    private static Completude attendu(int parents, int grandsParents, int generations, boolean complet) {
        return new Completude(parents, 2, grandsParents, 4, generations, complet);
    }

    @Test
    @DisplayName("Aucun parent connu : 0/2, 0/4, 0 génération")
    void personneSansParentConnu() {
        Long seul = personne("Seul", "M", "1980-01-01");

        assertThat(completude.evaluer(seul)).isEqualTo(attendu(0, 0, 0, false));
    }

    @Test
    @DisplayName("Deux parents connus, grands-parents partiels : 1 génération")
    void deuxParentsEtGrandsParentsPartiels() {
        FamilleDemo f = familleDemo();

        // Kid : Papa et Maman ; seuls les parents de Papa sont connus.
        assertThat(completude.evaluer(f.kid())).isEqualTo(attendu(2, 2, 1, false));
    }

    @Test
    @DisplayName("Deux parents connus, aucun grand-parent : 1 génération")
    void deuxParentsSansGrandParent() {
        FamilleDemo f = familleDemo();

        // Demi : Maman et Autre, sans parent connu.
        assertThat(completude.evaluer(f.demi())).isEqualTo(attendu(2, 0, 1, false));
    }

    @Test
    @DisplayName("Un seul parent connu : 0 génération complète, même avec ses 2 grands-parents")
    void unSeulParentConnu() {
        FamilleDemo f = familleDemo();
        Long solo = personne("Solo", "M", "1995-01-01", "Martin");
        parent(f.papa(), solo);

        assertThat(completude.evaluer(solo)).isEqualTo(attendu(1, 2, 0, false));
    }

    @Test
    @DisplayName("Arbre complet sur 2 générations")
    void arbreCompletSurDeuxGenerations() {
        FamilleDemo f = familleDemo();
        completerAscendance(f.kid());

        assertThat(completude.evaluer(f.kid())).isEqualTo(attendu(2, 4, 2, true));
        assertThat(completude.evaluer(f.papa())).isEqualTo(attendu(2, 0, 1, false));
    }
}
