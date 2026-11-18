package bj.ifri.parente.service;

import java.util.List;

import org.springframework.stereotype.Component;

import bj.ifri.parente.config.DateReference;
import bj.ifri.parente.domaine.BeauParente;
import bj.ifri.parente.domaine.Filiation;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.ontologie.OntologieService;
import bj.ifri.parente.ontologie.ProjectionABox;
import bj.ifri.parente.web.dto.BeauParenteDto;
import bj.ifri.parente.web.dto.FiliationDto;
import bj.ifri.parente.web.dto.MariageDto;
import bj.ifri.parente.web.dto.PersonneDto;

/** Conversion des entités en représentations JSON. */
@Component
public class Representations {

    private final DateReference dateReference;
    private final OntologieService ontologie;

    public Representations(DateReference dateReference, OntologieService ontologie) {
        this.dateReference = dateReference;
        this.ontologie = ontologie;
    }

    public PersonneDto personne(Personne p) {
        return new PersonneDto(p.getId(), p.getNom(), p.getPrenom(), p.getSexe().name(), p.getDateNaissance(),
                dateReference.age(p.getDateNaissance()),
                ontologie.namespace() + ProjectionABox.individuPersonne(p.getId()));
    }

    public List<PersonneDto> personnes(List<Personne> personnes) {
        return personnes.stream().map(this::personne).toList();
    }

    /** {@code classes} : classes du mariage inférées par HermiT. */
    public MariageDto mariage(Mariage m, List<String> classes) {
        return new MariageDto(m.getId(), m.getConjoint1Id(), m.getConjoint2Id(), m.getDateDebut(), m.getDateFin(),
                m.getCauseDissolution() == null ? null : m.getCauseDissolution().name(), m.estEnCours(), classes);
    }

    public FiliationDto filiation(Filiation f) {
        return new FiliationDto(f.getId(), f.getParentId(), f.getEnfantId());
    }

    public BeauParenteDto beauParente(BeauParente b) {
        return new BeauParenteDto(b.getId(), b.getBeauParentId(), b.getEnfantId());
    }
}
