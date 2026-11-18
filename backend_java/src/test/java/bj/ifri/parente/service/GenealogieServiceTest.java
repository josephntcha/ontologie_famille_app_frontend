package bj.ifri.parente.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;
import bj.ifri.parente.support.FamilleDemo;

/**
 * GenealogieService (SPEC_API_JAVA.md F ; algorithme 5.1 de SPEC_API.md) sur des données
 * créées par l'API.
 */
@DisplayName("GenealogieService")
class GenealogieServiceTest extends BaseTestApi {

    @Autowired
    private GenealogieService genealogie;

    @Test
    @DisplayName("ancetres : chaque ancêtre avec sa génération minimale, sans la personne")
    void ancetresAvecGenerationSansLaPersonne() {
        FamilleDemo f = familleDemo();

        assertThat(genealogie.ancetres(f.kid())).isEqualTo(Map.of(
                f.papa(), 1, f.maman(), 1, f.grandpa(), 2, f.grandma(), 2));
        assertThat(genealogie.ancetres(f.cousin())).isEqualTo(Map.of(
                f.tonton(), 1, f.tantine(), 1, f.grandpa(), 2, f.grandma(), 2));
        assertThat(genealogie.ancetres(f.maman())).isEmpty();
    }

    @Test
    @DisplayName("ancetres : à toute profondeur")
    void ancetresATouteProfondeur() {
        FamilleDemo f = familleDemo();
        Long aieul = personne("Aieul", "M", "1900-01-01", "Martin");
        parent(aieul, f.grandpa());

        assertThat(genealogie.ancetres(f.kid())).containsEntry(aieul, 3).hasSize(5);
    }

    @Test
    @DisplayName("ancetres : génération minimale si un ancêtre est atteint par deux chemins")
    void ancetresGenerationMinimaleParDeuxChemins() {
        // A est parent de B et de C, et B est aussi parent de C : A est à la génération 1 de C.
        Long a = personne("A", "M", "1900-01-01");
        Long b = personne("B", "F", "1925-01-01");
        Long c = personne("C", "M", "1950-01-01");
        parent(a, b);
        parent(a, c);
        parent(b, c);

        assertThat(genealogie.ancetres(c)).isEqualTo(Map.of(a, 1, b, 1));
        assertThat(genealogie.descendants(a)).isEqualTo(Map.of(b, 1, c, 1));
    }

    @Test
    @DisplayName("descendants : chaque descendant avec sa génération minimale, sans la personne")
    void descendantsAvecGenerationSansLaPersonne() {
        FamilleDemo f = familleDemo();

        assertThat(genealogie.descendants(f.grandpa())).isEqualTo(Map.of(
                f.papa(), 1, f.tonton(), 1, f.kid(), 2, f.cousin(), 2));
        assertThat(genealogie.descendants(f.maman())).isEqualTo(Map.of(f.kid(), 1, f.demi(), 1));
        assertThat(genealogie.descendants(f.kid())).isEmpty();
    }

    @Test
    @DisplayName("creeraitUnCycle : vrai si l'enfant est le parent ou déjà son ancêtre")
    void creeraitUnCycleVrai() {
        FamilleDemo f = familleDemo();

        // Kid parentDe Grandpa : Grandpa est déjà ancêtre de Kid.
        assertThat(genealogie.creeraitUnCycle(f.kid(), f.grandpa())).isTrue();
        // Asymétrie : Papa parentDe Grandpa.
        assertThat(genealogie.creeraitUnCycle(f.papa(), f.grandpa())).isTrue();
        // Réflexivité.
        assertThat(genealogie.creeraitUnCycle(f.kid(), f.kid())).isTrue();
    }

    @Test
    @DisplayName("creeraitUnCycle : faux pour un lien légitime, même déjà connu ou impossible pour d'autres raisons")
    void creeraitUnCycleFaux() {
        FamilleDemo f = familleDemo();

        assertThat(genealogie.creeraitUnCycle(f.grandpa(), f.kid())).isFalse();
        assertThat(genealogie.creeraitUnCycle(f.tonton(), f.kid())).isFalse();
        assertThat(genealogie.creeraitUnCycle(f.autre(), f.maman())).isFalse();
        assertThat(genealogie.creeraitUnCycle(f.papa(), f.kid())).isFalse();
    }

    @Test
    @DisplayName("freresEtSoeurs : au moins un parent commun, demi compris, sans la personne")
    void freresEtSoeursDemiCompris() {
        FamilleDemo f = familleDemo();

        assertThat(genealogie.freresEtSoeurs(f.kid())).containsExactly(f.demi());
        assertThat(genealogie.freresEtSoeurs(f.demi())).containsExactly(f.kid());
        assertThat(genealogie.freresEtSoeurs(f.papa())).containsExactly(f.tonton());
        assertThat(genealogie.freresEtSoeurs(f.cousin())).isEmpty();
        assertThat(genealogie.freresEtSoeurs(f.grandpa())).isEmpty();
    }
}
