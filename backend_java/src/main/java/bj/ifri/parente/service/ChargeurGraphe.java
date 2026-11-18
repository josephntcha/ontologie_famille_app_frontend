package bj.ifri.parente.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bj.ifri.parente.repository.BeauParenteRepository;
import bj.ifri.parente.repository.FiliationRepository;
import bj.ifri.parente.repository.MariageRepository;
import bj.ifri.parente.repository.PersonneRepository;

/** Lit l'état courant de la base (sans cache) sous forme de {@link GrapheFamille}. */
@Component
public class ChargeurGraphe {

    private final PersonneRepository personnes;
    private final FiliationRepository filiations;
    private final BeauParenteRepository beauxLiens;
    private final MariageRepository mariages;

    public ChargeurGraphe(PersonneRepository personnes, FiliationRepository filiations,
            BeauParenteRepository beauxLiens, MariageRepository mariages) {
        this.personnes = personnes;
        this.filiations = filiations;
        this.beauxLiens = beauxLiens;
        this.mariages = mariages;
    }

    @Transactional(readOnly = true)
    public GrapheFamille charger() {
        return new GrapheFamille(personnes.findAllByOrderByIdAsc(), filiations.findAllByOrderByIdAsc(),
                beauxLiens.findAllByOrderByIdAsc(), mariages.findAllByOrderByIdAsc());
    }
}
