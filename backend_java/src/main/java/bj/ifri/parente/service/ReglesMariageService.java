package bj.ifri.parente.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import bj.ifri.parente.config.DateReference;
import bj.ifri.parente.domaine.Mariage;
import bj.ifri.parente.domaine.Personne;

/**
 * Empêchements au mariage, droit béninois (loi n° 2002-07) : art. 122 (sang et alliance),
 * art. 123 (âge), art. 125 et 143 (mariage en cours = sans date de fin). Indication informative.
 */
@Service
public class ReglesMariageService {

    public static final String INTERDIT = "interdit";
    public static final String AUTORISATION_REQUISE = "autorisation_requise";
    public static final String VERIFICATION_INCOMPLETE = "verification_incomplete";
    public static final String AUTORISE = "autorise";
    public static final int AGE_MINIMUM = 18;
    public static final String AVERTISSEMENT = "Indication fournie à titre informatif, pas un avis juridique : "
            + "adressez-vous à l'officier d'état civil.";

    private final DateReference dateReference;

    public ReglesMariageService(DateReference dateReference) {
        this.dateReference = dateReference;
    }

    public VerdictMariage evaluer(GrapheFamille graphe, Long x, Long y, LienDeSang sang, LienAlliance alliance,
            boolean complet) {
        List<RegleMariage> regles = new ArrayList<>();
        reglesDuSang(sang, regles);
        reglesDAlliance(alliance, regles);
        regleDAge(graphe.personne(x), graphe.personne(y), regles);
        regleMariageEnCours(graphe, graphe.personne(x), graphe.personne(y), regles);
        if (regles.isEmpty()) {
            regles.add(new RegleMariage("aucun_empechement", "Art. 122", AUTORISE,
                    "Aucun empêchement trouvé dans les données connues (ni parenté, ni alliance prohibée, "
                            + "ni âge, ni mariage en cours)."));
        }
        return new VerdictMariage(verdictGlobal(regles, complet), List.copyOf(regles), AVERTISSEMENT);
    }

    private static void reglesDuSang(LienDeSang sang, List<RegleMariage> regles) {
        if (!sang.existe()) {
            return;
        }
        String degre = NommageLien.degreEnLettres(sang.degre());
        if (NommageLien.LIGNE_DIRECTE.equals(sang.type())) {
            regles.add(new RegleMariage("ligne_directe_sang", "Art. 122", INTERDIT,
                    "Mariage prohibé entre parents en ligne directe par le sang, à tout degré (" + degre + ")."));
        } else if (sang.degre() <= 3) {
            regles.add(new RegleMariage("collaterale_sang", "Art. 122", INTERDIT,
                    "Mariage prohibé entre collatéraux par le sang jusqu'au 3e degré inclus, demi-frères et "
                            + "demi-sœurs compris (" + degre + ")."));
        } else {
            regles.add(new RegleMariage("collaterale_eloignee", "Art. 122", AUTORISE,
                    "Collatéraux au-delà du 3e degré (" + degre + ") : le lien de sang n'est pas un empêchement."));
        }
    }

    private static void reglesDAlliance(LienAlliance alliance, List<RegleMariage> regles) {
        if (!alliance.existe()) {
            return;
        }
        String degre = NommageLien.degreEnLettres(alliance.degre());
        if (NommageLien.LIGNE_DIRECTE.equals(alliance.type())) {
            regles.add(new RegleMariage("ligne_directe_alliance", "Art. 122", INTERDIT,
                    "Mariage prohibé entre alliés en ligne directe, à tout degré (" + degre
                            + "), même après la dissolution du mariage qui a créé l'alliance."));
        } else if (alliance.degre() <= 3) {
            if ("beau_frere_belle_soeur".equals(alliance.code()) && alliance.mariageDissousParDeces()) {
                regles.add(new RegleMariage("beau_frere_belle_soeur_deces", "Art. 122", AUTORISATION_REQUISE,
                        "Beau-frère et belle-sœur dont l'union a été dissoute par décès : mariage possible avec "
                                + "l'autorisation du procureur de la République pour motif grave."));
            } else {
                regles.add(new RegleMariage("collaterale_alliance", "Art. 122", INTERDIT,
                        "Mariage prohibé entre alliés en ligne collatérale jusqu'au 3e degré inclus (" + degre + ")."));
            }
        }
    }

    private void regleDAge(Personne x, Personne y, List<RegleMariage> regles) {
        List<String> mineurs = new ArrayList<>();
        for (Personne p : List.of(x, y)) {
            int age = dateReference.age(p.getDateNaissance());
            if (age < AGE_MINIMUM) {
                mineurs.add(p.nomComplet() + " (" + age + " ans)");
            }
        }
        if (!mineurs.isEmpty()) {
            regles.add(new RegleMariage("age_minimum", "Art. 123", INTERDIT,
                    "Âge minimum de 18 ans non atteint : " + String.join(", ", mineurs)
                            + ". Mariage impossible sauf dispense d'âge accordée par le tribunal."));
        }
    }

    private static void regleMariageEnCours(GrapheFamille graphe, Personne x, Personne y, List<RegleMariage> regles) {
        List<String> maries = new ArrayList<>();
        for (Personne p : List.of(x, y)) {
            if (graphe.mariagesDe(p.getId()).stream().anyMatch(Mariage::estEnCours)) {
                maries.add(p.nomComplet());
            }
        }
        if (!maries.isEmpty()) {
            regles.add(new RegleMariage("mariage_en_cours", "Art. 125 et 143", INTERDIT,
                    "Mariage en cours (sans date de fin enregistrée) pour " + String.join(" et ", maries)
                            + " : un nouveau mariage n'est possible qu'après la dissolution du précédent."));
        }
    }

    private static String verdictGlobal(List<RegleMariage> regles, boolean complet) {
        if (regles.stream().anyMatch(r -> INTERDIT.equals(r.verdict()))) {
            return INTERDIT;
        }
        if (regles.stream().anyMatch(r -> AUTORISATION_REQUISE.equals(r.verdict()))) {
            return AUTORISATION_REQUISE;
        }
        return complet ? AUTORISE : VERIFICATION_INCOMPLETE;
    }
}
