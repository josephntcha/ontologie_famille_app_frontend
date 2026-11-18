package bj.ifri.parente.config;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * « Aujourd'hui » pour l'application : propriété {@code app.date-reference} (yyyy-MM-dd) si elle
 * est définie, sinon la date du jour. Sert au calcul de l'âge, à la règle des 18 ans (art. 123)
 * et au refus des dates de naissance dans le futur.
 */
@Component
public class DateReference {

    private final LocalDate fixe;

    public DateReference(@Value("${app.date-reference:}") String dateReference) {
        this.fixe = StringUtils.hasText(dateReference) ? LocalDate.parse(dateReference.trim()) : null;
    }

    public LocalDate aujourdhui() {
        return fixe != null ? fixe : LocalDate.now();
    }

    /** Âge en années révolues à la date de référence. */
    public int age(LocalDate naissance) {
        LocalDate ref = aujourdhui();
        if (naissance.isAfter(ref)) {
            return 0;
        }
        return Period.between(naissance, ref).getYears();
    }
}
