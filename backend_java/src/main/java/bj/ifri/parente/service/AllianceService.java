package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import bj.ifri.parente.domaine.CauseDissolution;
import bj.ifri.parente.domaine.Mariage;

/**
 * Lien d'alliance (étape 5 du MVP) : lien de sang entre chaque conjoint de l'un (mariage en
 * cours ou dissous) et l'autre personne, plus les liens beauParentDe déclarés entre les deux.
 * On retient la plus restrictive : ligne directe, puis plus petit degré, puis un mariage non
 * dissous par décès.
 */
@Service
public class AllianceService {

    private final GenealogieService genealogie;

    public AllianceService(GenealogieService genealogie) {
        this.genealogie = genealogie;
    }

    public LienAlliance lienAlliance(GrapheFamille graphe, Long x, Long y) {
        List<LienAlliance> candidats = new ArrayList<>();
        for (Long[] paire : new Long[][] {{x, y}, {y, x}}) {
            Long epoux = paire[0];
            Long autre = paire[1];
            for (Mariage m : graphe.mariagesDe(epoux)) {
                Long conjoint = GrapheFamille.autreConjoint(m, epoux);
                if (conjoint.equals(autre) || conjoint.equals(epoux)) {
                    continue;
                }
                LienDeSang sang = genealogie.lienDeSang(graphe, conjoint, autre);
                if (sang.existe()) {
                    candidats.add(candidat(sang.type(), sang.degre(), conjoint, m));
                }
            }
        }
        if (graphe.beauxEnfantsDe(x).contains(y) || graphe.beauxEnfantsDe(y).contains(x)) {
            candidats.add(candidat(NommageLien.LIGNE_DIRECTE, 1, null, null));
        }
        return candidats.stream()
                .min(Comparator.comparingInt((LienAlliance a) -> NommageLien.LIGNE_DIRECTE.equals(a.type()) ? 0 : 1)
                        .thenComparingInt(LienAlliance::degre)
                        .thenComparingInt(a -> a.mariageDissousParDeces() ? 1 : 0))
                .orElse(LienAlliance.aucune());
    }

    private static LienAlliance candidat(String type, int degre, Long conjointId, Mariage mariage) {
        boolean ligneDirecte = NommageLien.LIGNE_DIRECTE.equals(type);
        String code;
        String libelle;
        if (ligneDirecte && degre <= 1) {
            code = "beau_parent_bel_enfant";
            libelle = "Beau-parent et bel-enfant";
        } else if (ligneDirecte) {
            code = "ascendant_descendant_par_alliance";
            libelle = "Ascendant et descendant par alliance";
        } else if (degre <= 2) {
            code = "beau_frere_belle_soeur";
            libelle = "Beau-frère et belle-sœur";
        } else if (degre == 3) {
            code = "oncle_neveu_par_alliance";
            libelle = "Oncle ou tante et neveu ou nièce par alliance";
        } else {
            code = "allie_eloigne";
            libelle = "Allié éloigné";
        }
        boolean deces = mariage != null && mariage.getCauseDissolution() == CauseDissolution.deces;
        String message = "Lien d'alliance : " + libelle.toLowerCase() + " (" + NommageLien.degreEnLettres(degre) + ")"
                + (mariage != null
                        ? ", par le mariage n° " + mariage.getId() + (deces ? " (dissous par décès)" : "")
                        : ", lien beauParentDe déclaré");
        return new LienAlliance(true, type, degre, code, libelle, conjointId, mariage == null ? null : mariage.getId(),
                deces, message);
    }
}
