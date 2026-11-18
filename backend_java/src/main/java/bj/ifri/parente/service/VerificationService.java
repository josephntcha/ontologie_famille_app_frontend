package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.erreur.Erreurs;
import bj.ifri.parente.ontologie.RaisonneurService;
import bj.ifri.parente.ontologie.SessionRaisonnement;
import bj.ifri.parente.web.dto.VerificationDto;
import bj.ifri.parente.web.dto.VerificationDto.AncetreDto;
import bj.ifri.parente.web.dto.VerificationDto.CompletudeDto;
import bj.ifri.parente.web.dto.VerificationDto.LienDeSangDto;

/**
 * Écran « Vérification » : lien de sang (degré et nom calculés en Java, existence confirmée par
 * HermiT via lienDeSangAvec), alliance, complétude et verdict sur le mariage.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private final ChargeurGraphe chargeur;
    private final GenealogieService genealogie;
    private final AllianceService alliance;
    private final CompletudeService completude;
    private final ReglesMariageService regles;
    private final RaisonneurService raisonneur;
    private final Representations rep;

    public VerificationService(ChargeurGraphe chargeur, GenealogieService genealogie, AllianceService alliance,
            CompletudeService completude, ReglesMariageService regles, RaisonneurService raisonneur,
            Representations rep) {
        this.chargeur = chargeur;
        this.genealogie = genealogie;
        this.alliance = alliance;
        this.completude = completude;
        this.regles = regles;
        this.raisonneur = raisonneur;
        this.rep = rep;
    }

    public VerificationDto verifier(Long x, Long y) {
        GrapheFamille g = chargeur.charger();
        Erreurs erreurs = new Erreurs();
        if (!g.existe(x)) {
            erreurs.ajouter("personne1_id", "La personne " + x + " n'existe pas.");
        }
        if (!g.existe(y)) {
            erreurs.ajouter("personne2_id", "La personne " + y + " n'existe pas.");
        } else if (y.equals(x)) {
            erreurs.ajouter("personne2_id", "Choisissez deux personnes différentes.");
        }
        erreurs.lever();

        LienDeSang sang = genealogie.lienDeSang(g, x, y);
        boolean infere;
        try (SessionRaisonnement session = raisonneur.ouvrir(g)) {
            infere = session.lienDeSangInfere(x, y);
        }
        LienAlliance lienAlliance = alliance.lienAlliance(g, x, y);

        Completude c1 = completude.evaluer(g, x);
        Completude c2 = completude.evaluer(g, y);
        boolean complet = c1.complet() && c2.complet();
        Map<Long, Manquant> manquants = new LinkedHashMap<>();
        for (Long id : List.of(x, y)) {
            completude.manquants(g, id).forEach(m -> manquants.putIfAbsent(m.personneId(), m));
        }

        List<AncetreDto> ancetres = new ArrayList<>();
        for (Long id : sang.ancetresCommuns()) {
            Personne a = g.personne(id);
            ancetres.add(new AncetreDto(a.getId(), a.getNom(), a.getPrenom()));
        }
        LienDeSangDto sangDto = new LienDeSangDto(sang.existe(), sang.type(), sang.degre(), sang.generationPersonne1(),
                sang.generationPersonne2(), sang.code(), sang.libelle(), sang.demi(), ancetres, sang.message(), infere);

        return new VerificationDto(rep.personne(g.personne(x)), rep.personne(g.personne(y)), sangDto, lienAlliance,
                regles.evaluer(g, x, y, sang, lienAlliance, complet),
                new CompletudeDto(c1, c2, complet, List.copyOf(manquants.values())));
    }
}
