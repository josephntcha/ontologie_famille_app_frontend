package bj.ifri.parente.web.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Chaîne au format yyyy-MM-dd représentant une date qui existe (null accepté). */
@Documented
@Constraint(validatedBy = DateIsoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateIso {

    String message() default "doit être une date valide au format AAAA-MM-JJ.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
