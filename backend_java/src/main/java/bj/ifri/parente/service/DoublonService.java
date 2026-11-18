package bj.ifri.parente.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.repository.PersonneRepository;

/**
 * Doublons probables : même nom (insensible à la casse et aux espaces de bord) et même date de
 * naissance. Une personne saisie deux fois casserait la détection des liens.
 */
@Service
@Transactional(readOnly = true)
public class DoublonService {

    private final PersonneRepository personnes;

    public DoublonService(PersonneRepository personnes) {
        this.personnes = personnes;
    }

    public static String cle(String nom) {
        return nom == null ? "" : nom.trim().toLowerCase(Locale.ROOT);
    }

    /** Personnes existantes correspondant à p (hors p). */
    public List<Personne> doublonsDe(Personne p) {
        return personnes.findByDateNaissanceOrderByIdAsc(p.getDateNaissance()).stream()
                .filter(autre -> !Objects.equals(autre.getId(), p.getId()))
                .filter(autre -> cle(autre.getNom()).equals(cle(p.getNom())))
                .toList();
    }

    /** Groupes d'au moins deux personnes, triés par date de naissance puis par nom. */
    public List<List<Personne>> groupes() {
        Map<String, List<Personne>> parCle = new TreeMap<>();
        for (Personne p : personnes.findAllByOrderByIdAsc()) {
            LocalDate d = p.getDateNaissance();
            parCle.computeIfAbsent(d + "|" + cle(p.getNom()), k -> new ArrayList<>()).add(p);
        }
        return parCle.values().stream().filter(g -> g.size() >= 2).toList();
    }
}
