package bj.ifri.parente.web.validation;

import java.time.LocalDate;

import bj.ifri.parente.config.DateReference;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validateur instancié par Spring (injection de la date de référence). */
public class PasDansLeFuturValidator implements ConstraintValidator<PasDansLeFutur, String> {

    private final DateReference dateReference;

    public PasDansLeFuturValidator(DateReference dateReference) {
        this.dateReference = dateReference;
    }

    @Override
    public boolean isValid(String valeur, ConstraintValidatorContext contexte) {
        LocalDate date = DateIsoValidator.analyser(valeur);
        return date == null || !date.isAfter(dateReference.aujourdhui());
    }
}
