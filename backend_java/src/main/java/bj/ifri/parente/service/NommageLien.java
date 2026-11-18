package bj.ifri.parente.service;

import org.springframework.stereotype.Service;

/**
 * Nommage d'un lien de sang à partir des générations g1, g2 qui séparent chaque personne de
 * l'ancêtre commun (méthode civile : degré = g1 + g2). OWL dit si deux personnes sont liées,
 * pas à combien de générations : ce calcul reste dans le code (MVP).
 */
@Service
public class NommageLien {

    public static final String LIGNE_DIRECTE = "ligne_directe";
    public static final String COLLATERALE = "collaterale";

    public LienNomme nommer(int g1, int g2, boolean demi) {
        if (g1 < 0 || g2 < 0 || g1 + g2 == 0) {
            throw new IllegalArgumentException("Générations invalides (" + g1 + ", " + g2 + ")");
        }
        int a = Math.min(g1, g2);
        int b = Math.max(g1, g2);
        String code;
        String libelle;
        if (a == 0 && b == 1) {
            code = "parent_enfant";
            libelle = "Parent et enfant";
        } else if (a == 0 && b == 2) {
            code = "grand_parent_petit_enfant";
            libelle = "Grand-parent et petit-enfant";
        } else if (a == 0) {
            code = "ascendant_descendant";
            libelle = "Ascendant et descendant (ligne directe)";
        } else if (a == 1 && b == 1) {
            code = demi ? "demi_freres_soeurs" : "freres_soeurs";
            libelle = demi ? "Demi-frère et demi-sœur" : "Frère et sœur";
        } else if (a == 1 && b == 2) {
            code = "oncle_neveu";
            libelle = "Oncle ou tante et neveu ou nièce";
        } else if (a == 2 && b == 2) {
            code = "cousins_germains";
            libelle = "Cousins germains";
        } else if (a == 1 && b == 3) {
            code = "grand_oncle_petit_neveu";
            libelle = "Grand-oncle et petit-neveu";
        } else if (a == 3 && b == 3) {
            code = "cousins_issus_de_germains";
            libelle = "Cousins issus de germains";
        } else {
            code = "collateral_eloigne";
            libelle = "Parents collatéraux";
        }
        return new LienNomme(code, libelle, g1 + g2, a == 0 ? LIGNE_DIRECTE : COLLATERALE);
    }

    /** « 1er degré », « 4e degré »… */
    public static String degreEnLettres(int degre) {
        return (degre == 1 ? "1er" : degre + "e") + " degré";
    }
}
