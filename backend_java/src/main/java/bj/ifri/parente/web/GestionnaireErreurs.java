package bj.ifri.parente.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import bj.ifri.parente.erreur.ErreurValidation;
import bj.ifri.parente.erreur.RessourceIntrouvable;
import bj.ifri.parente.ontologie.OntologieIncoherente;
import tools.jackson.core.JacksonException;

/**
 * Erreurs au format de l'API : 422 {@code {message, errors: {champ: [...]}}} (champs en
 * snake_case), 404 {@code {message}}, 409 si les données rendent l'ontologie incohérente.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        Map<String, List<String>> erreurs = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            erreurs.computeIfAbsent(snakeCase(fe.getField()), k -> new ArrayList<>()).add(fe.getDefaultMessage());
        }
        e.getBindingResult().getGlobalErrors()
                .forEach(ge -> erreurs.computeIfAbsent("corps", k -> new ArrayList<>()).add(ge.getDefaultMessage()));
        return erreur422(erreurs);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> illisible(HttpMessageNotReadableException e) {
        String champ = "corps";
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof JacksonException je && !je.getPath().isEmpty()) {
                List<String> segments = new ArrayList<>();
                je.getPath().forEach(r -> segments.add(r.getPropertyName() != null ? r.getPropertyName()
                        : String.valueOf(r.getIndex())));
                champ = String.join(".", segments);
                break;
            }
        }
        String message = "corps".equals(champ) ? "Le corps de la requête n'est pas un JSON valide."
                : "Le champ " + champ + " a un type ou un format invalide.";
        return erreur422(Map.of(champ, List.of(message)));
    }

    @ExceptionHandler(ErreurValidation.class)
    public ResponseEntity<Map<String, Object>> metier(ErreurValidation e) {
        return erreur422(e.erreurs());
    }

    @ExceptionHandler(RessourceIntrouvable.class)
    public ResponseEntity<Map<String, Object>> introuvable(RessourceIntrouvable e) {
        return message(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> routeInconnue(Exception e) {
        return message(HttpStatus.NOT_FOUND, "Ressource introuvable.");
    }

    @ExceptionHandler(OntologieIncoherente.class)
    public ResponseEntity<Map<String, Object>> incoherente(OntologieIncoherente e) {
        return message(HttpStatus.CONFLICT, e.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> message(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("message", message));
    }

    private static ResponseEntity<Map<String, Object>> erreur422(Map<String, List<String>> erreurs) {
        long total = erreurs.values().stream().mapToLong(List::size).sum();
        String premier = erreurs.values().stream().flatMap(List::stream).findFirst().orElse("Données invalides.");
        String message = total > 1 ? premier + " (et " + (total - 1) + " autre(s) erreur(s))" : premier;
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("message", message);
        corps.put("errors", erreurs);
        return ResponseEntity.unprocessableContent().body(corps);
    }

    /** dateNaissance → date_naissance, conjoint1Id → conjoint1_id. */
    static String snakeCase(String nom) {
        StringBuilder sb = new StringBuilder();
        for (char c : nom.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
