package bj.ifri.parente.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.BeauParente;
import bj.ifri.parente.domaine.Filiation;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.erreur.Erreurs;
import bj.ifri.parente.erreur.RessourceIntrouvable;
import bj.ifri.parente.repository.BeauParenteRepository;
import bj.ifri.parente.repository.FiliationRepository;
import bj.ifri.parente.repository.PersonneRepository;
import bj.ifri.parente.web.dto.BeauParenteDto;
import bj.ifri.parente.web.dto.CreationBeauParente;
import bj.ifri.parente.web.dto.CreationFiliation;
import bj.ifri.parente.web.dto.FiliationDto;

/**
 * Écran « Liens » : parentDe et beauParentDe, avec les refus que l'ontologie ne peut pas
 * imposer seule (cycles, irréflexivité, au plus deux parents, propriétés disjointes).
 */
@Service
@Transactional
public class LienService {

    private final PersonneRepository personnes;
    private final FiliationRepository filiations;
    private final BeauParenteRepository beauxLiens;
    private final GenealogieService genealogie;
    private final ChargeurGraphe chargeur;
    private final Representations rep;

    public LienService(PersonneRepository personnes, FiliationRepository filiations,
            BeauParenteRepository beauxLiens, GenealogieService genealogie, ChargeurGraphe chargeur,
            Representations rep) {
        this.personnes = personnes;
        this.filiations = filiations;
        this.beauxLiens = beauxLiens;
        this.genealogie = genealogie;
        this.chargeur = chargeur;
        this.rep = rep;
    }

    // ------------------------------------------------------------------ parentDe

    @Transactional(readOnly = true)
    public List<FiliationDto> filiations() {
        return filiations.findAllByOrderByIdAsc().stream().map(rep::filiation).toList();
    }

    public FiliationDto creerFiliation(CreationFiliation c) {
        Erreurs erreurs = new Erreurs();
        Personne parent = personnes.findById(c.parentId()).orElse(null);
        Personne enfant = personnes.findById(c.enfantId()).orElse(null);
        if (parent == null) {
            erreurs.ajouter("parent_id", "Le parent " + c.parentId() + " n'existe pas.");
        }
        if (enfant == null) {
            erreurs.ajouter("enfant_id", "L'enfant " + c.enfantId() + " n'existe pas.");
        }
        erreurs.lever();

        if (parent.getId().equals(enfant.getId())) {
            erreurs.ajouter("enfant_id", "Une personne ne peut pas être son propre parent (parentDe est irréflexive).");
            erreurs.lever();
        }
        if (filiations.existe(parent.getId(), enfant.getId())) {
            erreurs.ajouter("enfant_id", "Ce lien parentDe existe déjà.");
        } else if (filiations.nombreDeParents(enfant.getId()) >= 2) {
            erreurs.ajouter("enfant_id", enfant.nomComplet() + " a déjà deux parents.");
        }
        if (genealogie.creeraitUnCycle(chargeur.charger(), parent.getId(), enfant.getId())) {
            erreurs.ajouter("parent_id", enfant.nomComplet() + " est déjà ancêtre de " + parent.nomComplet()
                    + " : ce lien créerait un cycle.");
        }
        if (!parent.getDateNaissance().isBefore(enfant.getDateNaissance())) {
            erreurs.ajouter("parent_id", "Le parent doit être né strictement avant l'enfant.");
        }
        if (beauxLiens.existe(parent.getId(), enfant.getId())
                || beauxLiens.existe(enfant.getId(), parent.getId())) {
            erreurs.ajouter("parent_id", "Un lien beauParentDe existe déjà entre ces deux personnes "
                    + "(parentDe et beauParentDe sont disjointes).");
        }
        erreurs.lever();
        return rep.filiation(filiations.save(new Filiation(parent, enfant)));
    }

    public void supprimerFiliation(Long id) {
        Filiation f = filiations.findById(id)
                .orElseThrow(() -> new RessourceIntrouvable("Lien parentDe " + id + " introuvable."));
        filiations.delete(f);
    }

    // ------------------------------------------------------------------ beauParentDe

    @Transactional(readOnly = true)
    public List<BeauParenteDto> beauxLiens() {
        return beauxLiens.findAllByOrderByIdAsc().stream().map(rep::beauParente).toList();
    }

    public BeauParenteDto creerBeauParente(CreationBeauParente c) {
        Erreurs erreurs = new Erreurs();
        Personne beauParent = personnes.findById(c.beauParentId()).orElse(null);
        Personne enfant = personnes.findById(c.enfantId()).orElse(null);
        if (beauParent == null) {
            erreurs.ajouter("beau_parent_id", "Le beau-parent " + c.beauParentId() + " n'existe pas.");
        }
        if (enfant == null) {
            erreurs.ajouter("enfant_id", "L'enfant " + c.enfantId() + " n'existe pas.");
        }
        erreurs.lever();

        if (beauParent.getId().equals(enfant.getId())) {
            erreurs.ajouter("enfant_id", "Une personne ne peut pas être son propre beau-parent.");
            erreurs.lever();
        }
        if (beauxLiens.existe(beauParent.getId(), enfant.getId())) {
            erreurs.ajouter("enfant_id", "Ce lien beauParentDe existe déjà.");
        }
        if (filiations.existe(beauParent.getId(), enfant.getId())
                || filiations.existe(enfant.getId(), beauParent.getId())) {
            erreurs.ajouter("beau_parent_id", "Un lien parentDe existe déjà entre ces deux personnes "
                    + "(parentDe et beauParentDe sont disjointes).");
        }
        erreurs.lever();
        return rep.beauParente(beauxLiens.save(new BeauParente(beauParent, enfant)));
    }

    public void supprimerBeauParente(Long id) {
        BeauParente b = beauxLiens.findById(id)
                .orElseThrow(() -> new RessourceIntrouvable("Lien beauParentDe " + id + " introuvable."));
        beauxLiens.delete(b);
    }
}
