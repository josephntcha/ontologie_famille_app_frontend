package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Généalogie calculée dans le code : ancêtres et descendants avec génération minimale, refus des
 * cycles (OWL ne peut pas rendre irréflexive une propriété transitive), frères et sœurs déduits
 * des parents communs, lien de sang avec degré (SPEC_API.md 5.1).
 */
@Service
@Transactional(readOnly = true)
public class GenealogieService {

    private final ChargeurGraphe chargeur;
    private final NommageLien nommage;

    public GenealogieService(ChargeurGraphe chargeur, NommageLien nommage) {
        this.chargeur = chargeur;
        this.nommage = nommage;
    }

    public Map<Long, Integer> ancetres(Long personneId) {
        return chargeur.charger().ancetres(personneId);
    }

    public Map<Long, Integer> descendants(Long personneId) {
        return chargeur.charger().descendants(personneId);
    }

    /** Vrai si enfant = parent, ou si l'enfant est déjà ancêtre (à toute profondeur) du parent. */
    public boolean creeraitUnCycle(Long parentId, Long enfantId) {
        return creeraitUnCycle(chargeur.charger(), parentId, enfantId);
    }

    public boolean creeraitUnCycle(GrapheFamille graphe, Long parentId, Long enfantId) {
        return parentId.equals(enfantId) || graphe.ancetres(parentId).containsKey(enfantId);
    }

    public Set<Long> freresEtSoeurs(Long personneId) {
        return chargeur.charger().freresEtSoeurs(personneId);
    }

    /** Lien de sang entre x et y (algorithme 5.1). */
    public LienDeSang lienDeSang(GrapheFamille graphe, Long x, Long y) {
        Map<Long, Integer> ancX = graphe.ancetres(x);
        Map<Long, Integer> ancY = graphe.ancetres(y);

        if (ancX.containsKey(y)) {
            return ligneDirecte(ancX.get(y), 0, y);
        }
        if (ancY.containsKey(x)) {
            return ligneDirecte(0, ancY.get(x), x);
        }

        int minimum = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> e : ancX.entrySet()) {
            Integer gy = ancY.get(e.getKey());
            if (gy != null) {
                minimum = Math.min(minimum, e.getValue() + gy);
            }
        }
        if (minimum == Integer.MAX_VALUE) {
            return LienDeSang.aucun();
        }

        List<Long> communs = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : ancX.entrySet()) {
            Integer gy = ancY.get(e.getKey());
            if (gy != null && e.getValue() + gy == minimum) {
                communs.add(e.getKey());
            }
        }
        communs.sort(null);
        Long retenu = communs.get(0);
        int g1 = ancX.get(retenu);
        int g2 = ancY.get(retenu);

        boolean demi = false;
        if (g1 == 1 && g2 == 1) {
            Set<Long> parentsCommuns = new HashSet<>(graphe.parentsDe(x));
            parentsCommuns.retainAll(graphe.parentsDe(y));
            demi = parentsCommuns.size() == 1;
        }
        LienNomme nom = nommage.nommer(g1, g2, demi);
        return new LienDeSang(true, nom.type(), nom.degre(), g1, g2, nom.code(), nom.libelle(), demi,
                List.copyOf(communs), message(nom));
    }

    private LienDeSang ligneDirecte(int g1, int g2, Long ancetre) {
        LienNomme nom = nommage.nommer(g1, g2, false);
        return new LienDeSang(true, nom.type(), nom.degre(), g1, g2, nom.code(), nom.libelle(), false,
                List.of(ancetre), message(nom));
    }

    private static String message(LienNomme nom) {
        return "Lien de sang : " + nom.libelle().toLowerCase() + " (" + NommageLien.degreEnLettres(nom.degre()) + ")";
    }

    /**
     * Chemin de x à y par l'ancêtre commun le plus proche (en ligne directe : de l'un à l'autre),
     * ou chemin vide s'il n'y a pas de lien de sang.
     */
    public Chemin chemin(GrapheFamille graphe, Long x, Long y) {
        LienDeSang lien = lienDeSang(graphe, x, y);
        if (!lien.existe()) {
            return new Chemin(List.of(), null);
        }
        Long ancetre = lien.ancetresCommuns().get(0);
        List<Long> montee = graphe.cheminVersAncetre(x, ancetre);
        List<Long> descente = new ArrayList<>(graphe.cheminVersAncetre(y, ancetre));
        java.util.Collections.reverse(descente);
        List<Long> chemin = new ArrayList<>(montee);
        chemin.addAll(descente.subList(1, descente.size()));
        return new Chemin(chemin, ancetre);
    }

    /** Chemin entre deux personnes et ancêtre commun retenu. */
    public record Chemin(List<Long> ids, Long ancetreCommunId) {
    }
}
