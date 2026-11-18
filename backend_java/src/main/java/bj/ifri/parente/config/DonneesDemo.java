package bj.ifri.parente.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.repository.PersonneRepository;
import bj.ifri.parente.service.LienService;
import bj.ifri.parente.service.MariageService;
import bj.ifri.parente.service.PersonneService;
import bj.ifri.parente.web.dto.CreationBeauParente;
import bj.ifri.parente.web.dto.CreationFiliation;
import bj.ifri.parente.web.dto.CreationMariage;
import bj.ifri.parente.web.dto.CreationPersonne;

/**
 * Famille de démonstration (profil {@code demo}), créée si la base est vide, en passant par les
 * services (mêmes règles que l'API) :
 * <pre>
 *   Grandpa ══ Grandma (mariés en 1955, dissous par décès en 2015)        Autre
 *       ├── Papa ══ Maman (en cours)          Maman ── Demi (avec Autre)
 *       │     └── Kid                         Papa beauParentDe Demi (recomposition)
 *       └── Tonton ══ Tantine (divorcés en 2010)
 *             └── Cousin
 * </pre>
 * Avec le beauParentDe, HermiT classe Demi en DemiFrere et Kid en DemiSoeur.
 */
@Component
@Profile("demo")
public class DonneesDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DonneesDemo.class);

    private final PersonneRepository personnes;
    private final PersonneService personneService;
    private final LienService liens;
    private final MariageService mariages;

    public DonneesDemo(PersonneRepository personnes, PersonneService personneService, LienService liens,
            MariageService mariages) {
        this.personnes = personnes;
        this.personneService = personneService;
        this.liens = liens;
        this.mariages = mariages;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (personnes.count() > 0) {
            LOG.info("Données de démonstration non créées : la base contient déjà des personnes.");
            return;
        }
        Long grandpa = personne("Martin", "Grandpa", "M", "1930-03-10");
        Long grandma = personne("Martin", "Grandma", "F", "1932-07-21");
        Long papa = personne("Martin", "Papa", "M", "1958-05-02");
        Long tonton = personne("Martin", "Tonton", "M", "1960-09-15");
        Long maman = personne("Durand", "Maman", "F", "1962-02-14");
        Long autre = personne("Bernard", "Autre", "M", "1955-11-30");
        Long demi = personne("Bernard", "Demi", "M", "1982-08-30");
        Long tantine = personne("Petit", "Tantine", "F", "1963-12-01");
        Long kid = personne("Martin", "Kid", "F", "1990-04-12");
        Long cousin = personne("Martin", "Cousin", "M", "1991-10-05");

        parent(grandpa, papa);
        parent(grandma, papa);
        parent(grandpa, tonton);
        parent(grandma, tonton);
        parent(papa, kid);
        parent(maman, kid);
        parent(maman, demi);
        parent(autre, demi);
        parent(tonton, cousin);
        parent(tantine, cousin);

        liens.creerBeauParente(new CreationBeauParente(papa, demi));

        mariages.creer(new CreationMariage(grandpa, grandma, "1955-06-18", "2015-01-20", "deces"));
        mariages.creer(new CreationMariage(papa, maman, "1988-06-01", null, null));
        mariages.creer(new CreationMariage(tonton, tantine, "1989-09-09", "2010-03-15", "divorce"));

        LOG.info("Famille de démonstration créée : Kid = {}, Cousin = {}, Demi = {}, Tonton = {}, Maman = {}.",
                kid, cousin, demi, tonton, maman);
    }

    private Long personne(String nom, String prenom, String sexe, String naissance) {
        return personneService.creer(new CreationPersonne(nom, prenom, sexe, naissance)).data().id();
    }

    private void parent(Long parent, Long enfant) {
        liens.creerFiliation(new CreationFiliation(parent, enfant));
    }
}
