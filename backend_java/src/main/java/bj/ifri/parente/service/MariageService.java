package bj.ifri.parente.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.CauseDissolution;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.erreur.Erreurs;
import bj.ifri.parente.erreur.RessourceIntrouvable;
import bj.ifri.parente.ontologie.RaisonneurService;
import bj.ifri.parente.ontologie.SessionRaisonnement;
import bj.ifri.parente.repository.MariageRepository;
import bj.ifri.parente.repository.PersonneRepository;
import bj.ifri.parente.web.dto.CreationMariage;
import bj.ifri.parente.web.dto.DissolutionMariage;
import bj.ifri.parente.web.dto.MariageDto;
import bj.ifri.parente.web.validation.DateIsoValidator;

/**
 * Mariages (individus f:Mariage) : saisie, dissolution, suppression. Un mariage sans date de fin
 * est en cours (art. 125 et 143) ; les classes renvoyées (MariageDissous…) viennent de HermiT.
 */
@Service
@Transactional
public class MariageService {

    private final MariageRepository mariages;
    private final PersonneRepository personnes;
    private final RaisonneurService raisonneur;
    private final Representations rep;

    public MariageService(MariageRepository mariages, PersonneRepository personnes, RaisonneurService raisonneur,
            Representations rep) {
        this.mariages = mariages;
        this.personnes = personnes;
        this.raisonneur = raisonneur;
        this.rep = rep;
    }

    private Mariage trouver(Long id) {
        return mariages.findById(id).orElseThrow(() -> RessourceIntrouvable.mariage(id));
    }

    /** Représentation avec les classes inférées (un raisonneur pour la requête). */
    private MariageDto representer(Mariage m) {
        mariages.flush();
        try (SessionRaisonnement session = raisonneur.ouvrir()) {
            return rep.mariage(m, session.classesMariage(m.getId()));
        }
    }

    @Transactional(readOnly = true)
    public List<MariageDto> lister() {
        List<Mariage> tous = mariages.findAllByOrderByIdAsc();
        if (tous.isEmpty()) {
            return List.of();
        }
        try (SessionRaisonnement session = raisonneur.ouvrir()) {
            session.precalculerTypes();
            return tous.stream().map(m -> rep.mariage(m, session.classesMariage(m.getId()))).toList();
        }
    }

    @Transactional(readOnly = true)
    public MariageDto afficher(Long id) {
        return representer(trouver(id));
    }

    public MariageDto creer(CreationMariage c) {
        Erreurs erreurs = new Erreurs();
        Personne c1 = personnes.findById(c.conjoint1Id()).orElse(null);
        Personne c2 = personnes.findById(c.conjoint2Id()).orElse(null);
        if (c1 == null) {
            erreurs.ajouter("conjoint1_id", "Le conjoint " + c.conjoint1Id() + " n'existe pas.");
        }
        if (c2 == null) {
            erreurs.ajouter("conjoint2_id", "Le conjoint " + c.conjoint2Id() + " n'existe pas.");
        }
        if (c1 != null && c2 != null && c1.getId().equals(c2.getId())) {
            erreurs.ajouter("conjoint2_id", "On ne peut pas se marier avec soi-même (marieA est irréflexive).");
        }
        LocalDate debut = DateIsoValidator.analyser(c.dateDebut());
        LocalDate fin = DateIsoValidator.analyser(c.dateFin());
        CauseDissolution cause = CauseDissolution.depuis(c.causeDissolution());
        verifierDissolution(debut, fin, cause, erreurs);
        erreurs.lever();

        if (fin == null) {
            if (mariages.aUnMariageEnCours(c1.getId())) {
                erreurs.ajouter("conjoint1_id", c1.nomComplet() + " a déjà un mariage en cours (art. 125 et 143).");
            } else if (mariages.aUnMariageEnCours(c2.getId())) {
                erreurs.ajouter("conjoint2_id", c2.nomComplet() + " a déjà un mariage en cours (art. 125 et 143).");
            }
            erreurs.lever();
        }
        return representer(mariages.save(new Mariage(c1, c2, debut, fin, cause)));
    }

    /** Dissolution : date de fin et cause vont ensemble, la fin n'est pas antérieure au début. */
    public MariageDto dissoudre(Long id, DissolutionMariage d) {
        Mariage m = trouver(id);
        Erreurs erreurs = new Erreurs();
        LocalDate fin = DateIsoValidator.analyser(d.dateFin());
        CauseDissolution cause = CauseDissolution.depuis(d.causeDissolution());
        if (fin == null && cause == null) {
            erreurs.ajouter("date_fin", "La date de fin et la cause sont requises pour dissoudre un mariage.");
        }
        verifierDissolution(m.getDateDebut(), fin, cause, erreurs);
        erreurs.lever();
        m.dissoudre(fin, cause);
        return representer(mariages.save(m));
    }

    private static void verifierDissolution(LocalDate debut, LocalDate fin, CauseDissolution cause, Erreurs erreurs) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            erreurs.ajouter("date_fin", "La date de fin ne peut pas être antérieure à la date de début.");
        }
        if ((fin == null) != (cause == null)) {
            erreurs.ajouter("cause_dissolution",
                    "La date de fin et la cause de dissolution (deces ou divorce) vont ensemble.");
        }
    }

    public void supprimer(Long id) {
        mariages.delete(trouver(id));
    }
}
