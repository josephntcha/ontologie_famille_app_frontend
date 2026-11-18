package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.Personne;

/**
 * Complétude de l'arbre (monde ouvert : un parent non saisi est inconnu, pas inexistant) :
 * parents connus sur 2 et grands-parents connus sur 4.
 */
@Service
@Transactional(readOnly = true)
public class CompletudeService {

    public static final int PARENTS_ATTENDUS = 2;
    public static final int GRANDS_PARENTS_ATTENDUS = 4;

    private final ChargeurGraphe chargeur;

    public CompletudeService(ChargeurGraphe chargeur) {
        this.chargeur = chargeur;
    }

    public Completude evaluer(Long personneId) {
        return evaluer(chargeur.charger(), personneId);
    }

    public Completude evaluer(GrapheFamille graphe, Long personneId) {
        Set<Long> parents = graphe.parentsDe(personneId);
        int parentsConnus = Math.min(parents.size(), PARENTS_ATTENDUS);
        int grandsParents = 0;
        for (Long p : parents) {
            grandsParents += graphe.parentsDe(p).size();
        }
        grandsParents = Math.min(grandsParents, GRANDS_PARENTS_ATTENDUS);
        int generations = parentsConnus == PARENTS_ATTENDUS
                ? (grandsParents == GRANDS_PARENTS_ATTENDUS ? 2 : 1)
                : 0;
        return new Completude(parentsConnus, PARENTS_ATTENDUS, grandsParents, GRANDS_PARENTS_ATTENDUS, generations,
                generations == 2);
    }

    /**
     * Une entrée pour la personne si l'un de ses parents est inconnu, et une pour chaque parent
     * connu dont un parent est inconnu.
     */
    public List<Manquant> manquants(GrapheFamille graphe, Long personneId) {
        List<Manquant> r = new ArrayList<>();
        Personne p = graphe.personne(personneId);
        int inconnus = PARENTS_ATTENDUS - graphe.parentsDe(personneId).size();
        if (inconnus > 0) {
            r.add(new Manquant(personneId, p.nomComplet() + " : " + decrire(inconnus)));
        }
        for (Long parentId : graphe.parentsDe(personneId)) {
            int inconnusDuParent = PARENTS_ATTENDUS - graphe.parentsDe(parentId).size();
            if (inconnusDuParent > 0) {
                r.add(new Manquant(parentId, graphe.personne(parentId).nomComplet() + " (parent de "
                        + p.nomComplet() + ") : " + decrire(inconnusDuParent)));
            }
        }
        return r;
    }

    private static String decrire(int inconnus) {
        return inconnus == 1 ? "1 parent inconnu sur 2" : "2 parents inconnus sur 2";
    }
}
