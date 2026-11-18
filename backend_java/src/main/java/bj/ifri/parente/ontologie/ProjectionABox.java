package bj.ifri.parente.ontologie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import bj.ifri.parente.config.DateReference;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;
import bj.ifri.parente.domaine.Sexe;
import bj.ifri.parente.ontologie.AboxAssertee.Donnee;
import bj.ifri.parente.ontologie.AboxAssertee.Individu;
import bj.ifri.parente.ontologie.AboxAssertee.Lien;
import bj.ifri.parente.ontologie.AboxAssertee.TypeLitteral;
import bj.ifri.parente.service.GrapheFamille;

/**
 * Projection des données de la base en ABox (SPEC_API_JAVA.md, section C) :
 * <ul>
 *   <li>chaque personne : {@code f:personne_{id}} typée Personne et Masculin/Feminin, avec nom,
 *       prénom, dateNaissance et l'âge calculé par l'application à la date de référence ;</li>
 *   <li>parentDe, beauParentDe ; frereDe / soeurDe ajoutés par l'application entre personnes
 *       partageant un parent (OWL ne sait pas les déduire) ;</li>
 *   <li>chaque mariage : {@code f:mariage_{id}} typé Mariage, aPourConjoint, dates, cause, et
 *       marieA entre les conjoints (dans les deux sens), mariage en cours ou dissous.</li>
 * </ul>
 * Seules des assertions sont produites : les classes déduites viennent de HermiT.
 */
@Component
public class ProjectionABox {

    private final DateReference dateReference;

    public ProjectionABox(DateReference dateReference) {
        this.dateReference = dateReference;
    }

    public static String individuPersonne(Long id) {
        return "personne_" + id;
    }

    public static String individuMariage(Long id) {
        return "mariage_" + id;
    }

    public AboxAssertee projeter(GrapheFamille graphe) {
        List<Individu> individus = new ArrayList<>();
        for (Personne p : graphe.personnes()) {
            List<Donnee> donnees = List.of(
                    new Donnee("nom", p.getNom(), TypeLitteral.STRING),
                    new Donnee("prenom", p.getPrenom(), TypeLitteral.STRING),
                    new Donnee("dateNaissance", dateTime(p.getDateNaissance()), TypeLitteral.DATE_TIME),
                    new Donnee("age", Integer.toString(dateReference.age(p.getDateNaissance())), TypeLitteral.INT));
            List<Lien> liens = new ArrayList<>();
            graphe.enfantsDe(p.getId()).forEach(e -> liens.add(new Lien("parentDe", individuPersonne(e))));
            graphe.beauxEnfantsDe(p.getId()).forEach(e -> liens.add(new Lien("beauParentDe", individuPersonne(e))));
            String fratrie = p.getSexe() == Sexe.M ? "frereDe" : "soeurDe";
            graphe.freresEtSoeurs(p.getId()).forEach(f -> liens.add(new Lien(fratrie, individuPersonne(f))));
            graphe.conjointsDe(p.getId()).forEach(c -> liens.add(new Lien("marieA", individuPersonne(c))));
            individus.add(new Individu(individuPersonne(p.getId()), List.of("Personne", p.getSexe().classeOwl()),
                    donnees, List.copyOf(liens)));
        }
        for (Mariage m : graphe.mariages()) {
            List<Donnee> donnees = new ArrayList<>();
            donnees.add(new Donnee("dateDebut", dateTime(m.getDateDebut()), TypeLitteral.DATE_TIME));
            if (m.getDateFin() != null) {
                donnees.add(new Donnee("dateFin", dateTime(m.getDateFin()), TypeLitteral.DATE_TIME));
            }
            if (m.getCauseDissolution() != null) {
                donnees.add(new Donnee("causeDissolution", m.getCauseDissolution().name(), TypeLitteral.STRING));
            }
            List<Lien> liens = List.of(
                    new Lien("aPourConjoint", individuPersonne(m.getConjoint1Id())),
                    new Lien("aPourConjoint", individuPersonne(m.getConjoint2Id())));
            individus.add(new Individu(individuMariage(m.getId()), List.of("Mariage"), List.copyOf(donnees), liens));
        }
        return new AboxAssertee(List.copyOf(individus));
    }

    private static String dateTime(LocalDate date) {
        return date + "T00:00:00";
    }
}
