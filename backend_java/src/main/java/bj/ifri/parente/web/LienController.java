package bj.ifri.parente.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bj.ifri.parente.service.LienService;
import bj.ifri.parente.web.dto.BeauParenteDto;
import bj.ifri.parente.web.dto.CreationBeauParente;
import bj.ifri.parente.web.dto.CreationFiliation;
import bj.ifri.parente.web.dto.Donnees;
import bj.ifri.parente.web.dto.FiliationDto;
import jakarta.validation.Valid;

/** Écran « Liens » : parentDe et beauParentDe. */
@RestController
@RequestMapping("/api/liens")
public class LienController {

    private final LienService service;

    public LienController(LienService service) {
        this.service = service;
    }

    @GetMapping("/parente")
    public Donnees<List<FiliationDto>> filiations() {
        return new Donnees<>(service.filiations());
    }

    @PostMapping("/parente")
    @ResponseStatus(HttpStatus.CREATED)
    public Donnees<FiliationDto> creerFiliation(@Valid @RequestBody CreationFiliation corps) {
        return new Donnees<>(service.creerFiliation(corps));
    }

    @DeleteMapping("/parente/{id}")
    public ResponseEntity<Void> supprimerFiliation(@PathVariable Long id) {
        service.supprimerFiliation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/beau-parente")
    public Donnees<List<BeauParenteDto>> beauxLiens() {
        return new Donnees<>(service.beauxLiens());
    }

    @PostMapping("/beau-parente")
    @ResponseStatus(HttpStatus.CREATED)
    public Donnees<BeauParenteDto> creerBeauParente(@Valid @RequestBody CreationBeauParente corps) {
        return new Donnees<>(service.creerBeauParente(corps));
    }

    @DeleteMapping("/beau-parente/{id}")
    public ResponseEntity<Void> supprimerBeauParente(@PathVariable Long id) {
        service.supprimerBeauParente(id);
        return ResponseEntity.noContent().build();
    }
}
