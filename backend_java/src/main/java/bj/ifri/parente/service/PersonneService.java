package bj.ifri.parente.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.domaine.Sexe;
import bj.ifri.parente.erreur.RessourceIntrouvable;
import bj.ifri.parente.ontologie.RaisonneurService;
import bj.ifri.parente.ontologie.SessionRaisonnement;
import bj.ifri.parente.repository.BeauParenteRepository;
import bj.ifri.parente.repository.FiliationRepository;
import bj.ifri.parente.repository.MariageRepository;
import bj.ifri.parente.repository.PersonneRepository;
import bj.ifri.parente.web.dto.CreationPersonne;
import bj.ifri.parente.web.dto.CreationPersonneReponse;
import bj.ifri.parente.web.dto.GroupeDoublonsDto;
import bj.ifri.parente.web.dto.ModificationPersonne;
import bj.ifri.parente.web.dto.PersonneDetailDto;
import bj.ifri.parente.web.dto.PersonneDto;
import bj.ifri.parente.web.validation.DateIsoValidator;

/** Écran « Personnes » : CRUD, doublons probables, détail avec classes inférées par HermiT. */
@Service
@Transactional
public class PersonneService {

    private final PersonneRepository personnes;
    private final FiliationRepository filiations;
    private final BeauParenteRepository beauxLiens;
    private final MariageRepository mariages;
    private final DoublonService doublons;
    private final ChargeurGraphe chargeur;
    private final RaisonneurService raisonneur;
    private final Representations rep;

    public PersonneService(PersonneRepository personnes, FiliationRepository filiations,
            BeauParenteRepository beauxLiens, MariageRepository mariages, DoublonService doublons,
            ChargeurGraphe chargeur, RaisonneurService raisonneur, Representations rep) {
        this.personnes = personnes;
        this.filiations = filiations;
        this.beauxLiens = beauxLiens;
        this.mariages = mariages;
        this.doublons = doublons;
        this.chargeur = chargeur;
        this.raisonneur = raisonneur;
        this.rep = rep;
    }

    public Personne trouver(Long id) {
        return personnes.findById(id).orElseThrow(() -> RessourceIntrouvable.personne(id));
    }

    @Transactional(readOnly = true)
    public List<PersonneDto> lister() {
        return rep.personnes(personnes.findAllByOrderByIdAsc());
    }

    public CreationPersonneReponse creer(CreationPersonne c) {
        Personne p = personnes.save(new Personne(c.nom().trim(), c.prenom().trim(), Sexe.valueOf(c.sexe()),
                DateIsoValidator.analyser(c.dateNaissance())));
        return new CreationPersonneReponse(rep.personne(p), rep.personnes(doublons.doublonsDe(p)));
    }

    @Transactional(readOnly = true)
    public List<GroupeDoublonsDto> groupesDeDoublons() {
        return doublons.groupes().stream()
                .map(g -> new GroupeDoublonsDto(g.get(0).getNom().trim(), g.get(0).getDateNaissance(),
                        rep.personnes(g)))
                .toList();
    }

    /** Détail : relations lues en base, classes de la personne et de ses mariages par un seul raisonneur. */
    @Transactional(readOnly = true)
    public PersonneDetailDto detail(Long id) {
        GrapheFamille g = chargeur.charger();
        Personne p = g.personne(id);
        if (p == null) {
            throw RessourceIntrouvable.personne(id);
        }
        Set<Long> fratrie = g.freresEtSoeurs(id);
        List<Mariage> sesMariages = g.mariagesDe(id);
        try (SessionRaisonnement session = raisonneur.ouvrir(g)) {
            PersonneDto base = rep.personne(p);
            return new PersonneDetailDto(base.id(), base.nom(), base.prenom(), base.sexe(), base.dateNaissance(),
                    base.age(), base.uri(),
                    personnes(g, g.parentsDe(id)),
                    personnes(g, g.enfantsDe(id)),
                    personnes(g, g.beauxParentsDe(id)),
                    personnes(g, g.beauxEnfantsDe(id)),
                    personnes(g, fratrie.stream().filter(f -> g.personne(f).getSexe() == Sexe.M).toList()),
                    personnes(g, fratrie.stream().filter(f -> g.personne(f).getSexe() == Sexe.F).toList()),
                    personnes(g, g.conjointsDe(id)),
                    sesMariages.stream().map(m -> rep.mariage(m, session.classesMariage(m.getId()))).toList(),
                    session.classesPersonne(id));
        }
    }

    private List<PersonneDto> personnes(GrapheFamille g, java.util.Collection<Long> ids) {
        return ids.stream().sorted().map(g::personne).map(rep::personne).toList();
    }

    /** Mise à jour partielle : la requête a déjà été validée, rien n'est modifié en cas d'erreur. */
    public PersonneDto modifier(Long id, ModificationPersonne m) {
        Personne p = trouver(id);
        if (m.nom() != null) {
            p.setNom(m.nom().trim());
        }
        if (m.prenom() != null) {
            p.setPrenom(m.prenom().trim());
        }
        if (m.sexe() != null) {
            p.setSexe(Sexe.valueOf(m.sexe()));
        }
        if (m.dateNaissance() != null) {
            p.setDateNaissance(DateIsoValidator.analyser(m.dateNaissance()));
        }
        return rep.personne(personnes.save(p));
    }

    /** Supprime la personne, ses filiations, ses beaux-liens et ses mariages. */
    public void supprimer(Long id) {
        Personne p = trouver(id);
        filiations.deleteAll(filiations.concernant(id));
        beauxLiens.deleteAll(beauxLiens.concernant(id));
        mariages.deleteAll(mariages.concernant(id));
        personnes.delete(p);
    }
}
