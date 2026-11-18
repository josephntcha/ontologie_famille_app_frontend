package bj.ifri.parente.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import bj.ifri.parente.support.BaseTestApi;

/**
 * NommageLien.nommer(g1, g2, demi) : table 5.2 de SPEC_API.md, avec (a, b) = (min, max),
 * degre = g1 + g2 et type ligne_directe si a = 0, collaterale sinon.
 */
@DisplayName("NommageLien (table 5.2)")
class NommageLienTest extends BaseTestApi {

    @Autowired
    private NommageLien nommage;

    @ParameterizedTest(name = "({0}, {1}, demi={2}) -> {3}")
    @CsvSource(delimiter = '|', textBlock = """
            0 | 1 | false | parent_enfant             | Parent et enfant                        | 1 | ligne_directe
            1 | 0 | false | parent_enfant             | Parent et enfant                        | 1 | ligne_directe
            0 | 2 | false | grand_parent_petit_enfant | Grand-parent et petit-enfant            | 2 | ligne_directe
            0 | 3 | false | ascendant_descendant      | Ascendant et descendant (ligne directe) | 3 | ligne_directe
            5 | 0 | false | ascendant_descendant      | Ascendant et descendant (ligne directe) | 5 | ligne_directe
            1 | 1 | false | freres_soeurs             | Frère et sœur                           | 2 | collaterale
            1 | 1 | true  | demi_freres_soeurs        | Demi-frère et demi-sœur                 | 2 | collaterale
            1 | 2 | false | oncle_neveu               | Oncle ou tante et neveu ou nièce        | 3 | collaterale
            2 | 2 | false | cousins_germains          | Cousins germains                        | 4 | collaterale
            1 | 3 | false | grand_oncle_petit_neveu   | Grand-oncle et petit-neveu              | 4 | collaterale
            3 | 3 | false | cousins_issus_de_germains | Cousins issus de germains               | 6 | collaterale
            2 | 3 | false | collateral_eloigne        | Parents collatéraux                     | 5 | collaterale
            4 | 1 | false | collateral_eloigne        | Parents collatéraux                     | 5 | collaterale
            """)
    @DisplayName("Code, libellé, degré et type selon la table")
    void nommerSelonLaTable(int g1, int g2, boolean demi, String code, String libelle, int degre, String type) {
        LienNomme lien = nommage.nommer(g1, g2, demi);

        assertThat(lien.code()).isEqualTo(code);
        assertThat(lien.libelle()).isEqualTo(libelle);
        assertThat(lien.degre()).isEqualTo(degre);
        assertThat(lien.type()).isEqualTo(type);
    }

    @Test
    @DisplayName("Le nommage est symétrique en (g1, g2)")
    void leNommageEstSymetrique() {
        int[][] paires = {{0, 1}, {0, 2}, {0, 5}, {1, 2}, {1, 3}, {2, 5}, {4, 4}};
        for (int[] p : paires) {
            for (boolean demi : new boolean[] {false, true}) {
                assertThat(nommage.nommer(p[1], p[0], demi))
                        .as("(%d, %d, %s)", p[0], p[1], demi)
                        .isEqualTo(nommage.nommer(p[0], p[1], demi));
            }
        }
    }

    @Test
    @DisplayName("L'indicateur demi n'a d'effet qu'en (1, 1)")
    void demiNeChangeQueLeCasFreresSoeurs() {
        assertThat(nommage.nommer(0, 1, true).code()).isEqualTo("parent_enfant");
        assertThat(nommage.nommer(1, 2, true).code()).isEqualTo("oncle_neveu");
        assertThat(nommage.nommer(2, 2, true).code()).isEqualTo("cousins_germains");
        assertThat(nommage.nommer(1, 1, true).code()).isEqualTo("demi_freres_soeurs");
        assertThat(nommage.nommer(1, 1, false).code()).isEqualTo("freres_soeurs");
    }
}
