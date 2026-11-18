package bj.ifri.parente.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import bj.ifri.parente.domaine.BeauParente;
import bj.ifri.parente.domaine.Filiation;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;

/**
 * Instantané de l'arbre lu en base pour une requête (personnes, parentDe, beauParentDe,
 * mariages) avec les parcours utiles : ancêtres et descendants avec génération minimale
 * (parcours en largeur), frères et sœurs déduits des parents communs, conjoints.
 * Il n'est jamais conservé d'une requête à l'autre.
 */
public final class GrapheFamille {

    private final Map<Long, Personne> personnes = new TreeMap<>();
    private final List<Filiation> filiations;
    private final List<BeauParente> beauxLiens;
    private final List<Mariage> mariages;
    private final Map<Long, Set<Long>> parents = new HashMap<>();
    private final Map<Long, Set<Long>> enfants = new HashMap<>();
    private final Map<Long, Set<Long>> beauxParents = new HashMap<>();
    private final Map<Long, Set<Long>> beauxEnfants = new HashMap<>();
    private final Map<Long, List<Mariage>> mariagesParPersonne = new HashMap<>();

    public GrapheFamille(List<Personne> personnes, List<Filiation> filiations, List<BeauParente> beauxLiens,
            List<Mariage> mariages) {
        personnes.forEach(p -> this.personnes.put(p.getId(), p));
        this.filiations = List.copyOf(filiations);
        this.beauxLiens = List.copyOf(beauxLiens);
        this.mariages = List.copyOf(mariages);
        for (Filiation f : filiations) {
            ajouter(parents, f.getEnfantId(), f.getParentId());
            ajouter(enfants, f.getParentId(), f.getEnfantId());
        }
        for (BeauParente b : beauxLiens) {
            ajouter(beauxParents, b.getEnfantId(), b.getBeauParentId());
            ajouter(beauxEnfants, b.getBeauParentId(), b.getEnfantId());
        }
        for (Mariage m : mariages) {
            mariagesParPersonne.computeIfAbsent(m.getConjoint1Id(), k -> new ArrayList<>()).add(m);
            mariagesParPersonne.computeIfAbsent(m.getConjoint2Id(), k -> new ArrayList<>()).add(m);
        }
    }

    private static void ajouter(Map<Long, Set<Long>> index, Long cle, Long valeur) {
        index.computeIfAbsent(cle, k -> new TreeSet<>()).add(valeur);
    }

    // ------------------------------------------------------------------ accès

    public Personne personne(Long id) {
        return personnes.get(id);
    }

    public boolean existe(Long id) {
        return personnes.containsKey(id);
    }

    /** Personnes triées par id. */
    public List<Personne> personnes() {
        return List.copyOf(personnes.values());
    }

    public List<Filiation> filiations() {
        return filiations;
    }

    public List<BeauParente> beauxLiens() {
        return beauxLiens;
    }

    public List<Mariage> mariages() {
        return mariages;
    }

    public Set<Long> parentsDe(Long id) {
        return parents.getOrDefault(id, Collections.emptySet());
    }

    public Set<Long> enfantsDe(Long id) {
        return enfants.getOrDefault(id, Collections.emptySet());
    }

    public Set<Long> beauxParentsDe(Long id) {
        return beauxParents.getOrDefault(id, Collections.emptySet());
    }

    public Set<Long> beauxEnfantsDe(Long id) {
        return beauxEnfants.getOrDefault(id, Collections.emptySet());
    }

    /** Mariages (en cours ou dissous) de la personne, par id croissant. */
    public List<Mariage> mariagesDe(Long id) {
        List<Mariage> l = new ArrayList<>(mariagesParPersonne.getOrDefault(id, List.of()));
        l.sort((a, b) -> a.getId().compareTo(b.getId()));
        return l;
    }

    /** Conjoints (mariages en cours ou dissous), triés. */
    public Set<Long> conjointsDe(Long id) {
        Set<Long> r = new TreeSet<>();
        for (Mariage m : mariagesParPersonne.getOrDefault(id, List.of())) {
            r.add(autreConjoint(m, id));
        }
        r.remove(id);
        return r;
    }

    public static Long autreConjoint(Mariage m, Long id) {
        return m.getConjoint1Id().equals(id) ? m.getConjoint2Id() : m.getConjoint1Id();
    }

    // ------------------------------------------------------------------ parcours

    /** Ancêtres (id → génération minimale : parents = 1…), sans la personne. */
    public Map<Long, Integer> ancetres(Long id) {
        return parcoursEnLargeur(id, this::parentsDe);
    }

    /** Descendants (id → génération minimale : enfants = 1…), sans la personne. */
    public Map<Long, Integer> descendants(Long id) {
        return parcoursEnLargeur(id, this::enfantsDe);
    }

    private static Map<Long, Integer> parcoursEnLargeur(Long depart, Function<Long, Set<Long>> voisins) {
        Map<Long, Integer> generations = new LinkedHashMap<>();
        Deque<Long> file = new ArrayDeque<>();
        Map<Long, Integer> vus = new HashMap<>();
        vus.put(depart, 0);
        file.add(depart);
        while (!file.isEmpty()) {
            Long courant = file.poll();
            int g = vus.get(courant);
            for (Long v : voisins.apply(courant)) {
                if (!vus.containsKey(v)) {
                    vus.put(v, g + 1);
                    generations.put(v, g + 1);
                    file.add(v);
                }
            }
        }
        return generations;
    }

    /**
     * Plus court chemin en remontant les parents, de {@code depart} jusqu'à {@code ancetre}
     * inclus (liste vide si ancetre n'est pas un ancêtre).
     */
    public List<Long> cheminVersAncetre(Long depart, Long ancetre) {
        Map<Long, Long> precedent = new HashMap<>();
        Deque<Long> file = new ArrayDeque<>();
        precedent.put(depart, null);
        file.add(depart);
        while (!file.isEmpty()) {
            Long courant = file.poll();
            if (courant.equals(ancetre)) {
                List<Long> chemin = new ArrayList<>();
                for (Long n = courant; n != null; n = precedent.get(n)) {
                    chemin.add(n);
                }
                Collections.reverse(chemin);
                return chemin;
            }
            for (Long p : parentsDe(courant)) {
                if (!precedent.containsKey(p)) {
                    precedent.put(p, courant);
                    file.add(p);
                }
            }
        }
        return List.of();
    }

    /** Frères et sœurs déduits : au moins un parent commun (demi compris), sans la personne. */
    public Set<Long> freresEtSoeurs(Long id) {
        Set<Long> r = new TreeSet<>();
        for (Long p : parentsDe(id)) {
            r.addAll(enfantsDe(p));
        }
        r.remove(id);
        return r;
    }
}
