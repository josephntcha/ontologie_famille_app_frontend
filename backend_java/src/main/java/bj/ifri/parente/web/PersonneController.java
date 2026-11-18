package bj.ifri.parente.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import bj.ifri.parente.service.PersonneService;
import bj.ifri.parente.web.dto.CreationPersonne;
import bj.ifri.parente.web.dto.CreationPersonneReponse;
import bj.ifri.parente.web.dto.Donnees;
import bj.ifri.parente.web.dto.GroupeDoublonsDto;
import bj.ifri.parente.web.dto.ModificationPersonne;
import bj.ifri.parente.web.dto.PersonneDetailDto;
import bj.ifri.parente.web.dto.PersonneDto;
import jakarta.validation.Valid;

/** Écran « Personnes ». */
@RestController
@RequestMapping("/api/personnes")
public class PersonneController {

    private final PersonneService service;

    public PersonneController(PersonneService service) {
        this.service = service;
    }

    @GetMapping
    public Donnees<List<PersonneDto>> lister() {
        return new Donnees<>(service.lister());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreationPersonneReponse creer(@Valid @RequestBody CreationPersonne corps) {
        return service.creer(corps);
    }

    @GetMapping("/doublons")
    public Donnees<List<GroupeDoublonsDto>> doublons() {
        return new Donnees<>(service.groupesDeDoublons());
    }

    @GetMapping("/{id}")
    public Donnees<PersonneDetailDto> detail(@PathVariable Long id) {
        return new Donnees<>(service.detail(id));
    }

    @PutMapping("/{id}")
    public Donnees<PersonneDto> remplacer(@PathVariable Long id, @Valid @RequestBody ModificationPersonne corps) {
        return new Donnees<>(service.modifier(id, corps));
    }

    @PatchMapping("/{id}")
    public Donnees<PersonneDto> modifier(@PathVariable Long id, @Valid @RequestBody ModificationPersonne corps) {
        return new Donnees<>(service.modifier(id, corps));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
