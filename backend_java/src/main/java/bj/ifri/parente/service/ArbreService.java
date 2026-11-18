package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.BeauParente;
import bj.ifri.parente.domaine.Filiation;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.erreur.Erreurs;
import bj.ifri.parente.ontologie.RaisonneurService;
import bj.ifri.parente.ontologie.SessionRaisonnement;
import bj.ifri.parente.web.dto.ArbreDto;
import bj.ifri.parente.web.dto.ArbreDto.Arete;
import bj.ifri.parente.web.dto.ArbreDto.Noeud;

/**
 * Écran « Arbre » : ascendants, descendants et conjoints des deux personnes, arêtes entre ces
 * nœuds, chemin par l'ancêtre commun le plus proche ; classes de tous les nœuds inférées par un
 * même raisonneur HermiT.
 */
@Service
@Transactional(readOnly = true)
public class ArbreService {

    private final ChargeurGraphe chargeur;
    private final GenealogieService genealogie;
    private final RaisonneurService raisonneur;

    public ArbreService(ChargeurGraphe chargeur, GenealogieService genealogie, RaisonneurService raisonneur) {
        this.chargeur = chargeur;
        this.genealogie = genealogie;
        this.raisonneur = raisonneur;
    }

    /** Paramètres bruts de la requête (chaînes) : 422 si absents, non numériques ou inexistants. */
    public ArbreDto construire(String personne1, String personne2) {
        GrapheFamille g = chargeur.charger();
        Erreurs erreurs = new Erreurs();
        Long x = identifiant("personne1_id", personne1, g, erreurs);
        Long y = identifiant("personne2_id", personne2, g, erreurs);
        erreurs.lever();
        return construire(g, x, y);
    }

    private static Long identifiant(String champ, String valeur, GrapheFamille g, Erreurs erreurs) {
        if (valeur == null || valeur.isBlank()) {
            erreurs.ajouter(champ, "Le champ " + champ + " est requis.");
            return null;
        }
        try {
            Long id = Long.valueOf(valeur.trim());
            if (!g.existe(id)) {
                erreurs.ajouter(champ, "La personne " + id + " n'existe pas.");
            }
            return id;
        } catch (NumberFormatException e) {
            erreurs.ajouter(champ, "Le champ " + champ + " doit être un identifiant entier.");
            return null;
        }
    }

    private ArbreDto construire(GrapheFamille g, Long x, Long y) {
        GenealogieService.Chemin chemin = genealogie.chemin(g, x, y);

        Set<Long> ids = new TreeSet<>();
        for (Long p : new LinkedHashSet<>(List.of(x, y))) {
            ids.add(p);
            ids.addAll(g.ancetres(p).keySet());
            ids.addAll(g.descendants(p).keySet());
            ids.addAll(g.conjointsDe(p));
        }
        ids.addAll(chemin.ids());

        List<Noeud> noeuds = new ArrayList<>();
        try (SessionRaisonnement session = raisonneur.ouvrir(g)) {
            session.precalculerTypes();
            for (Long id : ids) {
                Personne p = g.personne(id);
                noeuds.add(new Noeud(p.getId(), p.getNom(), p.getPrenom(), p.getSexe().name(),
                        session.classesPersonne(id)));
            }
        }

        Set<Arete> aretes = new LinkedHashSet<>();
        for (Filiation f : g.filiations()) {
            ajouter(aretes, ids, f.getParentId(), f.getEnfantId(), "parentDe");
        }
        for (BeauParente b : g.beauxLiens()) {
            ajouter(aretes, ids, b.getBeauParentId(), b.getEnfantId(), "beauParentDe");
        }
        for (Mariage m : g.mariages()) {
            ajouter(aretes, ids, m.getConjoint1Id(), m.getConjoint2Id(), "marieA");
        }
        return new ArbreDto(noeuds, List.copyOf(aretes), chemin.ids(), chemin.ancetreCommunId());
    }

    private static void ajouter(Set<Arete> aretes, Set<Long> ids, Long source, Long cible, String type) {
        if (ids.contains(source) && ids.contains(cible)) {
            aretes.add(new Arete(source, cible, type));
        }
    }
}
