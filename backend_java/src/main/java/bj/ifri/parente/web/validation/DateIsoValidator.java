package bj.ifri.parente.web.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateIsoValidator implements ConstraintValidator<DateIso, String> {

    /** yyyy-MM-dd strict : refuse par exemple le 30 février. */
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Date analysée, ou null si la chaîne n'est pas une date valide. */
    public static LocalDate analyser(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return LocalDate.parse(valeur.trim(), FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public boolean isValid(String valeur, ConstraintValidatorContext contexte) {
        return valeur == null || analyser(valeur) != null;
    }
}
