package bj.ifri.parente.web.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Date (chaîne yyyy-MM-dd) postérieure à la date de référence de l'application refusée.
 * Null et chaînes invalides acceptées (laissées à {@link DateIso}).
 */
@Documented
@Constraint(validatedBy = PasDansLeFuturValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasDansLeFutur {

    String message() default "ne doit pas être dans le futur.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
